import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { API_BASE_URL } from '../config/api';

const KakaoCallback = () => {
  const navigate = useNavigate();
  const isCalled = useRef(false); // ✨ 두 번 호출 방지용 스위치 추가!

  useEffect(() => {
    // ✨ 이미 한 번 실행됐다면 여기서 멈춤!
    if (isCalled.current) return;
    isCalled.current = true;

    const params = new URL(window.location.href).searchParams;
    const code = params.get("code");

    if (code) {
      axios.post(`${API_BASE_URL}/api/oauth/kakao`, { code: code })
        .then(response => {
          localStorage.setItem('memberId', response.data.id);
          localStorage.setItem('nickname', response.data.nickname);
          
          navigate('/'); 
        })
        .catch(error => {
          console.error("로그인 에러:", {
            status: error.response?.status,
            message: error.message,
          });
          // 알림창이 너무 많이 뜨는 것을 막기 위해 에러 알림은 주석 처리하거나 빼두셔도 좋습니다.
          // alert("로그인 처리 중 문제가 발생했습니다."); 
          navigate('/');
        });
    }
  }, [navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50">
      <div className="text-xl font-bold text-indigo-600 animate-pulse">
        카카오 로그인 처리 중입니다... ⏳
      </div>
    </div>
  );
};

export default KakaoCallback;
