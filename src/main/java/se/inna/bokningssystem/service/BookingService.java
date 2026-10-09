
package se.inna.bokningssystem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.inna.bokningssystem.dto.BookingRequestDto;
import se.inna.bokningssystem.dto.BookingResponseDto;
import se.inna.bokningssystem.exception.BookingConflictException;
import se.inna.bokningssystem.exception.BookingNotFoundException;
import se.inna.bokningssystem.exception.TreatmentNotFoundException;
import se.inna.bokningssystem.exception.UserNotFoundException;
import se.inna.bokningssystem.mapper.BookingMapper;
import se.inna.bokningssystem.model.Booking;
import se.inna.bokningssystem.model.Treatment;
import se.inna.bokningssystem.model.User;
import se.inna.bokningssystem.repository.BookingRepository;
import se.inna.bokningssystem.repository.TreatmentRepository;
import se.inna.bokningssystem.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TreatmentRepository treatmentRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    public BookingService(
            BookingRepository bookingRepository,
            TreatmentRepository treatmentRepository,
            UserRepository userRepository,
            BookingMapper bookingMapper) {

        this.bookingRepository = bookingRepository;
        this.treatmentRepository = treatmentRepository;
        this.userRepository = userRepository;
        this.bookingMapper = bookingMapper;
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getAll() {
        return bookingRepository.findAll()
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        return bookingMapper.toDto(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getById(Long id, String username) {
        Booking booking = bookingRepository
                .findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new BookingNotFoundException(id));

        return bookingMapper.toDto(booking);
    }

    @Transactional
    public BookingResponseDto create(
            BookingRequestDto dto,
            String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        Treatment treatment = treatmentRepository.findById(dto.getTreatmentId())
                .orElseThrow(() ->
                        new TreatmentNotFoundException(dto.getTreatmentId()));

        if (!treatment.isActive()) {
            throw new BookingConflictException(
                    "Behandlingen är inte tillgänglig för bokning");
        }

        LocalDateTime startTime = dto.getStartTime();
        LocalDateTime endTime = startTime.plusMinutes(treatment.getDuration());

        boolean alreadyBooked =
                bookingRepository
                        .existsByTreatmentIdAndStartTimeLessThanAndEndTimeGreaterThan(
                                dto.getTreatmentId(),
                                endTime,
                                startTime);

        if (alreadyBooked) {
            throw new BookingConflictException(
                    "Behandlingen är redan bokad under den valda tiden");
        }

        Booking booking = bookingMapper.toEntity(dto);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setUser(user);
        booking.setTreatment(treatment);
        booking.setStatus("CONFIRMED");

        return bookingMapper.toDto(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        return bookingRepository.findByUserId(user.getId())
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return bookingRepository.findByUserId(user.getId())
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getByTreatmentId(Long treatmentId) {
        return bookingRepository.findByTreatmentId(treatmentId)
                .stream()
                .map(bookingMapper::toDto)
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        bookingRepository.delete(booking);
    }

    @Transactional
    public void delete(Long id, String username) {
        Booking booking = bookingRepository
                .findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new BookingNotFoundException(id));

        bookingRepository.delete(booking);
    }
}
