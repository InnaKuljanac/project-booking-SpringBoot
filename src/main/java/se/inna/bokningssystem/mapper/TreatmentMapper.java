package se.inna.bokningssystem.mapper;

import org.mapstruct.Mapper;
import se.inna.bokningssystem.dto.TreatmentRequestDto;
import se.inna.bokningssystem.dto.TreatmentResponseDto;
import se.inna.bokningssystem.model.Treatment;

@Mapper(componentModel = "spring")
public interface TreatmentMapper {

    TreatmentResponseDto toDto(Treatment treatment);

    Treatment toEntity(TreatmentRequestDto dto);
}