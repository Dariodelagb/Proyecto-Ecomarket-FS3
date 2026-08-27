package com.ecomarket.db.service;

import com.ecomarket.db.model.Session;
import com.ecomarket.db.model.User;
import com.ecomarket.db.repository.SessionRepository;
import com.ecomarket.db.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    // session TTL (e.g., 5 minutes)
    private final Duration sessionTtl = Duration.ofMinutes(5);

    public AuthService(UserRepository userRepository, SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public User upsertUserFromClaims(String providerId, String email, String name, String givenName, String familyName, String picture) {
        Optional<User> maybe = Optional.empty();
        if (providerId != null && !providerId.isEmpty()) {
            maybe = userRepository.findByProviderId(providerId);
        }
        if (maybe.isEmpty() && email != null && !email.isEmpty()) {
            maybe = userRepository.findByEmail(email);
        }

        User user;
        if (maybe.isPresent()) {
            user = maybe.get();
            user.setProviderId(providerId != null ? providerId : user.getProviderId());
            user.setEmail(email != null ? email : user.getEmail());
            user.setName(name != null ? name : user.getName());
            user.setGivenName(givenName != null ? givenName : user.getGivenName());
            user.setFamilyName(familyName != null ? familyName : user.getFamilyName());
            user.setPicture(picture != null ? picture : user.getPicture());
            user.setUpdatedAt(Instant.now());
        } else {
            user = new User();
            user.setProviderId(providerId);
            user.setEmail(email);
            user.setName(name);
            user.setGivenName(givenName);
            user.setFamilyName(familyName);
            user.setPicture(picture);
            user.setCreatedAt(Instant.now());
            user.setUpdatedAt(Instant.now());
        }

        return userRepository.save(user);
    }

    @Transactional
    public Session createOrRefreshSession(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(sessionTtl);

        // create a new session each time; you can also reuse existing session per user
        Session s = new Session();
        s.setUser(user);
        s.setCreatedAt(now);
        s.setLastPing(now);
        s.setExpiresAt(expiresAt);

        return sessionRepository.save(s);
    }

    public List<Session> listActiveSessions() {
        return sessionRepository.findByExpiresAtAfter(Instant.now());
    }
}
