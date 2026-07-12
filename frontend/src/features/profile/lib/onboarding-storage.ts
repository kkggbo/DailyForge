const PROFILE_ONBOARDING_KEY_PREFIX = "dailyforge.profile.onboarding.completed";

export function hasCompletedProfileOnboarding(userId: number) {
  return window.localStorage.getItem(getStorageKey(userId)) === "true";
}

export function markProfileOnboardingCompleted(userId: number) {
  window.localStorage.setItem(getStorageKey(userId), "true");
}

function getStorageKey(userId: number) {
  return `${PROFILE_ONBOARDING_KEY_PREFIX}.${userId}`;
}
