package com.system.chattalk_serverside.security;


import com.system.chattalk_serverside.utils.TokenManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenManager tokenManager;

    private final CustomUserDetailsService customUserDetailsService;

    @Autowired
    public JwtAuthenticationFilter( TokenManager tokenManager, CustomUserDetailsService customUserDetailsService ) {
        this.tokenManager = tokenManager;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain ) throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader("Authorization");
//            final String authToken =request.getRequestURI()
            final String jwt;
            final String userEmail;

            // Check if Authorization header exists and starts with "Bearer "
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            // Extract JWT token from Authorization header
            jwt = authHeader.substring(7);
            userEmail = tokenManager.extractUsername(jwt);

            // If user email is extracted and no authentication exists in SecurityContext
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(userEmail);

                // Validate token
                if (tokenManager.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("User authenticated successfully: {}", userEmail);
                } else {
                    log.warn("Invalid JWT token for user: {}", userEmail);
                }
            }

        } catch (Exception e) {
            log.error("Error processing JWT authentication", e);
            // Don't throw exception, just continue with the filter chain
            // This allows the request to proceed without authentication
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter( HttpServletRequest request ) throws ServletException {
        String path = request.getRequestURI();

        // Skip JWT filter for authentication endpoints and public resources
        return path.startsWith("/api/auth/") || path.startsWith("/swagger-ui/") || path.startsWith("/v3/api-docs/") || path.startsWith("/actuator/") || path.equals("/error");
    }
}
