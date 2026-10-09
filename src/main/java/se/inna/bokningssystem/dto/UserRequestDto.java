package se.inna.bokningssystem.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class UserRequestDto {

    @NotBlank(message = "Username får inte vara tomt")
    private String username;

    @NotBlank(message = "Password får inte vara tomt")
    private String password;

    @NotBlank(message = "Email får inte vara tomt")
    @Email(message = "Ogiltig email")
    private String email;

    public UserRequestDto() {
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}