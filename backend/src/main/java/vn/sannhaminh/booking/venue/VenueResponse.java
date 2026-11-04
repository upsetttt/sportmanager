package vn.sannhaminh.booking.venue;

import java.util.Arrays;
import java.util.List;

public record VenueResponse(
        Long id,
        String name,
        String sport,
        String address,
        long pricePerHour,
        String imageUrl,
        double rating,
        int reviewCount,
        List<String> amenities,
        boolean featured,
        Long ownerId
) {
    public static VenueResponse from(Venue venue) {
        List<String> amenities = venue.getAmenities() == null || venue.getAmenities().isBlank()
                ? List.of()
                : Arrays.stream(venue.getAmenities().split("\\|")).toList();
        return new VenueResponse(venue.getId(), venue.getName(), venue.getSport(), venue.getAddress(),
                venue.getPricePerHour(), venue.getImageUrl(), venue.getRating(), venue.getReviewCount(),
                amenities, venue.isFeatured(), venue.getOwner() == null ? null : venue.getOwner().getId());
    }
}
