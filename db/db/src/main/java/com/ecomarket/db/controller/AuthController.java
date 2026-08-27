package com.ecomarket.db.controller;

import com.ecomarket.db.model.Session;
import com.ecomarket.db.model.User;
import com.ecomarket.db.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }

        String providerId = jwt.getClaimAsString("oid");
        if (providerId == null) {
            providerId = jwt.getSubject();
        }
        String email = jwt.getClaimAsString("preferred_username");
        if (email == null) email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String picture = jwt.getClaimAsString("picture");

        User user = authService.upsertUserFromClaims(providerId, email, name, givenName, familyName, picture);
        Session session = authService.createOrRefreshSession(user);

        // return user + session id
        return ResponseEntity.ok(Map.of(
                "user", user,
                "sessionId", session.getId(),
                "expiresAt", session.getExpiresAt()
        ));
    }

    @GetMapping("/online")
    public ResponseEntity<?> online() {
        List<Session> sessions = authService.listActiveSessions();
        var result = sessions.stream().map(s -> Map.<String,Object>of(
                "sessionId", s.getId(),
                "userId", s.getUser().getId(),
                "email", s.getUser().getEmail(),
                "name", s.getUser().getName(),
                "expiresAt", s.getExpiresAt()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}
