package wdutil.wdjws.rest;

import java.io.IOException;
import java.util.Base64;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;

public class BasicAuthenticator implements ClientRequestFilter {

	private final String user;
	private final String password;

	public BasicAuthenticator(String user, String password) {
		this.user = user;
		this.password = password;
	}

	public void filter(ClientRequestContext requestContext) throws IOException {
		try {
			final String token = this.user + ":" + this.password;
			final String basicAuthentication = "Basic " + Base64.getEncoder().encodeToString(token.getBytes());
			MultivaluedMap<String, Object> headers = requestContext.getHeaders();
			headers.add("Authorization", basicAuthentication);
		} catch (IllegalArgumentException ex) {
			throw new IOException(ex);
		}
	}

}
