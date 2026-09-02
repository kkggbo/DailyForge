package com.dailyforge.modules.diet.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthSecurityUtils;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.FoodEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.UserFoodFavoriteEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.DietFoodLogMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.FoodMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.UserFoodFavoriteMapper;
import com.dailyforge.modules.diet.interfaces.dto.UploadFoodRequest;
import com.dailyforge.modules.diet.interfaces.vo.FoodItemVO;
import com.dailyforge.modules.diet.interfaces.vo.FoodSearchVO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DietFoodService {

    /** Candidate cap for recent/frequent ordering before in-memory pagination (supports multi-page scroll). */
    private static final int FILTER_LIMIT = 500;

    private final FoodMapper foodMapper;
    private final UserFoodFavoriteMapper favoriteMapper;
    private final DietFoodLogMapper logMapper;
    private final UserMapper userMapper;

    public DietFoodService(
            FoodMapper foodMapper,
            UserFoodFavoriteMapper favoriteMapper,
            DietFoodLogMapper logMapper,
            UserMapper userMapper) {
        this.foodMapper = foodMapper;
        this.favoriteMapper = favoriteMapper;
        this.logMapper = logMapper;
        this.userMapper = userMapper;
    }

    public FoodSearchVO searchFoods(String keyword, String filter, int page, int pageSize) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        String kw = normalize(keyword);
        List<FoodEntity> foods = foodMapper.searchActive(kw);

        List<FoodEntity> ordered;
        if ("favorite".equals(filter)) {
            Set<Long> fav = new LinkedHashSet<>(favoriteMapper.selectFoodIdsByUserId(userId));
            ordered = new ArrayList<>();
            for (FoodEntity f : foods) {
                if (fav.contains(f.getId())) {
                    ordered.add(f);
                }
            }
        } else if ("recent".equals(filter)) {
            List<Long> orderedIds = logMapper.selectRecentFoodIds(userId, FILTER_LIMIT);
            ordered = preserveOrder(foods, orderedIds);
        } else if ("frequent".equals(filter)) {
            List<Long> ids = foods.stream().map(FoodEntity::getId).toList();
            List<Long> orderedIds = ids.isEmpty()
                    ? List.of()
                    : logMapper.selectMostFrequentFoodIds(userId, ids, FILTER_LIMIT);
            ordered = preserveOrder(foods, orderedIds);
        } else {
            ordered = foods;
        }

        // In-memory pagination over the ordered candidate list.
        int fromIndex = (page - 1) * pageSize;
        boolean hasMore = fromIndex + pageSize < ordered.size();
        List<FoodEntity> pageFoods = fromIndex >= ordered.size()
                ? List.of()
                : ordered.subList(fromIndex, Math.min(fromIndex + pageSize, ordered.size()));

        Set<Long> favoritedIds = new LinkedHashSet<>(favoriteMapper.selectFoodIdsByUserId(userId));
        Map<Long, String> nicknames = resolveOwnerNicknames(pageFoods);
        List<FoodItemVO> items = pageFoods.stream()
                .map(f -> toFoodItem(f, favoritedIds.contains(f.getId()), ownerNickname(nicknames, f.getOwnerUserId())))
                .toList();
        return new FoodSearchVO(items, hasMore);
    }

    public FoodItemVO getFoodDetail(Long foodId) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        FoodEntity food = foodMapper.selectActiveById(foodId);
        if (food == null) {
            throw new BusinessException(ErrorCode.FOOD_NOT_FOUND);
        }
        boolean favorited = favoriteMapper.selectByUserAndFood(userId, foodId) != null;
        Map<Long, String> nicknames = resolveOwnerNicknames(List.of(food));
        return toFoodItem(food, favorited, ownerNickname(nicknames, food.getOwnerUserId()));
    }

    @Transactional
    public FoodItemVO uploadFood(UploadFoodRequest request) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        validateUpload(request);
        FoodEntity food = new FoodEntity();
        food.setName(request.name().trim());
        food.setCategory(request.category());
        food.setCaloriesKcal(request.caloriesKcal());
        food.setProteinG(request.proteinG());
        food.setCarbsG(request.carbsG());
        food.setFatG(request.fatG());
        food.setSource("user");
        food.setOwnerUserId(userId);
        food.setIsActive(true);
        foodMapper.insert(food);
        return toFoodItem(food, false, null);
    }

    @Transactional
    public void addFavorite(Long foodId) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        requireFood(foodId);
        if (favoriteMapper.selectByUserAndFood(userId, foodId) != null) {
            return; // already favorited (idempotent)
        }
        UserFoodFavoriteEntity fav = new UserFoodFavoriteEntity();
        fav.setUserId(userId);
        fav.setFoodId(foodId);
        try {
            favoriteMapper.insert(fav);
        } catch (DuplicateKeyException exception) {
            // concurrent duplicate: treat as idempotent success
        }
    }

    @Transactional
    public void removeFavorite(Long foodId) {
        Long userId = AuthSecurityUtils.getCurrentUserId();
        favoriteMapper.delete(new LambdaQueryWrapper<UserFoodFavoriteEntity>()
                .eq(UserFoodFavoriteEntity::getUserId, userId)
                .eq(UserFoodFavoriteEntity::getFoodId, foodId));
    }

    private void requireFood(Long foodId) {
        if (foodMapper.selectActiveById(foodId) == null) {
            throw new BusinessException(ErrorCode.FOOD_NOT_FOUND);
        }
    }

    private void validateUpload(UploadFoodRequest request) {
        BigDecimal sum = request.caloriesKcal()
                .add(request.proteinG())
                .add(request.carbsG())
                .add(request.fatG());
        if (sum.signum() <= 0) {
            throw new BusinessException(ErrorCode.FOOD_UPLOAD_INVALID);
        }
        // Guard against exceeding DECIMAL(8,2) capacity.
        if (overMax(request.caloriesKcal()) || overMax(request.proteinG())
                || overMax(request.carbsG()) || overMax(request.fatG())) {
            throw new BusinessException(ErrorCode.FOOD_UPLOAD_INVALID);
        }
    }

    private boolean overMax(BigDecimal value) {
        return value != null && value.compareTo(new BigDecimal("999999.99")) > 0;
    }

    private List<FoodEntity> preserveOrder(List<FoodEntity> foods, List<Long> orderedIds) {
        Map<Long, FoodEntity> byId = foods.stream()
                .collect(Collectors.toMap(FoodEntity::getId, Function.identity(), (a, b) -> a));
        List<FoodEntity> out = new ArrayList<>();
        for (Long id : orderedIds) {
            FoodEntity f = byId.get(id);
            if (f != null) {
                out.add(f);
            }
        }
        return out;
    }

    private Map<Long, String> resolveOwnerNicknames(List<FoodEntity> foods) {
        Set<Long> ownerIds = foods.stream()
                .filter(f -> "user".equals(f.getSource()) && f.getOwnerUserId() != null)
                .map(FoodEntity::getOwnerUserId)
                .collect(Collectors.toSet());
        if (ownerIds.isEmpty()) {
            return Map.of();
        }
        List<UserEntity> users = userMapper.selectByIds(List.copyOf(ownerIds));
        Map<Long, String> map = new java.util.HashMap<>();
        for (UserEntity u : users) {
            map.put(u.getId(), maskNickname(u.getUserName()));
        }
        return map;
    }

    private String ownerNickname(Map<Long, String> nicknames, Long ownerUserId) {
        return ownerUserId == null ? null : nicknames.get(ownerUserId);
    }

    private FoodItemVO toFoodItem(FoodEntity f, boolean favorited, String ownerNickname) {
        boolean system = "system".equals(f.getSource());
        return new FoodItemVO(
                f.getId(),
                f.getName(),
                f.getCategory(),
                f.getSource(),
                system ? "官方" : "用户",
                system ? null : ownerNickname,
                f.getCaloriesKcal(),
                f.getProteinG(),
                f.getCarbsG(),
                f.getFatG(),
                favorited);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String maskNickname(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        return name.trim().substring(0, 1) + "**";
    }
}
