package com.be.razorpay.merchant.security;

import com.be.razorpay.merchant.entity.ApiKey;
import com.be.razorpay.merchant.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;


@Component
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    private static final String BASIC_PREFIX = "Basic ";
    private final ApiKeyRepository apiKeyRepository;
    private final MerchantContext merchantContext;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final PasswordEncoder BCRYPT = new BCryptPasswordEncoder();
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("Incoming request: {}", request.getRequestURI());

        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith(BASIC_PREFIX)) {
                filterChain.doFilter(request, response);
                return;
            }

//        Authorization: Basic key_asdlfjaosduf:secret_asdflauouadf
//        Authorization: Basic ASDFUAOSJDFLAKSJDFA89SDUFLIJalsdjflakjsdflk==


            String[] credentials = decode(header);
            if (credentials == null) {
                handlerExceptionResolver.resolveException(request, response, null, new BadRequestException("Malformed API Key Header"));

//                throw new BadRequestException("Malformed API Key Header");
            }

            String keyId = credentials[0];
            String rawSecret = credentials[1];


            Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyId(keyId);
            ApiKey apiKey;

            if (apiKeyOpt.isPresent()) {
                apiKey = apiKeyOpt.get();
            } else {
                handlerExceptionResolver.resolveException(
                        request,
                        response,
                        null,
                        new BadRequestException("Invalid or missing API Key")
                );
                return; // Stop further filter execution
            }

// Proceed with apiKey

            if (apiKey == null || !apiKey.isEnabled() || !secretMatches(rawSecret, apiKey)) {
                handlerExceptionResolver.resolveException(request, response, null, new BadRequestException("Invalid or missing API Key"));

//                throw new BadRequestException("Invalid or missing API Key");
            }

            var auth = new UsernamePasswordAuthenticationToken(keyId, null,
                    List.of(new SimpleGrantedAuthority("API_KEY_ROLE"))
            );

            SecurityContextHolder.getContext().setAuthentication(auth);
            merchantContext.setMerchantId(apiKey.getMerchant().getId());
            merchantContext.setKeyId(apiKey.getKeyId());

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }

    private String[] decode(String header) {
        String encoded = header.substring(BASIC_PREFIX.length());
        byte[] de=Base64.getDecoder().decode(encoded);
        String decoded = new String(de, StandardCharsets.UTF_8);

        int colon = decoded.indexOf(":");
        if (colon < 1) return null;

        return new String[]{decoded.substring(0, colon), decoded.substring(colon+1)};
    }
    private boolean secretMatches(String rawSecret, ApiKey apiKey) {
        if (BCRYPT.matches(rawSecret, apiKey.getKeySecretHash())) {
            return true;
        }
        return apiKey.isInGracePeriod()
                && apiKey.getPreviousKeySecretHash() != null
                && BCRYPT.matches(rawSecret, apiKey.getPreviousKeySecretHash());
    }
}
