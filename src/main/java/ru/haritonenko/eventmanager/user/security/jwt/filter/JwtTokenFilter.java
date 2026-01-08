package ru.haritonenko.eventmanager.user.security.jwt.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.haritonenko.eventmanager.user.security.jwt.JwtTokenManager;
import ru.haritonenko.eventmanager.user.domain.User;
import ru.haritonenko.eventmanager.user.domain.service.UserService;

import java.io.IOException;
import java.util.List;

import static java.util.Objects.isNull;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenManager jwtTokenManager;
    private final UserService userService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (isNull(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            log.warn("Invalid header");
            filterChain.doFilter(request, response);
            return;
        }
        var jwtTokenWithoutWordBearer = authorizationHeader.substring(7);
        String loginFromToken;
        try {
            log.info("Getting login from token");
            loginFromToken = jwtTokenManager.getLoginFromToken(jwtTokenWithoutWordBearer);
        } catch (Exception ex) {
            log.warn("Error while reading jwt", ex);
            filterChain.doFilter(request, response);
            return;
        }
        log.info("Searching for user by login");
        User user = userService.findByLogin(loginFromToken);
        log.info("Getting token for user");
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                user,
                null,
                List.of(new SimpleGrantedAuthority(user.role().toString()))
        );
        SecurityContextHolder.getContext()
                .setAuthentication(token);
        filterChain.doFilter(request, response);
        log.info("Token was set for user");
    }
}
