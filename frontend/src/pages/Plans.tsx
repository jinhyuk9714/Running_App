import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  getPlans,
  getRecommendedPlans,
  getMyPlans,
  startPlan,
  getPlanSchedule,
} from "../api/client";
import type {
  PlanResponse,
  UserPlanResponse,
  PlanWeekResponse,
  GoalType,
  PlanDifficulty,
} from "../types";

const GOAL_LABEL: Record<GoalType, string> = {
  FIVE_K: "5km",
  TEN_K: "10km",
  HALF_MARATHON: "하프마라톤",
};
const DIFF_LABEL: Record<PlanDifficulty, string> = {
  BEGINNER: "초급",
  INTERMEDIATE: "중급",
  ADVANCED: "고급",
};

function formatDate(s: string) {
  try {
    return new Date(s).toLocaleDateString("ko-KR", {
      month: "short",
      day: "numeric",
    });
  } catch {
    return s;
  }
}

export default function Plans() {
  const navigate = useNavigate();
  const { id: planIdParam } = useParams<{ id: string }>();
  const planId = planIdParam ? Number(planIdParam) : null;

  const [plans, setPlans] = useState<PlanResponse[]>([]);
  const [recommended, setRecommended] = useState<PlanResponse[]>([]);
  const [myPlans, setMyPlans] = useState<UserPlanResponse[]>([]);
  const [schedule, setSchedule] = useState<PlanWeekResponse[]>([]);
  const [goalFilter, setGoalFilter] = useState<GoalType | "">("");
  const [diffFilter, setDiffFilter] = useState<PlanDifficulty | "">("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [starting, setStarting] = useState<number | null>(null);

  const loadPlans = async () => {
    setError("");
    try {
      const [p, r, m] = await Promise.all([
        getPlans(goalFilter || undefined, diffFilter || undefined),
        getRecommendedPlans(goalFilter || undefined),
        getMyPlans(),
      ]);
      setPlans(p);
      setRecommended(r);
      setMyPlans(m);
    } catch {
      setError("플랜 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPlans();
  }, [goalFilter, diffFilter]);

  useEffect(() => {
    if (!planId) {
      setSchedule([]);
      return;
    }
    getPlanSchedule(planId)
      .then(setSchedule)
      .catch(() => setSchedule([]));
  }, [planId]);

  const handleStart = async (id: number) => {
    setStarting(id);
    setError("");
    try {
      await startPlan(id);
      await loadPlans();
    } catch (err) {
      setError(err instanceof Error ? err.message : "시작 실패");
    } finally {
      setStarting(null);
    }
  };

  const selectedPlan = planId
    ? plans.find((p) => p.id === planId) ??
      recommended.find((p) => p.id === planId) ??
      myPlans.find((up) => up.plan.id === planId)?.plan
    : null;

  if (loading && !schedule.length) {
    return (
      <div className="flex justify-center py-12">
        <span className="text-slate-500">불러오는 중…</span>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <h1 className="text-xl font-bold text-slate-800">트레이닝 플랜</h1>
      {error && (
        <div className="rounded-lg bg-red-50 p-4 text-sm text-red-700">
          {error}
          <button onClick={() => setError("")} className="ml-2 underline">
            닫기
          </button>
        </div>
      )}

      {planId && selectedPlan && (
        <section className="rounded-2xl border-2 border-emerald-200 bg-white p-6 shadow-sm">
          <h2 className="font-semibold text-slate-800">{selectedPlan.name}</h2>
          <p className="mt-1 text-sm text-slate-600">
            {selectedPlan.description}
          </p>
          <p className="mt-1 text-xs text-slate-500">
            {GOAL_LABEL[selectedPlan.goalType]} ·{" "}
            {DIFF_LABEL[selectedPlan.difficulty]} · {selectedPlan.totalWeeks}주
          </p>
          {schedule.length > 0 && (
            <div className="mt-4">
              <h3 className="text-sm font-semibold text-slate-600">
                주차별 스케줄
              </h3>
              <ul className="mt-2 space-y-2">
                {schedule.map((w) => (
                  <li
                    key={w.weekNumber}
                    className="flex items-center gap-4 rounded-lg bg-slate-50 px-3 py-2 text-sm"
                  >
                    <span className="font-medium text-slate-700">
                      주차 {w.weekNumber}
                    </span>
                    <span className="text-slate-600">
                      {w.targetDistance != null && `${w.targetDistance} km`}
                      {w.targetRuns != null && ` · ${w.targetRuns}회`}
                    </span>
                    {w.description && (
                      <span className="text-slate-500">{w.description}</span>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          )}
          <button
            onClick={() => navigate("/plans")}
            className="mt-4 rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-50"
          >
            목록으로
          </button>
        </section>
      )}

      <section>
        <h2 className="mb-3 text-sm font-semibold text-slate-500">필터</h2>
        <div className="flex flex-wrap gap-2">
          <select
            value={goalFilter}
            onChange={(e) => setGoalFilter(e.target.value as GoalType | "")}
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          >
            <option value="">목표 전체</option>
            {(Object.keys(GOAL_LABEL) as GoalType[]).map((g) => (
              <option key={g} value={g}>
                {GOAL_LABEL[g]}
              </option>
            ))}
          </select>
          <select
            value={diffFilter}
            onChange={(e) =>
              setDiffFilter(e.target.value as PlanDifficulty | "")
            }
            className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          >
            <option value="">난이도 전체</option>
            {(Object.keys(DIFF_LABEL) as PlanDifficulty[]).map((d) => (
              <option key={d} value={d}>
                {DIFF_LABEL[d]}
              </option>
            ))}
          </select>
        </div>
      </section>

      <section>
        <h2 className="mb-3 text-sm font-semibold text-slate-500">추천 플랜</h2>
        {recommended.length === 0 ? (
          <p className="rounded-xl bg-white p-4 text-slate-500 shadow-sm">
            추천 플랜이 없습니다.
          </p>
        ) : (
          <ul className="space-y-3">
            {recommended.map((p) => {
              const joined = myPlans.find((up) => up.plan.id === p.id);
              return (
                <li
                  key={p.id}
                  className={`rounded-xl p-4 shadow-sm ${
                    joined
                      ? "border-l-4 border-emerald-500 bg-emerald-50"
                      : "bg-white"
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <p className="font-semibold text-slate-800">{p.name}</p>
                    {joined && (
                      <span
                        className={`rounded-full px-2 py-0.5 text-xs font-medium text-white ${
                          joined.inProgress ? "bg-emerald-600" : "bg-slate-500"
                        }`}
                      >
                        {joined.inProgress ? "진행 중" : "완료"}
                      </span>
                    )}
                  </div>
                  <p className="mt-1 text-sm text-slate-600">{p.description}</p>
                  <p className="mt-1 text-xs text-slate-500">
                    {GOAL_LABEL[p.goalType]} · {DIFF_LABEL[p.difficulty]} ·{" "}
                    {p.totalWeeks}주 · {p.totalRuns}회
                  </p>
                  {joined ? (
                    <div className="mt-3 flex items-center gap-3">
                      <span className="text-sm text-slate-600">
                        {joined.currentWeek ?? 0}주차 진행 중
                      </span>
                      <button
                        onClick={() => navigate(`/plans/${p.id}`)}
                        className="text-sm text-emerald-600 hover:underline"
                      >
                        스케줄 보기
                      </button>
                    </div>
                  ) : (
                    <div className="mt-3 flex gap-2">
                      <button
                        onClick={() => navigate(`/plans/${p.id}`)}
                        className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
                      >
                        스케줄 보기
                      </button>
                      <button
                        onClick={() => handleStart(p.id)}
                        disabled={starting === p.id}
                        className="rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
                      >
                        {starting === p.id ? "시작 중…" : "시작하기"}
                      </button>
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-sm font-semibold text-slate-500">플랜 목록</h2>
        {plans.length === 0 ? (
          <p className="rounded-xl bg-white p-4 text-slate-500 shadow-sm">
            플랜이 없습니다.
          </p>
        ) : (
          <ul className="space-y-3">
            {plans.map((p) => {
              const joined = myPlans.find((up) => up.plan.id === p.id);
              return (
                <li
                  key={p.id}
                  className={`rounded-xl p-4 shadow-sm ${
                    joined
                      ? "border-l-4 border-emerald-500 bg-emerald-50"
                      : "bg-white"
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <p className="font-semibold text-slate-800">{p.name}</p>
                    {joined && (
                      <span
                        className={`rounded-full px-2 py-0.5 text-xs font-medium text-white ${
                          joined.inProgress ? "bg-emerald-600" : "bg-slate-500"
                        }`}
                      >
                        {joined.inProgress ? "진행 중" : "완료"}
                      </span>
                    )}
                  </div>
                  <p className="mt-1 text-sm text-slate-600">{p.description}</p>
                  <p className="mt-1 text-xs text-slate-500">
                    {GOAL_LABEL[p.goalType]} · {DIFF_LABEL[p.difficulty]} ·{" "}
                    {p.totalWeeks}주
                  </p>
                  {joined ? (
                    <div className="mt-3 flex items-center gap-3">
                      <span className="text-sm text-slate-600">
                        {joined.currentWeek ?? 0}주차 진행 중
                      </span>
                      <button
                        onClick={() => navigate(`/plans/${p.id}`)}
                        className="text-sm text-emerald-600 hover:underline"
                      >
                        스케줄 보기
                      </button>
                    </div>
                  ) : (
                    <div className="mt-3 flex gap-2">
                      <button
                        onClick={() => navigate(`/plans/${p.id}`)}
                        className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-700 hover:bg-slate-50"
                      >
                        스케줄 보기
                      </button>
                      <button
                        onClick={() => handleStart(p.id)}
                        disabled={starting === p.id}
                        className="rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
                      >
                        {starting === p.id ? "시작 중…" : "시작하기"}
                      </button>
                    </div>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-sm font-semibold text-slate-500">내 플랜</h2>
        {myPlans.length === 0 ? (
          <p className="rounded-xl bg-white p-4 text-slate-500 shadow-sm">
            진행중인 플랜이 없습니다.
          </p>
        ) : (
          <ul className="space-y-3">
            {myPlans.map((up) => (
              <li key={up.id} className="rounded-xl bg-white p-4 shadow-sm">
                <p className="font-semibold text-slate-800">{up.plan.name}</p>
                <p className="mt-1 text-sm text-slate-600">
                  시작일: {formatDate(up.startedAt)} · 현재 주차:{" "}
                  {up.currentWeek ?? 0} · {up.inProgress ? "진행중" : "완료"}
                </p>
                <button
                  onClick={() => navigate(`/plans/${up.plan.id}`)}
                  className="mt-2 text-sm text-emerald-600 hover:underline"
                >
                  스케줄 보기
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
