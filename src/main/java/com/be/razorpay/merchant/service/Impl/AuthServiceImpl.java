package com.be.razorpay.merchant.service.Impl;

import com.be.razorpay.common.enums.MerchantStatus;
import com.be.razorpay.common.enums.UserRole;
import com.be.razorpay.common.exception.DuplicateResourceException;
import com.be.razorpay.common.exception.ResourceNotFoundException;
import com.be.razorpay.merchant.dto.request.LoginRequest;
import com.be.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.be.razorpay.merchant.dto.response.LoginResponse;
import com.be.razorpay.merchant.dto.response.MerchantResponse;
import com.be.razorpay.merchant.entity.AppUser;
import com.be.razorpay.merchant.entity.Merchant;
import com.be.razorpay.merchant.repository.AppUserRepository;
import com.be.razorpay.merchant.repository.MerchantRepository;
import com.be.razorpay.merchant.security.JwtUtil;
import com.be.razorpay.merchant.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final AppUserRepository appUserRepository;
    private final MerchantRepository merchantRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;



    @Override
    public MerchantResponse signup(MerchantSignupRequest request) {
        if (merchantRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("DUPLICATE_MERCHANT_EMAIL",
                    "Merchant with email already exists: " + request.email());
        }

//        Merchant merchant = merchantMapper.toEntityFromSignUpRequest(request);
        Merchant merchant = Merchant.builder()
                .businessName(request.businessName())
                .businessType(request.businessType())
                .name(request.name())
                .email(request.email())
                .status(MerchantStatus.PENDING_KYC)
                .build();


        merchant = merchantRepository.save(merchant);
        AppUser appUser = AppUser.builder()
                .email(request.email())
                .merchant(merchant)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.OWNER)
                .build();
        appUserRepository.save(appUser);
        MerchantResponse merchantResponse = new MerchantResponse(
                merchant.getId(), merchant.getName(), merchant.getEmail(),
                merchant.getBusinessName(), merchant.getBusinessType(),
                merchant.getStatus()
        );
        return merchantResponse;
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        AppUser appUser = appUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));

        String token = jwtUtil.generateAccessToken(request.email(), appUser.getMerchant().getId(), appUser.getRole().toString());

        return new LoginResponse(token);    }
}
