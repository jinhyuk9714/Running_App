export interface AuthResponse {
  accessToken: string;
  tokenType?: string;
  user?: UserInfo;
}

export interface UserInfo {
  id: number;
  email: string;
  nickname: string;
  level?: number;
  totalDistance?: number;
}

export interface ProfileUpdateRequest {
  nickname?: string;
  weight?: number;
  height?: number;
}

// Activities
export interface ActivityItem {
  id: number;
  distance: number;
  duration: number;
  averagePace?: number;
  calories?: number;
  averageHeartRate?: number;
  cadence?: number;
  route?: RoutePoint[];
  startedAt: string;
  memo?: string;
  createdAt?: string;
}

export interface RoutePoint {
  lat: number;
  lng: number;
  timestamp?: string;
}

export interface ActivitiesPage {
  content: ActivityItem[];
  totalElements: number;
  totalPages?: number;
  number?: number;
}

export interface PeriodSummary {
  totalDistance?: number;
  totalCount?: number;
  totalDuration?: number;
  averagePace?: number;
}

export interface ActivitySummaryResponse {
  thisWeek?: PeriodSummary;
  thisMonth?: PeriodSummary;
  lastMonth?: PeriodSummary;
}

export interface ActivityStatsResponse {
  totalDistance?: number;
  totalCount?: number;
  totalDuration?: number;
  averagePace?: number;
}

export interface ActivityRequest {
  distance: number;
  duration: number;
  startedAt: string;
  averagePace?: number;
  calories?: number;
  averageHeartRate?: number;
  cadence?: number;
  route?: Record<string, unknown>[];
  memo?: string;
}

// Challenges
export type ChallengeType = "DISTANCE" | "COUNT";

export interface ChallengeResponse {
  id: number;
  name: string;
  description?: string;
  targetDistance?: number;
  targetCount?: number;
  startDate: string;
  endDate: string;
  type: ChallengeType;
  createdAt?: string;
}

export interface UserChallengeResponse {
  id: number;
  challenge: ChallengeResponse;
  currentDistance?: number;
  currentCount?: number;
  targetDistance?: number;
  targetCount?: number;
  progressPercent?: number;
  completed?: boolean;
  joinedAt?: string;
  completedAt?: string;
}

// Plans
export type GoalType = "FIVE_K" | "TEN_K" | "HALF_MARATHON";
export type PlanDifficulty = "BEGINNER" | "INTERMEDIATE" | "ADVANCED";

export interface PlanResponse {
  id: number;
  name: string;
  description?: string;
  goalType: GoalType;
  difficulty: PlanDifficulty;
  totalWeeks?: number;
  totalRuns?: number;
  createdAt?: string;
}

export interface PlanWeekResponse {
  weekNumber: number;
  targetDistance?: number;
  targetRuns?: number;
  description?: string;
}

export interface UserPlanResponse {
  id: number;
  plan: PlanResponse;
  startedAt: string;
  currentWeek?: number;
  inProgress?: boolean;
  completedAt?: string;
}
