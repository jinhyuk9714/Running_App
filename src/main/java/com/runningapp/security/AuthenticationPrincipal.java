package com.runningapp.security;

import java.lang.annotation.*;

/**
 * 현재 로그인한 사용자 ID를 주입하는 커스텀 어노테이션
 *
 * UserIdArgumentResolver가 이 어노테이션이 붙은 Long 파라미터에
 * SecurityContext의 principal(userId)을 주입
 *
 * Spring 기본 @AuthenticationPrincipal은 UserDetails를 주입하는데,
 * 우리는 JWT에서 userId만 사용하므로 커스텀 어노테이션 사용
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthenticationPrincipal {
    boolean required() default true;
}
