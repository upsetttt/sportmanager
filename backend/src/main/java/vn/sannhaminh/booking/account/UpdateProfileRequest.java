package vn.sannhaminh.booking.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Pattern(regexp = "^[0-9+() .-]{8,20}$") String phone
) {
}
