package com.carboncell.User.Authentication.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.carboncell.User.Authentication.model.Role;
import com.carboncell.User.Authentication.model.User;
import com.carboncell.User.Authentication.model.UserDetailsImpl;
import com.carboncell.User.Authentication.model.enums.ERole;
import com.carboncell.User.Authentication.model.payloads.LoginRequest;
import com.carboncell.User.Authentication.model.payloads.MessageResponse;
import com.carboncell.User.Authentication.model.payloads.SignupRequest;
import com.carboncell.User.Authentication.model.payloads.UserInfoResponse;
import com.carboncell.User.Authentication.repository.RoleRepository;
import com.carboncell.User.Authentication.repository.UserRepository;
import com.carboncell.User.Authentication.security.JwtUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth controller", description = "This is authentication controller")
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthController(AuthenticationManager authenticationManager, UserRepository userRepository,
                          RoleRepository roleRepository, PasswordEncoder encoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signIn")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Attempting to authenticate user: {}", loginRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

            ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            log.info("User {} successfully authenticated.", loginRequest.getUsername());
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                    .body(new UserInfoResponse(userDetails.getId(),
                            userDetails.getUsername(),
                            userDetails.getEmail(),
                            roles));
        } catch (Exception ex) {
            log.error("Authentication failed for user: {}", loginRequest.getUsername(), ex);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Authentication failed."));
        }
    }

    @PostMapping("/signUp")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        log.info("Attempting to register new user with username: {}", signUpRequest.getUsername());
        try {
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                log.warn("Username {} is already taken.", signUpRequest.getUsername());
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Username is already taken!"));
            }

            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                log.warn("Email {} is already in use.", signUpRequest.getEmail());
                return ResponseEntity.badRequest().body(new MessageResponse("Error: Email is already in use!"));
            }

            User user = new User(signUpRequest.getUsername(),
                    signUpRequest.getEmail(),
                    encoder.encode(signUpRequest.getPassword()));

            Set<String> strRoles = signUpRequest.getRole();
            Set<Role> roles = new HashSet<>();

            if (strRoles == null) {
                Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                        .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(userRole);
            } else {
                strRoles.forEach(role -> {
                    switch (role) {
                        case "admin":
                            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                            roles.add(adminRole);
                            break;
                        case "mod":
                            Role modRole = roleRepository.findByName(ERole.ROLE_MODERATOR)
                                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                            roles.add(modRole);
                            break;
                        default:
                            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                            roles.add(userRole);
                    }
                });
            }

            user.setRoles(roles);
            userRepository.save(user);

            String roleMessage;
            if (roles.stream().anyMatch(r -> r.getName().equals(ERole.ROLE_ADMIN))) {
                roleMessage = "Admin";
            } else if (roles.stream().anyMatch(r -> r.getName().equals(ERole.ROLE_MODERATOR))) {
                roleMessage = "Moderator";
            } else {
                roleMessage = "User";
            }

            log.info("{} registered successfully.", roleMessage);
            return ResponseEntity.ok(new MessageResponse(roleMessage + " registered successfully!"));
        } catch (Exception ex) {
            log.error("Registration failed for user: {}", signUpRequest.getUsername(), ex);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Registration failed."));
        }
    }

    @PostMapping("/signOut")
    public ResponseEntity<?> logoutUser() {
        log.info("User is signing out.");
        try {
            ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
            log.info("User signed out successfully.");
            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new MessageResponse("You've been signed out!"));
        } catch (Exception ex) {
            log.error("Sign out failed.", ex);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Sign out failed."));
        }
    }
}