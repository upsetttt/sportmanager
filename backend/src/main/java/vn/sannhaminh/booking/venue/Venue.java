package vn.sannhaminh.booking.venue;

import vn.sannhaminh.booking.account.Account;

public class Venue {
    private Long id;
    private String name;
    private String sport;
    private String address;
    private long pricePerHour;
    private String imageUrl;
    private double rating;
    private int reviewCount;
    private String amenities;
    private boolean featured;
    private Account owner;
    private boolean active = true;

    public Venue(String name, String sport, String address, long pricePerHour, String imageUrl,
                 double rating, int reviewCount, String amenities, boolean featured) {
        this.name = name;
        this.sport = sport;
        this.address = address;
        this.pricePerHour = pricePerHour;
        this.imageUrl = imageUrl;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.amenities = amenities;
        this.featured = featured;
    }

    public Venue(String name, String sport, String address, long pricePerHour, String imageUrl,
                 double rating, int reviewCount, String amenities, boolean featured, Account owner) {
        this(name, sport, address, pricePerHour, imageUrl, rating, reviewCount, amenities, featured);
        this.owner = owner;
    }

    public Venue(String name, String sport, String address, long pricePerHour, String imageUrl,
                 String amenities, Account owner) {
        this(name, sport, address, pricePerHour, imageUrl, 0, 0, amenities, false);
        this.owner = owner;
    }

    public Venue(Long id, String name, String sport, String address, long pricePerHour, String imageUrl,
                 double rating, int reviewCount, String amenities, boolean featured, Account owner, boolean active) {
        this(name, sport, address, pricePerHour, imageUrl, rating, reviewCount, amenities, featured, owner);
        this.id = id;
        this.active = active;
    }

    public void update(String name, String sport, String address, long pricePerHour, String imageUrl,
                       String amenities) {
        this.name = name;
        this.sport = sport;
        this.address = address;
        this.pricePerHour = pricePerHour;
        this.imageUrl = imageUrl;
        this.amenities = amenities;
    }

    public void archive() {
        this.active = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public String getSport() { return sport; }
    public String getAddress() { return address; }
    public long getPricePerHour() { return pricePerHour; }
    public String getImageUrl() { return imageUrl; }
    public double getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public String getAmenities() { return amenities; }
    public boolean isFeatured() { return featured; }
    public Account getOwner() { return owner; }
    public boolean isActive() { return active; }
}
