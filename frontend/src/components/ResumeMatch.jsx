import { useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import './ResumeMatch.css';

const API_BASE_URL = 'http://localhost:8080/api/resume';
const JOB_ANALYZE_API_URL = 'http://localhost:8080/api/job/analyze';
const JOB_EXTRACT_URL_API_URL = 'http://localhost:8080/api/job/extract-url';
const MAX_JD_LENGTH = 5000;
const RESUME_SKILL_LIMIT = 8;
const RESUME_SKILL_PRIORITY = [
  'Java',
  'Spring Boot',
  'JPA',
  'MySQL',
  'React',
  'TypeScript',
  'Docker',
  'AWS',
  'QueryDSL',
  'PostgreSQL',
  'Redis',
  'MongoDB',
  'Kubernetes',
  'Jenkins',
  'GitHub Actions',
  'REST API',
  'Git',
];

const steps = [
  '이력서 업로드',
  '채용공고 입력',
  '분석 시작',
];

const sampleExtractedJob = {
  targetJob: '백엔드 개발자',
  company: '분석 결과',
  requiredSkills: ['Java', 'Spring Boot', 'JPA', 'MySQL'],
  preferredSkills: ['AWS', 'Docker'],
  mainTasks: ['REST API 개발', '데이터베이스 연동', '서버 운영'],
  keywords: ['Java', 'Spring Boot', 'JPA', 'MySQL', 'AWS', 'REST API'],
  summary: '채용공고 분석에 실패한 경우 표시하는 기본 분석 결과입니다.',
  details: [
    'Spring Boot 기반 REST API 개발 역량이 중요합니다.',
    'JPA와 MySQL을 사용한 데이터 모델링 경험을 요구합니다.',
    'AWS 환경에서 서비스 배포와 운영 경험이 있으면 유리합니다.',
  ],
};

const toArray = (value) => (Array.isArray(value) ? value.filter(Boolean) : []);

const prioritizeResumeSkills = (skills) => {
  const priorityIndex = new Map(
    RESUME_SKILL_PRIORITY.map((skill, index) => [skill.toLowerCase(), index])
  );

  return [...skills].sort((a, b) => {
    const aIndex = priorityIndex.get(String(a).toLowerCase());
    const bIndex = priorityIndex.get(String(b).toLowerCase());

    if (aIndex !== undefined && bIndex !== undefined) {
      return aIndex - bIndex;
    }

    if (aIndex !== undefined) {
      return -1;
    }

    if (bIndex !== undefined) {
      return 1;
    }

    return 0;
  });
};

const normalizeJobAnalysis = (data) => {
  const requiredSkills = toArray(data?.requiredSkills);
  const preferredSkills = toArray(data?.preferredSkills);
  const mainTasks = toArray(data?.mainTasks);
  const keywords = toArray(data?.keywords);
  const mergedKeywords = keywords.length > 0
    ? keywords
    : [...requiredSkills, ...preferredSkills].filter(Boolean);

  return {
    targetJob: data?.targetJob || '분석된 직무',
    company: 'LLM 분석 결과',
    requiredSkills,
    preferredSkills,
    mainTasks,
    keywords: mergedKeywords,
    summary: data?.summary || '채용공고 분석 결과입니다.',
    details: mainTasks.length > 0 ? mainTasks : ['채용공고 분석 결과를 불러오지 못했습니다.'],
  };
};

const ResumeMatch = () => {
  const navigate = useNavigate();

  const [resumeFile, setResumeFile] = useState(null);
  const [parsedResumeData, setParsedResumeData] = useState(null);
  const [isParsing, setIsParsing] = useState(false);
  const [inputMode, setInputMode] = useState('url');
  const [jobUrl, setJobUrl] = useState('');
  const [targetJob, setTargetJob] = useState('');
  const [jobDescription, setJobDescription] = useState('');
  const [extractedJobDescription, setExtractedJobDescription] = useState('');
  const [jobUrlExtractMessage, setJobUrlExtractMessage] = useState('');
  const [jobAnalysisResult, setJobAnalysisResult] = useState(null);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [isLoadingJob, setIsLoadingJob] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [isResumeSkillsExpanded, setIsResumeSkillsExpanded] = useState(false);

  const isResumeCompleted = Boolean(parsedResumeData?.skills?.length);
  const isJobCompleted = Boolean(jobAnalysisResult);
  const resumeSkills = prioritizeResumeSkills(parsedResumeData?.skills || []);
  const visibleResumeSkills = isResumeSkillsExpanded
    ? resumeSkills
    : resumeSkills.slice(0, RESUME_SKILL_LIMIT);
  const hiddenResumeSkillCount = resumeSkills.length - visibleResumeSkills.length;

  const canAnalyze =
    isResumeCompleted &&
    isJobCompleted &&
    !isParsing &&
    !isLoadingJob &&
    !isAnalyzing;

  const getStepState = (index) => {
    if (index === 0) {
      return isResumeCompleted ? 'completed' : 'pending';
    }

    if (index === 1) {
      return isJobCompleted ? 'completed' : 'pending';
    }

    if (index === 2) {
      return isResumeCompleted && isJobCompleted ? 'active' : 'pending';
    }

    return 'pending';
  };

  const getStepLineState = (index) => {
    if (index === 0 || index === 1) {
      return isResumeCompleted && isJobCompleted ? 'line-completed' : '';
    }

    return '';
  };

  const handleFileUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    setResumeFile(file);
    setParsedResumeData(null);
    setIsParsing(true);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await axios.post(`${API_BASE_URL}/parse-resume`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setParsedResumeData(response.data);
      setIsResumeSkillsExpanded(false);
    } catch (error) {
      console.error('이력서 파싱 오류:', error);
      alert('이력서 파싱에 실패했습니다. 백엔드 서버 상태를 확인해주세요.');
      setResumeFile(null);
    } finally {
      setIsParsing(false);
      e.target.value = '';
    }
  };

  const resetFile = () => {
    setResumeFile(null);
    setParsedResumeData(null);
    setIsResumeSkillsExpanded(false);
  };

  const handleModeChange = (mode) => {
    setInputMode(mode);
  };

  const applyJobAnalysis = (analysis) => {
    setJobAnalysisResult(analysis);
    setTargetJob(analysis.targetJob);
    setIsDetailOpen(false);
  };

  const requestJobAnalysis = async (description) => {
    const response = await axios.post(
      JOB_ANALYZE_API_URL,
      { jobDescription: description },
      { headers: { 'Content-Type': 'application/json' } },
    );

    return normalizeJobAnalysis(response.data);
  };

  const requestJobUrlExtraction = async (url) => {
    const response = await axios.post(
      JOB_EXTRACT_URL_API_URL,
      { url },
      { headers: { 'Content-Type': 'application/json' } },
    );

    return response.data;
  };

  const handleLoadJobFromUrl = async () => {
    const description = jobUrl.trim();

    if (!description) {
      alert('채용공고 URL을 입력해주세요.');
      return;
    }

    setIsLoadingJob(true);
    setIsDetailOpen(false);
    setJobUrlExtractMessage('');

    try {
      const extracted = await requestJobUrlExtraction(description);
      if (!extracted?.success || !extracted.content?.trim()) {
        setJobAnalysisResult(null);
        setExtractedJobDescription('');
        setJobUrlExtractMessage(
          extracted?.message || 'URL 본문을 불러오지 못했습니다. 직접 입력 탭에 공고 내용을 붙여넣어 주세요.',
        );
        alert('URL 본문을 불러오지 못했습니다. 직접 입력 탭에 공고 내용을 붙여넣어 주세요.');
        return;
      }

      setExtractedJobDescription(extracted.content);
      setJobUrlExtractMessage('본문 추출 완료');
      const analysis = await requestJobAnalysis(extracted.content);
      applyJobAnalysis(analysis);
    } catch (error) {
      console.error('채용공고 URL 분석 오류:', error);
      setJobAnalysisResult(null);
      setExtractedJobDescription('');
      setJobUrlExtractMessage('URL 본문을 불러오지 못했습니다. 직접 입력 탭에 공고 내용을 붙여넣어 주세요.');
      alert('URL 본문을 불러오지 못했습니다. 직접 입력 탭에 공고 내용을 붙여넣어 주세요.');
    } finally {
      setIsLoadingJob(false);
    }
  };

  const handleAnalyzeJobDescription = async () => {
    const originalDescription = jobDescription.trim();

    // TODO: 분석 안정화 후 임시 디버그 로그는 제거합니다.
    console.log('채용공고 분석 버튼 클릭');
    console.log('분석 요청 jobDescription:', originalDescription);

    if (!originalDescription) {
      alert('채용공고 내용을 입력해주세요.');
      return;
    }

    setIsLoadingJob(true);
    setIsDetailOpen(false);

    try {
      const analysis = await requestJobAnalysis(originalDescription);
      console.log('채용공고 분석 응답:', analysis);
      setExtractedJobDescription('');
      setJobUrlExtractMessage('');
      applyJobAnalysis(analysis);
    } catch (error) {
      console.error('채용공고 분석 오류:', error);
      applyJobAnalysis(sampleExtractedJob);
      alert('채용공고 분석에 실패했습니다. 직접 입력 내용을 확인해주세요.');
    } finally {
      setIsLoadingJob(false);
    }
  };

  const handleAnalyzeJD = async () => {
    const currentJobDescription = inputMode === 'url'
      ? extractedJobDescription.trim()
      : jobDescription.trim();

    if (!resumeFile || !parsedResumeData?.skills?.length) {
      alert('먼저 이력서를 업로드해주세요.');
      return;
    }

    if (!jobAnalysisResult) {
      alert('먼저 채용공고 분석을 실행해주세요.');
      return;
    }

    if (!targetJob.trim()) {
      alert('목표 직무를 입력해주세요.');
      return;
    }

    if (!currentJobDescription) {
      alert('채용공고 내용을 입력해주세요.');
      return;
    }

    setIsAnalyzing(true);

    try {
      const response = await axios.post(
        `${API_BASE_URL}/gap-match`,
        {
          resumeSkills: parsedResumeData.skills,
          technicalSkills: parsedResumeData.technicalSkills || [],
          resumeKeywords: parsedResumeData.keywords || [],
          experienceSummary: parsedResumeData.experienceSummary || '',
          recommendedJobTypes: parsedResumeData.recommendedJobTypes || [],
          jdText: currentJobDescription,
          targetJob,
          requiredSkills: jobAnalysisResult?.requiredSkills || [],
          preferredSkills: jobAnalysisResult?.preferredSkills || [],
          mainTasks: jobAnalysisResult?.mainTasks || [],
          keywords: jobAnalysisResult?.keywords || [],
          summary: jobAnalysisResult?.summary || '',
        },
        {
          headers: { 'Content-Type': 'application/json' },
        },
      );

      navigate('/result', {
        state: {
          analysisResult: {
            ...response.data,
            targetJob,
            jdText: currentJobDescription,
            requiredSkills: jobAnalysisResult?.requiredSkills || [],
            preferredSkills: jobAnalysisResult?.preferredSkills || [],
            mainTasks: jobAnalysisResult?.mainTasks || [],
            jobKeywords: jobAnalysisResult?.keywords || [],
            jobSummary: jobAnalysisResult?.summary || '',
          },
        },
      });
    } catch (error) {
      console.error('스킬 갭 분석 오류:', error);
      alert('AI 분석에 실패했습니다. 백엔드 서버 상태와 콘솔 로그를 확인해주세요.');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleJobDescriptionChange = (value) => {
    setJobDescription(value);
    setExtractedJobDescription('');
    setJobUrlExtractMessage('');
    setJobAnalysisResult(null);
    setIsDetailOpen(false);
  };

  const visibleKeywords = jobAnalysisResult?.keywords.slice(0, 3) || [];
  const extraKeywordCount = jobAnalysisResult
    ? Math.max(jobAnalysisResult.keywords.length - visibleKeywords.length, 0)
    : 0;

  const renderExtractedJobCard = () => {
    if (!jobAnalysisResult) return null;

    return (
      <div className={`rm-extract-summary${isDetailOpen ? ' open' : ''}`}>
        <strong>AI 추출 완료</strong>
        <p>{jobAnalysisResult.targetJob} · {jobAnalysisResult.company}</p>
        <span>
          {visibleKeywords.join(' · ')}
          {extraKeywordCount > 0 ? ` 외 ${extraKeywordCount}개` : ''}
        </span>
        <button type="button" onClick={() => setIsDetailOpen((open) => !open)}>
          {isDetailOpen ? '상세 닫기' : '상세 보기'}
        </button>

        {jobUrlExtractMessage ? (
          <p className={`rm-url-status compact${extractedJobDescription ? ' success' : ' error'}`}>
            {jobUrlExtractMessage}
          </p>
        ) : null}

        {isDetailOpen ? (
          <div className="rm-extract-detail">
            <h3>상세 분석 내용</h3>

            <div className="rm-keyword-block">
              <strong>필수 스킬</strong>
              <div className="rm-keywords">
                {jobAnalysisResult.requiredSkills.map((skill) => (
                  <span key={skill}>{skill}</span>
                ))}
              </div>
            </div>

            <div className="rm-keyword-block">
              <strong>우대 스킬</strong>
              <div className="rm-keywords">
                {jobAnalysisResult.preferredSkills.map((skill) => (
                  <span key={skill}>{skill}</span>
                ))}
              </div>
            </div>

            <div className="rm-keyword-block">
              <strong>핵심 키워드</strong>
              <div className="rm-keywords">
                {jobAnalysisResult.keywords.map((keyword) => (
                  <span key={keyword}>{keyword}</span>
                ))}
              </div>
            </div>

            <div className="rm-ai-note">{jobAnalysisResult.summary}</div>

            <ul>
              {jobAnalysisResult.details.map((detail) => (
                <li key={detail}>{detail}</li>
              ))}
            </ul>
          </div>
        ) : null}
      </div>
    );
  };

  return (
    <main className="rm-page">
      <section className="rm-stepper" aria-label="분석 단계">
        {steps.map((step, index) => {
          const state = getStepState(index);
          const lineState = getStepLineState(index);

          return (
            <div className={`rm-step ${state} ${lineState}`} key={step}>
              <div className={`rm-step-dot ${state}`}>
                {index + 1}
              </div>
              <span className={`rm-step-label ${state}`}>
                {step}
              </span>
            </div>
          );
        })}
      </section>

      <section className="rm-workspace">
        <div className="rm-panel">
          <div className="rm-panel-header">
            <h2>이력서 업로드</h2>
            <p>PDF, Word 파일을 업로드해주세요.</p>
          </div>

          <label className={isParsing ? 'rm-dropzone loading' : 'rm-dropzone'}>
            <input
              type="file"
              onChange={handleFileUpload}
              accept=".pdf,.docx"
              disabled={isParsing}
            />
            <span className="rm-upload-icon" aria-hidden="true">↑</span>
            <strong>{isParsing ? '이력서 분석 중...' : '파일 선택 또는 드래그 & 드롭'}</strong>
            <small>PDF, DOCX 파일 · 최대 10MB</small>
          </label>

          <div className="rm-file-list">
            <h3>업로드 파일</h3>
            {resumeFile ? (
              <div className="rm-file-card">
                <div className="rm-file-icon" aria-hidden="true">▤</div>
                <div>
                  <strong>{resumeFile.name}</strong>
                  <span>{resumeFile.type || '문서 파일'} · {(resumeFile.size / 1024).toFixed(0)}KB</span>
                </div>
                <button type="button" onClick={resetFile} aria-label="업로드 파일 삭제">
                  ×
                </button>
                {parsedResumeData?.skills?.length ? (
                  <span className="rm-file-check" aria-label="파싱 완료">✓</span>
                ) : null}
              </div>
            ) : (
              <p className="rm-empty-text">아직 업로드된 파일이 없습니다.</p>
            )}
          </div>

          {resumeSkills.length ? (
            <div className="rm-skills">
              {visibleResumeSkills.map((skill) => (
                <span key={skill}>{skill}</span>
              ))}
              {!isResumeSkillsExpanded && hiddenResumeSkillCount > 0 ? (
                <button
                  type="button"
                  className="rm-skill-toggle"
                  onClick={() => setIsResumeSkillsExpanded(true)}
                  aria-label={`숨겨진 스킬 ${hiddenResumeSkillCount}개 더 보기`}
                >
                  +{hiddenResumeSkillCount}
                </button>
              ) : null}
              {isResumeSkillsExpanded && resumeSkills.length > RESUME_SKILL_LIMIT ? (
                <button
                  type="button"
                  className="rm-skill-toggle"
                  onClick={() => setIsResumeSkillsExpanded(false)}
                >
                  접기
                </button>
              ) : null}
            </div>
          ) : null}
        </div>

        <div className="rm-panel">
          <div className="rm-panel-header">
            <h2>채용공고 입력</h2>
            <p>공고 URL을 입력하거나 내용을 직접 붙여넣어 주세요.</p>
          </div>

          <div className="rm-mode-tabs" role="tablist" aria-label="채용공고 입력 방식">
            <button
              type="button"
              className={inputMode === 'url' ? 'active' : ''}
              onClick={() => handleModeChange('url')}
            >
              URL로 불러오기
            </button>
            <button
              type="button"
              className={inputMode === 'manual' ? 'active' : ''}
              onClick={() => handleModeChange('manual')}
            >
              직접 입력
            </button>
          </div>

          {inputMode === 'url' ? (
            <div className="rm-url-panel">
              <label className="rm-field">
                <span>채용공고 URL</span>
                <input
                  type="url"
                  value={jobUrl}
                  onChange={(e) => {
                    setJobUrl(e.target.value);
                    setExtractedJobDescription('');
                    setJobUrlExtractMessage('');
                    setJobAnalysisResult(null);
                    setIsDetailOpen(false);
                  }}
                  placeholder="https://www.jobsite.co.kr/jobs/12345"
                  spellCheck={false}
                />
              </label>

              <div className="rm-url-action">
                <button type="button" onClick={handleLoadJobFromUrl} disabled={isLoadingJob || !jobUrl.trim()}>
                  {isLoadingJob ? '분석 중...' : '공고 불러오기'}
                </button>
              </div>

              {renderExtractedJobCard()}

              <p className="rm-url-help">URL 분석이 어려운 경우 직접 입력 탭을 이용하세요.</p>
            </div>
          ) : (
            <div className="rm-manual-panel">
              <label className="rm-field">
                <span className="sr-only">목표 직무</span>
                <input
                  type="text"
                  value={targetJob}
                  onChange={(e) => setTargetJob(e.target.value)}
                  placeholder="예: 백엔드 개발자"
                  spellCheck={false}
                />
              </label>

              <label className="rm-field">
                <span className="sr-only">상세 채용공고</span>
                <textarea
                  value={jobDescription}
                  maxLength={MAX_JD_LENGTH}
                  onChange={(e) => handleJobDescriptionChange(e.target.value)}
                  placeholder="주요 업무, 자격 요건, 우대 사항 등을 입력해 주세요."
                  spellCheck={false}
                />
              </label>

              <div className="rm-count">
                {jobDescription.length.toLocaleString()} / {MAX_JD_LENGTH.toLocaleString()}
              </div>

              <div className="rm-manual-action">
                <button
                  type="button"
                  onClick={handleAnalyzeJobDescription}
                  disabled={isLoadingJob || !jobDescription.trim()}
                >
                  {isLoadingJob ? '분석 중...' : '채용공고 분석'}
                </button>
              </div>

              {renderExtractedJobCard()}
            </div>
          )}
        </div>
      </section>

      <div className="rm-action-bar">
        <button
          type="button"
          className="rm-analyze-button"
          disabled={!canAnalyze}
          onClick={handleAnalyzeJD}
        >
          {isAnalyzing ? 'AI 스킬 갭 분석 중...' : 'AI 스킬 갭 분석 시작 →'}
        </button>
      </div>

      <p className="rm-helper">
        업로드된 정보는 분석에만 사용되며 서버 저장 정책에 따라 처리됩니다.
      </p>
    </main>
  );
};

export default ResumeMatch;
