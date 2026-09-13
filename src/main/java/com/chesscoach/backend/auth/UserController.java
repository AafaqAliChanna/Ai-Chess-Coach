package com.chesscoach.backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserRepository userRepository, CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    public UserProfileResponse getCurrentUser() {
        User user = loadCurrentUser();
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateCurrentUser(@RequestBody UserProfileUpdateRequest request) {
        User user = loadCurrentUser();
        user.setDisplayName(request.displayName());
        User saved = userRepository.save(user);
        return ResponseEntity.ok(new UserProfileResponse(saved.getId(), saved.getEmail(), saved.getDisplayName()));
    }

    // Identity comes entirely from the validated JWT, never from a path
    // variable or request body — a user can only ever read/edit their OWN
    // profile through this endpoint. There is deliberately no
    // GET/PATCH /api/users/{id} — that would need a separate authorization
    // check (are you an admin? do you have permission to view this other
    // user?) that doesn't exist yet and shouldn't be casually added.
    private User loadCurrentUser() {
        Long userId = currentUserProvider.requireCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Authenticated user no longer exists"));
    }
}