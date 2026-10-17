
package com.hospital.booking.dto;

public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private String username;
    private String role;

    public LoginResponse() {
    }

    public LoginResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String username,
            String role) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.username = username;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }
}
