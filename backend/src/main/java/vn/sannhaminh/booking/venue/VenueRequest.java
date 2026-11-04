package vn.sannhaminh.booking.venue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record VenueRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 40) String sport,
        @NotBlank @Size(max = 200) String address,
        @NotNull @Min(1000) Long pricePerHour,
        @Size(max = 500) String imageUrl,
        @Size(max = 10) List<@NotBlank @Size(max = 40) String> amenities
) {
}
