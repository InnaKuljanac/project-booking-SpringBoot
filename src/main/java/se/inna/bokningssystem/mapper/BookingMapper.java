package se.inna.bokningssystem.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import se.inna.bokningssystem.dto.BookingRequestDto;
import se.inna.bokningssystem.dto.BookingResponseDto;
import se.inna.bokningssystem.model.Booking;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "treatment.id", target = "treatmentId")
    BookingResponseDto toDto(Booking booking);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "treatment", ignore = true)
    Booking toEntity(BookingRequestDto dto);
}