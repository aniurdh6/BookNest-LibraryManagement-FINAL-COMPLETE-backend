package com.booknest.library.security;

import com.booknest.library.model.AppUser;
import com.booknest.library.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7).trim();

            try {
                String email = jwtService.extractUsername(token);
                users.findByEmailIgnoreCase(email).ifPresent(user -> authenticate(user));
            } catch (Exception ignored) {
                // Invalid/expired JWTs are handled by Spring Security as unauthenticated.
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(AppUser user) {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.role));
        var authentication = new UsernamePasswordAuthenticationToken(
                user.email,
                null,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
