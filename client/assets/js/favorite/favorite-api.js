// 즐겨찾기 비교 화면이 쓰는 API 모음 (담당 C). 서버 API 는 docs/api-spec.md "즐겨찾기 (C)".
// 주소에 ?mock=1 을 붙이면 서버 없이 mock/favorites.json 으로 동작한다 (?mock=0 으로 해제).
// B 화면과 같은 sessionStorage 키를 써서, 한 탭에서 마이페이지 탭을 오가도 목 모드가 유지된다.
import { api } from '/assets/js/common/api.js';

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

// ---------------------------------------------------------------- 실제 서버

const server = {
  me: () => api('/users/me'),
  list: (category) => {
    const qs = new URLSearchParams({ page: 1, size: 100 });
    if (category) qs.set('category', category);
    return api(`/favorites?${qs}`);
  },
  compare: (ids) => api(`/favorites/compare?modelIds=${ids.join(',')}`),
  remove: (modelId) => api(`/favorites/${modelId}`, { method: 'DELETE' }),
  add: (modelId) => api('/favorites', { method: 'POST', body: { modelId } }),
  count: (path) => api(path).then((r) => r.totalElements),
};

// ---------------------------------------------------------------- 목 (?mock=1)

let mockModels = null;
const mockFavorites = new Set();

async function mockData() {
  if (!mockModels) {
    const res = await fetch('/assets/js/favorite/mock/favorites.json');
    mockModels = (await res.json()).models;
    mockModels.forEach((m) => mockFavorites.add(m.modelId));
  }
  return mockModels;
}
const wait = (ms = 120) => new Promise((r) => setTimeout(r, ms));

const mock = {
  me: async () => ({ username: 'user123', email: 'user@email.com' }),
  list: async (category) => {
    await wait();
    const all = (await mockData()).filter((m) => mockFavorites.has(m.modelId));
    const items = all.filter((m) => !category || m.category === category);
    return {
      items,
      categories: [...new Set(all.map((m) => m.category))].sort(),
      page: 1,
      size: 100,
      totalElements: items.length,
      totalPages: 1,
    };
  },
  compare: async (ids) => {
    await wait();
    const byId = new Map((await mockData()).map((m) => [m.modelId, m]));
    return { items: ids.map((id) => ({ ...byId.get(id), isFavorited: mockFavorites.has(id) })) };
  },
  remove: async (modelId) => {
    await wait();
    mockFavorites.delete(modelId);
    return null;
  },
  add: async (modelId) => {
    await wait();
    mockFavorites.add(modelId);
    return { modelId, isFavorited: true };
  },
  count: async (path) => (path.startsWith('/pipelines') ? 12 : 3),
};

const impl = USE_MOCK ? mock : server;

/** 로그인한 사용자 { username, email }. 비로그인이면 401 ApiError */
export const loadUser = () => impl.me();
/** 내 즐겨찾기 목록 { items, categories, totalElements } */
export const listFavorites = (category) => impl.list(category);
/** 비교표 { items } (ids 순서대로) */
export const compareModels = (ids) => impl.compare(ids);
export const removeFavorite = (modelId) => impl.remove(modelId);
export const addFavorite = (modelId) => impl.add(modelId);
/** 목록 API 의 totalElements (프로필 카드 숫자용). 실패하면 null */
export const countOf = (path) => impl.count(path).catch(() => null);

/** 로그인 화면 주소. 로그인하면 이 화면으로 돌아온다 (A 의 login.html 이 ?redirect= 를 처리) */
export function loginUrl() {
  return '/pages/auth/login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
}
