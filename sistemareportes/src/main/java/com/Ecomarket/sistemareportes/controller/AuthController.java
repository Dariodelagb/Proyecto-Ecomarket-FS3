package com.Ecomarket.sistemareportes.controller;

import com.Ecomarket.sistemareportes.model.User;
import com.Ecomarket.sistemareportes.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }

        // Extract common claims. Azure AD uses 'oid' for user id and 'preferred_username' or 'email'
        String providerId = jwt.getClaimAsString("oid");
        if (providerId == null) {
            // fallback to sub
            providerId = jwt.getSubject();
        }
        String email = jwt.getClaimAsString("preferred_username");
        if (email == null) {
            email = jwt.getClaimAsString("email");
        }
        String name = jwt.getClaimAsString("name");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String picture = jwt.getClaimAsString("picture");

        User user = userService.upsertFromClaims(providerId, email, name, givenName, familyName, picture);
        return ResponseEntity.ok(user);
    }
}
