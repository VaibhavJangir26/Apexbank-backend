package com.bluewave.apexbank.profiles;

import com.bluewave.apexbank.profiles.dto.ProfileResponseDTO;
import com.bluewave.apexbank.profiles.dto.ProfileUpdateRequestDTO;
import com.bluewave.apexbank.users.Users;
import com.bluewave.apexbank.users.UsersRepo;
import com.bluewave.apexbank.utils.common.CommonApiResponse;
import com.bluewave.apexbank.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final SecurityUtils securityUtils;
    private final UsersRepo usersRepo;


    @Transactional(readOnly = true)
    @Cacheable(value = "profile", key = "@securityUtils.getCurrentUsername()")
    public CommonApiResponse<ProfileResponseDTO> getMe() {
        Users user = securityUtils.getCurrentUserEntity();
        ProfileResponseDTO responseDTO = mapToResponseDTO(user);

        return CommonApiResponse.<ProfileResponseDTO>builder()
                .message("Profile fetched successfully")
                .data(responseDTO)
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now())
                .build();
    }


    @Transactional
    @CacheEvict(value = "profile", key = "@securityUtils.getCurrentUsername()")
    public CommonApiResponse<ProfileResponseDTO> updateMe(ProfileUpdateRequestDTO dto) {
        Users user = securityUtils.getCurrentUserEntity();

        // 2. Fetch existing associated Profile entity or initialize a new one (1-to-1)
        Profiles profile = user.getProfiles();
        if (profile == null) {
            profile = new Profiles();
        }

        // 3. Patch fields if provided in request
        if (dto.getFullName() != null && !dto.getFullName().isBlank()) {
            profile.setFullName(dto.getFullName().trim());
        }

        if (dto.getMobileNo() != null && !dto.getMobileNo().isBlank()) {
            profile.setMobileNo(dto.getMobileNo().trim());
        }

        if (dto.getAddress() != null) {
            profile.setAddress(dto.getAddress());
        }

        // 4. Maintain bidirectional relationship between Users and Profiles
        user.setProfiles(profile);
        profile.setUsers(user);

        // 5. Save user (Cascades to Profiles via CascadeType.ALL)
        Users updatedUser = usersRepo.save(user);

        log.info("Profile updated successfully for user: {}", updatedUser.getUsername());

        return CommonApiResponse.<ProfileResponseDTO>builder()
                .message("Profile updated successfully")
                .data(mapToResponseDTO(updatedUser))
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }


    private ProfileResponseDTO mapToResponseDTO(Users user) {
        Profiles profile = user.getProfiles();

        return ProfileResponseDTO.builder()
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(securityUtils.getUsersRoles(user))
                .id(profile != null ? profile.getId() : null)
                .fullName(profile != null ? profile.getFullName() : null)
                .mobileNo(profile != null ? profile.getMobileNo() : null)
                .address(profile != null ? profile.getAddress() : null)
                .createdAt(profile != null ? profile.getCreatedAt() : null)
                .updatedAt(profile != null ? profile.getUpdatedAt() : null)
                .build();
    }
}