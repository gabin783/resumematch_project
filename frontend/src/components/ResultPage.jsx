import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  ChevronRight,
  FileSearch,
  Map,
  X,
} from 'lucide-react';
import './ResultPage.css';

const defaultOwnedSkills = ['의사소통', '문제 해결', '데이터 분석', '협업', '기획'];
const defaultMatchedSkills = ['데이터 처리', 'Python 기반 분석'];
const defaultPartialSkills = ['머신러닝 기초', '통계 분석'];
const defaultNeedSkills = ['고급 SQL', '모델 검증 경험'];

// TODO: 배지 색상 확인이 끝나면 false로 바꾸거나 badgePreviewData와 함께 제거하세요.
const ENABLE_BADGE_PREVIEW = import.meta.env.DEV;

const badgePreviewData = {
  matchScore: 68,
  targetJob: '백엔드 개발자',
  analysis: '상태 배지 색상 확인을 위한 개발용 미리보기 데이터입니다.',
  learningDirection: 'Java와 Spring Boot 기본 문법을 먼저 학습하세요.\nREST API CRUD 프로젝트를 만들어 이력서 근거를 추가하세요.\nDocker와 Jenkins를 활용한 배포 흐름을 실습하세요.',
  requiredSkills: ['Java', 'Docker'],
  preferredSkills: ['Jenkins'],
  jobKeywords: ['JavaScript', 'React', 'REST API', 'Java', 'Docker', 'Jenkins'],
  mainTasks: [
    'Spring Boot 기반 REST API 개발',
    '데이터베이스 설계 및 성능 개선',
  ],
  ownedSkills: [
    { name: 'JavaScript', score: 90, status: '강점', evidence: '배지 색상 확인용 강점 스킬입니다.' },
    { name: 'React', score: 76, status: '보유', evidence: '배지 색상 확인용 보유 스킬입니다.' },
    { name: 'REST API', score: 60, status: '관련 경험', evidence: '배지 색상 확인용 관련 경험 스킬입니다.' },
  ],
  missingSkills: [
    { name: 'Java', priority: 'high', status: '필수 보완', evidence: '배지 색상 확인용 필수 보완 스킬입니다.' },
    { name: 'Docker', priority: 'medium', status: '우선 학습', evidence: '배지 색상 확인용 우선 학습 스킬입니다.' },
    { name: 'Jenkins', priority: 'low', status: '추가 학습', evidence: '배지 색상 확인용 추가 학습 스킬입니다.' },
  ],
};

const toSkillArray = (value) => {
  if (!value) return [];

  if (Array.isArray(value)) {
    return value
      .map((item) => {
        if (typeof item === 'string') return item.trim();
        return item?.name || item?.skill || item?.title || '';
      })
      .filter(Boolean);
  }

  if (typeof value === 'string') {
    const trimmed = value.trim();
    if (trimmed.startsWith('[')) {
      try {
        const parsed = JSON.parse(trimmed);
        if (Array.isArray(parsed)) {
          return parsed.map((item) => String(item).trim()).filter(Boolean);
        }
      } catch {
        // Fall through to delimiter based parsing for legacy comma strings.
      }
    }

    return value
      .split(/[,|\n]/)
      .map((skill) => skill.trim())
      .filter(Boolean);
  }

  return [];
};

const parseStoredGapReport = (data) => {
  if (!data?.analysis || typeof data.analysis !== 'string') {
    return null;
  }

  const trimmed = data.analysis.trim();
  if (!trimmed.startsWith('{')) {
    return null;
  }

  try {
    const parsed = JSON.parse(trimmed);
    return {
      ...parsed,
      analysis: parsed.analysis || '',
    };
  } catch {
    return null;
  }
};

const toSkillScoreArray = (value, fallbackScores = []) => {
  if (!value) return [];

  if (Array.isArray(value)) {
    return value
      .map((item, index) => {
        if (typeof item === 'string') {
          return {
            name: item.trim(),
            score: fallbackScores[index] ?? fallbackScores[fallbackScores.length - 1] ?? 60,
          reason: '',
          evidence: '',
          priority: '',
          hasScore: false,
          statusLabel: '',
          };
        }

        const hasScore = Number.isFinite(Number(item?.score));

        return {
          name: item?.name || item?.skill || item?.title || '',
          score: hasScore
            ? Math.max(0, Math.min(Number(item.score), 100))
            : fallbackScores[index] ?? fallbackScores[fallbackScores.length - 1] ?? 60,
          reason: item?.reason || '',
          evidence: item?.evidence || '',
          priority: item?.priority || '',
          hasScore,
          statusLabel: item?.statusLabel || item?.status || '',
        };
      })
      .filter((item) => item.name);
  }

  return toSkillArray(value).map((skill, index) => ({
    name: skill,
    score: fallbackScores[index] ?? fallbackScores[fallbackScores.length - 1] ?? 60,
    reason: '',
    evidence: '',
    priority: '',
    hasScore: false,
    statusLabel: '',
  }));
};

const splitTextLines = (value) => {
  if (!value || typeof value !== 'string') return [];

  return value
    .split(/\n|\. /)
    .map((line) => line.replace(/^[\s\-??d.]+/, '').trim())
    .filter(Boolean);
};

const getScore = (data) => {
  const candidates = [
    data?.matchScore,
    data?.matchingScore,
    data?.matchRate,
    data?.matchingRate,
    data?.score,
    data?.totalScore,
    data?.similarity,
  ];

  const found = candidates.find((value) => value !== undefined && value !== null && value !== '');
  const parsed = Number.parseInt(String(found).replace(/[^\d]/g, ''), 10);

  if (Number.isFinite(parsed)) {
    return Math.max(0, Math.min(parsed, 100));
  }

  return 72;
};

const getScoreLabel = (score) => {
  if (score >= 85) return '우수';
  if (score >= 70) return '보통';
  if (score >= 50) return '보완 필요';
  return '집중 보완';
};

const normalizeSkillName = (skill) => String(skill || '').trim().toLowerCase();

const hasSkill = (skills, targetSkill) => {
  const normalizedTarget = normalizeSkillName(targetSkill);
  return skills.some((skill) => normalizeSkillName(skill) === normalizedTarget);
};

const getOwnedSkillStatus = (item) => {
  if (item?.statusLabel) return item.statusLabel;
  if (!item?.hasScore) return '보유';
  if (item.score >= 85) return '강점';
  if (item.score >= 70) return '보유';
  if (item.score >= 50) return '관련 경험';
  return '관련 경험';
};

const getMissingSkillStatus = (item, requiredSkills = [], preferredSkills = []) => {
  if (item?.statusLabel) return item.statusLabel;
  const priority = String(item?.priority || '').toLowerCase();

  if (priority === 'high') return '필수 보완';
  if (priority === 'medium') return '우선 학습';
  if (priority === 'low') return '추가 학습';
  if (hasSkill(requiredSkills, item?.name)) return '필수 보완';
  if (hasSkill(preferredSkills, item?.name)) return '추가 학습';
  return '우선 학습';
};

const getSkillStatusClassName = (statusLabel) => {
  switch (statusLabel) {
    case '강점':
      return 'status-strong';
    case '보유':
      return 'status-owned';
    case '관련 경험':
      return 'status-related';
    case '필수 보완':
      return 'status-required';
    case '우선 학습':
      return 'status-priority';
    case '추가 학습':
      return 'status-extra';
    default:
      return 'status-neutral';
  }
};

const formatSkillNames = (skills) => skills.filter(Boolean).slice(0, 3).join(', ');

const buildStrengthSummary = (skills) => {
  const skillNames = formatSkillNames(skills);

  if (!skillNames) {
    return '직무와 연결되는 기본 역량이 확인됩니다. 프로젝트 경험을 통해 API 연동과 기술 스택 이해도도 일부 확인됩니다.';
  }

  return `${skillNames} 경험이 확인됩니다. 프로젝트 경험을 통해 API 연동과 기술 스택 이해도도 일부 확인됩니다.`;
};

const buildWeaknessSummary = (skills) => {
  const skillNames = formatSkillNames(skills);

  if (!skillNames) {
    return '채용공고 요구사항에 맞춘 보완 항목 점검이 필요합니다. 작은 실습 프로젝트로 이력서 근거를 추가하면 좋습니다.';
  }

  return `${skillNames} 역량 보완이 필요합니다. 작은 실습 프로젝트로 이력서 근거를 추가하면 좋습니다.`;
};

const buildProgressItems = (skills, baseScores) =>
  toSkillScoreArray(skills, baseScores).slice(0, 5);

const CardHeader = ({ title, action }) => (
  <div className="card-header">
    <div className="card-header-left" aria-hidden="true" />
    <h2 className="card-title">{title}</h2>
    <div className="card-header-action">{action}</div>
  </div>
);

const ResultPage = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const initialData = location.state?.analysisResult || location.state?.resultData;
  const [resultData, setResultData] = useState(initialData || null);
  const [detailModal, setDetailModal] = useState(null);
  const [isSummaryModalOpen, setIsSummaryModalOpen] = useState(false);

  useEffect(() => {
    const incomingData = location.state?.analysisResult || location.state?.resultData;
    if (incomingData) {
      setResultData(incomingData);
    }
  }, [location.state]);

  const report = useMemo(() => {
    const useBadgePreview =
      ENABLE_BADGE_PREVIEW && new URLSearchParams(location.search).get('mockBadges') === 'true';
    const activeResultData = resultData || (useBadgePreview ? badgePreviewData : null);
    if (!activeResultData) return null;

    const storedGapReport = parseStoredGapReport(activeResultData);
    const sourceData = storedGapReport
      ? {
          ...activeResultData,
          ...storedGapReport,
          learningDirection: storedGapReport.learningDirection || activeResultData.learningDirection,
        }
      : activeResultData;

    const missingSkillScores = toSkillScoreArray(sourceData.missingSkills, [30, 25, 35, 30, 45]);
    const matchedSkillScores = toSkillScoreArray(sourceData.matchedSkills, [90, 85, 80, 75, 70]);
    const partialSkillScores = toSkillScoreArray(sourceData.partialSkills, [60, 58, 55, 52, 50]);
    const ownedSkillScores = toSkillScoreArray(sourceData.ownedSkills, [85, 80, 75, 70, 65]);
    const missingSkills = missingSkillScores.map((item) => item.name);
    const matchedSkills =
      matchedSkillScores.length > 0
        ? matchedSkillScores.map((item) => item.name)
        : defaultMatchedSkills;
    const partialSkills =
      partialSkillScores.length > 0
        ? partialSkillScores.map((item) => item.name)
        : defaultPartialSkills;
    const needSkills = missingSkills.length > 0 ? missingSkills : defaultNeedSkills;
    const ownedSkills =
      ownedSkillScores.length > 0
        ? ownedSkillScores.map((item) => item.name)
        : toSkillArray(sourceData.resumeSkills).length > 0
          ? toSkillArray(sourceData.resumeSkills)
          : defaultOwnedSkills;

    const learningLines = splitTextLines(sourceData.learningDirection);
    const jobRequiredSkills = toSkillArray(sourceData.requiredSkills);
    const jobPreferredSkills = toSkillArray(sourceData.preferredSkills);
    const jobMainTasks = toSkillArray(sourceData.jobMainTasks || sourceData.mainTasks);
    const jobKeywords = toSkillArray(sourceData.jobKeywords || sourceData.keywords);
    const jobSummary = sourceData.jobSummary || sourceData.summary || '';
    const roadmapItems =
      learningLines.length > 0
        ? learningLines.slice(0, 3)
        : needSkills.slice(0, 3).map((skill) => `${skill} 학습`);
    const rawOwnedProgress = ownedSkillScores.length > 0
      ? ownedSkillScores.slice(0, 5)
      : buildProgressItems(ownedSkills, [85, 80, 75, 70, 65]);
    const rawMissingProgress = missingSkillScores.length > 0
      ? missingSkillScores.slice(0, 5)
      : buildProgressItems(needSkills, [30, 25, 35, 30, 45]);
    const ownedProgress = rawOwnedProgress.map((item) => ({
      ...item,
      statusLabel: getOwnedSkillStatus(item),
    }));
    const missingProgress = rawMissingProgress.map((item) => ({
      ...item,
      statusLabel: getMissingSkillStatus(item, jobRequiredSkills, jobPreferredSkills),
    }));
    const strengthSkills =
      matchedSkills.length > 0
        ? matchedSkills
        : ownedProgress.map((item) => item.name);
    const weaknessSkills = needSkills.length > 0
      ? needSkills
      : missingProgress.map((item) => item.name);
    const strengthSummary = buildStrengthSummary(strengthSkills);
    const weaknessSummary = buildWeaknessSummary(weaknessSkills);

    return {
      score: getScore(sourceData),
      targetJob: sourceData.targetJob || sourceData.jobTitle || sourceData.position || '데이터 분석가',
      analysis:
        sourceData.analysis ||
        '이력서와 채용공고를 비교한 결과, 일부 핵심 역량은 잘 맞지만 추가 보완이 필요한 스킬이 있습니다.',
      learningDirection:
        sourceData.learningDirection ||
        '부족한 핵심 스킬을 우선순위에 따라 학습하면 직무 적합도를 높일 수 있습니다.',
      missingSkills,
      matchedSkills,
      partialSkills,
      needSkills,
      ownedProgress,
      missingProgress,
      roadmapItems,
      jobRequiredSkills,
      jobPreferredSkills,
      jobMainTasks,
      jobKeywords,
      jobSummary,
      strengthSummary,
      weaknessSummary,
      hasJobAnalysis:
        jobRequiredSkills.length > 0 ||
        jobPreferredSkills.length > 0 ||
        jobMainTasks.length > 0 ||
        jobKeywords.length > 0 ||
        jobSummary.length > 0,
    };
  }, [location.search, resultData]);

  if (!report) {
    return (
      <main className="result-empty">
        <section className="result-empty-card">
          <div className="result-empty-visual" aria-hidden="true">
            <FileSearch size={88} />
          </div>
          <h2>아직 분석 결과가 없습니다</h2>
          <p>
            이력서와 채용공고를 먼저 분석하면
            <br />
            매칭 점수와 부족한 역량을 확인할 수 있습니다.
          </p>
          <div className="result-empty-badges" aria-label="분석 후 확인 가능한 항목">
            <span>매칭 점수</span>
            <span>보유 스킬</span>
            <span>부족 스킬</span>
            <span>추천 학습 방향</span>
          </div>
          <div className="result-empty-actions">
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

  const scoreLabel = getScoreLabel(report.score);
  const circleStyle = {
    background: `conic-gradient(#4f67ee ${report.score * 3.6}deg, #edf1ff 0deg)`,
  };

  const openSkillDetailModal = (type) => {
    const isMissing = type === 'missing';
    const source = isMissing ? report.missingProgress : report.ownedProgress;
    const title = isMissing ? '부족 스킬 상세' : '보유 스킬 상세';
    const buildOwnedEvidence = (item) => {
      const evidence = item.evidence || `이력서에서 ${item.name} 관련 경험과 역량을 확인할 수 있습니다.`;
      const reason = item.reason || `${item.name}은 ${report.targetJob} 직무와 연결되는 핵심 보유 역량입니다.`;

      if (evidence.length >= 55 || evidence.includes('\n')) {
        return evidence;
      }

      return `${evidence}\n${reason}`;
    };
    const buildMissingEvidence = (item) => {
      const evidence = item.evidence || `이력서에서 ${item.name}을 직접 활용한 프로젝트나 성과가 충분히 드러나지 않았습니다.`;
      const reason = item.reason || `${report.targetJob} 공고에서 ${item.name} 역량이 필요하지만 이력서 근거가 부족합니다.`;

      if (evidence.length >= 55 || evidence.includes('\n')) {
        return evidence;
      }

      return `${evidence}\n${reason}`;
    };

    setDetailModal({
      type,
      title,
      description: isMissing
        ? '채용공고 요구사항 대비 보완이 필요한 역량입니다.'
        : '이력서에서 확인된 강점 역량입니다.',
      items: source.map((item) => ({
        name: item.name,
        statusLabel: item.statusLabel,
        jdRequirement: `${report.targetJob} 공고에서 ${item.name} 관련 실무 활용 경험을 요구합니다.`,
        resumeEvidence: isMissing
          ? buildMissingEvidence(item)
          : buildOwnedEvidence(item),
      })),
    });
  };

  return (
    <main className="result-page">
      <header className="result-hero">
        <h1>스킬 갭 분석 결과</h1>
        <p>이력서와 채용공고 비교 결과입니다.</p>
      </header>

      <section className="result-grid">
        <article className="result-card score-card">
          <CardHeader title="전체 매칭 점수" />
          <div className="score-ring" style={circleStyle}>
            <div className="score-ring-inner">
              <strong>{report.score}%</strong>
              <span>{scoreLabel}</span>
            </div>
          </div>
        </article>

        <article className="result-card summary-card">
          <CardHeader
            title="매칭 요약"
            action={(
              <button type="button" onClick={() => setIsSummaryModalOpen(true)}>
              더 보기 <ChevronRight size={16} />
              </button>
            )}
          />
          <div className="summary-box summary-box-compact">
            <div className="summary-section">
              <span className="summary-label strength">강점</span>
              <p>{report.strengthSummary}</p>
            </div>
            <div className="summary-section">
              <span className="summary-label weakness">약점</span>
              <p>{report.weaknessSummary}</p>
            </div>
          </div>
        </article>

        <article className="result-card jd-card">
          <CardHeader title="JD 핵심 요구사항" />

          {report.hasJobAnalysis ? (
            <div className="jd-summary-list">
              <div className="jd-summary-row">
                <span className="jd-summary-label required">필수</span>
                <div className="jd-summary-content result-tags">
                  {report.jobRequiredSkills.slice(0, 4).map((skill) => (
                    <span key={skill}>{skill}</span>
                  ))}
                </div>
              </div>

              <div className="jd-summary-row">
                <span className="jd-summary-label preferred">우대</span>
                <div className="jd-summary-content result-tags">
                  {report.jobPreferredSkills.slice(0, 4).map((skill) => (
                    <span key={skill}>{skill}</span>
                  ))}
                </div>
              </div>

              <div className="jd-summary-row">
                <span className="jd-summary-label task">업무</span>
                <div className="jd-summary-content result-tags jd-task-tags">
                  {report.jobMainTasks.slice(0, 2).map((task) => (
                    <span key={task}>{task}</span>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="jd-summary-list">
              <div className="jd-summary-row">
                <span className="jd-summary-label required">일치</span>
                <div className="jd-summary-content result-tags">
                  {report.matchedSkills.slice(0, 3).map((skill) => (
                    <span key={skill}>{skill}</span>
                  ))}
                </div>
              </div>

              <div className="jd-summary-row">
                <span className="jd-summary-label preferred">부분</span>
                <div className="jd-summary-content result-tags">
                  {report.partialSkills.slice(0, 3).map((skill) => (
                    <span key={skill}>{skill}</span>
                  ))}
                </div>
              </div>

              <div className="jd-summary-row">
                <span className="jd-summary-label task">보완</span>
                <div className="jd-summary-content result-tags">
                  {report.needSkills.slice(0, 3).map((skill) => (
                    <span key={skill}>{skill}</span>
                  ))}
                </div>
              </div>
            </div>
          )}
        </article>

        <article className="result-card skill-card">
          <CardHeader
            title="보유 스킬"
            action={(
              <button type="button" onClick={() => openSkillDetailModal('owned')}>
              더 보기 <ChevronRight size={16} />
              </button>
            )}
          />

          <div className="progress-list">
            {report.ownedProgress.map((item) => (
              <div className={`progress-item blue ${getSkillStatusClassName(item.statusLabel)}`} key={item.name}>
                <div>
                  <span>{item.name}</span>
                  <strong className={`skill-status-badge ${getSkillStatusClassName(item.statusLabel)}`}>
                    {item.statusLabel}
                  </strong>
                </div>
              </div>
            ))}
          </div>
        </article>


        <article className="result-card skill-card">
          <CardHeader
            title="부족 스킬"
            action={(
              <button type="button" onClick={() => openSkillDetailModal('missing')}>
              더 보기 <ChevronRight size={16} />
              </button>
            )}
          />

          <div className="progress-list">
            {report.missingProgress.map((item) => (
              <div className={`progress-item red ${getSkillStatusClassName(item.statusLabel)}`} key={item.name}>
                <div>
                  <span>{item.name}</span>
                  <strong className={`skill-status-badge ${getSkillStatusClassName(item.statusLabel)}`}>
                    {item.statusLabel}
                  </strong>
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="result-card roadmap-card">
          <CardHeader title="추천 학습 방향" />
          <div className="roadmap-list">
            {report.roadmapItems.map((item, index) => (
              <div className="roadmap-item" key={`${item}-${index}`}>
                <span>{index + 1}</span>
                <strong>{item}</strong>
              </div>
            ))}
          </div>
        </article>
      </section>

      <section className="result-cta">
        <button
          type="button"
          onClick={() =>
            navigate('/roadmap', {
              state: {
                missingSkills: report.needSkills,
                targetJob: report.targetJob,
              },
            })
          }
        >
          <Map size={24} />
          학습 로드맵 생성
        </button>
        <p>맞춤 학습 로드맵을 생성하면 단계별 학습 계획과 추천 강의를 확인할 수 있습니다.</p>
      </section>

      {detailModal ? (
        <div className="skill-modal-backdrop" role="presentation" onClick={() => setDetailModal(null)}>
          <section
            className="skill-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="skill-modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="skill-modal-header">
              <div>
                <h2 id="skill-modal-title">{detailModal.title}</h2>
                <p>{detailModal.description}</p>
              </div>
              <button type="button" onClick={() => setDetailModal(null)} aria-label="상세 모달 닫기">
                <X size={22} />
              </button>
            </div>

            <div className="skill-modal-list">
              {detailModal.items.map((item) => (
                <article className={`skill-detail-card ${getSkillStatusClassName(item.statusLabel)}`} key={item.name}>
                  <div className="skill-detail-top">
                    <h3>{item.name}</h3>
                    <span className={getSkillStatusClassName(item.statusLabel)}>
                      {item.statusLabel}
                    </span>
                  </div>

                  <dl>
                    <div>
                      <dt>JD 요구사항</dt>
                      <dd>{item.jdRequirement}</dd>
                    </div>
                    <div>
                      <dt>{detailModal.type === 'missing' ? '이력서 분석 근거' : '이력서 근거'}</dt>
                      <dd>{item.resumeEvidence}</dd>
                    </div>
                  </dl>
                </article>
              ))}
            </div>
          </section>
        </div>
      ) : null}

      {isSummaryModalOpen ? (
        <div className="skill-modal-backdrop" role="presentation" onClick={() => setIsSummaryModalOpen(false)}>
          <section
            className="skill-modal summary-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="summary-modal-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="skill-modal-header">
              <div>
                <h2 id="summary-modal-title">매칭 요약 상세</h2>
                <p>이력서와 채용공고를 비교한 전체 분석 내용입니다.</p>
              </div>
              <button type="button" onClick={() => setIsSummaryModalOpen(false)} aria-label="매칭 요약 모달 닫기">
                <X size={22} />
              </button>
            </div>

            <div className="summary-modal-content">
              <dl>
                <div>
                  <dt>지원 직무</dt>
                  <dd>{report.targetJob}</dd>
                </div>
                <div>
                  <dt>전체 분석 요약</dt>
                  <dd>{report.analysis}</dd>
                </div>
                <div>
                  <dt>추천 학습 방향</dt>
                  <dd>{report.learningDirection}</dd>
                </div>
                <div>
                  <dt>확인된 강점</dt>
                  <dd>{report.ownedProgress.map((skill) => skill.name).slice(0, 5).join(', ')}</dd>
                </div>
                <div>
                  <dt>보완 필요 스킬</dt>
                  <dd>{report.missingProgress.map((skill) => skill.name).slice(0, 5).join(', ')}</dd>
                </div>
              </dl>
            </div>
          </section>
        </div>
      ) : null}
    </main>
  );
};

export default ResultPage;
