import { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  CheckCircle2,
  ChevronRight,
  CircleEllipsis,
  Map,
  X,
} from 'lucide-react';
import './ResultPage.css';

const defaultOwnedSkills = ['의사소통', '문제해결', '데이터 분석', '협업', '기획력'];
const defaultMatchedSkills = ['데이터 처리', 'Python 기반 분석'];
const defaultPartialSkills = ['머신러닝 기초', '통계 분석'];
const defaultNeedSkills = ['SQL 고급 활용', '모델 검증 경험'];

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
    return value
      .split(/[,|\n]/)
      .map((skill) => skill.trim())
      .filter(Boolean);
  }

  return [];
};

const splitTextLines = (value) => {
  if (!value || typeof value !== 'string') return [];

  return value
    .split(/\n|\. /)
    .map((line) => line.replace(/^[\s\-•\d.]+/, '').trim())
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

const buildProgressItems = (skills, baseScores) =>
  skills.slice(0, 5).map((skill, index) => ({
    name: skill,
    score: baseScores[index] ?? baseScores[baseScores.length - 1],
  }));

const ResultPage = () => {
  const location = useLocation();
  const navigate = useNavigate();

  const initialData = location.state?.analysisResult || location.state?.resultData;
  const [resultData, setResultData] = useState(initialData || null);
  const [detailModal, setDetailModal] = useState(null);

  useEffect(() => {
    const incomingData = location.state?.analysisResult || location.state?.resultData;
    if (incomingData) {
      setResultData(incomingData);
    }
  }, [location.state]);

  const report = useMemo(() => {
    if (!resultData) return null;

    const missingSkills = toSkillArray(resultData.missingSkills);
    const matchedSkills =
      toSkillArray(resultData.matchedSkills).length > 0
        ? toSkillArray(resultData.matchedSkills)
        : defaultMatchedSkills;
    const partialSkills =
      toSkillArray(resultData.partialSkills).length > 0
        ? toSkillArray(resultData.partialSkills)
        : defaultPartialSkills;
    const needSkills = missingSkills.length > 0 ? missingSkills : defaultNeedSkills;
    const ownedSkills =
      toSkillArray(resultData.ownedSkills).length > 0
        ? toSkillArray(resultData.ownedSkills)
        : toSkillArray(resultData.resumeSkills).length > 0
          ? toSkillArray(resultData.resumeSkills)
          : defaultOwnedSkills;

    const learningLines = splitTextLines(resultData.learningDirection);
    const roadmapItems =
      learningLines.length > 0
        ? learningLines.slice(0, 3)
        : needSkills.slice(0, 3).map((skill) => `${skill} 학습`);

    return {
      score: getScore(resultData),
      targetJob: resultData.targetJob || resultData.jobTitle || resultData.position || '데이터 분석가',
      analysis:
        resultData.analysis ||
        '이력서와 채용공고를 비교한 결과, 일부 핵심 역량은 잘 맞지만 추가 보완이 필요한 스킬이 있습니다.',
      learningDirection:
        resultData.learningDirection ||
        '부족한 핵심 스킬을 우선순위에 따라 학습하면 직무 적합도를 높일 수 있습니다.',
      missingSkills,
      matchedSkills,
      partialSkills,
      needSkills,
      ownedProgress: buildProgressItems(ownedSkills, [85, 80, 75, 70, 65]),
      missingProgress: buildProgressItems(needSkills, [30, 25, 35, 30, 45]),
      roadmapItems,
    };
  }, [resultData]);

  if (!report) {
    return (
      <main className="result-empty">
        <h2>분석 데이터가 없습니다.</h2>
        <button type="button" onClick={() => navigate('/')}>
          메인으로 돌아가기
        </button>
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

    setDetailModal({
      type,
      title,
      description: isMissing
        ? '채용공고 요구사항 대비 보완이 필요한 역량입니다.'
        : '이력서에서 확인된 강점 역량입니다.',
      items: source.map((item) => ({
        name: item.name,
        levelLabel: isMissing ? '부족도' : '보유도',
        level: item.score,
        jdRequirement: `${report.targetJob} 공고에서 ${item.name} 관련 실무 활용 경험을 요구합니다.`,
        resumeEvidence: isMissing
          ? `이력서에는 ${item.name}을 직접 수행한 프로젝트, 성과, 사용 맥락이 충분히 드러나지 않습니다.`
          : `이력서에서 ${item.name} 관련 경험과 역량을 확인할 수 있습니다.`,
        recommendation: isMissing
          ? `${item.name} 기초 개념을 정리한 뒤 작은 실습 프로젝트로 사용 근거를 보완하세요.`
          : `${item.name} 강점을 유지하면서 JD 키워드와 연결되는 성과 표현을 더 구체화하세요.`,
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
          <h2>전체 매칭 점수</h2>
          <div className="score-ring" style={circleStyle}>
            <div className="score-ring-inner">
              <strong>{report.score}%</strong>
              <span>{scoreLabel}</span>
            </div>
          </div>
        </article>

        <article className="result-card summary-card">
          <h2>매칭 요약</h2>
          <div className="summary-box">
            <span>지원 직무</span>
            <strong>{report.targetJob}</strong>
            <p>{report.analysis}</p>
          </div>
          <div className="summary-tags">
            <div className="result-tags">
              {[...report.matchedSkills, ...report.partialSkills].slice(0, 5).map((skill) => (
                <span key={skill}>{skill}</span>
              ))}
            </div>
          </div>
        </article>

        <article className="result-card jd-card">
          <h2>JD 핵심 요구사항 매칭</h2>

          <div className="jd-group good">
            <div className="jd-title">
              <CheckCircle2 size={22} />
              <strong>일치</strong>
            </div>
            <div className="result-tags">
              {report.matchedSkills.slice(0, 3).map((skill) => (
                <span key={skill}>{skill}</span>
              ))}
            </div>
          </div>

          <div className="jd-group partial">
            <div className="jd-title">
              <CircleEllipsis size={22} />
              <strong>부분 일치</strong>
            </div>
            <div className="result-tags">
              {report.partialSkills.slice(0, 3).map((skill) => (
                <span key={skill}>{skill}</span>
              ))}
            </div>
          </div>

          <div className="jd-group need">
            <div className="jd-title">
              <AlertTriangle size={22} />
              <strong>보완 필요</strong>
            </div>
            <div className="result-tags">
              {report.needSkills.slice(0, 3).map((skill) => (
                <span key={skill}>{skill}</span>
              ))}
            </div>
          </div>
        </article>

        <article className="result-card skill-card">
          <div className="card-title-row">
            <h2>보유 스킬</h2>
            <button type="button" onClick={() => openSkillDetailModal('owned')}>
              더 보기 <ChevronRight size={16} />
            </button>
          </div>

          <div className="progress-list">
            {report.ownedProgress.map((item) => (
              <div className="progress-item blue" key={item.name}>
                <div>
                  <span>{item.name}</span>
                  <strong>{item.score}%</strong>
                </div>
                <div className="progress-track">
                  <span style={{ width: `${item.score}%` }} />
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="result-card skill-card">
          <div className="card-title-row">
            <h2>부족 스킬</h2>
            <button type="button" onClick={() => openSkillDetailModal('missing')}>
              더 보기 <ChevronRight size={16} />
            </button>
          </div>

          <div className="progress-list">
            {report.missingProgress.map((item) => (
              <div className="progress-item red" key={item.name}>
                <div>
                  <span>{item.name}</span>
                  <strong>{item.score}%</strong>
                </div>
                <div className="progress-track">
                  <span style={{ width: `${item.score}%` }} />
                </div>
              </div>
            ))}
          </div>
        </article>

        <article className="result-card roadmap-card">
          <h2>추천 학습 방향</h2>
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
                <article className="skill-detail-card" key={item.name}>
                  <div className="skill-detail-top">
                    <h3>{item.name}</h3>
                    <span className={detailModal.type === 'missing' ? 'danger' : 'primary'}>
                      {item.levelLabel} {item.level}%
                    </span>
                  </div>

                  <dl>
                    <div>
                      <dt>JD 요구사항</dt>
                      <dd>{item.jdRequirement}</dd>
                    </div>
                    <div>
                      <dt>이력서에서 부족한 근거</dt>
                      <dd>{item.resumeEvidence}</dd>
                    </div>
                    <div>
                      <dt>보완 학습 추천</dt>
                      <dd>{item.recommendation}</dd>
                    </div>
                  </dl>
                </article>
              ))}
            </div>
          </section>
        </div>
      ) : null}
    </main>
  );
};

export default ResultPage;
