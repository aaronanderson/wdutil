package wdutil.wdjws.rest;

import java.io.IOException;
import java.util.Base64;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;

public class BearerAuthenticator implements ClientRequestFilter {

	private final String accessToken;

	public BearerAuthenticator(String accessToken) {
		this.accessToken = accessToken;
	}

	public void filter(ClientRequestContext requestContext) throws IOException {
		try {
			final String bearerAuthentication = "Bearer " + accessToken;
			MultivaluedMap<String, Object> headers = requestContext.getHeaders();
			headers.add("Authorization", bearerAuthentication);
		} catch (IllegalArgumentException ex) {
			throw new IOException(ex);
		}
	}

}
