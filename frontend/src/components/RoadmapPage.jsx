import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import axios from 'axios';
import {
  BookOpen,
  Check,
  Clock,
  ExternalLink,
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

const buildWeekTasks = (week) => [
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

const createInitialTaskState = (weeks) =>
  weeks.reduce((acc, week) => {
    buildWeekTasks(week).forEach((task, taskIndex) => {
      acc[task.id] = week.week === 1 || (week.week === 2 && taskIndex === 1);
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

  const { missingSkills, roadmapData, nickname } = location.state || {};
  const userName = nickname || '지원자';
  const skillKeywords = useMemo(() => toArray(missingSkills), [missingSkills]);

  const [courses, setCourses] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [detailModal, setDetailModal] = useState(null);
  const [checkedTasks, setCheckedTasks] = useState({});
  const [selectedWeek, setSelectedWeek] = useState(1);

  useEffect(() => {
    if (roadmapData) {
      try {
        const parsedCourses =
          typeof roadmapData.content === 'string'
            ? JSON.parse(roadmapData.content)
            : roadmapData.content;

        setCourses(normalizeCourses(parsedCourses));
      } catch (error) {
        console.error('로드맵 데이터 파싱 에러:', error);
        setCourses([]);
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

      setIsLoading(true);

      try {
        const response = await axios.post(API_BASE_URL, {
          keywords: skillKeywords,
        });

        setCourses(normalizeCourses(response.data));
      } catch (error) {
        console.error('로드맵 API 호출 오류:', error);
        alert('로드맵을 생성하는 중 문제가 발생했습니다. 백엔드 서버를 확인해주세요.');
        setCourses([]);
      } finally {
        setIsLoading(false);
      }
    };

    fetchRoadmap();
  }, [roadmapData, skillKeywords]);

  const displayCourses = courses.length > 0 ? courses : fallbackCourses;
  const weeks = useMemo(() => buildWeeks(skillKeywords, displayCourses), [skillKeywords, displayCourses]);
  const currentWeek = getCurrentWeekNumber(weeks, checkedTasks);
  const selectedWeekData = weeks.find((week) => week.week === selectedWeek) || weeks[0];
  const selectedWeekTasks = buildWeekTasks(selectedWeekData);
  const isFutureSelected = selectedWeek > currentWeek;
  const primarySkill = skillKeywords[0] || selectedWeekData.tags[0] || selectedWeekData.title;
  const overviewSkills = skillKeywords.length > 0 ? skillKeywords.slice(0, 2) : selectedWeekData.tags.slice(0, 2);
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
  const allTasks = weeks.flatMap((week) => buildWeekTasks(week));
  const completedTaskCount = allTasks.filter((task) => checkedTasks[task.id]).length;
  const progressPercent = allTasks.length > 0 ? Math.round((completedTaskCount / allTasks.length) * 100) : 0;

  useEffect(() => {
    if (weeks.length === 0 || Object.keys(checkedTasks).length > 0) return;

    const initialTaskState = createInitialTaskState(weeks);
    const initialCurrentWeek = getCurrentWeekNumber(weeks, initialTaskState);

    setCheckedTasks(initialTaskState);
    setSelectedWeek(initialCurrentWeek);
  }, [checkedTasks, weeks]);

  const handleTaskToggle = (taskId) => {
    if (isFutureSelected) return;

    setCheckedTasks((prev) => ({
      ...prev,
      [taskId]: !prev[taskId],
    }));
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
        <h2>표시할 로드맵 데이터가 없습니다.</h2>
        <button type="button" onClick={() => navigate('/')}>
          메인으로 돌아가기
        </button>
      </main>
    );
  }

  return (
    <main className="roadmap-page">
      <section className="week-roadmap" aria-label="주차별 로드맵">
        <div className="week-roadmap-header">
          <h2>주차별 로드맵</h2>
          <span>전체 진행률 {progressPercent}%</span>
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
              {isFutureSelected ? (
                <p className="locked-week-notice">이전 주차를 완료하면 진행할 수 있습니다.</p>
              ) : null}
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
              <h2>추천 커리큘럼</h2>
              <div className="curriculum-box">
                <div className="curriculum-icon">
                  <BookOpen size={24} />
                </div>
                <div>
                  <strong>{userName} 님을 위한 {selectedWeekData.title} 커리큘럼</strong>
                  <p>부족 역량과 현재 주차 목표에 맞춰 바로 따라갈 수 있는 학습 순서입니다.</p>
                  <small>총 {displayCourses.length}강 · {selectedWeekData.time} 기준</small>
                </div>
                <button type="button">자세히 보기</button>
              </div>
            </article>

            <article className="roadmap-card lecture-card">
              <h2>추천 강의</h2>
              <div className="lecture-list">
                {displayCourses.slice(0, 3).map((course) => (
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
                    <a href={course.url} target="_blank" rel="noopener noreferrer">
                      강의 보기
                      <ExternalLink size={13} />
                    </a>
                  </div>
                ))}
              </div>
            </article>
          </section>
        </>
      )}

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
                  {detailModal === 'overview' ? '현재 단계 상세' : '이번 주 할 일 상세'}
                </h2>
                <p>
                  {detailModal === 'overview'
                    ? `${selectedWeekData.week}주차 학습 목표와 핵심 스킬입니다.`
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
            ) : (
              <div className="roadmap-modal-body">
                {taskItems.map((task) => (
                  <article className="roadmap-detail-block task-detail" key={task.title}>
                    <div>
                      <span>{task.status}</span>
                      <strong>{task.title}</strong>
                    </div>
                    <p>{task.desc}</p>
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>
      ) : null}
    </main>
  );
};

export default RoadmapPage;
