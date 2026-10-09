package se.inna.bokningssystem.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import se.inna.bokningssystem.dto.AuthResponseDto;
import se.inna.bokningssystem.dto.LoginRequestDto;
import se.inna.bokningssystem.exception.InvalidCredentialsException;
import se.inna.bokningssystem.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
    }

    public AuthResponseDto login(LoginRequestDto dto) {

        org.springframework.security.core.userdetails.UserDetails userDetails;

        try {
            userDetails = userDetailsService.loadUserByUsername(dto.getUsername());
        } catch (org.springframework.security.core.userdetails.UsernameNotFoundException ex) {
            throw new InvalidCredentialsException(
                    "Felaktiga inloggningsuppgifter"
            );
        }

        var user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Felaktiga inloggningsuppgifter"
                ));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException(
                    "Felaktiga inloggningsuppgifter"
            );
        }

        String token = jwtService.generateToken(userDetails);

        return new AuthResponseDto(token);
    }
}