import { BrowserRouter, Routes, Route } from 'react-router-dom';
import MainPage from './components/MainPage'; 
import ResumeMatch from './components/ResumeMatch';
import ResultPage from "./components/ResultPage"; 
import RoadmapPage from "./components/RoadmapPage"; 
import Navbar from './components/Navbar';
import MyPage from './components/MyPage'; 
import KakaoCallback from './components/KakaoCallback'; 
import LoginPage from './components/LoginPage';

// ✨ 새로 만든 채용 공고 리스트 컴포넌트 임포트!
import JobPostingList from './components/JobPostingList'; 

function App() {
  return (
    <BrowserRouter>
      <Navbar /> 
      <Routes>
        <Route path="/" element={<MainPage />} />
        <Route path="/match" element={<ResumeMatch />} />
        <Route path="/result" element={<ResultPage />} />
        <Route path="/roadmap" element={<RoadmapPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/mypage" element={<MyPage />} /> 

        {/* 카카오가 인가 코드를 들고 돌아올 도착지 주소 연결! */}
        <Route path="/oauth/kakao/callback" element={<KakaoCallback />} /> 
        
        {/* ✨ 채용 공고 추천 리스트 화면 연결! */}
        <Route path="/jobs" element={<JobPostingList />} /> 
      </Routes>
    </BrowserRouter>
  );
}

export default App;
