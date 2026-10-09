package se.inna.bokningssystem.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.inna.bokningssystem.dto.UserRequestDto;
import se.inna.bokningssystem.dto.UserResponseDto;
import se.inna.bokningssystem.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @PostMapping
    public ResponseEntity<UserResponseDto> create(
            @Valid @RequestBody UserRequestDto dto) {

        return ResponseEntity.status(201)
                .body(userService.create(dto));
    }
}