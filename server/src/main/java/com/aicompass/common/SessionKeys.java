package com.aicompass.common;

/** HttpSession 에 저장하는 키. 로그인 성공 시 auth 가 넣고, 다른 패키지는 이 상수로 꺼낸다. */
public final class SessionKeys {
    /** 값: users.user_id (Integer) */
    public static final String USER_ID = "userId";

    private SessionKeys() {}
}
