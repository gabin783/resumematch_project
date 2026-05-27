import { useState, useEffect } from 'react';
import axios from 'axios';
import './JobPostingList.css';

function JobPostingList() {
    const [jobs, setJobs] = useState([]);
    
    // ✨ 모달창과 매칭 결과를 관리하기 위한 상태값들 추가
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [matchResult, setMatchResult] = useState(null);

    useEffect(() => {
        axios.get('http://localhost:8080/api/jobs')
            .then(response => {
                setJobs(response.data);
            })
            .catch(error => {
                console.error("데이터를 불러오는데 실패했습니다!", error);
            });
    }, []);

    // ✨ 매칭 분석 버튼 클릭 시 실행되는 함수
    const handleMatchClick = (jobId) => {
        setIsModalOpen(true); // 모달창 열기
        setIsLoading(true);   // 로딩 스피너 돌리기
        setMatchResult(null); // 이전 결과 초기화

        // 로그인한 유저 ID를 로컬스토리지에서 가져옵니다 (없으면 임시로 1번 세팅)
        const memberId = localStorage.getItem('memberId') || 1; 

        // 우리가 방금 뚫어놓은 백엔드 API 호출! (POST 방식)
        axios.post(`http://localhost:8080/api/match/${jobId}?memberId=${memberId}`)
            .then(response => {
                setMatchResult(response.data); // 결과 데이터를 상태에 저장
            })
            .catch(error => {
                console.error("매칭 분석 실패:", error);
                setMatchResult({ aiFeedback: "분석 중 오류가 발생했습니다." });
            })
            .finally(() => {
                setIsLoading(false); // 로딩 끝!
            });
    };

    return (
        <div className="job-list-container">
            <h2 className="title">🎯 맞춤 추천 채용 공고</h2>
            <div className="job-grid">
                {jobs.map(job => (
                    <div className="job-card" key={job.id}>
                        <div className="card-header">
                            <span className="company-name">{job.companyName}</span>
                            <button className="bookmark-btn">🔖</button>
                        </div>
                        <h3 className="job-title">{job.title}</h3>
                        
                        <div className="card-actions">
                            <a href={job.url} target="_blank" rel="noopener noreferrer" className="apply-btn">
                                공고 확인
                            </a>
                            {/* ✨ 매칭 분석 버튼 추가! */}
                            <button 
                                className="match-btn" 
                                onClick={() => handleMatchClick(job.id)}
                            >
                                ✨ 매칭 분석
                            </button>
                        </div>
                    </div>
                ))}
            </div>

            {/* ✨ 매칭 결과 모달창 UI */}
            {isModalOpen && (
                <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
                    {/* onClick 이벤트 버블링 방지 (모달 내부 클릭시 안 닫히게) */}
                    <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                        <button className="close-btn" onClick={() => setIsModalOpen(false)}>✕</button>
                        
                        <h2 className="modal-title">📊 이력서 매칭 결과</h2>
                        
                        {isLoading ? (
                            <div className="loading-container">
                                <div className="spinner"></div>
                                <p>AI가 가빈 님의 이력서를 분석 중입니다...</p>
                            </div>
                        ) : matchResult && (
                            <div className="result-container">
                                <div className="match-rate-box">
                                    <span className="rate-label">종합 매칭률</span>
                                    <span className="rate-value">{matchResult.matchRate}%</span>
                                </div>
                                
                                <div className="skills-box">
                                    <div className="skill-group">
                                        <h4>✅ 보유 스킬</h4>
                                        <div className="skill-tags">
                                            {matchResult.matchedSkills?.map(skill => (
                                                <span key={skill} className="tag matched">{skill}</span>
                                            ))}
                                        </div>
                                    </div>
                                    <div className="skill-group">
                                        <h4>⚠️ 부족한 스킬</h4>
                                        <div className="skill-tags">
                                            {matchResult.missingSkills?.length > 0 
                                                ? matchResult.missingSkills.map(skill => <span key={skill} className="tag missing">{skill}</span>)
                                                : <span className="tag none">없음 (완벽합니다!)</span>
                                            }
                                        </div>
                                    </div>
                                </div>

                                <div className="ai-feedback-box">
                                    <h4>💡 AI 멘토의 팁</h4>
                                    <p>{matchResult.aiFeedback}</p>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

export default JobPostingList;
