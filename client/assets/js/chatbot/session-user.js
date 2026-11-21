// 로그인 사용자 도우미 (B 화면: 챗봇 · 설문 · 보관함).
// - 헤더의 "로그인" 칸에 이름을 채우고(A 의 다른 페이지와 같은 방식), 프로필 카드에 쓸 사용자 정보를 돌려준다.
// - 401 이면 로그인 화면으로 보내거나 "로그인하기" 링크를 보여준다. 로그인하면 원래 화면으로 돌아온다(?redirect=).
// 목 모드(?mock=1)에서는 서버 없이 임시 사용자를 쓴다.
import { api } from '/assets/js/common/api.js';
import { h } from './util.js';
import { USE_MOCK } from './pipeline-api.js';

export const MOCK_USER = { username: 'user123', email: 'user@email.com' };

/** 로그인 화면 주소. 로그인하면 지금 보던 화면으로 돌아온다 */
export function loginUrl() {
  return '/pages/auth/login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
}

/** 안내 문구 뒤에 붙이는 "로그인하기" 링크 */
export function loginLink(text = '로그인하기') {
  return h('a', { class: 'alert-link', href: loginUrl() }, text);
}

/** 오류가 401(로그인 필요)이면 로그인 화면으로 보내고 true 를 돌려준다 */
export function redirectIfUnauthorized(err) {
  if (err && err.status === 401) {
    location.href = loginUrl();
    return true;
  }
  return false;
}

/** 로그인한 사용자 { username, email, ... }. 비로그인이거나 불러오지 못하면 null */
export async function loadUser() {
  if (USE_MOCK) return MOCK_USER;
  try {
    return await api('/users/me');
  } catch {
    return null;
  }
}

/** 헤더 사용자 칸을 채운다(비로그인이면 기본 "로그인" 그대로). 사용자 정보를 돌려준다 */
export async function showHeaderUser() {
  const [user] = await Promise.all([loadUser(), window.layoutReady]);
  const link = document.getElementById('header-user');
  if (user && link) {
    link.querySelector('.pl-user-name').textContent = user.username;
    link.setAttribute('href', '/pages/mypage/index.html');
  }
  return user;
}
