// 목 모드에서만: 로그인 연동 전이라 헤더 사용자 pill 에 임시 이름을 보여준다.
// 실서버 모드에서는 아무것도 하지 않는다(헤더 기본값 "로그인" 유지 → A 의 auth 코드가 채움).
import { USE_MOCK } from './pipeline-api.js';

export const MOCK_USER = { username: 'user123', email: 'user@email.com' };

export async function showMockUser() {
  if (!USE_MOCK) return;
  await window.layoutReady;
  const link = document.getElementById('header-user');
  if (!link) return;
  link.querySelector('.pl-user-name').textContent = MOCK_USER.username;
  link.setAttribute('href', '/pages/mypage/library.html');
}
