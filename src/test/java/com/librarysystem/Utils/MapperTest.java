package com.librarysystem.Utils;

import com.librarysystem.Dto.RegisterDto;
import com.librarysystem.Entities.Role;
import com.librarysystem.Entities.User;
import com.librarysystem.ResponseDto.RegisterResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class MapperTest {

    @InjectMocks
    private Mapper mapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    public void maptoUser_copiesFieldsAndEncodesPassword() {
        RegisterDto registerDto = new RegisterDto(
                "Harry",
                "harryl423@gmail.com",
                "not.a.password",
                "member"
        );
        when(passwordEncoder.encode("not.a.password")).thenReturn("hashed");

        User user = mapper.maptoUser(registerDto);

        assertEquals("Harry", user.getName());
        assertEquals("harryl423@gmail.com", user.getEmail());
        assertEquals("hashed", user.getPassword());
        assertEquals(Role.MEMBER, user.getRole());
    }

    @Test
    public void maptoUser_throwExceptionInRoleMatching() {
        RegisterDto registerDto = new RegisterDto(
                "Harry",
                "harryl423@gmail.com",
                "not.a.password",
                "boss"
        );
        when(passwordEncoder.encode("not.a.password")).thenReturn("hashed");

        assertThrows(IllegalArgumentException.class, () -> mapper.maptoUser(registerDto));
    }

    @Test
    public void maptoRegisterResponseDto_copiesFields() {
        User user = new User(
                "user-id",
                "Harry",
                "harryl423@gmail.com",
                null,
                Role.MEMBER
        );

        RegisterResponseDto dto  = mapper.maptoRegisterReponseDto(user);

        assertEquals("user-id", dto.id());
        assertEquals("Harry", dto.name());
        assertEquals("harryl423@gmail.com", dto.email());
        assertEquals("MEMBER", dto.role());
    }


}
