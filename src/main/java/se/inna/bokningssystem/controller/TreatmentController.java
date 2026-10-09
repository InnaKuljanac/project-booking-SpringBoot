package se.inna.bokningssystem.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.inna.bokningssystem.dto.TreatmentRequestDto;
import se.inna.bokningssystem.dto.TreatmentResponseDto;
import se.inna.bokningssystem.service.TreatmentService;

import java.util.List;

@RestController
@RequestMapping("/api/treatments")
public class TreatmentController {

    private final TreatmentService treatmentService;

    public TreatmentController(TreatmentService treatmentService) {
        this.treatmentService = treatmentService;
    }

    @GetMapping
    public ResponseEntity<List<TreatmentResponseDto>> getAll() {
        return ResponseEntity.ok(treatmentService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TreatmentResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(treatmentService.getById(id));
    }

    @PostMapping
    public ResponseEntity<TreatmentResponseDto> create(
            @Valid @RequestBody TreatmentRequestDto dto) {

        return ResponseEntity.status(201)
                .body(treatmentService.create(dto));
    }


    @PutMapping("/{id}")
    public ResponseEntity<TreatmentResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody TreatmentRequestDto dto) {

        return ResponseEntity.ok(treatmentService.update(id, dto));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        treatmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}