package com.example.MyFirstApp.security;

import com.example.MyFirstApp.JwtUtil.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {

        String requestUri = request.getRequestURI();

        System.out.println("========================================");
        System.out.println("🔐 JWT FILTER");
        System.out.println("METHOD: " + request.getMethod());
        System.out.println("URI: " + requestUri);
        System.out.println(
                "X-DEBUG-REQUEST: " +
                        request.getHeader("X-Debug-Request")
        );
        String authHeader =
                request.getHeader("Authorization");

        System.out.println(
                "AUTH HEADER PRESENT: "
                        + (authHeader != null)
        );

        try {

            // =====================================================
            // NO AUTH HEADER
            // =====================================================

            if (
                    authHeader == null
                            || !authHeader.startsWith("Bearer ")
            ) {

                System.out.println(
                        "⚠️ NO BEARER TOKEN"
                );

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }


            // =====================================================
            // EXTRACT TOKEN
            // =====================================================

            String jwt =
                    authHeader.substring(7);

            System.out.println(
                    "TOKEN LENGTH: "
                            + jwt.length()
            );


            // =====================================================
            // EXTRACT EMAIL
            // =====================================================

            String email =
                    jwtUtil.extractUsername(jwt);

            System.out.println(
                    "JWT EMAIL: "
                            + email
            );


            if (
                    email == null
                            || email.isBlank()
            ) {

                System.err.println(
                        "❌ JWT EMAIL IS NULL/EMPTY"
                );

                filterChain.doFilter(
                        request,
                        response
                );

                return;
            }


            // =====================================================
            // AUTHENTICATION NOT ALREADY SET
            // =====================================================

            if (
                    SecurityContextHolder
                            .getContext()
                            .getAuthentication()
                            == null
            ) {

                UserDetails userDetails =
                        userDetailsService
                                .loadUserByUsername(
                                        email
                                );


                // =================================================
                // VALIDATE TOKEN
                // =================================================

                boolean valid =
                        jwtUtil.validateToken(
                                jwt,
                                userDetails
                        );

                System.out.println(
                        "JWT VALID: "
                                + valid
                );


                if (valid) {

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(
                                            request
                                    )
                    );


                    SecurityContextHolder
                            .getContext()
                            .setAuthentication(
                                    authToken
                            );


                    System.out.println(
                            "✅ SECURITY CONTEXT AUTHENTICATED"
                    );

                    System.out.println(
                            "USER: "
                                    + userDetails.getUsername()
                    );

                } else {

                    System.err.println(
                            "❌ JWT VALIDATION FAILED"
                    );
                }

            } else {

                System.out.println(
                        "✅ SECURITY CONTEXT ALREADY AUTHENTICATED"
                );
            }


        } catch (Exception e) {

            System.err.println(
                    "========================================"
            );

            System.err.println(
                    "❌ JWT FILTER ERROR"
            );

            System.err.println(
                    "TYPE: "
                            + e.getClass().getName()
            );

            System.err.println(
                    "MESSAGE: "
                            + e.getMessage()
            );

            System.err.println(
                    "========================================"

            );

            e.printStackTrace();
        }


        System.out.println(
                "========================================"
        );

        filterChain.doFilter(
                request,
                response
        );
    }
}