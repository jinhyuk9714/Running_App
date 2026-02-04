import { useEffect, useState } from "react";
import { getMe, updateProfile } from "../api/client";
import type { UserInfo, ProfileUpdateRequest } from "../types";

export default function Profile() {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [edit, setEdit] = useState(false);
  const [nickname, setNickname] = useState("");
  const [weight, setWeight] = useState("");
  const [height, setHeight] = useState("");
  const [saving, setSaving] = useState(false);

  const load = async () => {
    setLoading(true);
    setError("");
    try {
      const u = await getMe();
      setUser(u);
      setNickname(u.nickname ?? "");
      setWeight(u.id ? "" : "");
      setHeight(u.id ? "" : "");
    } catch {
      setError("내 정보를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError("");
    try {
      const body: ProfileUpdateRequest = {};
      if (nickname.trim()) body.nickname = nickname.trim();
      const w = parseFloat(weight);
      if (!Number.isNaN(w)) body.weight = w;
      const h = parseFloat(height);
      if (!Number.isNaN(h)) body.height = h;
      const u = await updateProfile(body);
      setUser(u);
      setEdit(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "수정 실패");
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <span className="text-slate-500">불러오는 중…</span>
      </div>
    );
  }
  if (error && !user) {
    return (
      <div className="rounded-lg bg-red-50 p-4 text-red-700">
        {error}
        <button onClick={load} className="ml-2 underline">
          다시 시도
        </button>
      </div>
    );
  }
  if (!user) return null;

  return (
    <div className="rounded-2xl bg-white p-6 shadow-sm">
      <h1 className="text-xl font-bold text-slate-800">내 정보</h1>
      {!edit ? (
        <div className="mt-4 space-y-2">
          <p>
            <span className="text-slate-500">이메일</span> {user.email}
          </p>
          <p>
            <span className="text-slate-500">닉네임</span> {user.nickname}
          </p>
          {user.level != null && (
            <p>
              <span className="text-slate-500">레벨</span> {user.level}
            </p>
          )}
          {user.totalDistance != null && (
            <p>
              <span className="text-slate-500">누적 거리</span>{" "}
              {user.totalDistance.toFixed(2)} km
            </p>
          )}
          <button
            onClick={() => setEdit(true)}
            className="mt-4 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700"
          >
            프로필 수정
          </button>
        </div>
      ) : (
        <form onSubmit={handleSave} className="mt-4 space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-600">
              닉네임 (2~20자)
            </label>
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              className="mt-1 w-full max-w-xs rounded-lg border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
              minLength={2}
              maxLength={20}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-600">
              체중 (kg, 선택)
            </label>
            <input
              type="number"
              step="0.1"
              value={weight}
              onChange={(e) => setWeight(e.target.value)}
              placeholder="70"
              className="mt-1 w-full max-w-xs rounded-lg border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-600">
              신장 (cm, 선택)
            </label>
            <input
              type="number"
              step="0.1"
              value={height}
              onChange={(e) => setHeight(e.target.value)}
              placeholder="175"
              className="mt-1 w-full max-w-xs rounded-lg border border-slate-300 px-3 py-2 focus:border-emerald-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
            />
          </div>
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex gap-2">
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
            >
              {saving ? "저장 중…" : "저장"}
            </button>
            <button
              type="button"
              onClick={() => setEdit(false)}
              className="rounded-lg border border-slate-300 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50"
            >
              취소
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
