import { useNavigate } from 'react-router-dom';
import resumeMatchLogo from '../assets/resumematch-logo.svg';
import './LoginPage.css';

const KAKAO_CLIENT_ID = import.meta.env.VITE_KAKAO_CLIENT_ID || '6c26e45a1be28d1c9d6d41d9edaeb2d2';
const REDIRECT_URI = import.meta.env.VITE_KAKAO_REDIRECT_URI || 'http://localhost:5173/oauth/kakao/callback';

const LoginPage = () => {
  const navigate = useNavigate();
  const kakaoURL = `https://kauth.kakao.com/oauth/authorize?client_id=${KAKAO_CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code`;

  const handleKakaoLogin = () => {
    window.location.href = kakaoURL;
  };

  return (
    <main className="login-page">
      <section className="login-card">
        <button type="button" className="login-logo-button" onClick={() => navigate('/')}>
          <span className="login-logo-mark" aria-hidden="true">
            <img src={resumeMatchLogo} alt="" />
          </span>
          <span className="login-logo-text">ResumeMatch</span>
        </button>

        <div className="login-copy">
          <span>ResumeMatch MVP</span>
          <h1>로그인</h1>
          <p>AI 이력서 분석을 시작하세요.</p>
          <small>카카오 계정으로 간편하게 로그인합니다.</small>
        </div>

        <button type="button" className="kakao-login-button" onClick={handleKakaoLogin}>
          카카오로 계속하기
        </button>

        <p className="login-mvp-note">현재 MVP에서는 카카오 로그인만 지원합니다.</p>
      </section>
    </main>
  );
};

export default LoginPage;
