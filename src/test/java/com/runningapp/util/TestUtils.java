package com.runningapp.util;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.ResultActions;

/** 테스트 유틸리티 - JWT 토큰 추출 등 */
public class TestUtils {

    /** 로그인/회원가입 응답에서 accessToken 추출 (인증 테스트용) */
    public static String extractAccessToken(ResultActions resultActions) throws Exception {
        String response = resultActions.andReturn().getResponse().getContentAsString();
        return JsonPath.parse(response).read("$.accessToken", String.class);
    }
}
