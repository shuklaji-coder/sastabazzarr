package com.rohan.service.imp;

import com.rohan.Config.JwtProvider;
import com.rohan.Repository.CartRepository;
import com.rohan.Repository.UserRepository;
import com.rohan.Repository.VerificationCodeRepository;
import com.rohan.model.*;
import com.rohan.request.Loginrequest;
import com.rohan.response.AuthResponse;
import com.rohan.response.SignupRequest;
import com.rohan.utils.OtpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CartRepository cartRepository;
    private final JwtProvider jwtProvider;
    private final VerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;
    private final BrevoEmailService brevoEmailService;
    private final CustomUserImpl customUserImpl;

    @Value("${frontend.url}")
    private String frontend_url;

    // ================= SEND OTP =================
    @Override
    public void sentLoginOtp(String email, USER_ROLE role) {
        System.out.println("🔐 DEBUG: Sending OTP to email: " + email);
        
        // Delete old OTP if exists
        VerificationCode existingCode =
                verificationCodeRepository.findByEmail(email);

        if (existingCode != null) {
            System.out.println("🗑️ DEBUG: Deleting old OTP for email: " + email);
            verificationCodeRepository.delete(existingCode);
        }

        String otp = OtpUtil.generateOtp();
        System.out.println("🎲 DEBUG: Generated OTP: " + otp + " for email: " + email);

        VerificationCode verificationCode = new VerificationCode();
        verificationCode.setEmail(email);
        verificationCode.setOtp(otp);
        verificationCode.setExpiryTime(LocalDateTime.now().plusMinutes(5)); // 5 minutes expiry
        
        VerificationCode savedCode = verificationCodeRepository.save(verificationCode);
        System.out.println("💾 DEBUG: OTP saved with ID: " + savedCode.getId() + " expiry: " + savedCode.getExpiryTime());

        // Send email using Brevo API - don't let email failure affect OTP saving
        try {
            boolean emailSent = brevoEmailService.sendOtpEmail(email, otp);
            if (emailSent) {
                System.out.println("📧 DEBUG: Email sent successfully via Brevo to: " + email);
            } else {
                System.err.println("❌ DEBUG: Email sending failed via Brevo to: " + email);
            }
        } catch (Exception e) {
            System.err.println("❌ DEBUG: Email sending failed for: " + email + " Error: " + e.getMessage());
            // Don't re-throw - OTP is still saved and valid
        }
        
        System.out.println("✅ DEBUG: OTP generation completed for: " + email);
    }

    // ================= SIGNUP =================
    @Override
    public String createUser(SignupRequest req) {
        System.out.println(" DEBUG: Signup attempt for email: " + req.getEmail());

        VerificationCode verificationCode =
                verificationCodeRepository.findByEmail(req.getEmail());

        if (verificationCode == null) {
            System.out.println(" DEBUG: No OTP found for email: " + req.getEmail());
            throw new RuntimeException("Invalid OTP");
        }

        if (verificationCode.isExpired()) {
            System.out.println(" DEBUG: OTP expired for email: " + req.getEmail() + " expired at: " + verificationCode.getExpiryTime());
            verificationCodeRepository.delete(verificationCode);
            throw new RuntimeException("OTP expired. Please request a new OTP.");
        }

        if (!verificationCode.getOtp().equals(req.getOtp())) {
            System.out.println(" DEBUG: OTP mismatch for email: " + req.getEmail() + " expected: " + verificationCode.getOtp() + " got: " + req.getOtp());
            throw new RuntimeException("Invalid OTP");
        }

        System.out.println(" DEBUG: OTP verified successfully for email: " + req.getEmail());

        // OTP delete after success
        verificationCodeRepository.delete(verificationCode);
        System.out.println(" DEBUG: OTP deleted after successful verification for: " + req.getEmail());

        if (userRepository.findByEmail(req.getEmail()) != null) {
            throw new RuntimeException("User already exists");
        }

        User createdUser = new User();
        createdUser.setEmail(req.getEmail());
        createdUser.setFullname(req.getFullName());
        createdUser.setMobile(req.getMobile());
        createdUser.setRole(USER_ROLE.ROLE_CUSTOMER);
        createdUser.setPassword(
                passwordEncoder.encode(req.getPassword())
        );

        userRepository.save(createdUser);
        System.out.println(" DEBUG: User created successfully: " + createdUser.getEmail());

        // Create cart automatically
        Cart cart = new Cart();
        cart.setUser(createdUser);
        cartRepository.save(cart);

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(
                new SimpleGrantedAuthority(
                        createdUser.getRole().name()
                )
        );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        createdUser.getEmail(),
                        null,
                        authorities
                );

        return jwtProvider.generateToken(authentication);
    }

    // ================= LOGIN WITH OTP =================
    @Override
    public AuthResponse signing(Loginrequest request) throws Exception {

        Authentication authentication =
                authenticate(request.getEmail(), request.getOtp());

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        String token = jwtProvider.generateToken(authentication);

        AuthResponse response = new AuthResponse();
        response.setJwt(token);
        response.setMessage("Login successful");

        Collection<? extends GrantedAuthority> authorities =
                authentication.getAuthorities();

        String roleName =
                authorities.isEmpty() ? null :
                        authorities.iterator().next().getAuthority();

        response.setRole(USER_ROLE.valueOf(roleName));

        return response;
    }

    private Authentication authenticate(String email, String otp) throws Exception {
        System.out.println(" DEBUG: Login attempt for email: " + email + " with OTP: " + otp);

        UserDetails userDetails =
                customUserImpl.loadUserByUsername(email);

        if (userDetails == null) {
            System.out.println(" DEBUG: User not found for email: " + email);
            throw new BadCredentialsException("Invalid Email");
        }

        VerificationCode verificationCode =
                verificationCodeRepository.findByEmail(email);

        if (verificationCode == null) {
            System.out.println(" DEBUG: No OTP found for email: " + email);
            throw new BadCredentialsException("Wrong OTP");
        }

        if (verificationCode.isExpired()) {
            System.out.println(" DEBUG: OTP expired for email: " + email + " expired at: " + verificationCode.getExpiryTime());
            verificationCodeRepository.delete(verificationCode);
            throw new BadCredentialsException("OTP expired. Please request a new OTP.");
        }

        if (!verificationCode.getOtp().equals(otp)) {
            System.out.println(" DEBUG: OTP mismatch for email: " + email + " expected: " + verificationCode.getOtp() + " got: " + otp);
            throw new BadCredentialsException("Wrong OTP");
        }

        System.out.println(" DEBUG: OTP verified successfully for email: " + email);

        // OTP delete after successful login
        verificationCodeRepository.delete(verificationCode);
        System.out.println(" DEBUG: OTP deleted after successful login for: " + email);

        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }
}