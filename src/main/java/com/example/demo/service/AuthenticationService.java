package com.example.demo.service;

import com.example.demo.dto.LoginUserDto;
import com.example.demo.dto.RegisterUserDto;
import com.example.demo.dto.VerifyUserDto;
import com.example.demo.modal.User;
import com.example.demo.repository.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public User signup(RegisterUserDto input) {
        User user = new User(input.getUsername(), input.getEmail(), passwordEncoder.encode(input.getPassword()));
        user.setVerificationCode(generateVerificationCode());
        user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
        user.setEnabled(false);
        sendVerificationEmail(user);
        return userRepository.save(user);
    }

    public User authenticate(LoginUserDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified. Please verify your account.");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        return user;
    }

    public void verifyUser(VerifyUserDto input) {
        Optional<User> optionalUser = userRepository.findByEmail(input.getEmail());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("Verification code has expired");
            }
            if (user.getVerificationCode().equals(input.getVerificationCode())) {
                user.setEnabled(true);
                user.setVerificationCode(null);
                user.setVerificationCodeExpiresAt(null);
                userRepository.save(user);
            } else {
                throw new RuntimeException("Invalid verification code");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public void resendVerificationCode(String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account is already verified");
            }
            user.setVerificationCode(generateVerificationCode());
            user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
            sendVerificationEmail(user);
            userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    private void sendVerificationEmail(User user) { 
        // TODO: Update with company logo URL if needed
        String logoUrl = "https://i.postimg.cc/fR7Hr76N/header1-logo.png"; // Company logo URL
        String subject = "Account Verification";
        String verificationCode = user.getVerificationCode(); // Assuming this is the actual verification code
        String htmlMessage = "<html>"
                + "<head>"
                + "<style>"
                + "body {"
                + "    font-family: Arial, sans-serif;"
                + "    margin: 0;"
                + "    padding: 0;"
                + "    background-color: #f5f5f5;"
                + "}"
                + ".container {"
                + "    background-color: #ffffff;"
                + "    padding: 20px;"
                + "    width: 100%;"
                + "    max-width: 600px;"
                + "    margin: 20px auto;"
                + "    border-radius: 5px;"
                + "    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);"
                + "}"
                + ".header {"
                + "    text-align: center;"
                + "    margin-bottom: 20px;"
                + "}"
                + ".header img {"
                + "    max-width: 200px;"
                + "}"
                + "h2 {"
                + "    color: #333;"
                + "    text-align: center;"
                + "}"
                + "p {"
                + "    font-size: 16px;"
                + "    text-align: center;"
                + "}"
                + ".verification-code {"
                + "    background-color: #fff;"
                + "    padding: 20px;"
                + "    border-radius: 5px;"
                + "    box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);"
                + "    margin: 0 auto;"
                + "    width: fit-content;"
                + "}"
                + ".verification-code h3 {"
                + "    color: #333;"
                + "    text-align: center;"
                + "}"
                + ".code {"
                + "    font-size: 24px;"
                + "    font-weight: bold;"
                + "    color: #007bff;"
                + "    text-align: center;"
                + "}"
                + ".disclaimer {"
                + "    font-size: 14px;"
                + "    text-align: center;"
                + "    margin-top: 20px;"
                + "}"
                + ".footer {"
                + "    margin-top: 40px;"
                + "    text-align: center;"
                + "}"
                + ".footer p {"
                + "    font-size: 12px;"
                + "    color: #777;"
                + "}"
                + ".footer a {"
                + "    color: #007bff;"
                + "    text-decoration: none;"
                + "}"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class=\"container\">"
                + "<div class=\"header\">"
                + "<img src=\"" + logoUrl + "\" alt=\"I&T Labs\" />"
                + "</div>"
                + "<h2>Welcome to Our I&T Labs JobPortal [ SDP Project ]!</h2>"
                + "<p>Please enter the verification code below to continue:</p>"
                + "<div class=\"verification-code\">"
                + "<h3>Verification Code:</h3>"
                + "<p class=\"code\">" + verificationCode + "</p>" // Insert actual code here
                + "</div>"
                + "<p class=\"disclaimer\">If you did not request this email, please ignore it.</p>"
                + "<div class=\"footer\">"
                + "<p>&copy; <span id=\"year\"></span> I&T Labs [ JFSD SDP Project ]. All rights reserved.</p>"
                + "<p>Contact us at <a href=\"mailto:support@example.com\">2200090018csit@gmail.com</a></p>"
                + "</div>"
                + "</div>"
                + "<script>"
                + "document.getElementById('year').innerText = new Date().getFullYear();"
                + "</script>"
                + "</body>"
                + "</html>";

        try {
            emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
        } catch (MessagingException e) {
            // Handle email sending exception
            e.printStackTrace();
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}