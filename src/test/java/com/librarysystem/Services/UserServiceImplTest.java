package com.librarysystem.Services;


import com.librarysystem.Entities.Role;
import com.librarysystem.Entities.User;
import com.librarysystem.Exception.ResourceNotFoundException;
import com.librarysystem.Repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserServiceImpl userService;

    private List<User> createUserListSize10() {
        List<User> userList = new ArrayList<>();
        for (int i = 0; i< 10; i++) {
            String userId = "user" + i;
            User u = new User(userId,
                    userId,
                    userId+"@gmail.com",
                    "userPassword" + i,
                    Role.MEMBER);

            userList.add(u);
        }
        return userList;
    }

    @Test
    public void getAllUsers_returnsAllUser() {
        List<User> userList = createUserListSize10();
        when(userRepository.findAll()).thenReturn(userList);

        List<User> users = userService.getAllUsers();

        assertEquals(10, users.size());
        assertSame(userList, users);
        verify(userRepository).findAll();
    }

    @Test
    public void getUserById_returnsFoundUser() {
        User u = new User();
        when(userRepository.findById("1")).thenReturn(Optional.of(u));

        User foundUser = userService.getUserById("1");

        assertSame(u, foundUser);
        verify(userRepository).findById("1");

    }

    @Test
    public void getUserById_returnsNotFound() {
        when(userRepository.findById("1")).thenReturn(Optional.empty());

        Exception e = assertThrows(ResourceNotFoundException.class, () ->
            userService.getUserById("1")
        );

        assertTrue(e.getMessage().startsWith("User not found with id: 1"));
        verify(userRepository).findById("1");
    }

    @Test
    public void getUserByUsername_returnsFoundUsernameEmail() {
        User u = new User();
        u.setEmail("email");
        when(userRepository.findByEmail("email")).thenReturn(Optional.of(u));

        User foundUser = userService.getUserByUsername("email");

        assertSame(u, foundUser);
        verify(userRepository).findByEmail("email");
    }

    @Test
    public void getUserByUsername_returnsNotFound() {
        when(userRepository.findByEmail("email")).thenReturn(Optional.empty());

        Exception e = assertThrows(ResourceNotFoundException.class, () ->
                userService.getUserByUsername("email")
        );

        assertTrue(e.getMessage().startsWith("User not found with Email: email"));
        verify(userRepository).findByEmail("email");
    }

}
