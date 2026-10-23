package com.pantry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationForm {

    @NotBlank(message = "Scegli un nome utente")
    @Size(min = 3, max = 50, message = "Il nome utente deve avere da 3 a 50 caratteri")
    @Pattern(regexp = "^[A-Za-z0-9._-]*$", message = "Usa solo lettere, numeri, punto, trattino e trattino basso")
    private String username;

    @NotBlank(message = "Inserisci la tua email")
    @Email(message = "Inserisci un indirizzo email valido")
    @Size(max = 120, message = "Massimo 120 caratteri")
    private String email;

    @NotBlank(message = "Scegli una password")
    @Size(min = 8, max = 72, message = "La password deve avere da 8 a 72 caratteri")
    private String password;

    @NotBlank(message = "Ripeti la password")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
