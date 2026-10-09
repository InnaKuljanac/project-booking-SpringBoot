
package se.inna.bokningssystem.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import se.inna.bokningssystem.dto.BookingRequestDto;
import se.inna.bokningssystem.dto.BookingResponseDto;
import se.inna.bokningssystem.service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<List<BookingResponseDto>> getAll(
            Authentication authentication) {

        if (isAdmin(authentication)) {
            return ResponseEntity.ok(bookingService.getAll());
        }

        return ResponseEntity.ok(
                bookingService.getByUsername(authentication.getName())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDto> getById(
            @PathVariable Long id,
            Authentication authentication) {

        if (isAdmin(authentication)) {
            return ResponseEntity.ok(bookingService.getById(id));
        }

        return ResponseEntity.ok(
                bookingService.getById(id, authentication.getName())
        );
    }

    @PostMapping
    public ResponseEntity<BookingResponseDto> create(
            @Valid @RequestBody BookingRequestDto dto,
            Authentication authentication) {

        return ResponseEntity.status(201)
                .body(bookingService.create(dto, authentication.getName()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(
            Authentication authentication) {

        return ResponseEntity.ok(
                bookingService.getByUsername(authentication.getName())
        );
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDto>> getByUserId(
            @PathVariable Long userId,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(bookingService.getByUserId(userId));
    }

    @GetMapping("/treatment/{treatmentId}")
    public ResponseEntity<List<BookingResponseDto>> getByTreatmentId(
            @PathVariable Long treatmentId,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                bookingService.getByTreatmentId(treatmentId)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication) {

        if (isAdmin(authentication)) {
            bookingService.delete(id);
        } else {
            bookingService.delete(id, authentication.getName());
        }

        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
