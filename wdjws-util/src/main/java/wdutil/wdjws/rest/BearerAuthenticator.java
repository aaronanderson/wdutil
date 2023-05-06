package wdutil.wdjws.rest;

import java.io.IOException;
import java.util.function.Supplier;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;

public class BearerAuthenticator implements ClientRequestFilter {

    private final Supplier<String> tokenProvider;

    public BearerAuthenticator(String accessToken) {
        this.tokenProvider = () -> accessToken;
    }

    public BearerAuthenticator(Supplier<String> tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public void filter(ClientRequestContext requestContext) throws IOException {
        try {
            final String bearerAuthentication = "Bearer " + tokenProvider.get();
            MultivaluedMap<String, Object> headers = requestContext.getHeaders();
            headers.add("Authorization", bearerAuthentication);
        } catch (IllegalArgumentException ex) {
            throw new IOException(ex);
        }
    }

}
