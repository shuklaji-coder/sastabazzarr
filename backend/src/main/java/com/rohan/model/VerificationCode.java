package com.rohan.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class VerificationCode {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String otp;
    private String email;
    
    @Column(name = "expiry_time")
    private LocalDateTime expiryTime;

    @OneToOne
    private User user;

    @OneToOne
    private Seller seller;
    
    // Check if OTP is expired
    public boolean isExpired() {
        return expiryTime != null && LocalDateTime.now().isAfter(expiryTime);
    }
}
