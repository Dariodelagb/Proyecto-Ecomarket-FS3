package com.Ecomarket.sistemareportes.service;

import com.Ecomarket.sistemareportes.model.User;
import com.Ecomarket.sistemareportes.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User upsertFromClaims(String providerId, String email, String name, String givenName, String familyName, String picture) {
        Optional<User> maybe = Optional.empty();
        if (StringUtils.hasText(providerId)) {
            maybe = userRepository.findByProviderId(providerId);
        }
        if (maybe.isEmpty() && StringUtils.hasText(email)) {
            maybe = userRepository.findByEmail(email);
        }

        User user;
        if (maybe.isPresent()) {
            user = maybe.get();
            user.setEmail(email != null ? email : user.getEmail());
            user.setName(name != null ? name : user.getName());
            user.setGivenName(givenName != null ? givenName : user.getGivenName());
            user.setFamilyName(familyName != null ? familyName : user.getFamilyName());
            user.setPicture(picture != null ? picture : user.getPicture());
            user.setProviderId(providerId != null ? providerId : user.getProviderId());
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
}
