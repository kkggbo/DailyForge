package com.dailyforge.modules.diet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.dailyforge.common.BusinessException;
import com.dailyforge.common.ErrorCode;
import com.dailyforge.infrastructure.security.AuthUserPrincipal;
import com.dailyforge.modules.auth.infrastructure.persistence.entity.UserEntity;
import com.dailyforge.modules.auth.infrastructure.persistence.mapper.UserMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.entity.FoodEntity;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.DietFoodLogMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.FoodMapper;
import com.dailyforge.modules.diet.infrastructure.persistence.mapper.UserFoodFavoriteMapper;
import com.dailyforge.modules.diet.interfaces.dto.UploadFoodRequest;
import com.dailyforge.modules.diet.interfaces.vo.FoodItemVO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DietFoodServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private FoodMapper foodMapper;
    @Mock
    private UserFoodFavoriteMapper favoriteMapper;
    @Mock
    private DietFoodLogMapper logMapper;
    @Mock
    private UserMapper userMapper;

    private DietFoodService service;

    @BeforeEach
    void setUp() {
        service = new DietFoodService(foodMapper, favoriteMapper, logMapper, userMapper);
        SecurityContextHolder.getContext().setAuthentication(authentication());
    }

    @Test
    void uploadShouldRejectAllZeroNutrition() {
        assertThatThrownBy(() -> service.uploadFood(new UploadFoodRequest(
                "自制", "other", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FOOD_UPLOAD_INVALID);
    }

    @Test
    void searchFavoriteShouldReturnOnlyFavoritedFoods() {
        FoodEntity f1 = food(1L, "米饭", "system", null);
        FoodEntity f2 = food(2L, "鸡胸肉", "system", null);
        when(foodMapper.searchActive(null)).thenReturn(List.of(f1, f2));
        when(favoriteMapper.selectFoodIdsByUserId(USER_ID)).thenReturn(List.of(2L));

        var result = service.searchFoods(null, "favorite", 1, 20);

        assertThat(result.foods()).hasSize(1);
        assertThat(result.foods().getFirst().foodId()).isEqualTo(2L);
        assertThat(result.foods().getFirst().favorited()).isTrue();
        assertThat(result.hasMore()).isFalse();
    }

    @Test
    void searchAllPageOneShouldReturnFirstPageAndHasMore() {
        // more than 20 foods
        List<FoodEntity> many = new java.util.ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            many.add(food(i, "食物" + i, "system", null));
        }
        when(foodMapper.searchActive(null)).thenReturn(many);
        when(favoriteMapper.selectFoodIdsByUserId(USER_ID)).thenReturn(List.of());

        var result = service.searchFoods(null, "all", 1, 20);

        assertThat(result.foods()).hasSize(20);
        assertThat(result.hasMore()).isTrue();
        // first page food ids 1..20
        assertThat(result.foods().getFirst().foodId()).isEqualTo(1L);
        assertThat(result.foods().get(19).foodId()).isEqualTo(20L);
    }

    @Test
    void searchAllLastPageShouldReturnNoMore() {
        List<FoodEntity> many = new java.util.ArrayList<>();
        for (long i = 1; i <= 25; i++) {
            many.add(food(i, "食物" + i, "system", null));
        }
        when(foodMapper.searchActive(null)).thenReturn(many);
        when(favoriteMapper.selectFoodIdsByUserId(USER_ID)).thenReturn(List.of());

        var result = service.searchFoods(null, "all", 2, 20);

        assertThat(result.foods()).hasSize(5);
        assertThat(result.hasMore()).isFalse();
        assertThat(result.foods().getFirst().foodId()).isEqualTo(21L);
    }

    @Test
    void searchFavoriteShouldPaginateCorrectly() {
        List<FoodEntity> all = new java.util.ArrayList<>();
        for (long i = 1; i <= 30; i++) {
            all.add(food(i, "食物" + i, "system", null));
        }
        when(foodMapper.searchActive(null)).thenReturn(all);
        // user favorited food ids 2..30 (29 items)
        java.util.List<Long> favIds = new java.util.ArrayList<>();
        for (long i = 2; i <= 30; i++) {
            favIds.add(i);
        }
        when(favoriteMapper.selectFoodIdsByUserId(USER_ID)).thenReturn(favIds);

        var page1 = service.searchFoods(null, "favorite", 1, 20);
        assertThat(page1.foods()).hasSize(20);
        assertThat(page1.hasMore()).isTrue();
        assertThat(page1.foods().getFirst().foodId()).isEqualTo(2L);

        var page2 = service.searchFoods(null, "favorite", 2, 20);
        assertThat(page2.foods()).hasSize(9);
        assertThat(page2.hasMore()).isFalse();
        assertThat(page2.foods().getFirst().foodId()).isEqualTo(22L);
    }

    @Test
    void detailShouldMaskUserUploaderNickname() {
        FoodEntity userFood = food(9L, "自制鸡胸", "user", 77L);
        when(foodMapper.selectActiveById(9L)).thenReturn(userFood);
        when(favoriteMapper.selectByUserAndFood(USER_ID, 9L)).thenReturn(null);
        UserEntity owner = new UserEntity();
        owner.setId(77L);
        owner.setUserName("张三");
        when(userMapper.selectByIds(anyList())).thenReturn(List.of(owner));

        FoodItemVO item = service.getFoodDetail(9L);

        assertThat(item.source()).isEqualTo("user");
        assertThat(item.sourceLabel()).isEqualTo("用户");
        assertThat(item.ownerNickname()).isEqualTo("张**");
    }

    @Test
    void systemFoodShouldHaveNoOwnerNickname() {
        FoodEntity sys = food(1L, "米饭", "system", null);
        when(foodMapper.selectActiveById(1L)).thenReturn(sys);
        when(favoriteMapper.selectByUserAndFood(USER_ID, 1L)).thenReturn(null);

        FoodItemVO item = service.getFoodDetail(1L);

        assertThat(item.sourceLabel()).isEqualTo("官方");
        assertThat(item.ownerNickname()).isNull();
    }

    @Test
    void uploadShouldRejectOverMaxNutrition() {
        assertThatThrownBy(() -> service.uploadFood(new UploadFoodRequest(
                "超大热量", "other", new BigDecimal("999999999"), new BigDecimal("1"),
                new BigDecimal("1"), new BigDecimal("1"))))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FOOD_UPLOAD_INVALID);
    }

    @Test
    void addFavoriteShouldBeIdempotentWhenAlreadyExists() {
        when(foodMapper.selectActiveById(5L)).thenReturn(food(5L, "鸡胸肉", "system", null));
        when(favoriteMapper.selectByUserAndFood(USER_ID, 5L)).thenReturn(
                new com.dailyforge.modules.diet.infrastructure.persistence.entity.UserFoodFavoriteEntity());

        assertThatCode(() -> service.addFavorite(5L)).doesNotThrowAnyException();
        verify(favoriteMapper, never()).insert(any());
    }

    @Test
    void addFavoriteShouldSwallowDuplicateKeyRace() {
        when(foodMapper.selectActiveById(5L)).thenReturn(food(5L, "鸡胸肉", "system", null));
        when(favoriteMapper.selectByUserAndFood(USER_ID, 5L)).thenReturn(null);
        doThrow(new org.springframework.dao.DuplicateKeyException("dup"))
                .when(favoriteMapper).insert(any());

        assertThatCode(() -> service.addFavorite(5L)).doesNotThrowAnyException();
    }

    private FoodEntity food(Long id, String name, String source, Long owner) {
        FoodEntity f = new FoodEntity();
        f.setId(id);
        f.setName(name);
        f.setSource(source);
        f.setOwnerUserId(owner);
        f.setCaloriesKcal(new BigDecimal("100"));
        f.setProteinG(new BigDecimal("10"));
        f.setCarbsG(new BigDecimal("10"));
        f.setFatG(new BigDecimal("1"));
        return f;
    }

    private Authentication authentication() {
        return new Authentication() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getName() {
                return String.valueOf(USER_ID);
            }

            @Override
            public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
                return java.util.List.of();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return new AuthUserPrincipal(USER_ID, "u@example.com", "user", "basic");
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
            }
        };
    }
}
