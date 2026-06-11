import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './MyPage.css';

const parseStoredList = (value) => {
  if (!value) return [];
  if (Array.isArray(value)) {
    return value
      .map((item) => (typeof item === 'string' ? item : item?.name || item?.title || ''))
      .map((item) => String(item).trim())
      .filter(Boolean);
  }

  const text = String(value).trim();
  if (!text) return [];

  if (text.startsWith('[')) {
    try {
      const parsed = JSON.parse(text);
      if (Array.isArray(parsed)) {
        return parseStoredList(parsed);
      }
    } catch {
      // 이전 저장 데이터는 콤마 문자열일 수 있습니다.
    }
  }

  return text.split(',').map((item) => item.trim()).filter(Boolean);
};

const parseJsonSafely = (value) => {
  if (!value) return null;
  if (typeof value === 'object') return value;

  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
};

const formatDate = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return date.toLocaleString();
};

const uniqueList = (items) => [...new Set(items.map((item) => String(item).trim()).filter(Boolean))];

const getAnalysisPayload = (result) => parseJsonSafely(result?.analysis) || {};

const getMatchScore = (result) => {
  const parsed = getAnalysisPayload(result);
  return Number.isFinite(Number(parsed.matchScore)) ? Number(parsed.matchScore) : null;
};

const getRequirementSkills = (result) =>
  uniqueList([
    ...parseStoredList(result?.requiredSkills),
    ...parseStoredList(result?.preferredSkills),
  ]).slice(0, 8);

const getMissingSkills = (result) => {
  const storedMissing = parseStoredList(result?.missingSkills);
  if (storedMissing.length > 0) return storedMissing.slice(0, 6);

  const parsed = getAnalysisPayload(result);
  return parseStoredList(parsed.missingSkills).slice(0, 6);
};

const getRoadmapPayload = (roadmap) => parseJsonSafely(roadmap?.content) || {};

const getRoadmapSkills = (roadmap) => {
  const parsed = getRoadmapPayload(roadmap);
  const skills = Array.isArray(parsed.weeks)
    ? parsed.weeks.flatMap((week) => parseStoredList(week?.focusSkills || week?.tags))
    : [];

  return uniqueList(skills).slice(0, 6);
};

const getFavoriteJobsStorageKey = (memberId) => `favoriteJobs:${memberId || 'guest'}`;

const readFavoriteJobsFromStorage = (memberId) => {
  try {
    const parsed = JSON.parse(localStorage.getItem(getFavoriteJobsStorageKey(memberId)) || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
};

const MyPage = () => {
  const navigate = useNavigate();

  const [activeTab, setActiveTab] = useState('analysis');
  const [profile, setProfile] = useState(null);
  const [resumes, setResumes] = useState([]);
  const [analysisResults, setAnalysisResults] = useState([]);
  const [roadmaps, setRoadmaps] = useState([]);
  const [favoriteJobs, setFavoriteJobs] = useState([]);
  const [nicknameInput, setNicknameInput] = useState('');
  const [profileImage, setProfileImage] = useState(localStorage.getItem('profileImage') || '');
  const [loading, setLoading] = useState(true);
  const [isAuthenticated, setIsAuthenticated] = useState(true);

  const memberId = localStorage.getItem('memberId');

  useEffect(() => {
    const fetchDashboardData = async () => {
      if (!memberId) {
        setIsAuthenticated(false);
        setLoading(false);
        return;
      }

      try {
        const response = await fetch(
          `http://localhost:8080/api/mypage/dashboard?memberId=${encodeURIComponent(memberId)}`
        );

        if (!response.ok) {
          throw new Error('대시보드 데이터를 불러오지 못했습니다.');
        }

        const data = await response.json();

        setProfile(data.profile);
        setResumes(data.resumes || []);
        setAnalysisResults(data.analysisResults || []);
        setRoadmaps(data.roadmaps || []);
        setFavoriteJobs(
          data.favoriteJobs?.length
            ? data.favoriteJobs
            : readFavoriteJobsFromStorage(memberId)
        );
        setNicknameInput(data.profile?.nickname || localStorage.getItem('nickname') || '');
      } catch (error) {
        console.error('마이페이지 API 호출 오류:', error);
        setFavoriteJobs(readFavoriteJobsFromStorage(memberId));
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
  }, [memberId]);

  const summaryItems = useMemo(
    () => [
      {
        label: '이력서 분석',
        value: resumes.length,
        desc: '업로드한 이력서',
      },
      {
        label: '매칭 분석',
        value: analysisResults.length,
        desc: '채용공고 비교 결과',
      },
      {
        label: '학습 로드맵',
        value: roadmaps.length,
        desc: '생성된 학습 계획',
      },
      {
        label: '즐겨찾기',
        value: favoriteJobs.length,
        desc: '저장한 추천 공고',
      },
    ],
    [resumes.length, analysisResults.length, roadmaps.length, favoriteJobs.length]
  );

  const handleDelete = async (id) => {
    if (!window.confirm('정말 이 기록을 삭제하시겠습니까?')) return;

    try {
      const response = await fetch(`http://localhost:8080/api/resume/${id}`, {
        method: 'DELETE',
      });

      if (response.ok) {
        setResumes(resumes.filter((resume) => resume.id !== id));
      } else {
        alert('삭제에 실패했습니다.');
      }
    } catch (error) {
      console.error('삭제 오류:', error);
    }
  };

  const handleDeleteAnalysis = async (id) => {
    if (!window.confirm('정말 이 분석 기록을 삭제하시겠습니까?')) return;

    try {
      const response = await fetch(`http://localhost:8080/api/resume/analysis/${id}`, {
        method: 'DELETE',
      });

      if (response.ok) {
        setAnalysisResults(analysisResults.filter((result) => result.id !== id));
      } else {
        alert('삭제에 실패했습니다.');
      }
    } catch (error) {
      console.error('삭제 오류:', error);
    }
  };

  const handleDeleteRoadmap = async (id) => {
    if (!window.confirm('정말 이 로드맵을 삭제하시겠습니까?')) return;

    try {
      const response = await fetch(`http://localhost:8080/api/roadmap/${id}`, {
        method: 'DELETE',
      });

      if (response.ok) {
        setRoadmaps(roadmaps.filter((map) => map.id !== id));
      } else {
        alert('삭제에 실패했습니다.');
      }
    } catch (error) {
      console.error('삭제 오류:', error);
    }
  };

  const handleRemoveFavoriteJob = async (id) => {
    if (!window.confirm('즐겨찾기에서 제거하시겠습니까?')) return;

    const nextFavoriteJobs = favoriteJobs.filter((job) => String(job.id) !== String(id));
    localStorage.setItem(getFavoriteJobsStorageKey(memberId), JSON.stringify(nextFavoriteJobs));
    setFavoriteJobs(nextFavoriteJobs);
  };

  const handleSaveNickname = () => {
    const nextNickname = nicknameInput.trim();

    if (!nextNickname) {
      alert('닉네임을 입력해주세요.');
      return;
    }

    setProfile((prev) => ({
      ...(prev || {}),
      nickname: nextNickname,
    }));

    localStorage.setItem('nickname', nextNickname);
    alert('닉네임이 임시 저장되었습니다. 백엔드 API 연결 후 서버 저장으로 전환할 수 있습니다.');
  };

  const handleProfileImageChange = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      alert('이미지 파일만 업로드할 수 있습니다.');
      return;
    }

    const reader = new FileReader();

    reader.onload = () => {
      const imageUrl = reader.result;
      setProfileImage(imageUrl);
      localStorage.setItem('profileImage', imageUrl);
    };

    reader.readAsDataURL(file);
  };

  const handleResetProfileImage = () => {
    setProfileImage('');
    localStorage.removeItem('profileImage');
  };

  const handleWithdraw = () => {
    const firstConfirm = window.confirm('정말 회원 탈퇴를 진행하시겠습니까?');
    if (!firstConfirm) return;

    const secondConfirm = window.confirm(
      '회원 탈퇴 시 이력서 분석 기록, 매칭 분석 기록, 학습 로드맵이 삭제될 수 있습니다.\n그래도 탈퇴하시겠습니까?'
    );

    if (!secondConfirm) return;

    alert('회원 탈퇴 API 연결 후 처리할 수 있습니다.');
  };

  if (loading) {
    return <div className="mypage-loading">대시보드 데이터를 불러오는 중입니다...</div>;
  }

  if (!isAuthenticated) {
    return (
      <main className="mypage-auth-required">
        <section className="mypage-auth-card">
          <h1>로그인이 필요합니다</h1>
          <p>이력서 분석 기록과 학습 로드맵을 확인하세요.</p>
          <button type="button" onClick={() => navigate('/login')}>
            로그인하러 가기
          </button>
        </section>
      </main>
    );
  }

  return (
    <main className="mypage-shell">
      <section className="mypage-layout">
        <aside className="mypage-sidebar">
          <div className="profile-card">
            {profileImage ? (
              <img src={profileImage} alt="프로필" className="profile-avatar-image" />
            ) : (
              <div className="profile-avatar">
                {(profile?.nickname || 'U').slice(0, 1)}
              </div>
            )}

            <strong>{profile?.nickname || '사용자'} 님</strong>
            {profile?.email ? <span>{profile.email}</span> : <span>카카오 연동 계정</span>}
          </div>

          <nav className="mypage-menu" aria-label="마이페이지 메뉴">
            <button
              type="button"
              className={activeTab === 'analysis' ? 'active' : ''}
              onClick={() => setActiveTab('analysis')}
            >
              이력서 매칭 기록
            </button>
            <button
              type="button"
              className={activeTab === 'roadmap' ? 'active' : ''}
              onClick={() => setActiveTab('roadmap')}
            >
              학습 로드맵
            </button>
            <button
              type="button"
              className={activeTab === 'favorites' ? 'active' : ''}
              onClick={() => setActiveTab('favorites')}
            >
              즐겨찾기 공고
            </button>
            <button
              type="button"
              className={activeTab === 'profile' ? 'active' : ''}
              onClick={() => setActiveTab('profile')}
            >
              개인정보 수정
            </button>
          </nav>

          <div className="profile-progress-card">
            <span>내 분석 현황</span>
            <strong>{analysisResults.length + roadmaps.length}건</strong>
            <p>매칭 분석과 학습 로드맵을 기반으로 취업 준비 현황을 관리하세요.</p>
          </div>
        </aside>

        <section className="mypage-main">
          <header className="mypage-header">
            <div>
              <h1>마이페이지</h1>
            </div>
            <button type="button" className="primary-action-btn" onClick={() => navigate('/match')}>
              새 매칭 시작
            </button>
          </header>

          <section className="summary-strip" aria-label="분석 요약">
            {summaryItems.map((item) => (
              <article key={item.label} className="summary-mini-card">
                <span>{item.label}</span>
                <strong>{item.value}</strong>
                <p>{item.desc}</p>
              </article>
            ))}
          </section>

          {activeTab === 'analysis' ? (
            <section className="content-panel">
              <div className="panel-heading">
                <div>
                  <h2>이력서 매칭 기록</h2>
                  <p>최근 채용공고 비교 결과를 확인하고 리포트로 이동할 수 있습니다.</p>
                </div>
                <span>{analysisResults.length}건</span>
              </div>

              {analysisResults.length === 0 ? (
                <p className="empty-message">아직 생성된 매칭 분석이 없습니다.</p>
              ) : (
                <div className="match-card-list">
                  {analysisResults.map((result) => {
                    const matchScore = getMatchScore(result);
                    const requirementSkills = getRequirementSkills(result);
                    const missingSkills = getMissingSkills(result);

                    return (
                      <article key={result.id} className="match-record-card">
                        <div className="match-record-top">
                          <div className="match-record-title">
                            <strong>{result.targetJob || '분석된 직무'}</strong>
                            <span>{formatDate(result.createdAt)}</span>
                            {result.jobSummary ? <p>{result.jobSummary}</p> : null}
                          </div>

                          <div className="match-record-actions">
                            <span className="score-pill">
                              {matchScore !== null ? `${matchScore}점` : '완료'}
                            </span>
                            <button
                              type="button"
                              className="report-btn"
                              onClick={() => navigate('/result', { state: { resultData: result } })}
                            >
                              리포트
                            </button>
                            <button
                              type="button"
                              className="delete-btn"
                              onClick={() => handleDeleteAnalysis(result.id)}
                            >
                              삭제
                            </button>
                          </div>
                        </div>

                        <div className="match-skill-summary">
                          <div className="match-skill-row">
                            <span className="match-skill-label">요구 기술</span>
                            <div className="chip-list">
                              {requirementSkills.length > 0 ? (
                                requirementSkills.map((skill) => (
                                  <span key={`required-${result.id}-${skill}`} className="required-skill-tag">
                                    {skill}
                                  </span>
                                ))
                              ) : (
                                <span className="muted-text">저장된 요구 기술이 없습니다.</span>
                              )}
                            </div>
                          </div>

                          <div className="match-skill-row">
                            <span className="match-skill-label">부족 기술</span>
                            <div className="chip-list">
                              {missingSkills.length > 0 ? (
                                missingSkills.map((skill) => (
                                  <span key={`missing-${result.id}-${skill}`} className="missing-skill-tag">
                                    {skill}
                                  </span>
                                ))
                              ) : (
                                <span className="muted-text">주요 부족 기술이 없습니다.</span>
                              )}
                            </div>
                          </div>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>
          ) : null}

          {activeTab === 'roadmap' ? (
            <section className="content-panel">
              <div className="panel-heading">
                <div>
                  <h2>학습 로드맵</h2>
                  <p>스킬 갭 분석 결과를 바탕으로 생성된 학습 계획입니다.</p>
                </div>
                <span>{roadmaps.length}건</span>
              </div>

              {roadmaps.length === 0 ? (
                <p className="empty-message">아직 생성된 학습 로드맵이 없습니다.</p>
              ) : (
                <div className="card-list">
                  {roadmaps.map((map) => {
                    const parsedRoadmap = getRoadmapPayload(map);
                    const roadmapSkills = getRoadmapSkills(map);

                    return (
                      <article key={map.id} className="list-card">
                        <div>
                          <span className="eyebrow">로드맵</span>
                          <h3>{map.targetJob || parsedRoadmap.targetJob || '학습 로드맵'}</h3>
                          <p className="record-date">생성 일시: {formatDate(map.createdAt)}</p>
                          <p className="list-card-summary">
                            {parsedRoadmap.summary || '매칭 분석 결과를 기반으로 생성된 학습 계획입니다.'}
                          </p>

                          {roadmapSkills.length > 0 ? (
                            <div className="chip-list">
                              {roadmapSkills.map((skill) => (
                                <span key={`roadmap-${map.id}-${skill}`} className="roadmap-skill-tag">
                                  {skill}
                                </span>
                              ))}
                            </div>
                          ) : null}
                        </div>

                        <div className="row-actions">
                          <button
                            type="button"
                            className="report-btn roadmap-btn"
                            onClick={() => navigate('/roadmap', {
                              state: {
                                roadmapData: map,
                                nickname: profile?.nickname,
                              },
                            })}
                          >
                            로드맵 보기
                          </button>
                          <button
                            type="button"
                            className="delete-btn"
                            onClick={() => handleDeleteRoadmap(map.id)}
                          >
                            삭제
                          </button>
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </section>
          ) : null}

          {activeTab === 'favorites' ? (
            <section className="content-panel">
              <div className="panel-heading">
                <div>
                  <h2>즐겨찾기 공고</h2>
                  <p>관심 있는 추천 공고를 저장하고 다시 확인할 수 있습니다.</p>
                </div>
                <span>{favoriteJobs.length}건</span>
              </div>

              {favoriteJobs.length === 0 ? (
                <div className="empty-action-box">
                  <p>아직 즐겨찾기한 추천 공고가 없습니다.</p>
                  <button type="button" className="secondary-btn" onClick={() => navigate('/jobs')}>
                    추천 공고 보러가기
                  </button>
                </div>
              ) : (
                <div className="card-list">
                  {favoriteJobs.map((job) => (
                    <article key={job.id} className="list-card job-card">
                      <div>
                        <span className="eyebrow">{job.companyName || job.company || '회사명 미등록'}</span>
                        <h3>{job.title || job.position || '추천 공고'}</h3>
                        <p className="record-date">
                          {job.savedAt ? `저장 일시: ${formatDate(job.savedAt)}` : '저장한 추천 공고'}
                        </p>
                        {job.summary ? <p className="list-card-summary">{job.summary}</p> : null}

                        <div className="chip-list">
                          {parseStoredList(job.requiredSkills || job.skills || job.keywords).slice(0, 6).map((skill) => (
                            <span key={`favorite-${job.id}-${skill}`} className="required-skill-tag">
                              {skill}
                            </span>
                          ))}
                          {parseStoredList(job.missingSkills).slice(0, 3).map((skill) => (
                            <span key={`favorite-missing-${job.id}-${skill}`} className="missing-skill-tag">
                              {skill}
                            </span>
                          ))}
                        </div>
                      </div>

                      <div className="row-actions">
                        {job.url || job.applyUrl ? (
                          <button
                            type="button"
                            className="report-btn"
                            onClick={() => window.open(job.url || job.applyUrl, '_blank', 'noopener,noreferrer')}
                          >
                            공고 보기
                          </button>
                        ) : (
                          <button type="button" className="report-btn" onClick={() => navigate('/jobs')}>
                            공고 보기
                          </button>
                        )}
                        <button
                          type="button"
                          className="delete-btn"
                          onClick={() => handleRemoveFavoriteJob(job.id)}
                        >
                          해제
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>
          ) : null}

          {activeTab === 'profile' ? (
            <section className="content-panel">
              <div className="panel-heading">
                <div>
                  <h2>개인정보 수정</h2>
                  <p>카카오 연동 계정의 기본 표시 정보를 관리합니다.</p>
                </div>
              </div>

              <div className="settings-grid">
                <article className="settings-card">
                  <h3>프로필 이미지</h3>
                  <p>마이페이지에서 표시될 프로필 이미지를 설정할 수 있습니다.</p>

                  <div className="profile-image-setting">
                    {profileImage ? (
                      <img src={profileImage} alt="프로필 미리보기" className="profile-image-preview" />
                    ) : (
                      <div className="profile-image-placeholder">
                        {(profile?.nickname || 'U').slice(0, 1)}
                      </div>
                    )}

                    <div className="profile-image-actions">
                      <label className="secondary-btn image-upload-label" htmlFor="profileImage">
                        이미지 선택
                      </label>
                      <input
                        id="profileImage"
                        type="file"
                        accept="image/*"
                        onChange={handleProfileImageChange}
                        hidden
                      />
                      <button type="button" className="delete-btn" onClick={handleResetProfileImage}>
                        기본 이미지로 변경
                      </button>
                    </div>
                  </div>
                </article>

                <article className="settings-card">
                  <h3>닉네임 설정</h3>
                  <p>마이페이지와 분석 결과 화면에서 표시될 닉네임입니다.</p>

                  <label className="input-label" htmlFor="nickname">
                    닉네임
                  </label>
                  <div className="nickname-row">
                    <input
                      id="nickname"
                      type="text"
                      value={nicknameInput}
                      maxLength={20}
                      onChange={(event) => setNicknameInput(event.target.value)}
                      placeholder="닉네임을 입력하세요"
                    />
                    <button type="button" className="report-btn" onClick={handleSaveNickname}>
                      저장
                    </button>
                  </div>
                </article>

                <article className="settings-card danger-card">
                  <h3>회원 탈퇴</h3>
                  <p>
                    카카오 연동 계정은 ResumeMatch에서 아이디와 비밀번호를 직접 변경할 수 없습니다.
                    회원 탈퇴 시 분석 기록과 학습 로드맵 데이터가 삭제될 수 있습니다.
                  </p>

                  <button type="button" className="danger-btn" onClick={handleWithdraw}>
                    회원 탈퇴
                  </button>
                </article>
              </div>
            </section>
          ) : null}
        </section>
      </section>
    </main>
  );
};

export default MyPage;
