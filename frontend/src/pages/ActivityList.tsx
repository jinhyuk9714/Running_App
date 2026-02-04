import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getActivities } from "../api/client";
import type { ActivityItem } from "../types";

function formatDate(iso: string) {
  try {
    const d = new Date(iso);
    return d.toLocaleDateString("ko-KR", {
      month: "numeric",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return iso;
  }
}

function formatDuration(sec: number) {
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${String(s).padStart(2, "0")}`;
}

function formatPace(secPerKm?: number) {
  if (secPerKm == null) return "—";
  const m = Math.floor(secPerKm / 60);
  const s = secPerKm % 60;
  return `${m}'${String(s).padStart(2, "0")}"`;
}

export default function ActivityList() {
  const [items, setItems] = useState<ActivityItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const page = await getActivities(0, 50);
      setItems(page.content);
    } catch {
      setError("목록을 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-bold text-slate-800">러닝 기록</h1>

      {loading && (
        <div className="flex justify-center py-12">
          <span className="text-slate-500">불러오는 중…</span>
        </div>
      )}
      {error && (
        <div className="rounded-lg bg-red-50 p-4 text-red-700">
          {error}
          <button onClick={load} className="ml-2 underline">
            다시 시도
          </button>
        </div>
      )}
      {!loading && !error && items.length === 0 && (
        <div className="rounded-xl bg-white p-8 text-center text-slate-500 shadow-sm">
          저장된 러닝 기록이 없습니다.
        </div>
      )}
      {!loading && items.length > 0 && (
        <ul className="space-y-3">
          {items.map((item) => (
            <li key={item.id}>
              <Link
                to={`/activities/${item.id}`}
                className="block rounded-xl bg-white p-4 shadow-sm transition hover:shadow-md"
              >
                <p className="text-sm text-slate-500">
                  {formatDate(item.startedAt)}
                </p>
                <div className="mt-1 flex flex-wrap items-baseline gap-3">
                  <span className="font-semibold text-slate-800">
                    {item.distance.toFixed(2)} km
                  </span>
                  <span className="text-slate-600">
                    {formatDuration(item.duration)}
                  </span>
                  {item.averagePace != null && (
                    <span className="text-sm text-slate-500">
                      {formatPace(item.averagePace)}/km
                    </span>
                  )}
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
