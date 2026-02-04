import { useEffect, useState } from "react";
import {
  getActiveChallenges,
  getRecommendedChallenges,
  getMyChallenges,
  joinChallenge,
  getChallengeProgress,
} from "../api/client";
import type { ChallengeResponse, UserChallengeResponse } from "../types";

function formatDate(s: string) {
  try {
    return new Date(s).toLocaleDateString("ko-KR", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  } catch {
    return s;
  }
}

export default function Challenges() {
  const [active, setActive] = useState<ChallengeResponse[]>([]);
  const [recommended, setRecommended] = useState<ChallengeResponse[]>([]);
  const [my, setMy] = useState<UserChallengeResponse[]>([]);
  const [progressDetail, setProgressDetail] =
    useState<UserChallengeResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [joining, setJoining] = useState<number | null>(null);

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const [a, r, m] = await Promise.all([
        getActiveChallenges(),
        getRecommendedChallenges(),
        getMyChallenges(),
      ]);
      setActive(a);
      setRecommended(r);
      setMy(m);
    } catch {
      setError("챌린지 목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleJoin = async (id: number) => {
    setJoining(id);
    setError("");
    try {
      await joinChallenge(id);
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "참여 실패");
    } finally {
      setJoining(null);
    }
  };

  const handleProgress = async (id: number) => {
    setError("");
    try {
      const p = await getChallengeProgress(id);
      setProgressDetail(p);
    } catch {
      setProgressDetail(null);
      setError("진행률을 불러오지 못했습니다.");
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <span className="text-slate-500">불러오는 중…</span>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <h1 className="text-xl font-bold text-slate-800">챌린지</h1>
      {error && (
        <div className="rounded-lg bg-red-50 p-4 text-sm text-red-700">
          {error}
          <button onClick={() => setError("")} className="ml-2 underline">
            닫기
          </button>
        </div>
      )}

      {progressDetail && (
        <section className="rounded-2xl border-2 border-emerald-200 bg-emerald-50 p-6">
          <h2 className="font-semibold text-slate-800">진행률 상세</h2>
          <p className="mt-1 text-lg font-bold text-emerald-800">
            {progressDetail.challenge.name}
          </p>
          <p className="mt-2 text-sm text-slate-600">
            {progressDetail.challenge.description}
          </p>
          <div className="mt-4 flex items-center gap-4">
            <div className="h-3 flex-1 overflow-hidden rounded-full bg-slate-200">
              <div
                className="h-full bg-emerald-600"
                style={{
                  width: `${Math.min(
                    100,
                    progressDetail.progressPercent ?? 0
                  )}%`,
                }}
              />
            </div>
            <span className="font-semibold text-slate-800">
              {progressDetail.progressPercent ?? 0}%
            </span>
          </div>
          {progressDetail.challenge.type === "DISTANCE" && (
            <p className="mt-2 text-sm text-slate-600">
              {(progressDetail.currentDistance ?? 0).toFixed(2)} /{" "}
              {(progressDetail.targetDistance ?? 0).toFixed(2)} km
            </p>
          )}
          {progressDetail.challenge.type === "COUNT" && (
            <p className="mt-2 text-sm text-slate-600">
              {progressDetail.currentCount ?? 0} /{" "}
              {progressDetail.targetCount ?? 0}회
            </p>
          )}
          {progressDetail.completed && (
            <p className="mt-2 font-medium text-emerald-700">완료</p>
          )}
          <button
            onClick={() => setProgressDetail(null)}
            className="mt-4 rounded-lg border border-slate-300 px-3 py-1.5 text-sm text-slate-600 hover:bg-white"
          >
            닫기
          </button>
        </section>
      )}

      <section>
        <h2 className="mb-3 text-sm font-semibold text-slate-500">
          진행중인 챌린지
        </h2>
        {active.length === 0 ? (
          <p className="rounded-xl bg-white p-4 text-slate-500 shadow-sm">
            진행중인 챌린지가 없습니다.
          </p>
        ) : (
          <ul className="space-y-3">
            {active.map((c) => {
              const joined = my.find((uc) => uc.challenge.id === c.id);
              return (
                <li
                  key={c.id}
                  className={`rounded-xl p-4 shadow-sm ${
                    joined
                      ? "border-l-4 border-emerald-500 bg-emerald-50"
                      : "bg-white"
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <p className="font-semibold text-slate-800">{c.name}</p>
                    {joined && (
                      <span className="rounded-full bg-emerald-600 px-2 py-0.5 text-xs font-medium text-white">
                        참여 중
                      </span>
                    )}
                  </div>
                  {c.description && (
                    <p className="mt-1 text-sm text-slate-600">
                      {c.description}
                    </p>
                  )}
                  <p className="mt-1 text-xs text-slate-500">
                    {formatDate(c.startDate)} ~ {formatDate(c.endDate)} ·{" "}
                    {c.type === "DISTANCE"
                      ? `${c.targetDistance}km`
                      : `${c.targetCount}회`}
                  </p>
                  {joined ? (
                    <div className="mt-3 flex items-center gap-3">
                      <div className="h-2 w-24 overflow-hidden rounded-full bg-slate-200">
                        <div
                          className="h-full bg-emerald-600"
                          style={{
                            width: `${Math.min(100, joined.progressPercent ?? 0)}%`,
                          }}
                        />
                      </div>
                      <span className="text-sm font-medium text-emerald-700">
                        {joined.progressPercent ?? 0}%
                      </span>
                      <button
                        onClick={() => handleProgress(c.id)}
                        className="text-sm text-emerald-600 hover:underline"
                      >
                        상세
                      </button>
                    </div>
                  ) : (
                    <div className="mt-3 flex gap-2">
                      <button
                        onClick={() => handleJoin(c.id)}
                        disabled={joining === c.id}
                        className="rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
                      >
                        {joining === c.id ? "참여 중…" : "참여"}
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
        <h2 className="mb-3 text-sm font-semibold text-slate-500">
          추천 챌린지 (미참여)
        </h2>
        {recommended.length === 0 ? (
          <p className="rounded-xl bg-white p-4 text-slate-500 shadow-sm">
            추천 챌린지가 없습니다.
          </p>
        ) : (
          <ul className="space-y-3">
            {recommended.map((c) => {
              const joined = my.find((uc) => uc.challenge.id === c.id);
              return (
                <li
                  key={c.id}
                  className={`rounded-xl p-4 shadow-sm ${
                    joined
                      ? "border-l-4 border-emerald-500 bg-emerald-50"
                      : "bg-white"
                  }`}
                >
                  <div className="flex items-start justify-between">
                    <p className="font-semibold text-slate-800">{c.name}</p>
                    {joined && (
                      <span className="rounded-full bg-emerald-600 px-2 py-0.5 text-xs font-medium text-white">
                        참여 중
                      </span>
                    )}
                  </div>
                  {c.description && (
                    <p className="mt-1 text-sm text-slate-600">
                      {c.description}
                    </p>
                  )}
                  <p className="mt-1 text-xs text-slate-500">
                    {formatDate(c.startDate)} ~ {formatDate(c.endDate)} ·{" "}
                    {c.type === "DISTANCE"
                      ? `${c.targetDistance}km`
                      : `${c.targetCount}회`}
                  </p>
                  {joined ? (
                    <div className="mt-3 flex items-center gap-3">
                      <div className="h-2 w-24 overflow-hidden rounded-full bg-slate-200">
                        <div
                          className="h-full bg-emerald-600"
                          style={{
                            width: `${Math.min(100, joined.progressPercent ?? 0)}%`,
                          }}
                        />
                      </div>
                      <span className="text-sm font-medium text-emerald-700">
                        {joined.progressPercent ?? 0}%
                      </span>
                    </div>
                  ) : (
                    <button
                      onClick={() => handleJoin(c.id)}
                      disabled={joining === c.id}
                      className="mt-3 rounded-lg bg-emerald-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
                    >
                      {joining === c.id ? "참여 중…" : "참여"}
                    </button>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </section>

      <section>
        <h2 className="mb-3 text-sm font-semibold text-slate-500">내 챌린지</h2>
        {my.length === 0 ? (
          <p className="rounded-xl bg-white p-4 text-slate-500 shadow-sm">
            참여한 챌린지가 없습니다.
          </p>
        ) : (
          <ul className="space-y-3">
            {my.map((uc) => (
              <li key={uc.id} className="rounded-xl bg-white p-4 shadow-sm">
                <p className="font-semibold text-slate-800">
                  {uc.challenge.name}
                </p>
                <div className="mt-2 flex items-center gap-2">
                  <div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-200">
                    <div
                      className="h-full bg-emerald-600"
                      style={{
                        width: `${Math.min(100, uc.progressPercent ?? 0)}%`,
                      }}
                    />
                  </div>
                  <span className="text-sm font-medium text-slate-700">
                    {uc.progressPercent ?? 0}%
                  </span>
                </div>
                <button
                  onClick={() => handleProgress(uc.challenge.id)}
                  className="mt-2 text-sm text-emerald-600 hover:underline"
                >
                  상세 보기
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
