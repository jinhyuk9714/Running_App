import { useEffect, useState } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import { MapContainer, TileLayer, Polyline, useMap } from "react-leaflet";
import { getActivity, deleteActivity } from "../api/client";
import type { ActivityItem, RoutePoint } from "../types";

function formatDate(iso: string) {
  try {
    const d = new Date(iso);
    return d.toLocaleDateString("ko-KR", {
      month: "long",
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

function FitBounds({ points }: { points: [number, number][] }) {
  const map = useMap();
  useEffect(() => {
    if (points.length < 2) return;
    const lat = points.map((p) => p[0]);
    const lng = points.map((p) => p[1]);
    const pad = 0.002;
    map.fitBounds(
      [
        [Math.min(...lat) - pad, Math.min(...lng) - pad],
        [Math.max(...lat) + pad, Math.max(...lng) + pad],
      ],
      { padding: [20, 20] }
    );
  }, [map, points]);
  return null;
}

export default function ActivityDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [item, setItem] = useState<ActivityItem | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    setError("");
    getActivity(Number(id))
      .then(setItem)
      .catch(() => setError("상세를 불러오지 못했습니다."))
      .finally(() => setLoading(false));
  }, [id]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <span className="text-slate-500">불러오는 중…</span>
      </div>
    );
  }
  if (error || !item) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-6">
        <p className="text-red-600">{error || "데이터 없음"}</p>
        <Link
          to="/"
          className="mt-2 inline-block text-emerald-600 hover:underline"
        >
          목록으로
        </Link>
      </div>
    );
  }

  const route = item.route ?? [];
  const positions: [number, number][] = route.map((p: RoutePoint) => [
    p.lat,
    p.lng,
  ]);
  const center: [number, number] =
    positions.length >= 2
      ? [
          (Math.min(...positions.map((p) => p[0])) +
            Math.max(...positions.map((p) => p[0]))) /
            2,
          (Math.min(...positions.map((p) => p[1])) +
            Math.max(...positions.map((p) => p[1]))) /
            2,
        ]
      : [37.5, 127.0];

  return (
    <div className="mx-auto max-w-2xl px-4 py-6">
      <Link
        to="/"
        className="mb-4 inline-block text-sm text-slate-500 hover:text-slate-700"
      >
        ← 목록
      </Link>
      <h1 className="text-lg font-semibold text-slate-800">
        {formatDate(item.startedAt)}
      </h1>

      <div className="mt-4 grid grid-cols-2 gap-3">
        <StatCard label="거리" value={`${item.distance.toFixed(2)} km`} />
        <StatCard label="시간" value={formatDuration(item.duration)} />
        <StatCard label="평균 페이스" value={formatPace(item.averagePace)} />
        {item.calories != null && (
          <StatCard label="칼로리" value={`${item.calories} kcal`} />
        )}
        {item.averageHeartRate != null && (
          <StatCard
            label="평균 심박수"
            value={`${item.averageHeartRate} bpm`}
          />
        )}
        {item.cadence != null && (
          <StatCard label="케이던스" value={`${item.cadence} SPM`} />
        )}
      </div>

      {positions.length >= 2 && (
        <div className="mt-6">
          <p className="mb-2 text-sm font-medium text-slate-600">이동 경로</p>
          <div className="h-64 overflow-hidden rounded-xl border border-slate-200 bg-slate-100">
            <MapContainer
              center={center}
              zoom={14}
              style={{ height: "100%", width: "100%" }}
              scrollWheelZoom={false}
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              <Polyline
                positions={positions}
                pathOptions={{ color: "#059669", weight: 4 }}
              />
              <FitBounds points={positions} />
            </MapContainer>
          </div>
        </div>
      )}

      {item.memo && (
        <div className="mt-4 rounded-lg bg-slate-50 p-3 text-sm text-slate-600">
          <span className="font-medium text-slate-500">메모</span> {item.memo}
        </div>
      )}

      <div className="mt-6 flex gap-2">
        <button
          type="button"
          onClick={async () => {
            if (!item || !window.confirm("이 활동을 삭제할까요?")) return;
            setDeleting(true);
            try {
              await deleteActivity(item.id);
              navigate("/activities", { replace: true });
            } catch {
              setError("삭제에 실패했습니다.");
            } finally {
              setDeleting(false);
            }
          }}
          disabled={deleting}
          className="rounded-lg border border-red-300 bg-red-50 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100 disabled:opacity-50"
        >
          {deleting ? "삭제 중…" : "삭제"}
        </button>
      </div>
    </div>
  );
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-white p-3 shadow-sm">
      <p className="text-xs text-slate-500">{label}</p>
      <p className="font-semibold text-slate-800">{value}</p>
    </div>
  );
}
