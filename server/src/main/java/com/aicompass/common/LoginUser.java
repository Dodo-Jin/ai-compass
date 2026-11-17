package com.aicompass.common;

import jakarta.servlet.http.HttpSession;

/**
 * 세션에서 로그인한 사용자 번호(Users.user_id)를 꺼낸다. 세션 키는 {@link SessionKeys#USER_ID}.
 * <pre>
 * // 로그인 필수 API (즐겨찾기, 보관함, 리뷰 작성 등): 비로그인이면 401 LOGIN_REQUIRED
 * Integer userId = LoginUser.require(session);
 *
 * // 로그인 선택 API (상세 페이지의 "내 즐겨찾기 여부" 등): 비로그인이면 null
 * Integer userId = LoginUser.find(session);
 * </pre>
 */
public final class LoginUser {

    private LoginUser() {}

    /** 로그인했으면 사용자 번호, 아니면 null */
    public static Integer find(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SessionKeys.USER_ID);
        if (value instanceof Integer id) {
            return id;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    /** 로그인했으면 사용자 번호, 아니면 401 {@link ApiException#loginRequired()} */
    public static Integer require(HttpSession session) {
        Integer userId = find(session);
        if (userId == null) {
            throw ApiException.loginRequired();
        }
        return userId;
    }
}
