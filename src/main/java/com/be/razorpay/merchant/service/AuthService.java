package com.be.razorpay.merchant.service;

import com.be.razorpay.merchant.dto.request.LoginRequest;
import com.be.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.be.razorpay.merchant.dto.response.LoginResponse;
import com.be.razorpay.merchant.dto.response.MerchantResponse;

public interface AuthService {

    MerchantResponse signup(MerchantSignupRequest request);
    LoginResponse login(LoginRequest request);

}
