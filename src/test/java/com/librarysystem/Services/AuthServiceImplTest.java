package com.librarysystem.Services;

import com.librarysystem.Dto.LoginDto;
import com.librarysystem.Dto.RegisterDto;
import com.librarysystem.Entities.Role;
import com.librarysystem.Entities.User;
import com.librarysystem.Exception.AccountAlreadyExistsException;
import com.librarysystem.Repositories.UserRepository;
import com.librarysystem.ResponseDto.LoginResponseDto;
import com.librarysystem.ResponseDto.RegisterResponseDto;
import com.librarysystem.Utils.JwtService;
import com.librarysystem.Utils.Mapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @InjectMocks
    AuthServiceImpl authService;
    @Mock
    UserRepository userRepository;
    @Mock
    Mapper mapper;
    @Mock
    AuthenticationManager authenticationManager;
    @Mock
    JwtService jwtService;

    private User createNormalUser() {
        User u = new User();
        u.setName("Harry");
        u.setEmail("harryl423@gmail.com");
        u.setId(UUID.randomUUID().toString());
        u.setRole(Role.MEMBER);
        return u;
    }

    @Test
    public void signup_saveUserToDbAndReturnSavedInfo() {
        RegisterDto registerDto = new RegisterDto(
                "Harry",
                "harryl423@gmail.com",
                "not.a.password",
                "member"
        );
        User u = createNormalUser();
        User savedUser = createNormalUser();
        savedUser.setPassword("hashed_password");
        savedUser.setRole(Role.MEMBER);
        RegisterResponseDto responseDto =
                new RegisterResponseDto("saved_id",
                        "Harry",
                        "harryl423@gmail.com",
                        Role.MEMBER.toString()
                        );
        when(userRepository.findByEmail(registerDto.email()))
                .thenReturn(Optional.empty());

        when(mapper.maptoUser(registerDto)).thenReturn(u);
        when(userRepository.save(u)).thenReturn(savedUser);
        when(mapper.maptoRegisterReponseDto(savedUser)).thenReturn(responseDto);

        RegisterResponseDto dto = authService.signup(registerDto);
        assertSame(responseDto, dto);
    }

    @Test
    public void signup_throwExceptionAccountAlreadyExist() {
        RegisterDto registerDto = new RegisterDto(
                "Harry",
                "harryl423@gmail.com",
                "not.a.password",
                "boss"
        );
        User u = createNormalUser();
        when(userRepository.findByEmail("harryl423@gmail.com"))
                .thenReturn(Optional.of(u));

        AccountAlreadyExistsException ex = assertThrows(AccountAlreadyExistsException.class, () -> {
            authService.signup(registerDto);
        });

        assertEquals("Account already exists using this Email", ex.getMessage());

    }

    @Test
    public void login_returnsTokenExpiryAndRole() {
        User u = createNormalUser();
        LoginDto dto = new LoginDto("harryl423@gmail.com", "not.a.password");
        Authentication auth = new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password())
        )).thenReturn(auth);
        when(jwtService.generateToken(u)).thenReturn("jwt-goes-here");
        when(jwtService.getExpirationTime()).thenReturn(3600000L);

        LoginResponseDto result = authService.login(dto);

        assertEquals("jwt-goes-here", result.token());
        assertEquals(3600000L, result.expiredIn());
        assertEquals("MEMBER", result.role());
    }

    @Test
    public void login_throwsWhenPasswordWrong() {
        LoginDto dto = new LoginDto("harryl423@gmail.com", "wrong.password");
        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password())
        )).thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(dto));

        assertEquals("Bad credentials", ex.getMessage());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    public void login_throwsSameErrorWhenUserNotFound() {
        // Spring's AuthenticationManager reports an unknown email as BadCredentialsException,
        // the same as a wrong password, so callers can't tell which emails are registered.
        LoginDto dto = new LoginDto("henryl423@gmail.com", "any.password");
        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password())
        )).thenThrow(new BadCredentialsException("Bad credentials"));

        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> authService.login(dto));

        assertEquals("Bad credentials", ex.getMessage());
        // The service must not do its own lookup: that lookup is what leaked "User not Found".
        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    public void login_letsTokenErrorsThroughInsteadOfReportingBadCredentials() {
        // A broken JWT setup is a server problem, not a wrong password.
        User u = createNormalUser();
        LoginDto dto = new LoginDto("harryl423@gmail.com", "not.a.password");
        Authentication auth = new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities());
        when(authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password())
        )).thenReturn(auth);
        when(jwtService.generateToken(u))
                .thenThrow(new IllegalArgumentException("The secret key must be at least 256 bits"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(dto));

        assertEquals("The secret key must be at least 256 bits", ex.getMessage());
    }
}
