// 트렌드 페이지 데이터 계층 (담당 C)
// 지금은 mock/ 폴더의 JSON을 읽고, 서버 API가 완성되면 USE_MOCK 만 false 로 바꾸면 된다.
// 응답 모양은 docs/api-spec.md 공통 규칙({ success, data })을 그대로 따른다.
import { api } from '../common/api.js';

const USE_MOCK = true;
const MOCK_DIR = '/assets/js/trend/mock';

// period 별 정렬 기준 (mock 전용. 실제로는 서버가 기간별 점수로 정렬해서 준다)
const MOCK_SORT_KEY = { realtime: 'trendScore', weekly: 'favoriteCount', monthly: 'searchCount' };

/**
 * 랭킹 목록
 * @param {{period?: 'realtime'|'weekly'|'monthly', category?: string, page?: number, size?: number}} q
 * @returns {Promise<{items: object[], page: number, size: number, total: number, updatedAt: string}>}
 */
export async function fetchRankings({ period = 'realtime', category = '전체', page = 1, size = 10 } = {}) {
  if (!USE_MOCK) {
    const qs = new URLSearchParams({ period, page, size });
    if (category !== '전체') qs.set('category', category);
    return (await api(`/v1/trends/rankings?${qs}`)).data;
  }

  const { data } = await getJson(`${MOCK_DIR}/rankings.json`);
  const key = MOCK_SORT_KEY[period] || 'trendScore';
  const filtered = data.items
    .filter((it) => category === '전체' || it.category === category)
    .sort((a, b) => b[key] - a[key])
    .map((it, i) => ({ ...it, rank: i + 1, isFavorite: favoriteState.get(it.modelId) ?? it.isFavorite }));

  const start = (page - 1) * size;
  return {
    items: filtered.slice(start, start + size),
    page,
    size,
    total: Math.min(filtered.length, 100),
    updatedAt: data.updatedAt,
  };
}

/** 최신 AI 동향 + 급상승 키워드 */
export async function fetchNews() {
  if (!USE_MOCK) return (await api('/v1/trends/news')).data;
  return (await getJson(`${MOCK_DIR}/news.json`)).data;
}

// mock 모드에서 즐겨찾기 상태를 페이지 안에서만 기억한다
const favoriteState = new Map();

/**
 * 즐겨찾기 추가/삭제 (api-spec: POST /api/v1/favorites, DELETE /api/v1/favorites/{modelId})
 * 로그인 기능이 들어오기 전까지는 mock 으로 동작한다.
 */
export async function setFavorite(modelId, on) {
  if (!USE_MOCK) {
    if (on) await api('/v1/favorites', { method: 'POST', body: { modelId } });
    else await api(`/v1/favorites/${modelId}`, { method: 'DELETE' });
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
