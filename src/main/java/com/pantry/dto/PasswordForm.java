package com.pantry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordForm {

    @NotBlank(message = "Inserisci la password attuale")
    private String currentPassword;

    @NotBlank(message = "Scegli la nuova password")
    @Size(min = 8, max = 72, message = "La password deve avere da 8 a 72 caratteri")
    private String newPassword;

    @NotBlank(message = "Ripeti la nuova password")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
