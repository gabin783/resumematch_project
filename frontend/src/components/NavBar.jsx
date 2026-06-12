import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

const menuItems = [
  { path: '/match', label: '이력서 매칭' },
  { path: '/result', label: '스킬 갭 분석' },
  { path: '/roadmap', label: '학습 로드맵' },
  { path: '/jobs', label: '추천 공고' },
];

const Navbar = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    setIsLoggedIn(Boolean(localStorage.getItem('nickname')));
  }, [location.pathname]);

  const handleLogin = () => {
    navigate('/login');
  };

  const handleLogout = () => {
    localStorage.removeItem('memberId');
    localStorage.removeItem('nickname');
    setIsLoggedIn(false);
    navigate('/');
  };

  const isActive = (path) => location.pathname === path;

  const menuClass = (path) => `
    relative px-1 pb-2 text-[14px] font-semibold tracking-normal transition-colors
    ${isActive(path) ? 'text-indigo-600' : 'text-slate-700 hover:text-indigo-600'}
  `;

  const accountButtonClass = `
    h-8 rounded-[9px] border px-4 text-[13px] font-semibold transition-colors
    ${
      isActive('/mypage')
        ? 'border-indigo-600 bg-indigo-600 text-white shadow-sm'
        : 'border-indigo-200 bg-white text-indigo-600 hover:border-indigo-300 hover:bg-indigo-50'
    }
  `;

  return (
    <nav className="sticky top-0 z-50 flex h-[58px] w-full items-center justify-center border-b border-slate-200 bg-white shadow-sm">
      <div className="flex w-full max-w-7xl items-center justify-between px-8">
        <div
          className="cursor-pointer"
          onClick={() => navigate('/')}
          role="button"
          tabIndex={0}
          onKeyDown={(event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              navigate('/');
            }
          }}
        >
          <div className="flex items-center gap-2">
            <img
              src="/favicon.png"
              alt="ResumeMatch"
              className="h-[29px] w-[29px] rounded-[8px] object-contain"
            />
            <span className="text-[18px] font-black text-[#111827]">
              ResumeMatch
            </span>
          </div>
        </div>

        {/* TODO: 모바일 화면에서는 햄버거 메뉴 또는 하단 메뉴로 네비게이션을 보완해야 합니다. */}
        <ul className="m-0 hidden list-none gap-8 p-0 md:flex">
          {menuItems.map((item) => (
            <li key={item.path}>
              <Link to={item.path} className={menuClass(item.path)}>
                {item.label}
                {isActive(item.path) ? (
                  <span className="absolute bottom-0 left-1/2 h-[2px] w-4/5 -translate-x-1/2 rounded-full bg-indigo-600" />
                ) : null}
              </Link>
            </li>
          ))}
        </ul>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={() => navigate('/mypage')}
            className={accountButtonClass}
          >
            마이페이지
          </button>

          {isLoggedIn ? (
            <button
              type="button"
              onClick={handleLogout}
              className="h-8 rounded-[9px] border border-slate-200 bg-white px-4 text-[13px] font-semibold text-slate-600 transition hover:border-slate-300 hover:bg-slate-50 hover:text-slate-800"
            >
              로그아웃
            </button>
          ) : (
            <button
              type="button"
              onClick={handleLogin}
              className="h-8 rounded-[9px] bg-indigo-600 px-4 text-[13px] font-semibold text-white shadow-sm transition hover:bg-indigo-500"
            >
              로그인
            </button>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
