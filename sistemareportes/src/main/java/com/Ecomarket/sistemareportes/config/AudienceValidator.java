package com.Ecomarket.sistemareportes.config;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> allowedAudiences;

    public AudienceValidator(String... allowedAudiences) {
        this.allowedAudiences = List.of(allowedAudiences);
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        List<String> tokenAud = token.getAudience();
        for (String a : allowedAudiences) {
            if (tokenAud.contains(a)) {
                return OAuth2TokenValidatorResult.success();
            }
        }
        OAuth2Error err = new OAuth2Error("invalid_token", "The required audience is missing", null);
        return OAuth2TokenValidatorResult.failure(err);
    }
}
