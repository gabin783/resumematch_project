import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { BarChart3, ChevronDown, ExternalLink, ShieldCheck, Target } from 'lucide-react';
import './JobPostingList.css';

const RECOMMENDATION_API_URL = 'http://localhost:8080/api/jobs/recommendations';
const LEGACY_JOBS_API_URL = 'http://localhost:8080/api/jobs';

const filters = ['전체', '높은 매칭률', '백엔드', '프론트엔드', '신입 가능'];

const getScoreTone = (score) => {
  if (score >= 80) return 'high';
  if (score >= 60) return 'medium';
  return 'low';
};

const getScoreLabel = (score) => {
  if (score >= 80) return `높음 ${score}%`;
  if (score >= 60) return `보통 ${score}%`;
  return `도전 ${score}%`;
};

const includesAny = (value, keywords) => {
  const normalized = value.toLowerCase();
  return keywords.some((keyword) => normalized.includes(keyword.toLowerCase()));
};

const getDummyScore = (jobId, isBackend, isFrontend) => {
  const scorePool = isBackend || isFrontend ? [86, 81, 74, 68] : [74, 68, 58];
  return scorePool[Math.abs(Number(jobId) || 0) % scorePool.length];
};

const buildLegacyRecommendation = (job) => {
  const title = job.title || '';
  const isBackend = includesAny(title, ['백엔드', 'backend', 'back-end', 'server', '서버']);
  const isFrontend = includesAny(title, ['프론트', 'frontend', 'front-end', 'react']);
  const matchScore = getDummyScore(job.id, isBackend, isFrontend);

  return {
    jobPostingId: job.id,
    companyName: job.companyName,
    title: job.title,
    url: job.url,
    source: job.source || (job.url?.includes('wanted.co.kr') ? 'WANTED' : 'LOCAL'),
    matchScore,
    matchedSkills: isBackend ? ['Java', 'Spring Boot', 'JPA', 'MySQL'] : isFrontend ? ['React', 'TypeScript'] : ['문제해결', '협업'],
    missingSkills: isBackend ? ['AWS', 'Docker'] : isFrontend ? ['Next.js', 'UI 테스트'] : ['JPA', 'AWS', 'Docker'],
    reason: isBackend
      ? '이력서의 Java, Spring Boot 경험이 공고 요구사항과 잘 일치합니다.'
      : '이력서의 프로젝트 경험과 협업 역량이 공고와 일부 일치합니다.',
  };
};

const getCompanyInitial = (companyName = '') => companyName.trim().charAt(0).toUpperCase() || '?';

const getVisibleSkills = (skills = [], maxCount = 3) => ({
  visible: skills.slice(0, maxCount),
  hiddenCount: Math.max(skills.length - maxCount, 0),
});

function CompanyLogo({ companyName, logoUrl }) {
  const [hasImageError, setHasImageError] = useState(false);
  const initial = getCompanyInitial(companyName);
  const shouldShowImage = Boolean(logoUrl) && !hasImageError;

  if (shouldShowImage) {
    return (
      <div className="company-logo" aria-label={`${companyName || '회사'} 로고`}>
        <img
          src={logoUrl}
          alt={`${companyName || '회사'} 로고`}
          onError={() => setHasImageError(true)}
        />
      </div>
    );
  }

  return (
    <div className="company-logo initial-logo" aria-label={`${companyName || '회사'} 이니셜 로고`}>
      {initial}
    </div>
  );
}

function JobPostingList() {
  const [jobs, setJobs] = useState([]);
  const [selectedFilter, setSelectedFilter] = useState('전체');
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    setIsLoading(true);
    setErrorMessage('');

    axios.get(RECOMMENDATION_API_URL)
      .then((response) => {
        setJobs(response.data || []);
      })
      .catch(async (error) => {
        console.error('추천 공고 조회 실패, 기존 공고 API로 재시도합니다:', error);

        try {
          const fallbackResponse = await axios.get(LEGACY_JOBS_API_URL);
          setJobs((fallbackResponse.data || []).map(buildLegacyRecommendation));
        } catch (fallbackError) {
          console.error('기존 공고 조회도 실패했습니다:', fallbackError);
          setErrorMessage('추천 공고를 불러오지 못했습니다.');
        }
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, []);

  const filteredJobs = useMemo(() => {
    if (selectedFilter === '전체') return jobs;

    return jobs.filter((job) => {
      const title = job.title || '';

      if (selectedFilter === '높은 매칭률') {
        return job.matchScore >= 80;
      }

      if (selectedFilter === '백엔드') {
        return includesAny(title, ['백엔드', 'backend', 'back-end', 'server', '서버']);
      }

      if (selectedFilter === '프론트엔드') {
        return includesAny(title, ['프론트', 'frontend', 'front-end', 'react']);
      }

      if (selectedFilter === '신입 가능') {
        return includesAny(title, ['신입', 'junior', '주니어']);
      }

      return true;
    });
  }, [jobs, selectedFilter]);

  const handleOpenJob = (url) => {
    if (!url) return;
    window.open(url, '_blank', 'noopener,noreferrer');
  };

  const handleMatchAnalysis = () => {
    // TODO: Connect this action to the resume matching flow with the selected job context.
    alert('매칭 분석 기능 연결 예정입니다.');
  };

  return (
    <main className="job-recommendation-page">
      <section className="job-recommendation-hero">
        <div>
          <h1>
            <Target size={30} />
            맞춤 추천 채용 공고
          </h1>
          <p>이력서와 스킬 분석 결과를 기준으로 추천된 공고입니다.</p>
        </div>
        <aside className="recommendation-standard">
          <div className="recommendation-standard-title">
            <ShieldCheck size={18} />
            <strong>추천 기준</strong>
          </div>
          <span>이력서 기반 스킬 매칭 · 스킬 갭 분석 · 관심 직무</span>
        </aside>
      </section>

      <section className="job-filter-panel">
        <div className="job-filter-tabs" aria-label="추천 공고 필터">
          {filters.map((filter) => (
            <button
              type="button"
              key={filter}
              className={selectedFilter === filter ? 'active' : ''}
              onClick={() => setSelectedFilter(filter)}
            >
              {filter}
            </button>
          ))}
        </div>
        <button type="button" className="job-sort-button">
          최신 등록순
          <ChevronDown size={16} />
        </button>
      </section>

      {isLoading ? (
        <section className="job-state-card">추천 공고를 불러오는 중입니다...</section>
      ) : errorMessage ? (
        <section className="job-state-card error">{errorMessage}</section>
      ) : filteredJobs.length === 0 ? (
        <section className="job-state-card">아직 추천 공고가 없습니다. 원티드 공고를 먼저 수집해주세요.</section>
      ) : (
        <section className="recommendation-grid">
          {filteredJobs.map((job) => {
            const scoreTone = getScoreTone(job.matchScore);
            const missingSkills = getVisibleSkills(job.missingSkills || []);

            return (
              <article className="recommendation-card" key={job.jobPostingId}>
                <div className="recommendation-card-top">
                  <CompanyLogo companyName={job.companyName} logoUrl={job.logoUrl} />
                  <div className="recommendation-title-area">
                    <span className="recommendation-company">{job.companyName}</span>
                    <h2>{job.title}</h2>
                    <div className="recommendation-tags matched">
                      {(job.matchedSkills || []).map((skill) => (
                        <span key={skill}>{skill}</span>
                      ))}
                    </div>
                  </div>
                  <div className="recommendation-badges">
                    <span className={`match-score-badge ${scoreTone}`}>{getScoreLabel(job.matchScore)}</span>
                    <span className="source-badge">{job.source || 'LOCAL'}</span>
                  </div>
                </div>

                <div className="recommendation-detail-grid">
                  <div className="recommendation-reason-block">
                    <strong>추천 이유</strong>
                    <p>{job.reason}</p>
                  </div>
                  <div className="recommendation-missing-block">
                    <strong>보완 필요 스킬</strong>
                    <div className="recommendation-tags missing">
                      {missingSkills.visible.map((skill) => (
                        <span key={skill}>{skill}</span>
                      ))}
                      {missingSkills.hiddenCount > 0 ? (
                        <span>+{missingSkills.hiddenCount}</span>
                      ) : null}
                    </div>
                  </div>
                </div>

                <div className="recommendation-actions">
                  <button type="button" className="view-job-btn" onClick={() => handleOpenJob(job.url)}>
                    공고 보기
                    <ExternalLink size={15} />
                  </button>
                  <button type="button" className="analyze-job-btn" onClick={handleMatchAnalysis}>
                    매칭 분석
                    <BarChart3 size={15} />
                  </button>
                </div>
              </article>
            );
          })}
        </section>
      )}
    </main>
  );
}

export default JobPostingList;
