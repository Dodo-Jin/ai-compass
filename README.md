# 🧭 AI Compass
AI 툴 디렉토리 + 챗봇 파이프라인 추천 플랫폼 (Bootstrap 5 / FlexStart, Spring Boot(Java 21) + MySQL)

## 실행
```bash
cp .env.example .env      # 값 채우기
cd server && gradle bootRun     # gradle 미설치 시 IntelliJ에서 server 폴더를 Gradle 프로젝트로 열고 AiCompassApplication 실행
```
`http://localhost:3000` 에서 `client/` 가 정적으로 서빙됩니다.

## 담당
| 담당 | 영역 |
|---|---|
| A | db/, 로그인·가입(auth), AI 디렉토리·상세·리뷰 |
| B | 챗봇, 파이프라인 시각화, 마이페이지 보관함 |
| C | 트렌드 피드, 마이페이지 즐겨찾기 비교표 |

## 규칙
- `main` 직접 push 금지, `develop` 에서 `feature/<이름>-<기능>` 브랜치로 작업 후 PR
- 내 폴더만 수정, 공용 파일(`partials/`, `common.*`, `config/`)은 작은 PR로 먼저 머지
- 새 API: `server/src/main/java/com/aicompass/<기능>/` 패키지 안에 Controller/Service/Repository 를 추가 (기능 패키지 밖은 수정 금지)
- FlexStart 원본은 `client/assets/vendor/` 에 넣고 수정하지 않기
