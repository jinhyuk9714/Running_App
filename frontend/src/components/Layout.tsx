import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { logout } from "../api/client";

export default function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const path = location.pathname;

  const nav = [
    { to: "/", label: "대시보드" },
    { to: "/activities", label: "러닝 기록" },
    { to: "/challenges", label: "챌린지" },
    { to: "/plans", label: "플랜" },
    { to: "/profile", label: "내 정보" },
  ];

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <header className="sticky top-0 z-10 border-b border-slate-200 bg-white shadow-sm">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-4 py-3">
          <Link to="/" className="text-lg font-bold text-emerald-700">
            Running App
          </Link>
          <nav className="flex flex-wrap items-center gap-1">
            {nav.map(({ to, label }) => (
              <Link
                key={to}
                to={to}
                className={`rounded-lg px-3 py-1.5 text-sm font-medium ${
                  path === to || (to !== "/" && path.startsWith(to))
                    ? "bg-emerald-100 text-emerald-800"
                    : "text-slate-600 hover:bg-slate-100 hover:text-slate-800"
                }`}
              >
                {label}
              </Link>
            ))}
            <button
              onClick={handleLogout}
              className="rounded-lg px-3 py-1.5 text-sm text-slate-600 hover:bg-slate-100 hover:text-slate-800"
            >
              로그아웃
            </button>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-4xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
