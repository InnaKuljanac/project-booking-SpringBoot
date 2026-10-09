package se.inna.bokningssystem.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.inna.bokningssystem.dto.UserRequestDto;
import se.inna.bokningssystem.dto.UserResponseDto;
import se.inna.bokningssystem.exception.DuplicateUserException;
import se.inna.bokningssystem.mapper.UserMapper;
import se.inna.bokningssystem.model.User;
import se.inna.bokningssystem.repository.UserRepository;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Transactional
    public UserResponseDto create(UserRequestDto userRequestDto) {

        if (userRepository.existsByUsername(userRequestDto.getUsername())) {
            throw new DuplicateUserException(
                    "Username '" + userRequestDto.getUsername() + "' används redan");
        }

        if (userRepository.existsByEmail(userRequestDto.getEmail())) {
            throw new DuplicateUserException(
                    "Email '" + userRequestDto.getEmail() + "' används redan");
        }

        User user = userMapper.toEntity(userRequestDto);

        user.setPassword(
                passwordEncoder.encode(userRequestDto.getPassword())
        );

        user.setRole("USER");

        User savedUser = userRepository.save(user);

        return userMapper.toDto(savedUser);
    }
}