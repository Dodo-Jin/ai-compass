# 🧭 AI Compass
AI 툴 디렉토리 + 챗봇 파이프라인 추천 플랫폼 (Bootstrap 5 / FlexStart, Node.js + MySQL)

## 실행
```bash
cp .env.example .env      # 값 채우기
cd server && npm install && npm start
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
- 새 API 라우트: `server/routes/<이름>.routes.js` 만 만들면 `/api/<이름>` 으로 자동 등록
- FlexStart 원본은 `client/assets/vendor/` 에 넣고 수정하지 않기
