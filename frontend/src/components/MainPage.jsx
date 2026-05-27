import { BarChart3, ClipboardList, FileText, Lightbulb, Map } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import './MainPage.css';

function MainPage() {
  const navigate = useNavigate();

  const handleStartAnalysis = () => {
    navigate('/match');
  };

  return (
    <div className="main-container">
      <main>
        <section className="main-hero-section">
          <div className="main-hero-content">
            <span className="main-hero-badge">AI Resume Matching</span>
            <h1>
              이력서와 공고를 AI가 분석
            </h1>

            <p>
              매칭률, 부족한 역량, 학습 방향을 확인하세요.
            </p>

            <div className="main-hero-buttons">
              <button
                type="button"
                className="main-btn-primary"
                onClick={handleStartAnalysis}
              >
                지금 분석 시작하기 →
              </button>
            </div>
          </div>

          <div className="main-hero-visual" aria-hidden="true">
            <div className="main-mockup-card">
              <div className="main-mockup-inner">
                <div className="main-profile-card">
                  <div className="main-avatar"></div>
                  <div>
                    <span></span>
                    <span></span>
                  </div>
                </div>
                <div className="main-chart-lines">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
                <div className="main-bar-chart">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section className="main-features-section">
          <h2>서비스 한눈에 보기</h2>

          <div className="main-feature-grid">
            <div className="main-feature-card">
              <div className="main-feature-icon blue">
                <FileText size={21} strokeWidth={2.2} />
              </div>
              <h3>이력서 분석</h3>
              <p>PDF/DOCX 업로드</p>
            </div>

            <div className="main-feature-card">
              <div className="main-feature-icon peach">
                <ClipboardList size={21} strokeWidth={2.2} />
              </div>
              <h3>공고 분석</h3>
              <p>핵심 역량 추출</p>
            </div>

            <div className="main-feature-card">
              <div className="main-feature-icon rose">
                <BarChart3 size={21} strokeWidth={2.2} />
              </div>
              <h3>스킬 갭</h3>
              <p>부족한 역량 확인</p>
            </div>

            <div className="main-feature-card">
              <div className="main-feature-icon mint">
                <Map size={21} strokeWidth={2.2} />
              </div>
              <h3>로드맵</h3>
              <p>학습 방향 추천</p>
            </div>
          </div>
        </section>

        <section className="main-process-section">
          <div className="main-section-row">
            <h2>간단한 이용 흐름</h2>
          </div>

          <p className="main-process-desc">
            업로드부터 분석 결과 확인까지 빠르게 진행됩니다.
          </p>

          <div className="main-process-grid">
            <div className="main-process-card">
              <span>01</span>
              <div>
                <h3>이력서 업로드</h3>
                <p>분석할 이력서를 등록합니다.</p>
              </div>
            </div>

            <div className="main-process-card">
              <span>02</span>
              <div>
                <h3>채용공고 입력</h3>
                <p>지원할 공고를 입력합니다.</p>
              </div>
            </div>

            <div className="main-process-card">
              <span>03</span>
              <div>
                <h3>결과 확인</h3>
                <p>매칭률과 보완점 확인</p>
              </div>
            </div>
          </div>
        </section>

        <section className="main-cta-strip">
          <div className="main-cta-icon">
            <Lightbulb size={20} strokeWidth={2.2} />
          </div>
          <p>
            내 이력서는 어떤 공고와 잘 맞을까요? AI 분석으로 매칭률과 부족한 역량을 확인해 보세요.
          </p>
        </section>
      </main>

      <footer className="main-footer">
        <p>대표 이메일 : 1234567@naver.com</p>
        <p>담당자 : 홍길동</p>
        <p>주소 : 경기도 성남시 판교 캠퍼스</p>
        <span>copyright © 두드림 IT 교육원 성남 판교 캠퍼스</span>
      </footer>
    </div>
  );
}

export default MainPage;