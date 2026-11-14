// 백엔드 연동 전 화면 개발용 목(mock) API. pipeline-api.js 가 USE_MOCK 일 때만 사용한다.
// 응답 형태·검증 규칙·오류 문구는 서버의 Pipelines API(docs/api-spec.md)와 같게 맞춰 두었다.
// 파이프라인은 localStorage 에 저장되어 새로고침·페이지 이동 후에도 남는다.
import { TITLE_MAX, BLOCKS_MAX, PURPOSE_MAX } from './util.js';
import { SURVEY, labelOf } from './survey-options.js';

const STORAGE_KEY = 'aicompass.mock.pipelines.v1';

// 임시 툴 목록. 실제 category 값은 A 가 확정하면 그때 맞춘다.
export const TOOLS = [
  { modelId: 1, name: 'ChatGPT', category: '텍스트', logoUrl: null },
  { modelId: 2, name: 'Claude', category: '텍스트', logoUrl: null },
  { modelId: 3, name: 'Perplexity', category: '리서치', logoUrl: null },
  { modelId: 4, name: 'Midjourney', category: '이미지', logoUrl: null },
  { modelId: 5, name: 'DALL·E', category: '이미지', logoUrl: null },
  { modelId: 6, name: 'Runway', category: '영상', logoUrl: null },
  { modelId: 7, name: 'Pika', category: '영상', logoUrl: null },
  { modelId: 8, name: 'GitHub Copilot', category: '코딩', logoUrl: null },
  { modelId: 9, name: 'Cursor', category: '코딩', logoUrl: null },
  { modelId: 10, name: 'ElevenLabs', category: '음성', logoUrl: null },
  { modelId: 11, name: 'Gamma', category: '생산성', logoUrl: null },
  { modelId: 12, name: 'Notion AI', category: '생산성', logoUrl: null },
  { modelId: 13, name: 'DeepL', category: '텍스트', logoUrl: null },
];
const byId = new Map(TOOLS.map((t) => [t.modelId, t]));

// 챗봇 목 응답: 키워드가 들어 있으면 해당 조합을 추천한다
const CHAT_RULES = [
  { title: '블로그 글 자동화', keys: ['블로그', '글', '콘텐츠', '마케팅', 'sns', '카드뉴스'], picks: [[1, '초안 작성'], [4, '썸네일 이미지']] },
  { title: '유튜브 쇼츠 자동 제작', keys: ['유튜브', '영상', '쇼츠', '릴스'], picks: [[1, '스크립트 작성'], [10, '음성 생성'], [6, '영상 생성']] },
  { title: '발표 자료 제작', keys: ['발표', 'ppt', '보고서', '프레젠', '피칭'], picks: [[3, '자료 조사'], [2, '구조·내용 정리'], [11, '슬라이드 제작']] },
  { title: '개발 작업 흐름', keys: ['코드', '개발', '앱', '프로그래밍', '코딩'], picks: [[2, '설계·리뷰'], [9, '코드 작성'], [8, '자동완성']] },
  { title: '이미지 콘텐츠 제작', keys: ['이미지', '디자인', '포스터', '로고'], picks: [[1, '콘셉트 정리'], [4, '이미지 생성']] },
  { title: '논문 요약 흐름', keys: ['논문', '조사', '리서치', '공부', '분석'], picks: [[3, '자료 검색'], [2, '요약·정리']] },
];
const DEFAULT_TITLE = '나만의 AI 파이프라인';
const DEFAULT_PICKS = [[1, '아이디어 정리'], [2, '내용 다듬기']];

const SAMPLES = [
  ['블로그 글 + 썸네일 자동화', [[1, '초안 작성'], [4, '썸네일 이미지']]],
  ['유튜브 쇼츠 제작', [[1, '대본 작성'], [10, '내레이션'], [6, '영상 생성']]],
  ['발표 자료 만들기', [[3, '자료 조사'], [2, '구조 정리'], [11, '슬라이드 제작']]],
  ['사이드 프로젝트 개발', [[2, '설계'], [9, '코드 작성'], [8, '자동완성']]],
  ['논문 읽고 정리하기', [[3, '논문 검색'], [2, '요약']]],
  ['SNS 카드뉴스', [[1, '카피'], [5, '이미지'], [11, '레이아웃']]],
  ['회의록 정리', [[12, '회의 내용 정리']]],
  ['제품 소개 영상', [[1, '시나리오'], [4, '콘셉트 이미지'], [6, '영상 생성'], [10, '음성']]],
  ['영어 이메일 작성', [[1, '초안'], [13, '번역 검수']]],
  ['뉴스레터 발행', [[3, '소재 수집'], [1, '본문 작성']]],
  ['포트폴리오 사이트', [[2, '구조 설계'], [9, '코드 작성'], [4, '이미지 제작']]],
  ['팟캐스트 제작', [[1, '대본'], [10, '음성 합성']]],
  ['시장 조사 리포트', [[3, '자료 수집'], [2, '분석'], [11, '보고서 슬라이드']]],
  ['코드 리뷰 자동화', [[8, '리뷰 보조'], [2, '리팩터링 제안']]],
];

// 설문 모드 목 데이터: 직업별 추천 툴, 툴별 설명, 예산별 가격 문구
const SURVEY_PICKS = { marketer: 1, developer: 9, creator: 4, other: 12 };
const TOOL_INFO = {
  1: { kind: '텍스트 생성', subtitle: '텍스트 생성 · Text Generation', rating: 4.6, reviewCount: 342, blurb: '카피, 기획, 요약 같은 글 작업 전반에 쓰기 좋습니다.', features: ['자연스러운 한국어 글쓰기', '이미지·파일을 올려 분석', '다양한 외부 앱과 연동'] },
  4: { kind: '이미지 생성', subtitle: '이미지 생성 · Image Generation', rating: 4.3, reviewCount: 128, blurb: '썸네일, SNS 콘텐츠 제작에 특화되어 있습니다.', features: ['고품질 이미지 생성 (4K 지원)', 'Discord 기반 간편한 인터페이스', '다양한 스타일 프리셋 지원'] },
  9: { kind: '코딩', subtitle: '코드 에디터 · AI Coding', rating: 4.5, reviewCount: 96, blurb: '프로젝트 전체 맥락을 보고 코드를 작성하는 데 쓰기 좋습니다.', features: ['프로젝트 전체를 이해한 코드 제안', '대화로 코드 수정 요청', '익숙한 편집기 환경'] },
  12: { kind: '생산성', subtitle: '생산성 · Workspace AI', rating: 4.1, reviewCount: 74, blurb: '문서 정리와 아이디어 메모에 쓰기 좋습니다.', features: ['문서 요약·정리', '메모와 작업을 한곳에서 관리', '팀과 쉽게 공유'] },
};
const PRICE_TEXT = {
  free: '무료 티어로 시작할 수 있어요',
  under30k: '₩13,000 ~ 30,000 / 월 (Pro급)',
  '30k-150k': '₩30,000 ~ 150,000 / 월 (Max급)',
  over150k: '₩150,000 이상 / 월 (최상위 플랜)',
};

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

// 서버(LocalDateTime)와 같은 "2026-11-03T19:30:00" 형태
function isoLocal(date) {
  const p = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}T${p(date.getHours())}:${p(date.getMinutes())}:${p(date.getSeconds())}`;
}

function seed() {
  const now = Date.now();
  const items = SAMPLES.map(([title, picks], i) => ({
    pipelineId: i + 1,
    title,
    createdAt: isoLocal(new Date(now - (SAMPLES.length - i) * 36 * 3600 * 1000)),
    blocks: picks.map(([modelId, purpose], k) => ({ modelId, order: k + 1, purpose })),
  }));
  return { nextId: items.length + 1, items };
}

let db = null;
function state() {
  if (db) return db;
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) db = JSON.parse(raw);
  } catch {
    db = null;
  }
  if (!db) {
    db = seed();
    persist();
  }
  return db;
}

function persist() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(db));
  } catch {
    // 저장소를 못 쓰는 환경(시크릿 모드 등)에서는 페이지가 열려 있는 동안만 유지된다
  }
}

// 서버의 normalizeBlocks 와 같은 검증. 순서는 배열 순서로 1부터 다시 매긴다
function normalizeBlocks(input) {
  if (!Array.isArray(input) || input.length === 0) throw new Error('블록은 1개 이상이어야 합니다.');
  if (input.length > BLOCKS_MAX) throw new Error(`블록은 최대 ${BLOCKS_MAX}개까지 추가할 수 있습니다.`);
  const missing = [];
  const blocks = input.map((b, i) => {
    if (!b || b.modelId == null) throw new Error('각 블록에는 modelId 가 필요합니다.');
    const purpose = typeof b.purpose === 'string' && b.purpose.trim() ? b.purpose.trim() : null;
    if (purpose && purpose.length > PURPOSE_MAX) throw new Error(`purpose 는 ${PURPOSE_MAX}자 이하여야 합니다.`);
    if (!byId.has(b.modelId)) missing.push(b.modelId);
    return { modelId: b.modelId, order: i + 1, purpose };
  });
  if (missing.length) throw new Error(`존재하지 않는 툴입니다: [${missing.join(', ')}]`);
  return blocks;
}

function normalizeTitle(title) {
  const t = (title || '').trim();
  if (!t) throw new Error('제목을 입력해 주세요.');
  if (t.length > TITLE_MAX) throw new Error(`제목은 ${TITLE_MAX}자 이하여야 합니다.`);
  return t;
}

// 저장된 형태 → 응답 형태 (이름·분류는 조회 시 조인)
function view(p) {
  return {
    pipelineId: p.pipelineId,
    title: p.title,
    createdAt: p.createdAt,
    blocks: p.blocks.map((b) => {
      const m = byId.get(b.modelId);
      return { ...b, modelName: m ? m.name : null, logoUrl: m ? m.logoUrl : null, category: m ? m.category : null };
    }),
  };
}

function find(id) {
  const p = state().items.find((x) => x.pipelineId === Number(id));
  if (!p) throw new Error('파이프라인을 찾을 수 없습니다.');
  return p;
}

export const mockApi = {
  async chat(message) {
    await delay(900);
    const text = (message || '').trim();
    if (!text) throw new Error('고민을 입력해 주세요.');
    const lower = text.toLowerCase();
    const picks = [];
    let title = null;
    for (const rule of CHAT_RULES) {
      if (rule.keys.some((k) => lower.includes(k))) {
        picks.push(...rule.picks);
        title = title ?? rule.title; // 처음 맞은 규칙의 제목을 쓴다
      }
    }
    const chosen = (picks.length ? picks : DEFAULT_PICKS)
      .filter(([id], i, arr) => arr.findIndex(([x]) => x === id) === i) // 같은 툴은 한 번만
      .slice(0, 5);
    const blocks = chosen.map(([modelId, purpose]) => {
      const m = byId.get(modelId);
      return { modelId, purpose, modelName: m.name, category: m.category, logoUrl: m.logoUrl };
    });
    const short = text.length > 30 ? `${text.slice(0, 30)}…` : text;
    return {
      reply: `“${short}” 상황에 맞춰 ${blocks.length}단계 조합을 추천드려요.`,
      title: title ?? DEFAULT_TITLE,
      blocks,
    };
  },

  async recommendBySurvey({ job, usage, budget } = {}) {
    await delay(700);
    if (!SURVEY_PICKS[job] || !PRICE_TEXT[budget] || !SURVEY.usage.options.some(([c]) => c === usage)) {
      throw new Error('직업, 사용량, 예산을 모두 선택해 주세요.');
    }
    const tool = byId.get(SURVEY_PICKS[job]);
    const info = TOOL_INFO[tool.modelId];
    return {
      modelId: tool.modelId,
      name: tool.name,
      category: tool.category,
      logoUrl: tool.logoUrl,
      subtitle: info.subtitle,
      rating: info.rating,
      reviewCount: info.reviewCount,
      reason: `${labelOf('job', job)} · ${labelOf('usage', usage)} · ${labelOf('budget', budget)} 예산에 가장 적합한 ${info.kind} AI입니다. ${info.blurb}`,
      features: info.features,
      priceText: PRICE_TEXT[budget],
    };
  },

  async listTools() {
    await delay(200);
    return TOOLS.map((t) => ({ ...t }));
  },

  async listPipelines({ q = '', sort = 'latest', page = 0, size = 12 } = {}) {
    await delay(250);
    if (!['latest', 'oldest', 'title'].includes(sort)) throw new Error('sort 는 latest, oldest, title 중 하나여야 합니다.');
    const keyword = (q || '').trim().toLowerCase();
    let rows = state().items.filter((p) => !keyword || p.title.toLowerCase().includes(keyword));
    const by = {
      latest: (a, b) => b.createdAt.localeCompare(a.createdAt) || b.pipelineId - a.pipelineId,
      oldest: (a, b) => a.createdAt.localeCompare(b.createdAt) || a.pipelineId - b.pipelineId,
      title: (a, b) => a.title.localeCompare(b.title, 'ko') || b.pipelineId - a.pipelineId,
    }[sort];
    rows = [...rows].sort(by);
    const pageSize = Math.min(Math.max(size, 1), 50);
    const pageNo = Math.max(page, 0);
    return {
      items: rows.slice(pageNo * pageSize, (pageNo + 1) * pageSize).map(view),
      page: pageNo,
      size: pageSize,
      totalElements: rows.length,
      totalPages: Math.ceil(rows.length / pageSize),
    };
  },

  async getPipeline(id) {
    await delay(200);
    return view(find(id));
  },

  async createPipeline({ title, blocks }) {
    await delay(300);
    const t = normalizeTitle(title);
    const b = normalizeBlocks(blocks);
    const s = state();
    const p = { pipelineId: s.nextId++, title: t, createdAt: isoLocal(new Date()), blocks: b };
    s.items.push(p);
    persist();
    return view(p);
  },

  async updatePipeline(id, { title, blocks }) {
    await delay(300);
    const p = find(id);
    p.title = normalizeTitle(title);
    p.blocks = normalizeBlocks(blocks);
    persist();
    return view(p);
  },

  async deletePipeline(id) {
    await delay(250);
    const p = find(id);
    const s = state();
    s.items = s.items.filter((x) => x !== p);
    persist();
    return { deleted: p.pipelineId };
  },
};
