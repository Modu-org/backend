package com.ssafy.modu.global.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.modu.global.auth.jwt.JwtAuthenticationFilter;
import com.ssafy.modu.global.exception.ErrorCode;
import com.ssafy.modu.global.exception.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // JWT 토큰을 검증하고 SecurityContext에 인증 정보를 저장하는 커스텀 필터
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 인증 실패 시 JSON 형태의 에러 응답을 만들기 위해 사용
    private final ObjectMapper objectMapper;

    /**
     * Spring Security의 핵심 보안 설정을 정의한다.
     *
     * 현재 프로젝트는 JWT 기반 인증 방식을 사용하므로
     * 세션, formLogin, httpBasic을 사용하지 않고 stateless 방식으로 동작한다.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // REST API 서버에서는 CSRF 토큰 방식을 사용하지 않으므로 비활성화
                .csrf(AbstractHttpConfigurer::disable)

                // 기본 로그인 폼을 사용하지 않으므로 비활성화
                .formLogin(AbstractHttpConfigurer::disable)

                // 브라우저 기본 인증 방식인 HTTP Basic 인증을 사용하지 않으므로 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)

                // JWT 기반 인증은 서버 세션을 사용하지 않으므로 STATELESS로 설정
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 보안 관련 HTTP Header 설정 (개발 중이니까 비활성화)
//                .headers(headers -> headers
//                        // HTTPS 접속을 강제하기 위한 HSTS 설정
//                        // 실제 운영 환경에서 HTTPS가 적용되어 있을 때 의미가 있다.
//                        .httpStrictTransportSecurity(hsts -> hsts
//                                // 서브 도메인에도 HSTS 적용
//                                .includeSubDomains(true)
//
//                                // 브라우저 preload 목록 등록을 고려한 설정
//                                .preload(true)
//
//                                // HSTS 적용 기간: 1년
//                                .maxAgeInSeconds(31536000)
//                        )
//                )

                // 요청 경로별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 인증 관련 API
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/check-id",
                                "/api/auth/logout"
                        ).permitAll()

                        // Swagger 문서 접근 허용
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 위에서 허용한 경로를 제외한 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )

                // 인증/인가 예외 처리 설정
                .exceptionHandling(exception -> exception
                        // 인증되지 않은 사용자가 보호된 API에 접근했을 때 실행
                        .authenticationEntryPoint((request, response, authException) -> {
                            ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.UNAUTHORIZED);

                            response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })
                )

                // UsernamePasswordAuthenticationFilter 이전에 JWT 인증 필터를 등록
                // 요청이 컨트롤러에 도달하기 전에 JWT 검증이 먼저 수행된다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 사용자 인증을 처리하는 AuthenticationProvider 등록.
     *
     * UserDetailsService를 통해 사용자 정보를 조회하고,
     * PasswordEncoder를 통해 비밀번호 일치 여부를 검증한다.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);

        // BCrypt 방식으로 암호화된 비밀번호를 검증
        provider.setPasswordEncoder(passwordEncoder);

        // 사용자 존재 여부가 외부에 노출되지 않도록 설정
        // true이면 아이디가 틀린 경우와 비밀번호가 틀린 경우를 구분하지 않는다.
        provider.setHideUserNotFoundExceptions(true);

        return provider;
    }

    /**
     * AuthenticationManager Bean 등록.
     *
     * 로그인 시 AuthenticationManager를 통해
     * 아이디/비밀번호 인증을 수행할 수 있다.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 비밀번호 암호화에 사용할 PasswordEncoder 등록.
     *
     * 회원가입 시 비밀번호를 BCrypt로 암호화하고,
     * 로그인 시 입력된 비밀번호와 암호화된 비밀번호를 비교한다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of("*"));

        configuration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}