import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  Check,
  Clock,
  ExternalLink,
  Map,
  Play,
  X,
} from 'lucide-react';
import './RoadmapPage.css';

const API_BASE_URL = 'http://localhost:8080/api/roadmap/recommend';

const fallbackCourses = [
  {
    id: 'sample-1',
    step: '1단계',
    title: 'SQL 기본 쿼리 학습',
    provider: 'YouTube',
    time: '45분',
    tags: ['SQL', '기초', '쿼리'],
    url: '#',
    level: '입문',
  },
  {
    id: 'sample-2',
    step: '2단계',
    title: 'REST API와 JPA 실습',
    provider: 'YouTube',
    time: '1시간 10분',
    tags: ['REST API', 'JPA', 'Spring'],
    url: '#',
    level: '중급',
  },
  {
    id: 'sample-3',
    step: '3단계',
    title: '데이터 모델링과 검증 경험 쌓기',
    provider: 'YouTube',
    time: '55분',
    tags: ['MySQL', '모델링', '검증'],
    url: '#',
    level: '중급',
  },
];

const toArray = (value) => {
  if (!value) return [];
  if (Array.isArray(value)) return value;
  if (typeof value === 'string') {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean);
  }
  return [];
};

const normalizeCourses = (value) => {
  const list = toArray(value);

  return list.map((course, index) => {
    if (typeof course === 'string') {
      return {
        id: `course-${index}`,
        step: `${index + 1}단계`,
        title: course,
        provider: '추천 강의',
        time: '학습 시간 확인 필요',
        level: '입문',
        tags: [course],
        url: '#',
      };
    }

    return {
      id: course.id || course.videoId || `course-${index}`,
      step: course.step || `${index + 1}단계`,
      title: course.title || course.name || `추천 학습 ${index + 1}`,
      provider: course.provider || course.channel || 'YouTube',
      time: course.time || course.duration || '학습 시간 확인 필요',
      level: course.level || course.difficulty || '입문',
      tags: toArray(course.tags || course.keywords).slice(0, 4),
      url: course.url || course.link || '#',
    };
  });
};

const normalizeLearningSteps = (value) =>
  toArray(value).map((step, index) => {
    if (typeof step === 'string') {
      return {
        id: `learning-step-${index}`,
        type: 'concept',
        title: step,
        description: '',
        expectedOutput: '',
      };
    }

    return {
      id: step.id || `learning-step-${index}`,
      type: step.type || 'concept',
      title: step.title || `학습 가이드 ${index + 1}`,
      description: step.description || '',
      expectedOutput: step.expectedOutput || '',
    };
  });

const normalizePracticeProject = (value) => {
  if (!value || typeof value !== 'object') return null;

  return {
    title: value.title || '',
    goal: value.goal || '',
    requirements: toArray(value.requirements),
    completionDefinition: value.completionDefinition || '',
    resumeBullet: value.resumeBullet || '',
  };
};

const normalizeWeeks = (value) =>
  toArray(value).map((week, index) => {
    const focusSkills = toArray(week.focusSkills || week.tags);
    const courses = normalizeCourses(week.recommendedCourses);

    return {
      week: Number(week.week) || index + 1,
      title: week.title || focusSkills[0] || `추천 학습 ${index + 1}`,
      summary: week.summary || week.goal || '',
      tags: focusSkills.length > 0 ? focusSkills : ['실습', '정리'],
      time: week.time || '약 6시간',
      tasks: toArray(week.tasks),
      completionCriteria: toArray(week.completionCriteria),
      selfCheckItems: toArray(week.selfCheckItems),
      learningSteps: normalizeLearningSteps(week.learningSteps),
      practiceProject: normalizePracticeProject(week.practiceProject),
      recommendedSearchQueries: toArray(week.recommendedSearchQueries),
      recommendedCourses: courses,
    };
  });

const getLearningStepTypeLabel = (type) => {
  if (type === 'practice') return '실습';
  if (type === 'resume') return '이력서';
  return '개념';
};

const hasValidUrl = (url) => Boolean(url && url !== '#');

const createGoogleSearchUrl = (query) =>
  `https://www.google.com/search?q=${encodeURIComponent(query)}`;

const buildWeeks = (skills, courses) => {
  const skillList = skills.length > 0 ? skills : ['기초 다지기', '핵심 역량 정제', '실전 프로젝트'];
  const labels = ['기초 다지기', '핵심 역량 정제', '데이터 분석 기초', '시각화와 인사이트', '실전 프로젝트'];

  return Array.from({ length: 5 }, (_, index) => {
    const skill = skillList[index] || labels[index];
    const course = courses[index] || courses[courses.length - 1];

    return {
      week: index + 1,
      title: index === 0 ? '기초 다지기' : skill,
      summary:
        index === 1
          ? `${skill}를 중심으로 부족 역량을 보완하는 주차입니다.`
          : `${skill} 학습을 통해 직무 요구사항과 이력서 역량을 연결합니다.`,
      tags: course?.tags?.length ? course.tags : [skill, '실습', '정리'],
      time: course?.time || '약 6시간',
    };
  });
};

const buildWeekTasks = (week) => {
  if (week.tasks?.length) {
    return week.tasks.map((task, index) => ({
      id: `week-${week.week}-task-${index}`,
      title: typeof task === 'string' ? task : task.title || `학습 할 일 ${index + 1}`,
      desc: typeof task === 'string' ? '' : task.desc || task.description || '',
    }));
  }

  return [
    {
      id: `week-${week.week}-concept`,
      title: `${week.title} 개념 학습`,
      desc: '핵심 개념과 JD 요구사항을 연결해서 정리합니다.',
    },
    {
      id: `week-${week.week}-summary`,
      title: '핵심 개념 정리',
      desc: '학습 내용을 짧은 노트와 예제로 정리합니다.',
    },
    {
      id: `week-${week.week}-resume`,
      title: '학습 내용 이력서 반영',
      desc: '프로젝트 경험 문장으로 바꿔 이력서에 반영합니다.',
    },
  ];
};

const initialSelfCheckItems = [
  {
    id: 'jpa-entity-table',
    label: 'JPA 엔티티와 테이블의 관계를 설명할 수 있다.',
  },
  {
    id: 'jpa-relation-design',
    label: '일대다/다대일 연관관계를 엔티티로 설계할 수 있다.',
  },
  {
    id: 'mysql-entity-class',
    label: 'MySQL 테이블 구조를 기반으로 엔티티 클래스를 작성할 수 있다.',
  },
  {
    id: 'rest-api-database',
    label: 'REST API에서 DB 데이터를 조회하고 저장할 수 있다.',
  },
  {
    id: 'resume-project-sentence',
    label: '학습 내용을 이력서 프로젝트 경험 문장으로 정리할 수 있다.',
  },
];

const createSelfCheckItems = () =>
  initialSelfCheckItems.map((item) => ({
    ...item,
    checked: false,
  }));

const createInitialTaskState = (weeks) =>
  weeks.reduce((acc, week) => {
    buildWeekTasks(week).forEach((task) => {
      acc[task.id] = false;
    });
    return acc;
  }, {});

const getCurrentWeekNumber = (weeks, taskState) => {
  const incompleteWeek = weeks.find((week) =>
    buildWeekTasks(week).some((task) => !taskState[task.id])
  );

  return incompleteWeek?.week || weeks[weeks.length - 1]?.week || 1;
};

const RoadmapPage = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const {
    memberId: stateMemberId,
    missingSkills,
    roadmapData,
    targetJob,
    learningDirection,
    requiredSkills,
    preferredSkills,
    ownedSkills,
    matchedSkills,
    jobSummary,
    analysis,
  } = location.state || {};
  const memberId = stateMemberId || localStorage.getItem('memberId');
  const skillKeywords = useMemo(() => toArray(missingSkills), [missingSkills]);

  const [courses, setCourses] = useState([]);
  const [apiWeeks, setApiWeeks] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [roadmapLoadFailed, setRoadmapLoadFailed] = useState(false);
  const [detailModal, setDetailModal] = useState(null);
  const [checkedTasks, setCheckedTasks] = useState({});
  const [selectedWeek, setSelectedWeek] = useState(1);
  const [selfCheckItems, setSelfCheckItems] = useState(createSelfCheckItems);
  const [isSelfCheckModalOpen, setIsSelfCheckModalOpen] = useState(false);
  const [savedSelfCheckItems, setSavedSelfCheckItems] = useState(createSelfCheckItems);

  useEffect(() => {
    if (roadmapData) {
      try {
        const parsedCourses =
          typeof roadmapData.content === 'string'
            ? JSON.parse(roadmapData.content)
            : roadmapData.content;

        if (parsedCourses?.weeks) {
          const normalizedWeeks = normalizeWeeks(parsedCourses.weeks);
          const initialTaskState = createInitialTaskState(normalizedWeeks);

          setApiWeeks(normalizedWeeks);
          setCheckedTasks(initialTaskState);
          setSelectedWeek(getCurrentWeekNumber(normalizedWeeks, initialTaskState));
          setCourses(normalizeCourses(parsedCourses.recommendedCourses));
        } else {
          setApiWeeks([]);
          setCourses(normalizeCourses(parsedCourses));
        }
      } catch (error) {
        console.error('로드맵 데이터 파싱 에러:', error);
        setApiWeeks([]);
        setCourses([]);
        setRoadmapLoadFailed(true);
      } finally {
        setIsLoading(false);
      }
      return;
    }

    const fetchRoadmap = async () => {
      if (skillKeywords.length === 0) {
        setIsLoading(false);
        return;
      }

      if (!memberId) {
        alert('로그인이 필요합니다.');
        navigate('/login');
        setIsLoading(false);
        return;
      }

      setIsLoading(true);
      setRoadmapLoadFailed(false);

      try {
        const response = await axios.post(API_BASE_URL, {
          memberId: Number(memberId),
          keywords: skillKeywords,
          missingSkills: skillKeywords,
          targetJob,
          learningDirection,
          requiredSkills: toArray(requiredSkills),
          preferredSkills: toArray(preferredSkills),
          ownedSkills: toArray(ownedSkills),
          matchedSkills: toArray(matchedSkills),
          jobSummary,
          analysis,
        });

        if (response.data?.weeks) {
          const normalizedWeeks = normalizeWeeks(response.data.weeks);
          const initialTaskState = createInitialTaskState(normalizedWeeks);

          setApiWeeks(normalizedWeeks);
          setCheckedTasks(initialTaskState);
          setSelectedWeek(getCurrentWeekNumber(normalizedWeeks, initialTaskState));
          setCourses(normalizeCourses(response.data.recommendedCourses));
        } else {
          setApiWeeks([]);
          setCourses(normalizeCourses(response.data));
        }
      } catch (error) {
        console.error('로드맵 API 호출 오류:', error);
        alert('로드맵을 생성하는 중 문제가 발생했습니다. 백엔드 서버를 확인해주세요.');
        setApiWeeks([]);
        setCourses([]);
        setRoadmapLoadFailed(true);
      } finally {
        setIsLoading(false);
      }
    };

    fetchRoadmap();
  }, [analysis, jobSummary, learningDirection, matchedSkills, memberId, navigate, ownedSkills, preferredSkills, requiredSkills, roadmapData, skillKeywords, targetJob]);

  const displayCourses = courses.length > 0 ? courses : fallbackCourses;
  const fallbackWeeks = useMemo(() => buildWeeks(skillKeywords, displayCourses), [skillKeywords, displayCourses]);
  const weeks = apiWeeks.length > 0 ? apiWeeks : fallbackWeeks;
  const currentWeek = getCurrentWeekNumber(weeks, checkedTasks);
  const selectedWeekData = weeks.find((week) => week.week === selectedWeek) || weeks[0];
  const selectedWeekTasks = buildWeekTasks(selectedWeekData);
  const selectedLearningSteps = selectedWeekData.learningSteps || [];
  const selectedPracticeProject = selectedWeekData.practiceProject;
  const selectedSearchQueries = selectedWeekData.recommendedSearchQueries || [];
  const selectedWeekCourses =
    selectedWeekData.recommendedCourses?.length > 0
      ? selectedWeekData.recommendedCourses
      : courses;
  const hasPracticeProject =
    selectedPracticeProject &&
    (
      selectedPracticeProject.title ||
      selectedPracticeProject.goal ||
      selectedPracticeProject.requirements?.length ||
      selectedPracticeProject.completionDefinition ||
      selectedPracticeProject.resumeBullet
    );
  const hasLearningResources =
    selectedLearningSteps.length > 0 ||
    hasPracticeProject ||
    selectedSearchQueries.length > 0 ||
    selectedWeekCourses.length > 0;
  const isFutureSelected = selectedWeek > currentWeek;
  const weekSkills =
    toArray(selectedWeekData?.focusSkills).length > 0
      ? toArray(selectedWeekData.focusSkills)
      : toArray(selectedWeekData?.tags).length > 0
        ? toArray(selectedWeekData.tags)
        : skillKeywords;
  const primarySkill = weekSkills[0] || selectedWeekData.title;
  const overviewSkills = weekSkills.slice(0, 2);
  const reasonText = '채용공고의 핵심 요구사항과 연결된 보완 항목입니다.';
  const taskItems = selectedWeekTasks.map((task) => {
    const isChecked = Boolean(checkedTasks[task.id]);
    const status = isChecked ? '완료' : '할 일';

    return {
      ...task,
      isChecked,
      status,
      tone: isChecked ? 'done' : 'todo',
    };
  });
  const completedSelfCheckCount = savedSelfCheckItems.filter((item) => item.checked).length;
  const draftSelfCheckCount = selfCheckItems.filter((item) => item.checked).length;
  const selfCheckTotalCount = savedSelfCheckItems.length;
  const selfCheckMessage =
    draftSelfCheckCount >= 4
      ? '이번 주 학습 완료 기준을 충족했습니다.'
      : '아직 보완할 항목이 남아 있습니다.';

  useEffect(() => {
    if (isLoading || weeks.length === 0 || Object.keys(checkedTasks).length > 0) return;

    const initialTaskState = createInitialTaskState(weeks);
    const initialCurrentWeek = getCurrentWeekNumber(weeks, initialTaskState);

    setCheckedTasks(initialTaskState);
    setSelectedWeek(initialCurrentWeek);
  }, [checkedTasks, isLoading, weeks]);

  const handleTaskToggle = (taskId) => {
    if (isFutureSelected) return;

    setCheckedTasks((prev) => ({
      ...prev,
      [taskId]: !prev[taskId],
    }));
  };

  const openSelfCheckModal = () => {
    setSelfCheckItems(savedSelfCheckItems);
    setIsSelfCheckModalOpen(true);
  };

  const handleSelfCheckToggle = (itemId) => {
    setSelfCheckItems((prev) =>
      prev.map((item) =>
        item.id === itemId
          ? { ...item, checked: !item.checked }
          : item
      )
    );
  };

  const handleSelfCheckSave = () => {
    setSavedSelfCheckItems(selfCheckItems);
    setIsSelfCheckModalOpen(false);
  };

  useEffect(() => {
    if (weeks.length === 0 || isFutureSelected) return;

    const nextCurrentWeek = getCurrentWeekNumber(weeks, checkedTasks);
    const selectedTasksDone = selectedWeekTasks.every((task) => checkedTasks[task.id]);

    if (selectedWeek === currentWeek && selectedTasksDone && nextCurrentWeek !== selectedWeek) {
      setSelectedWeek(nextCurrentWeek);
    }
  }, [checkedTasks, currentWeek, isFutureSelected, selectedWeek, selectedWeekTasks, weeks]);

  if (!isLoading && courses.length === 0 && skillKeywords.length === 0 && !roadmapData) {
    return (
      <main className="roadmap-empty">
        <section className="roadmap-empty-card">
          <div className="roadmap-empty-visual" aria-hidden="true">
            <Map size={86} />
          </div>
          <h2>아직 학습 로드맵이 없습니다</h2>
          <p>
            스킬 갭 분석 결과를 바탕으로
            <br />
            부족한 역량에 맞춘 주차별 학습 계획을 생성할 수 있습니다.
          </p>
          <div className="roadmap-empty-badges" aria-label="로드맵에서 확인할 수 있는 항목">
            <span>주차별 로드맵</span>
            <span>이번 주 할 일</span>
            <span>추천 강의</span>
            <span>자가 점검</span>
          </div>
          <div className="roadmap-empty-actions">
            <button type="button" className="primary" onClick={() => navigate('/match')}>
              이력서 매칭 시작하기
            </button>
            <button type="button" className="secondary" onClick={() => navigate('/')}>
              메인으로 돌아가기
            </button>
          </div>
        </section>
      </main>
    );
  }

  if (isLoading) {
    return (
      <main className="roadmap-page">
        <section className="roadmap-loading">
          <div className="loading-dot" />
          <strong>AI가 맞춤 학습 로드맵과 추천 강의를 생성 중입니다.</strong>
        </section>
      </main>
    );
  }

  return (
    <main className="roadmap-page">
      {roadmapLoadFailed ? (
        <p className="roadmap-fallback-notice">기본 로드맵을 표시합니다.</p>
      ) : null}
      <section className="week-roadmap" aria-label="주차별 로드맵">
        <div className="week-roadmap-header">
          <h2>주차별 로드맵</h2>
        </div>
        <div className="week-timeline">
          {weeks.map((week) => {
            const weekTasks = buildWeekTasks(week);
            const completed = weekTasks.every((task) => checkedTasks[task.id]);
            const started = weekTasks.some((task) => checkedTasks[task.id]);
            const status = completed ? 'done' : week.week === currentWeek || started ? 'active' : 'waiting';
            const isSelected = week.week === selectedWeek;

            return (
            <button
              type="button"
              className={`week-node ${status} ${isSelected ? 'selected' : ''}`}
              key={week.week}
              onClick={() => setSelectedWeek(week.week)}
            >
              <div className="week-dot">
                {status === 'done' ? <Check size={16} /> : week.week}
              </div>
              <strong>{week.week}주차</strong>
              <span>{week.title}</span>
              <em>{status === 'done' ? '완료' : status === 'active' ? '진행중' : '대기'}</em>
            </button>
            );
          })}
        </div>
      </section>

      {isLoading ? (
        <section className="roadmap-loading">
          <div className="loading-dot" />
          <strong>AI가 최적의 유튜브 강의를 탐색 중입니다.</strong>
        </section>
      ) : (
        <>
          <section className="roadmap-main-grid">
            <article className="roadmap-card week-card">
              <h2>현재 단계 개요</h2>
              <div className="overview-rows">
                <p>
                  <strong>목표:</strong>
                  <span>{selectedWeekData.title} 보완</span>
                </p>
                <p>
                  <strong>핵심 스킬:</strong>
                  <span>{overviewSkills.join(', ')}</span>
                </p>
                <p>
                  <strong>필요한 이유:</strong>
                  <span>{reasonText}</span>
                </p>
              </div>
              <button
                type="button"
                className="roadmap-card-action"
                onClick={() => setDetailModal('overview')}
              >
                상세보기
              </button>
            </article>

            <article className="roadmap-card task-card">
              <h2>이번 주 할 일</h2>
              <div className="task-list-wrap">
                <div className="task-list">
                  {taskItems.map((task) => (
                    <div className={`task-item ${task.tone} ${isFutureSelected ? 'locked' : ''}`} key={task.title}>
                      <button
                        type="button"
                        className="task-check"
                        aria-label={`${task.title} 완료 상태 변경`}
                        aria-pressed={task.isChecked}
                        disabled={isFutureSelected}
                        onClick={() => handleTaskToggle(task.id)}
                      />
                      <div>
                        <strong>{task.title}</strong>
                      </div>
                      <em>{task.status}</em>
                    </div>
                  ))}
                </div>
                {isFutureSelected ? (
                  <div className="locked-week-notice">
                    <span>이전 주차를 완료하면 진행할 수 있습니다.</span>
                  </div>
                ) : null}
              </div>
              <button
                type="button"
                className="roadmap-card-action"
                onClick={() => setDetailModal('tasks')}
              >
                상세보기
              </button>
            </article>
          </section>

          <section className="roadmap-resource-grid">
            <article className="roadmap-card curriculum-card">
              <h2>완료 기준</h2>
              <div className="curriculum-box">
                <div className="curriculum-icon">
                  <Check size={24} />
                </div>
                <div className="completion-content">
                  <strong>이번 주 학습 완료 기준</strong>
                  <p>이번 주 학습을 마치면 아래 내용을 설명하거나 구현할 수 있어야 합니다.</p>
                  <ul className="completion-list">
                    <li>JPA 엔티티 관계 이해</li>
                    <li>MySQL 테이블 구조 설계</li>
                    <li>REST API와 DB 연동 흐름 설명</li>
                  </ul>
                </div>
                <button type="button" onClick={openSelfCheckModal}>자가 점검하기</button>
              </div>
              <p className="self-check-summary">
                자가 점검 {completedSelfCheckCount} / {selfCheckTotalCount} 완료
              </p>
            </article>

            <article className="roadmap-card lecture-card">
              <h2>추천 학습 자료</h2>
              {selectedLearningSteps.length > 0 ? (
                <>
                  <section className="learning-resource-section">
                    <h3>이번 주 학습 가이드</h3>
                    <div className="learning-step-list learning-step-preview">
                      {selectedLearningSteps.slice(0, 3).map((step) => (
                        <article className="learning-step-item" key={step.id}>
                          <span>{getLearningStepTypeLabel(step.type)}</span>
                          <div>
                            <strong>{step.title}</strong>
                          </div>
                        </article>
                      ))}
                    </div>
                  </section>
                  <button
                    type="button"
                    className="roadmap-card-action"
                    onClick={() => setDetailModal('resources')}
                  >
                    상세보기
                  </button>
                </>
              ) : (
                <p className="learning-resource-empty">이번 주 학습 자료가 없습니다.</p>
              )}
            </article>
          </section>
        </>
      )}

      {isSelfCheckModalOpen ? (
        <div className="roadmap-modal-backdrop" role="presentation" onClick={() => setIsSelfCheckModalOpen(false)}>
          <section
            className="roadmap-modal self-check-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="self-check-modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="roadmap-modal-header">
              <div>
                <h2 id="self-check-modal-title">이번 주 학습 자가 점검</h2>
                <p>학습 완료 기준을 스스로 확인하는 항목입니다.</p>
              </div>
              <button type="button" onClick={() => setIsSelfCheckModalOpen(false)} aria-label="자가 점검 닫기">
                <X size={22} />
              </button>
            </div>
            <div className="roadmap-modal-body">
              <div className="self-check-list">
                {selfCheckItems.map((item) => (
                  <label className="self-check-item" key={item.id}>
                    <input
                      type="checkbox"
                      checked={item.checked}
                      onChange={() => handleSelfCheckToggle(item.id)}
                    />
                    <span>{item.label}</span>
                  </label>
                ))}
              </div>
              <div className={`self-check-status ${draftSelfCheckCount >= 4 ? 'complete' : 'incomplete'}`}>
                <strong>{draftSelfCheckCount} / {selfCheckItems.length} 완료</strong>
                <p>{selfCheckMessage}</p>
              </div>
            </div>
            <div className="self-check-actions">
              <button type="button" className="self-check-secondary" onClick={() => setIsSelfCheckModalOpen(false)}>
                닫기
              </button>
              <button type="button" className="self-check-primary" onClick={handleSelfCheckSave}>
                저장하기
              </button>
            </div>
          </section>
        </div>
      ) : null}

      {detailModal ? (
        <div className="roadmap-modal-backdrop" role="presentation" onClick={() => setDetailModal(null)}>
          <section
            className="roadmap-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="roadmap-modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="roadmap-modal-header">
              <div>
                <h2 id="roadmap-modal-title">
                  {detailModal === 'overview'
                    ? '현재 단계 상세'
                    : detailModal === 'resources'
                      ? '추천 학습 자료 상세'
                      : '이번 주 할 일 상세'}
                </h2>
                <p>
                  {detailModal === 'overview'
                    ? `${selectedWeekData.week}주차 학습 목표와 핵심 스킬입니다.`
                    : detailModal === 'resources'
                      ? `${selectedWeekData.week}주차 추천 학습 자료입니다.`
                      : `${selectedWeekData.week}주차에 완료해야 할 학습 체크리스트입니다.`}
                </p>
              </div>
              <button type="button" onClick={() => setDetailModal(null)} aria-label="로드맵 상세 닫기">
                <X size={22} />
              </button>
            </div>

            {detailModal === 'overview' ? (
              <div className="roadmap-modal-body">
                <article className="roadmap-detail-block">
                  <span>목표</span>
                  <strong>{selectedWeekData.title} 역량 보완</strong>
                  <p>{selectedWeekData.summary}</p>
                </article>
                <article className="roadmap-detail-block">
                  <span>핵심 스킬</span>
                  <div className="roadmap-tags">
                    <em>{primarySkill}</em>
                    <em>AI 추천</em>
                    {selectedWeekData.tags
                      .filter((tag) => tag !== primarySkill)
                      .map((tag) => (
                        <em key={tag}>{tag}</em>
                      ))}
                  </div>
                </article>
                <article className="roadmap-detail-block">
                  <span>예상 소요 시간</span>
                  <strong>{selectedWeekData.time}</strong>
                  <p>추천 강의와 실습, 이력서 반영까지 포함한 예상 학습 시간입니다.</p>
                </article>
                <article className="roadmap-detail-block">
                  <span>필요한 이유</span>
                  <strong>JD 요구사항과 이력서 경험을 연결하는 구간입니다.</strong>
                  <p>{reasonText}</p>
                </article>
              </div>
            ) : detailModal === 'resources' ? (
              <div className="roadmap-modal-body">
                {selectedLearningSteps.length > 0 ? (
                  <article className="roadmap-detail-block">
                    <span>이번 주 학습 가이드</span>
                    <div className="learning-step-list">
                      {selectedLearningSteps.map((step) => (
                        <article className="learning-step-item" key={step.id}>
                          <span>{getLearningStepTypeLabel(step.type)}</span>
                          <div>
                            <strong>{step.title}</strong>
                            {step.description ? <p>{step.description}</p> : null}
                            {step.expectedOutput ? <small>{step.expectedOutput}</small> : null}
                          </div>
                        </article>
                      ))}
                    </div>
                  </article>
                ) : null}

                {hasPracticeProject ? (
                  <article className="roadmap-detail-block">
                    <span>실습 프로젝트</span>
                    <div className="practice-project-box">
                      {selectedPracticeProject.title ? <strong>{selectedPracticeProject.title}</strong> : null}
                      {selectedPracticeProject.goal ? <p>{selectedPracticeProject.goal}</p> : null}
                      {selectedPracticeProject.requirements?.length > 0 ? (
                        <ul>
                          {selectedPracticeProject.requirements.map((requirement) => (
                            <li key={requirement}>{requirement}</li>
                          ))}
                        </ul>
                      ) : null}
                      {selectedPracticeProject.completionDefinition ? (
                        <small>{selectedPracticeProject.completionDefinition}</small>
                      ) : null}
                      {selectedPracticeProject.resumeBullet ? (
                        <em>{selectedPracticeProject.resumeBullet}</em>
                      ) : null}
                    </div>
                  </article>
                ) : null}

                {selectedSearchQueries.length > 0 ? (
                  <article className="roadmap-detail-block">
                    <span>참고 검색어</span>
                    <div className="search-query-list">
                      {selectedSearchQueries.map((query) => (
                        <a
                          key={query}
                          href={createGoogleSearchUrl(query)}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          {query}
                          <ExternalLink size={12} />
                        </a>
                      ))}
                    </div>
                  </article>
                ) : null}

                {selectedWeekCourses.length > 0 ? (
                  <article className="roadmap-detail-block">
                    <span>보조 강의</span>
                    <div className="lecture-list">
                      {selectedWeekCourses.map((course) => (
                        <div className="lecture-item" key={course.id}>
                          <div className="play-icon">
                            <Play size={14} fill="currentColor" />
                          </div>
                          <div>
                            <strong>{course.title}</strong>
                            <small>
                              <Clock size={12} />
                              {course.level} · {course.time} · {course.provider}
                            </small>
                          </div>
                          {hasValidUrl(course.url) ? (
                            <a href={course.url} target="_blank" rel="noopener noreferrer">
                              {course.provider === 'YouTube' ? '강의 보기' : '자료 보기'}
                              <ExternalLink size={13} />
                            </a>
                          ) : null}
                        </div>
                      ))}
                    </div>
                  </article>
                ) : null}
              </div>
            ) : (
              <div className="roadmap-modal-body">
                {taskItems.map((task) => {
                  const shouldShowDesc =
                    task.desc &&
                    task.desc.trim() !== task.title?.trim();

                  return (
                    <article className="roadmap-detail-block task-detail" key={task.title}>
                      <div>
                        <span>{task.status}</span>
                        <strong>{task.title}</strong>
                      </div>
                      {shouldShowDesc ? <p>{task.desc}</p> : null}
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      ) : null}
    </main>
  );
};

export default RoadmapPage;
