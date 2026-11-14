// 설문 모드의 선택지. 서버로는 앞의 코드(marketer 등)를 보내고, 화면에는 뒤의 글자를 보여준다.
// 예산 구간은 A 와 확정되면 여기만 고치면 된다.
export const SURVEY = {
  job: {
    label: '직업 선택',
    options: [['marketer', '마케터'], ['developer', '개발자'], ['creator', '콘텐츠 제작자'], ['other', '기타']],
  },
  usage: {
    label: '사용량 선택',
    options: [['personal', '개인용'], ['team', '팀용'], ['enterprise', '엔터프라이즈']],
  },
  budget: {
    label: '예산 선택',
    options: [['free', '무료'], ['under30k', '3만원 이하'], ['30k-150k', '3~15만원'], ['over150k', '15만원 이상']],
  },
};

export function labelOf(group, code) {
  const found = SURVEY[group].options.find(([c]) => c === code);
  return found ? found[1] : code;
}
