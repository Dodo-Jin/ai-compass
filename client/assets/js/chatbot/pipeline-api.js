// 챗봇 · 파이프라인 · 보관함 화면이 쓰는 API 모음 (담당 B).
// 화면 코드는 이 파일의 함수만 부른다. 백엔드가 준비되면 DEFAULT_USE_MOCK 만 false 로 바꾸면 된다.
//   - 주소에 ?mock=1 / ?mock=0 을 붙이면 그 탭에서만 목/실서버를 바꿔 볼 수 있다(sessionStorage 에 기억).
import { api } from '/assets/js/common/api.js';
import { mockApi } from './mock-api.js';

const DEFAULT_USE_MOCK = true;

function resolveUseMock() {
  const param = new URLSearchParams(location.search).get('mock');
  try {
    if (param != null) sessionStorage.setItem('aicompass.useMock', param === '0' ? '0' : '1');
    const saved = sessionStorage.getItem('aicompass.useMock');
    if (saved != null) return saved !== '0';
  } catch {
    if (param != null) return param !== '0';
  }
  return DEFAULT_USE_MOCK;
}

export const USE_MOCK = resolveUseMock();

// A 의 툴 API 모양이 확정되기 전이라 여러 이름을 받아 하나로 맞춘다
function normalizeTool(t) {
  return {
    modelId: t.modelId ?? t.model_id ?? t.id,
    name: t.name,
    category: t.category ?? '',
    logoUrl: t.logoUrl ?? t.logo_url ?? null,
  };
}

const real = {
  /** POST /api/chat  { message } → { reply, blocks: [{ modelId, purpose, modelName, category, logoUrl }] } */
  chat: (message) => api('/chat', { method: 'POST', body: { message } }),

  /**
   * POST /api/pipelines/recommend-by-survey  { job, usage, budget } → 툴 1개
   *   { modelId, name, category, logoUrl, subtitle?, rating?, reviewCount?, reason?, features?, priceText? }
   * modelId·name 외의 항목은 DB 에 값이 있을 때만 내려주면 되고, 화면은 있는 항목만 보여준다.
   */
  recommendBySurvey: async (body) => {
    const r = await api('/pipelines/recommend-by-survey', { method: 'POST', body });
    return { ...r, modelId: r.modelId ?? r.model_id ?? r.id, name: r.name ?? r.modelName, logoUrl: r.logoUrl ?? r.logo_url ?? null };
  },

  /** 캔버스의 "툴 추가" 목록. A 의 툴 API(GET /api/tools)를 쓴다 — 응답 형태는 A 와 확정 필요 */
  listTools: async () => {
    const res = await api('/tools');
    return (Array.isArray(res) ? res : res.items ?? []).map(normalizeTool);
  },

  /** GET /api/pipelines?q=&sort=&page=&size= → { items, page, size, totalElements, totalPages } */
  listPipelines: ({ q = '', sort = 'latest', page = 0, size = 12 } = {}) => {
    const qs = new URLSearchParams({ sort, page, size });
    if (q) qs.set('q', q);
    return api(`/pipelines?${qs}`);
  },

  getPipeline: (id) => api(`/pipelines/${id}`),
  createPipeline: (body) => api('/pipelines', { method: 'POST', body }),
  updatePipeline: (id, body) => api(`/pipelines/${id}`, { method: 'PUT', body }),
  deletePipeline: (id) => api(`/pipelines/${id}`, { method: 'DELETE' }),
};

export const {
  chat: sendChat,
  recommendBySurvey,
  listTools,
  listPipelines,
  getPipeline,
  createPipeline,
  updatePipeline,
  deletePipeline,
} = USE_MOCK ? mockApi : real;
