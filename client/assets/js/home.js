// 홈 화면: 등록된 AI 툴·카테고리 수를 서버에서 가져와 보여준다.
// 가져오지 못하면(서버 꺼짐 등) 숫자 영역을 통째로 숨겨서 빈 값이나 가짜 값이 보이지 않게 한다.
import { api } from '/assets/js/common/api.js';

const box = document.getElementById('home-stats');

function show(id, n) {
  document.getElementById(id).textContent = Number(n).toLocaleString('ko-KR');
}

try {
  const [models, categories] = await Promise.all([api('/models?size=1'), api('/models/categories')]);
  const toolCount = models.totalElements;
  const categoryCount = (categories.categories ?? []).length;
  if (!Number.isFinite(toolCount) || toolCount < 1) throw new Error('no data');
  show('stat-tools', toolCount);
  show('stat-categories', categoryCount);
  box.hidden = false;
} catch {
  box.hidden = true;
}
