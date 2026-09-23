package com.bluewave.apexbank.utils.common;

import com.bluewave.apexbank.users.Roles;
import com.bluewave.apexbank.users.Users;
import com.bluewave.apexbank.users.UsersRepo;
import com.bluewave.apexbank.utils.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class SecurityUtils {

    private final UsersRepo usersRepo;

    public Users getUserByUsername(String username) {
        return usersRepo.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + username
                        ));
    }

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BadCredentialsException("No authenticated user found in security context");
        }
        return authentication.getName();
    }


    public Users getCurrentUserEntity() {
        String username = getCurrentUsername();
        return usersRepo.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found in database: " + username));
    }


    public Set<String> getUsersRoles(Users user) {
        if (user == null || user.getRoles() == null) {
            return Collections.emptySet();
        }
        return user.getRoles().stream()
                .map(role -> role.getAppRole().name())
                .collect(Collectors.toSet());
    }


    public Set<String> getCurrentUserAssignedRole() {
        return getUsersRoles(getCurrentUserEntity());
    }


    public boolean hasRole(AppRole appRole) {
        return hasRole(appRole.name());
    }

    public boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String expectedRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(expectedRole));
    }
}