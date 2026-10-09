package se.inna.bokningssystem.service;

import org.springframework.stereotype.Service;
import se.inna.bokningssystem.dto.TreatmentRequestDto;
import se.inna.bokningssystem.dto.TreatmentResponseDto;
import se.inna.bokningssystem.exception.TreatmentNotFoundException;
import se.inna.bokningssystem.mapper.TreatmentMapper;
import se.inna.bokningssystem.model.Treatment;
import se.inna.bokningssystem.repository.TreatmentRepository;

import se.inna.bokningssystem.exception.DuplicateTreatmentException;


import java.util.List;

@Service
public class TreatmentService {

    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper treatmentMapper;

    public TreatmentService(
            TreatmentRepository treatmentRepository,
            TreatmentMapper treatmentMapper) {
        this.treatmentRepository = treatmentRepository;
        this.treatmentMapper = treatmentMapper;
    }

    public List<TreatmentResponseDto> getAll() {
        return treatmentRepository.findAll()
                .stream()
                .map(treatmentMapper::toDto)
                .toList();
    }

    public TreatmentResponseDto getById(Long id) {
        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new TreatmentNotFoundException(id));

        return treatmentMapper.toDto(treatment);
    }


    public TreatmentResponseDto create(TreatmentRequestDto dto) {
        if (treatmentRepository.existsByNameIgnoreCase(dto.getName().trim())) {
            throw new DuplicateTreatmentException(dto.getName());
        }

        Treatment treatment = treatmentMapper.toEntity(dto);
        treatment.setName(dto.getName().trim());

        return treatmentMapper.toDto(
                treatmentRepository.save(treatment)
        );
    }


    public TreatmentResponseDto update(Long id, TreatmentRequestDto dto) {
        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new TreatmentNotFoundException(id));

        String name = dto.getName().trim();

        if (treatmentRepository.existsByNameIgnoreCase(name)
                && !treatment.getName().equalsIgnoreCase(name)) {
            throw new DuplicateTreatmentException(name);
        }

        treatment.setName(name);
        treatment.setDescription(dto.getDescription());
        treatment.setDuration(dto.getDuration());
        treatment.setPrice(dto.getPrice());

        return treatmentMapper.toDto(treatmentRepository.save(treatment));
    }



    public void delete(Long id) {
        if (!treatmentRepository.existsById(id)) {
            throw new TreatmentNotFoundException(id);
        }

        treatmentRepository.deleteById(id);
    }
}