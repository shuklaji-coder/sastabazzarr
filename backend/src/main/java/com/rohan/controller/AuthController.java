package com.rohan.controller;

import com.rohan.Config.JwtProvider;
import com.rohan.Repository.VerificationCodeRepository;
import com.rohan.model.USER_ROLE;
import com.rohan.model.VerificationCode;
import com.rohan.request.LoginOtpRequest;
import com.rohan.request.Loginrequest;
import com.rohan.response.ApiResponse;
import com.rohan.response.AuthResponse;
import com.rohan.response.SignupRequest;
import com.rohan.service.imp.AuthService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private VerificationCodeRepository verificationCodeRepository;




    @PostMapping("/sent/login-signup-otp")
    public ResponseEntity<ApiResponse> sentOtpHandler(
            @RequestBody LoginOtpRequest req) throws Exception {

        USER_ROLE role = USER_ROLE.valueOf(req.getRole().toUpperCase());

        authService.sentLoginOtp(req.getEmail(), role);
        
        ApiResponse res = new ApiResponse();
        res.setMessage("Otp sent successfully");
        
        // TEMPORARY: Return OTP in response for testing (REMOVE IN PRODUCTION)
        // Comment this out in production
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(req.getEmail());
        if (verificationCode != null) {
            res.setMessage("Otp sent successfully. OTP: " + verificationCode.getOtp() + " (Testing Mode)");
        }
        
        return ResponseEntity.ok(res);
    }


    @PostMapping("/signing")
    public ResponseEntity<AuthResponse> loginHandler(
            @RequestBody Loginrequest req) throws Exception {

        AuthResponse authResponse = authService.signing(req);

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signupHandler(
            @RequestBody SignupRequest req) throws Exception {

        String token = authService.createUser(req);

        AuthResponse res = new AuthResponse();
        res.setJwt(token);
        res.setMessage("Signup successful");
        res.setRole(USER_ROLE.ROLE_CUSTOMER); // From service layer defaults

        return ResponseEntity.ok(res);
    }

    // TEMPORARY: Get OTP for testing (REMOVE IN PRODUCTION)
    @GetMapping("/get-otp/{email}")
    public ResponseEntity<String> getOtpForTesting(@PathVariable String email) {
        VerificationCode verificationCode = verificationCodeRepository.findByEmail(email);
        if (verificationCode == null) {
            return ResponseEntity.ok("No OTP found for email: " + email);
        }
        
        String response = "OTP for " + email + ": " + verificationCode.getOtp() + 
                         " (Expires at: " + verificationCode.getExpiryTime() + ")";
        return ResponseEntity.ok(response);
    }
}
