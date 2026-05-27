import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

const Navbar = () => {
  const location = useLocation();
  const navigate = useNavigate();

  // 로그인 상태를 관리할 변수
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  // 화면 주소가 바뀔 때마다 로그인 상태 확인
  useEffect(() => {
    const storedName = localStorage.getItem('nickname');
    if (storedName) {
      setIsLoggedIn(true);
    } else {
      setIsLoggedIn(false);
    }
  }, [location.pathname]);

  // 카카오 로그인 주소 설정
  const KAKAO_CLIENT_ID = "6c26e45a1be28d1c9d6d41d9edaeb2d2"; 
  const REDIRECT_URI = "http://localhost:5173/oauth/kakao/callback";
  const kakaoURL = `https://kauth.kakao.com/oauth/authorize?client_id=${KAKAO_CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code`;

  const handleLogin = () => {
    window.location.href = kakaoURL; 
  };

  // 로그아웃 처리 함수
  const handleLogout = () => {
    localStorage.removeItem('memberId');
    localStorage.removeItem('nickname');
    setIsLoggedIn(false);
    alert('성공적으로 로그아웃 되었습니다.');
    navigate('/'); 
  };

  const isActive = (path) => location.pathname === path;

  const menuClass = (path) => `
    relative pb-2 font-semibold transition-colors
    ${isActive(path) ? 'text-white' : 'text-slate-400 hover:text-slate-200'}
  `;

  return (
    <nav className="sticky top-0 z-50 w-full h-[70px] bg-black border-b border-slate-800 shadow-lg flex justify-center items-center">
      <div className="w-full max-w-6xl px-6 flex justify-between items-center">
        
        {/* 좌측: 로고 */}
        <div 
          className="text-2xl font-extrabold text-white cursor-pointer tracking-tight"
          onClick={() => navigate('/')}
        >
          <span className="text-indigo-400">Match</span>Maker
        </div>

        {/* 중앙: 메인 메뉴 */}
        <ul className="hidden md:flex gap-10 m-0 p-0 list-none">
          <li>
            <Link to="/match" className={menuClass('/match')}>
              이력서 매칭
              {isActive('/match') && <span className="absolute bottom-0 left-0 w-full h-[3px] bg-indigo-400 rounded-t-sm"></span>}
            </Link>
          </li>
          <li>
            <Link to="/result" className={menuClass('/result')}>
              스킬 갭 분석
              {isActive('/result') && <span className="absolute bottom-0 left-0 w-full h-[3px] bg-indigo-400 rounded-t-sm"></span>}
            </Link>
          </li>
          <li>
            <Link to="/roadmap" className={menuClass('/roadmap')}>
              학습 로드맵
              {isActive('/roadmap') && <span className="absolute bottom-0 left-0 w-full h-[3px] bg-indigo-400 rounded-t-sm"></span>}
            </Link>
          </li>
          {/* ✨ 새로 추가된 추천 공고 메뉴! */}
          <li>
            <Link to="/jobs" className={menuClass('/jobs')}>
              추천 공고
              {isActive('/jobs') && <span className="absolute bottom-0 left-0 w-full h-[3px] bg-indigo-400 rounded-t-sm"></span>}
            </Link>
          </li>
        </ul>

        {/* 우측: 유저 메뉴 */}
        <div className="flex gap-4 items-center">
          <button 
            onClick={() => navigate('/mypage')}
            className="px-4 py-2 rounded-lg font-semibold border border-slate-600 text-slate-200 hover:bg-slate-800 hover:text-white transition"
          >
            마이페이지
          </button>

          {isLoggedIn ? (
            <div className="flex items-center gap-3">
              <button 
                onClick={handleLogout}
                className="px-4 py-2 rounded-lg font-bold bg-slate-700 text-white hover:bg-slate-600 transition shadow-md"
              >
                로그아웃
              </button>
            </div>
          ) : (
            <button 
              onClick={handleLogin}
              className="px-4 py-2 rounded-lg font-semibold bg-indigo-600 text-white hover:bg-indigo-700 transition shadow-md"
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
