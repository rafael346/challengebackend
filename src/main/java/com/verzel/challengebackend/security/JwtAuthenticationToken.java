package com.verzel.challengebackend.security;

import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final JwtService.ParsedToken principal;
    private final String credentials;

    public JwtAuthenticationToken(String rawToken) {
        super(List.of());
        this.credentials = rawToken;
        this.principal = null;
        setAuthenticated(false);
    }

    public JwtAuthenticationToken(JwtService.ParsedToken principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = null;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
