package se.inna.bokningssystem.dto;

import jakarta.validation.constraints.NotBlank;

public class LoginRequestDto {

    @NotBlank(message = "Username får inte vara tomt")
    private String username;

    @NotBlank(message = "Password får inte vara tomt")
    private String password;

    public LoginRequestDto() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}