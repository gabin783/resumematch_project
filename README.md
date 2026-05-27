# ResumeMatch Project

AI 기반 이력서 매칭 서비스입니다. 프론트엔드와 백엔드를 하나의 상위 저장소에서 관리합니다.

## 프로젝트 구조

```text
resumematch-project/
├─ backend/
├─ frontend/
├─ README.md
└─ .gitignore
```

## Frontend 실행

```bash
cd frontend
npm install
npm run dev
```

기본 개발 서버는 Vite 설정에 따라 실행됩니다.

빌드 확인:

```bash
npm run build
```

## Backend 실행

```bash
cd backend
./gradlew bootRun
```

Windows PowerShell 또는 CMD에서는 다음 명령을 사용할 수 있습니다.

```bash
gradlew.bat bootRun
```

## 참고

- 프론트엔드 API 주소, 라우팅, `package.json` 설정은 기존 구조를 유지합니다.
- 백엔드 `build.gradle`, Gradle Wrapper, 소스 구조는 기존 구조를 유지합니다.
- `node_modules`, `dist`, `build`, `.gradle`, `.env` 등 생성물과 환경 파일은 Git에 포함하지 않습니다.
