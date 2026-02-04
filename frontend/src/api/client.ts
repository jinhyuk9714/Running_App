import type {
  AuthResponse,
  UserInfo,
  ActivitiesPage,
  ActivityItem,
  ActivitySummaryResponse,
  ActivityStatsResponse,
  ActivityRequest,
  ChallengeResponse,
  UserChallengeResponse,
  PlanResponse,
  PlanWeekResponse,
  UserPlanResponse,
  GoalType,
  PlanDifficulty,
  ProfileUpdateRequest,
} from "../types";

const BASE = import.meta.env.VITE_API_BASE_URL || "";

function getToken(): string | null {
  return localStorage.getItem("RunningAppJWT");
}

function authHeaders(): HeadersInit {
  const t = getToken();
  return {
    "Content-Type": "application/json",
    ...(t ? { Authorization: `Bearer ${t}` } : {}),
  };
}

async function handleRes<T>(res: Response): Promise<T> {
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const msg =
      (data as { message?: string }).message || `요청 실패 (${res.status})`;
    throw new Error(msg);
  }
  return data as T;
}

// ─── Auth ─────────────────────────────────────────────────────────────
export async function login(
  email: string,
  password: string
): Promise<AuthResponse> {
  const data = await handleRes<AuthResponse>(
    await fetch(`${BASE}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
    })
  );
  if (data.accessToken) localStorage.setItem("RunningAppJWT", data.accessToken);
  return data;
}

export async function signup(
  email: string,
  password: string,
  nickname: string
): Promise<AuthResponse> {
  const data = await handleRes<AuthResponse>(
    await fetch(`${BASE}/api/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password, nickname }),
    })
  );
  if (data.accessToken) localStorage.setItem("RunningAppJWT", data.accessToken);
  return data;
}

export function logout(): void {
  localStorage.removeItem("RunningAppJWT");
}

export function isLoggedIn(): boolean {
  const t = getToken();
  return !!t && t.length > 0;
}

export async function getMe(): Promise<UserInfo> {
  return handleRes<UserInfo>(
    await fetch(`${BASE}/api/auth/me`, { headers: authHeaders() })
  );
}

export async function updateProfile(
  body: ProfileUpdateRequest
): Promise<UserInfo> {
  return handleRes<UserInfo>(
    await fetch(`${BASE}/api/auth/me`, {
      method: "PATCH",
      headers: authHeaders(),
      body: JSON.stringify(body),
    })
  );
}

// ─── Activities ───────────────────────────────────────────────────────
export async function getActivities(
  page = 0,
  size = 20
): Promise<ActivitiesPage> {
  const res = await fetch(`${BASE}/api/activities?page=${page}&size=${size}`, {
    headers: authHeaders(),
  });
  const data = await res.json();
  if (!res.ok)
    throw new Error((data as { message?: string }).message || "목록 조회 실패");
  return {
    content: data.content ?? [],
    totalElements: data.totalElements ?? 0,
    totalPages: data.totalPages,
    number: data.number,
  };
}

export async function getActivity(id: number): Promise<ActivityItem> {
  return handleRes<ActivityItem>(
    await fetch(`${BASE}/api/activities/${id}`, { headers: authHeaders() })
  );
}

export async function createActivity(
  body: ActivityRequest
): Promise<ActivityItem> {
  return handleRes<ActivityItem>(
    await fetch(`${BASE}/api/activities`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(body),
    })
  );
}

export async function updateActivity(
  id: number,
  body: ActivityRequest
): Promise<ActivityItem> {
  return handleRes<ActivityItem>(
    await fetch(`${BASE}/api/activities/${id}`, {
      method: "PUT",
      headers: authHeaders(),
      body: JSON.stringify(body),
    })
  );
}

export async function deleteActivity(id: number): Promise<void> {
  const res = await fetch(`${BASE}/api/activities/${id}`, {
    method: "DELETE",
    headers: authHeaders(),
  });
  if (!res.ok && res.status !== 204) {
    const data = await res.json().catch(() => ({}));
    throw new Error((data as { message?: string }).message || "삭제 실패");
  }
}

export async function getActivitySummary(): Promise<ActivitySummaryResponse> {
  return handleRes<ActivitySummaryResponse>(
    await fetch(`${BASE}/api/activities/summary`, { headers: authHeaders() })
  );
}

export async function getActivityStats(
  year?: number,
  month?: number
): Promise<ActivityStatsResponse> {
  const params = new URLSearchParams();
  if (year != null) params.set("year", String(year));
  if (month != null) params.set("month", String(month));
  const q = params.toString();
  return handleRes<ActivityStatsResponse>(
    await fetch(`${BASE}/api/activities/stats${q ? `?${q}` : ""}`, {
      headers: authHeaders(),
    })
  );
}

// ─── Challenges ───────────────────────────────────────────────────────
export async function getActiveChallenges(): Promise<ChallengeResponse[]> {
  return handleRes<ChallengeResponse[]>(
    await fetch(`${BASE}/api/challenges`, { headers: authHeaders() })
  );
}

export async function getRecommendedChallenges(): Promise<ChallengeResponse[]> {
  return handleRes<ChallengeResponse[]>(
    await fetch(`${BASE}/api/challenges/recommended`, {
      headers: authHeaders(),
    })
  );
}

export async function joinChallenge(
  id: number
): Promise<UserChallengeResponse> {
  return handleRes<UserChallengeResponse>(
    await fetch(`${BASE}/api/challenges/${id}/join`, {
      method: "POST",
      headers: authHeaders(),
    })
  );
}

export async function getMyChallenges(): Promise<UserChallengeResponse[]> {
  return handleRes<UserChallengeResponse[]>(
    await fetch(`${BASE}/api/challenges/my`, { headers: authHeaders() })
  );
}

export async function getChallengeProgress(
  id: number
): Promise<UserChallengeResponse> {
  return handleRes<UserChallengeResponse>(
    await fetch(`${BASE}/api/challenges/${id}/progress`, {
      headers: authHeaders(),
    })
  );
}

// ─── Plans ────────────────────────────────────────────────────────────
export async function getPlans(
  goalType?: GoalType,
  difficulty?: PlanDifficulty
): Promise<PlanResponse[]> {
  const params = new URLSearchParams();
  if (goalType) params.set("goalType", goalType);
  if (difficulty) params.set("difficulty", difficulty);
  const q = params.toString();
  return handleRes<PlanResponse[]>(
    await fetch(`${BASE}/api/plans${q ? `?${q}` : ""}`, {
      headers: authHeaders(),
    })
  );
}

export async function getRecommendedPlans(
  goalType?: GoalType
): Promise<PlanResponse[]> {
  const q = goalType ? `?goalType=${goalType}` : "";
  return handleRes<PlanResponse[]>(
    await fetch(`${BASE}/api/plans/recommended${q}`, { headers: authHeaders() })
  );
}

export async function startPlan(id: number): Promise<UserPlanResponse> {
  return handleRes<UserPlanResponse>(
    await fetch(`${BASE}/api/plans/${id}/start`, {
      method: "POST",
      headers: authHeaders(),
    })
  );
}

export async function getMyPlans(): Promise<UserPlanResponse[]> {
  return handleRes<UserPlanResponse[]>(
    await fetch(`${BASE}/api/plans/my`, { headers: authHeaders() })
  );
}

export async function getPlanSchedule(
  planId: number
): Promise<PlanWeekResponse[]> {
  return handleRes<PlanWeekResponse[]>(
    await fetch(`${BASE}/api/plans/${planId}/schedule`, {
      headers: authHeaders(),
    })
  );
}
