// 트렌드 페이지 데이터 계층 (담당 C). 서버 API 는 docs/api-spec.md "트렌드 (C)".
// - 랭킹: GET /api/v1/trends/rankings (api() 가 /api/v1 을 붙이고 data 만 돌려준다)
// - AI 동향·급상승 키워드: 정적 파일 data/news.json (DB 테이블 없이 팀이 직접 고친다)
// - 주소에 ?mock=1 을 붙이면 서버 없이 mock/rankings.json 으로 동작한다 (?mock=0 으로 해제).
//   마이페이지 화면들과 같은 sessionStorage 키를 쓴다.
import { api } from '../common/api.js';

const MOCK_KEY = 'aicompass.useMock';

function resolveUseMock() {
  const param = new URLSearchParams(location.search).get('mock');
  try {
    if (param != null) sessionStorage.setItem(MOCK_KEY, param === '0' ? '0' : '1');
    return sessionStorage.getItem(MOCK_KEY) === '1';
  } catch {
    return param != null && param !== '0';
  }
}

export const USE_MOCK = resolveUseMock();
const DIR = '/assets/js/trend';

// mock 에서 기간별 정렬 기준 (실제로는 서버가 기간별 점수로 정렬해서 준다)
const MOCK_SORT_KEY = { realtime: 'trendScore', weekly: 'favoriteCount', monthly: 'searchCount' };

/**
 * 랭킹 목록
 * @param {{period?: 'realtime'|'weekly'|'monthly', category?: string, page?: number, size?: number}} q category 는 '' 이면 전체
 * @returns {Promise<{items: object[], categories: string[], page: number, size: number, totalElements: number, updatedAt: string}>}
 */
export async function fetchRankings({ period = 'realtime', category = '', page = 1, size = 10 } = {}) {
  if (!USE_MOCK) {
    const qs = new URLSearchParams({ period, page, size });
    if (category) qs.set('category', category);
    return api(`/trends/rankings?${qs}`);
  }

  const { data } = await getJson(`${DIR}/mock/rankings.json`);
  const key = MOCK_SORT_KEY[period] || 'trendScore';
  const filtered = data.items
    .filter((it) => !category || it.category === category)
    .sort((a, b) => b[key] - a[key])
    .map((it, i) => ({ ...it, rank: i + 1, isFavorited: favoriteState.get(it.modelId) ?? it.isFavorited }));

  const start = (page - 1) * size;
  return {
    items: filtered.slice(start, start + size),
    categories: [...new Set(data.items.map((it) => it.category))],
    page,
    size,
    totalElements: Math.min(filtered.length, 100),
    updatedAt: data.updatedAt,
  };
}

/** 최신 AI 동향 + 급상승 키워드 (정적 파일) */
export async function fetchNews() {
  return (await getJson(`${DIR}/data/news.json`)).data;
}

// mock 모드에서 즐겨찾기 상태를 페이지 안에서만 기억한다
const favoriteState = new Map();

/** 즐겨찾기 추가/삭제 (POST /favorites, DELETE /favorites/{modelId}). 비로그인이면 401 ApiError */
export async function setFavorite(modelId, on) {
  if (!USE_MOCK) {
    if (on) await api('/favorites', { method: 'POST', body: { modelId } });
    else await api(`/favorites/${modelId}`, { method: 'DELETE' });
    return;
  }
  await new Promise((r) => setTimeout(r, 150));
  favoriteState.set(modelId, on);
}

async function getJson(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`${url} 를 불러오지 못했습니다 (${res.status})`);
  return res.json();
}
