// 공통 헤더/푸터 삽입. 모든 페이지에서 <div id="header">, <div id="footer"> + 이 스크립트 사용
for (const id of ['header', 'footer']) {
  fetch(`/partials/${id}.html`).then(r => r.text()).then(h => { document.getElementById(id).innerHTML = h; });
}
