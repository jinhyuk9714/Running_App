import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getActivitySummary, getActivityStats } from "../api/client";
import type { ActivitySummaryResponse, ActivityStatsResponse } from "../types";

function formatPace(secPerKm?: number) {
  if (secPerKm == null) return "—";
  const m = Math.floor(secPerKm / 60);
  const s = secPerKm % 60;
  return `${m}'${String(s).padStart(2, "0")}"`;
}

function formatDuration(sec?: number) {
  if (sec == null) return "—";
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}분 ${s}초`;
}

export default function Dashboard() {
  const [summary, setSummary] = useState<ActivitySummaryResponse | null>(null);
  const [stats, setStats] = useState<ActivityStatsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let ok = true;
    setLoading(true);
    setError("");
    Promise.all([getActivitySummary(), getActivityStats()])
      .then(([s, st]) => {
        if (ok) {
          setSummary(s);
          setStats(st);
        }
      })
      .catch(() => ok && setError("요약을 불러오지 못했습니다."))
      .finally(() => ok && setLoading(false));
    return () => {
      ok = false;
    };
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <span className="text-slate-500">불러오는 중…</span>
      </div>
    );
  }
  if (error) {
    return <div className="rounded-lg bg-red-50 p-4 text-red-700">{error}</div>;
  }

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-bold text-slate-800">대시보드</h1>

      {stats && (stats.totalCount ?? 0) > 0 && (
        <section className="rounded-2xl bg-white p-6 shadow-sm">
          <h2 className="mb-4 text-sm font-semibold text-slate-500">
            전체 누적
          </h2>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Card
              label="총 거리"
              value={`${(stats.totalDistance ?? 0).toFixed(2)} km`}
            />
            <Card label="활동 횟수" value={`${stats.totalCount ?? 0}회`} />
            <Card label="총 시간" value={formatDuration(stats.totalDuration)} />
            <Card label="평균 페이스" value={formatPace(stats.averagePace)} />
          </div>
        </section>
      )}

      <section className="rounded-2xl bg-white p-6 shadow-sm">
        <h2 className="mb-4 text-sm font-semibold text-slate-500">
          기간별 요약
        </h2>
        <div className="grid gap-4 sm:grid-cols-3">
          <PeriodCard title="이번 주" period={summary?.thisWeek} />
          <PeriodCard title="이번 달" period={summary?.thisMonth} />
          <PeriodCard title="지난달" period={summary?.lastMonth} />
        </div>
      </section>

      <div className="flex flex-wrap gap-3">
        <Link
          to="/activities"
          className="rounded-xl bg-emerald-600 px-5 py-2.5 font-medium text-white hover:bg-emerald-700"
        >
          러닝 기록 보기
        </Link>
        <Link
          to="/challenges"
          className="rounded-xl border border-slate-300 px-5 py-2.5 font-medium text-slate-700 hover:bg-slate-50"
        >
          챌린지
        </Link>
        <Link
          to="/plans"
          className="rounded-xl border border-slate-300 px-5 py-2.5 font-medium text-slate-700 hover:bg-slate-50"
        >
          플랜
        </Link>
      </div>
    </div>
  );
}

function Card({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-slate-50 p-3">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="font-semibold text-slate-800">{value}</p>
    </div>
  );
}

function PeriodCard({
  title,
  period,
}: {
  title: string;
  period?: {
    totalDistance?: number;
    totalCount?: number;
    totalDuration?: number;
    averagePace?: number;
  };
}) {
  if (!period) {
    return (
      <div className="rounded-xl border border-slate-200 p-4">
        <h3 className="font-medium text-slate-700">{title}</h3>
        <p className="mt-2 text-sm text-slate-500">데이터 없음</p>
      </div>
    );
  }
  const hasAny =
    (period.totalDistance ?? 0) > 0 || (period.totalCount ?? 0) > 0;
  return (
    <div className="rounded-xl border border-slate-200 p-4">
      <h3 className="font-medium text-slate-700">{title}</h3>
      {hasAny ? (
        <ul className="mt-2 space-y-1 text-sm text-slate-600">
          <li>거리: {(period.totalDistance ?? 0).toFixed(2)} km</li>
          <li>횟수: {period.totalCount ?? 0}회</li>
          <li>시간: {formatDuration(period.totalDuration)}</li>
          {period.averagePace != null && (
            <li>평균 페이스: {formatPace(period.averagePace)}</li>
          )}
        </ul>
      ) : (
        <p className="mt-2 text-sm text-slate-500">기록 없음</p>
      )}
    </div>
  );
}
