package vn.sannhaminh.booking.venue;

import java.util.List;
import java.util.Optional;

public interface VenueDao {
    List<Venue> findAllActive();
    List<Venue> findAllActiveByOwnerId(Long ownerId);
    Optional<Venue> findById(Long id);
    Optional<Venue> findByIdForUpdate(Long id);
    long count();
    Venue insert(Venue venue);
    void update(Venue venue);
    void archive(Long id);
    void assignOwnerToUnassignedDemoVenues(Long ownerId);
}
