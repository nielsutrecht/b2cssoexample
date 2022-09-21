package com.nibado.example.b2csso;

import com.microsoft.aad.msal4j.*;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

@Component
public class SsoComponent {
    @Value("${sso.secret}")
    private String secret;

    @Value("${sso.client-id}")
    private String clientId;

    @Value("${sso.authority}")
    private String authority;

    @Value("${sso.policy}")
    private String policy;

    @Value("${sso.scope}")
    private String scope;

    @Value("${sso.redirect-url}")
    private String redirectUrl;

    public ConfidentialClientApplication setup() throws Exception {
        var clientSecret = ClientCredentialFactory.createFromSecret(secret);
        return ConfidentialClientApplication.builder(clientId, clientSecret)
                .b2cAuthority(authority + policy)
                .build();
    }

    public String getRedirectUrl(String state) throws Exception {
        String nonce = UUID.randomUUID().toString();

        var parameters = AuthorizationRequestUrlParameters.builder(redirectUrl, Set.of(scope, clientId))
                .responseMode(ResponseMode.QUERY)
                .prompt(Prompt.SELECT_ACCOUNT).state(state).nonce(nonce).build();

        return setup().getAuthorizationRequestUrl(parameters).toString();
    }

    public UUID handleCallback(String code) throws Exception {
        var client = setup();
        AuthorizationCodeParameters authParams = AuthorizationCodeParameters
                .builder(code, new URI(redirectUrl)).scopes(Set.of(scope, clientId)).build();

        var future = client.acquireToken(authParams);
        var result = future.get();

        var jwt = SignedJWT.parse(result.idToken());
        var claims = jwt.getJWTClaimsSet();

        var userObjectId = UUID.fromString(claims.getStringClaim("oid"));

        return userObjectId;
    }
}
