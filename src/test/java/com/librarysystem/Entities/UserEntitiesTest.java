package com.librarysystem.Entities;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserEntitiesTest {

    private User createNormalUser() {
        User u = new User();
        u.setName("Harry");
        u.setEmail("harryl423@gmail.com");
        u.setId(UUID.randomUUID().toString());
        u.setRole(Role.MEMBER);
        return u;
    }

    @Test
    public void getAuthorities_returnsRolePrefixedWithROLE_() {
        User u = createNormalUser();

        var authorities = u.getAuthorities();
        assertEquals(1, authorities.size());
        assertEquals("ROLE_MEMBER", authorities.iterator().next().getAuthority());
       }

    @Test
    public void getUserName_returnsEmail(){
        User u = createNormalUser();
        assertEquals("harryl423@gmail.com", u.getUsername());
    }

    @Test
    public void getAccountStatus_returnsTrueAlways() {
        User u = createNormalUser();
        assertTrue(u.isEnabled());
        assertTrue(u.isAccountNonExpired());
        assertTrue(u.isAccountNonLocked());
        assertTrue(u.isCredentialsNonExpired());

    }
}
