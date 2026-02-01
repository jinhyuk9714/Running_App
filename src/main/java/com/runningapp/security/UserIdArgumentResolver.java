package com.runningapp.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @AuthenticationPrincipal Long userId 파라미터 해결
 *
 * HandlerMethodArgumentResolver: 컨트롤러 메서드 파라미터를 커스텀으로 해결
 * JwtAuthenticationFilter가 SecurityContext에 저장한 principal(Long userId)을 추출
 */
@Component
public class UserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @AuthenticationPrincipal Long userId 파라미터만 처리
        return parameter.getParameterType().equals(Long.class)
                && parameter.hasParameterAnnotation(com.runningapp.security.AuthenticationPrincipal.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof Long ? principal : null;
    }
}
