package com.sportcourt.backend.dto;

/**
 * DTO de solicitud de login
 */
public class LoginRequest {

    private String email;
    private String password;

    // Constructores
    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters y Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
