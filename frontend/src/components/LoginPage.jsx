import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import resumeMatchLogo from '../assets/resumematch-logo.svg';
import { API_BASE_URL } from '../config/api';
import './LoginPage.css';

const KAKAO_CLIENT_ID = import.meta.env.VITE_KAKAO_CLIENT_ID || '';
const REDIRECT_URI = import.meta.env.VITE_KAKAO_REDIRECT_URI || 'http://54.116.239.232/oauth/kakao/callback';

const LoginPage = () => {
  const navigate = useNavigate();
  const kakaoURL = `https://kauth.kakao.com/oauth/authorize?client_id=${KAKAO_CLIENT_ID}&redirect_uri=${REDIRECT_URI}&response_type=code`;

  const handleKakaoLogin = () => {
    window.location.href = kakaoURL;
  };

  const handleDemoLogin = async () => {
    try {
      const response = await axios.post(`${API_BASE_URL}/api/oauth/demo`);

      localStorage.setItem('memberId', response.data.id);
      localStorage.setItem('nickname', response.data.nickname);
      localStorage.setItem('isDemo', 'true');

      navigate('/');
    } catch (error) {
      console.error('Demo login error:', error);
      alert('시연용 로그인 중 오류가 발생했습니다.');
    }
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

        <button type="button" className="kakao-login-button" onClick={handleDemoLogin}>
          시연용 계정으로 체험하기
        </button>

        <p className="login-mvp-note">현재 MVP에서는 카카오 로그인만 지원합니다.</p>
      </section>
    </main>
  );
};

export default LoginPage;
