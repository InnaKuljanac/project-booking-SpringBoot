package se.inna.bokningssystem.mapper;

import org.mapstruct.Mapper;
import se.inna.bokningssystem.dto.UserRequestDto;
import se.inna.bokningssystem.dto.UserResponseDto;
import se.inna.bokningssystem.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDto toDto(User user);

    User toEntity(UserRequestDto dto);

}
