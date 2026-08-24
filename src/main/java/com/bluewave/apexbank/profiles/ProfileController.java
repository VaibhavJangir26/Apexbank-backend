package com.bluewave.apexbank.profiles;

import com.bluewave.apexbank.profiles.dto.ProfileResponseDTO;
import com.bluewave.apexbank.profiles.dto.ProfileUpdateRequestDTO;
import com.bluewave.apexbank.utils.common.CommonApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me")
    public ResponseEntity<CommonApiResponse<ProfileResponseDTO>> getMe() {
        return ResponseEntity.ok(profileService.getMe());
    }

    @PatchMapping("/me")
    public ResponseEntity<CommonApiResponse<ProfileResponseDTO>> updateMe(@Valid @RequestBody ProfileUpdateRequestDTO dto) {
        return ResponseEntity.ok(profileService.updateMe(dto));
    }
}