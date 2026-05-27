import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom'; 
import './MyPage.css';

const MyPage = () => {
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [resumes, setResumes] = useState([]);
    const [analysisResults, setAnalysisResults] = useState([]); 
    const [roadmaps, setRoadmaps] = useState([]); 
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchDashboardData = async () => {
            try {
                const response = await fetch('http://localhost:8080/api/mypage/dashboard');
                
                if (!response.ok) {
                    throw new Error('데이터를 불러오는데 실패했습니다.');
                }
                
                const data = await response.json();
                
                setProfile(data.profile);
                setResumes(data.resumes);
                setAnalysisResults(data.analysisResults || []); 
                setRoadmaps(data.roadmaps || []); 
            } catch (error) {
                console.error("API 호출 에러:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchDashboardData();
    }, []);

    const handleDelete = async (id) => {
        if (!window.confirm("정말 이 기록을 삭제하시겠습니까?")) return; 
        try {
            const response = await fetch(`http://localhost:8080/api/resume/${id}`, { method: 'DELETE' });
            if (response.ok) setResumes(resumes.filter(resume => resume.id !== id));
            else alert("삭제에 실패했습니다.");
        } catch (error) {
            console.error("삭제 에러:", error);
        }
    };

    const handleDeleteAnalysis = async (id) => {
        if (!window.confirm("정말 이 분석 기록을 삭제하시겠습니까?")) return; 
        try {
            const response = await fetch(`http://localhost:8080/api/resume/analysis/${id}`, { method: 'DELETE' });
            if (response.ok) setAnalysisResults(analysisResults.filter(result => result.id !== id));
            else alert("삭제에 실패했습니다.");
        } catch (error) {
            console.error("삭제 에러:", error);
        }
    };

    const handleDeleteRoadmap = async (id) => {
        if (!window.confirm("정말 이 로드맵을 삭제하시겠습니까?")) return; 
        try {
            const response = await fetch(`http://localhost:8080/api/roadmap/${id}`, { method: 'DELETE' });
            if (response.ok) setRoadmaps(roadmaps.filter(map => map.id !== id));
            else alert("삭제에 실패했습니다.");
        } catch (error) {
            console.error("삭제 에러:", error);
        }
    };

    if (loading) {
        return <div style={{ padding: '20px', textAlign: 'center' }}>데이터를 불러오는 중입니다... ⏳</div>;
    }

    return (
        <div className="mypage-container">
            <h1 className="mypage-title">마이페이지</h1>

            {/* 💡 내 프로필 영역 */}
            {profile && (
                <div className="profile-section">
                    <h2 className="profile-name">
                        {profile.nickname} 님, 환영합니다!
                    </h2>
                    <p className="profile-info"><strong>이메일:</strong> {profile.email}</p>
                    <p className="profile-info"><strong>소개:</strong> {profile.bio}</p>
                </div>
            )}

            {/* 💡 1. 이력서 분석 기록 영역 */}
            <div className="resume-list-container">
                <h3 className="section-title">내 이력서 파싱 기록 ({resumes.length}건)</h3>
                {resumes.length === 0 ? (
                    <p className="empty-message">기록이 없습니다.</p>
                ) : (
                    resumes.map((resume) => (
                        <div key={resume.id} className="resume-card">
                            <div className="resume-card-header">
                                <span>📄 {resume.originalFileName}</span>
                                <button onClick={() => handleDelete(resume.id)} className="delete-btn">삭제</button>
                            </div>
                            <p className="resume-date">{new Date(resume.createdAt).toLocaleString()}</p>
                        </div>
                    ))
                )}
            </div>

            {/* 💡 2. AI 스킬 갭 분석 기록 영역 */}
            <div style={{ marginBottom: '50px' }}>
                <h3 className="section-title">🔍 AI 스킬 갭 분석 기록 ({analysisResults.length}건)</h3>
                {analysisResults.length === 0 ? (
                    <p className="empty-message large-padding">아직 진행한 스킬 갭 분석이 없습니다.</p>
                ) : (
                    analysisResults.map((result) => (
                        <div key={result.id} className="analysis-card">
                            <div className="analysis-card-header" style={{ alignItems: 'flex-start' }}>
                                <div>
                                    <h4 className="target-job-title">🎯 목표 직무: {result.targetJob}</h4>
                                    <p className="analysis-date">분석 일시: {new Date(result.createdAt).toLocaleString()}</p>
                                </div>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'flex-end', marginLeft: '20px' }}>
                                    <button onClick={() => navigate('/result', { state: { resultData: result } })} className="report-btn">리포트 보기</button>
                                    <button onClick={() => handleDeleteAnalysis(result.id)} className="delete-btn" style={{ fontSize: '0.85em', marginRight: '5px' }}>삭제</button>
                                </div>
                            </div>
                            <div className="skill-tags-container">
                                {result.missingSkills && result.missingSkills.split(',').map((skill, index) => (
                                    <span key={index} className="missing-skill-tag">{skill.trim()}</span>
                                ))}
                            </div>
                        </div>
                    ))
                )}
            </div>

            {/* 💡 3. 학습 로드맵 기록 영역 */}
            <div>
                <h3 className="section-title">🗺️ 맞춤형 학습 로드맵 ({roadmaps.length}건)</h3>
                {roadmaps.length === 0 ? (
                    <p className="empty-message large-padding">생성된 학습 로드맵이 없습니다.</p>
                ) : (
                    roadmaps.map((map) => (
                        <div key={map.id} className="analysis-card"> 
                            <div className="analysis-card-header" style={{ alignItems: 'flex-start' }}>
                                <div>
                                    <h4 className="target-job-title">🚩 로드맵: {map.targetJob}</h4>
                                    <p className="analysis-date">생성 일시: {new Date(map.createdAt).toLocaleString()}</p>
                                </div>
                                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', alignItems: 'flex-end', marginLeft: '20px' }}>
                                    <button 
                                        onClick={() => navigate('/roadmap', { 
                                            state: { 
                                                roadmapData: map,
                                                nickname: profile?.nickname // 프로필에서 닉네임 추출
                                            } 
                                        })} 
                                        className="report-btn" 
                                        style={{ backgroundColor: '#52c41a' }}
                                    >
                                        로드맵 보기
                                    </button>
                                    <button onClick={() => handleDeleteRoadmap(map.id)} className="delete-btn" style={{ fontSize: '0.85em', marginRight: '5px' }}>삭제</button>
                                </div>
                            </div>
                        </div>
                    ))
                )}
            </div>
        </div>
    );
};

export default MyPage;
