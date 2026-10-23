package com.pantry.service;

import com.pantry.dto.PasswordForm;
import com.pantry.dto.ProfileForm;
import com.pantry.dto.RegistrationForm;
import com.pantry.model.Allergen;
import com.pantry.model.Role;
import com.pantry.model.User;
import com.pantry.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));
    }

    @Transactional
    public User register(RegistrationForm form) {
        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase();
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Le due password non coincidono");
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("Il nome utente \"" + username + "\" è già in uso");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Esiste già un account con questa email");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setRole(Role.USER);
        return userRepository.save(user);
    }

    @Transactional
    public void updateProfile(Long userId, ProfileForm form) {
        User user = getUser(userId);
        String email = form.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new BusinessException("Questa email è già usata da un altro account");
        }
        user.setEmail(email);
        user.setDiet(form.getDiet());
        user.getAllergens().clear();
        if (form.getAllergens() != null) {
            user.getAllergens().addAll(form.getAllergens());
        }
        String preferences = form.getFoodPreferences() == null ? null : form.getFoodPreferences().trim();
        user.setFoodPreferences(preferences == null || preferences.isEmpty() ? null : preferences);
    }

    @Transactional
    public void changePassword(Long userId, PasswordForm form) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("La password attuale non è corretta");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("Le due nuove password non coincidono");
        }
        user.setPassword(passwordEncoder.encode(form.getNewPassword()));
    }

    public ProfileForm toProfileForm(User user) {
        ProfileForm form = new ProfileForm();
        form.setEmail(user.getEmail());
        form.setDiet(user.getDiet());
        form.setAllergens(user.getAllergens().isEmpty()
                ? EnumSet.noneOf(Allergen.class)
                : EnumSet.copyOf(user.getAllergens()));
        form.setFoodPreferences(user.getFoodPreferences());
        return form;
    }

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
}
