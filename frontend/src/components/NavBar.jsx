import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import resumeMatchLogo from '../assets/resumematch-logo.svg';

const KAKAO_CLIENT_ID = import.meta.env.VITE_KAKAO_CLIENT_ID || '6c26e45a1be28d1c9d6d41d9edaeb2d2';
const REDIRECT_URI = import.meta.env.VITE_KAKAO_REDIRECT_URI || 'http://localhost:5173/oauth/kakao/callback';

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

  const kakaoURL = `https://kauth.kakao.com/oauth/authorize?client_id=${KAKAO_CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code`;

  const handleLogin = () => {
    window.location.href = kakaoURL;
  };

  const handleLogout = () => {
    localStorage.removeItem('memberId');
    localStorage.removeItem('nickname');
    setIsLoggedIn(false);
    alert('성공적으로 로그아웃 되었습니다.');
    navigate('/');
  };

  const isActive = (path) => location.pathname === path;

  const menuClass = (path) => `
    relative px-1 pb-2 text-[17px] font-semibold tracking-normal transition-colors
    ${isActive(path) ? 'text-white' : 'text-slate-400 hover:text-slate-100'}
  `;

  const accountButtonClass = `
    h-10 px-5 rounded-xl border text-[15px] font-semibold transition
    ${
      isActive('/mypage')
        ? 'border-indigo-400 bg-slate-900 text-white'
        : 'border-slate-600 text-slate-200 hover:bg-slate-900 hover:text-white'
    }
  `;

  return (
    <nav className="sticky top-0 z-50 flex h-[66px] w-full items-center justify-center border-b border-slate-800 bg-black shadow-lg">
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
          <img
            src={resumeMatchLogo}
            alt="ResumeMatch"
            className="h-9 w-auto object-contain"
          />
        </div>

        {/* TODO: 모바일 화면에서는 햄버거 메뉴 또는 하단 메뉴로 네비게이션을 보완해야 합니다. */}
        <ul className="m-0 hidden list-none gap-8 p-0 md:flex">
          {menuItems.map((item) => (
            <li key={item.path}>
              <Link to={item.path} className={menuClass(item.path)}>
                {item.label}
                {isActive(item.path) ? (
                  <span className="absolute bottom-0 left-1/2 h-[2.5px] w-4/5 -translate-x-1/2 rounded-full bg-indigo-400" />
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
              className="h-10 rounded-xl bg-slate-700 px-5 text-[15px] font-bold text-white shadow-md transition hover:bg-slate-600"
            >
              로그아웃
            </button>
          ) : (
            <button
              type="button"
              onClick={handleLogin}
              className="h-10 rounded-xl bg-indigo-600 px-5 text-[15px] font-semibold text-white shadow-md transition hover:bg-indigo-500"
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
