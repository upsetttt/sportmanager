package vn.sannhaminh.security;

public record AuthUser(long id, String email, String role, String fullName) {}
