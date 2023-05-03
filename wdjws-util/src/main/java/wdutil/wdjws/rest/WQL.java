package wdutil.wdjws.rest;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

public class WQL {

	public static <T> T datasources(String serviceHost, String tenantId, String alias, ClientRequestFilter auth, Class<T> resultType) throws Exception {

		Client client = ClientBuilder.newClient();
		UriBuilder uriBuilder = UriBuilder.fromUri(String.format("https://%s/ccx/api/wql/v1/%s/dataSources", serviceHost, tenantId));
		if (alias != null) {
			uriBuilder.queryParam("alias", alias);
		}
		Response response = client.target(uriBuilder).register(auth).request().header("X-Tenant", tenantId).get();
		return response.readEntity(resultType);

	}

	public static <T> T datasource(String serviceHost, String tenantId, String wid, ClientRequestFilter auth, Class<T> resultType) throws Exception {

		Client client = ClientBuilder.newClient();
		UriBuilder uriBuilder = UriBuilder.fromUri(String.format("https://%s/ccx/api/wql/v1/%s/dataSources/%s", serviceHost, tenantId, wid));
		Response response = client.target(uriBuilder).register(auth).request().header("X-Tenant", tenantId).get();
		return response.readEntity(resultType);

	}

	public static <T> T datasourceFields(String serviceHost, String tenantId, String wid, String alias, ClientRequestFilter auth, Class<T> resultType) throws Exception {

		Client client = ClientBuilder.newClient();
		UriBuilder uriBuilder = UriBuilder.fromUri(String.format("https://%s/ccx/api/wql/v1/%s/dataSources/%s/fields", serviceHost, tenantId, wid));
		if (alias != null) {
			uriBuilder.queryParam("alias", alias);
		}
		Response response = client.target(uriBuilder).register(auth).request().header("X-Tenant", tenantId).get();
		return response.readEntity(resultType);

	}

	public static <T> T datasourceFilters(String serviceHost, String tenantId, String wid, ClientRequestFilter auth, Class<T> resultType) throws Exception {

		Client client = ClientBuilder.newClient();
		UriBuilder uriBuilder = UriBuilder.fromUri(String.format("https://%s/ccx/api/wql/v1/%s/dataSources/%s/dataSourceFilters", serviceHost, tenantId, wid));
		Response response = client.target(uriBuilder).register(auth).request().header("X-Tenant", tenantId).get();
		return response.readEntity(resultType);

	}

	public static <T> T query(String serviceHost, String tenantId, String query, ClientRequestFilter auth, Class<T> resultType) throws Exception {

		Client client = ClientBuilder.newClient();
		UriBuilder uriBuilder = UriBuilder.fromUri(String.format("https://%s/ccx/api/wql/v1/%s/data", serviceHost, tenantId));
		Response response = null;
		if (query.length() <= 2048) {
			uriBuilder.queryParam("query", query);
			response = client.target(uriBuilder).register(auth).request().header("X-Tenant", tenantId).get();
		} else {
			String request = String.format("{\n" + "    \"query\":\"%s\"\n" + "}", query); // TODO: JSON encode the query
			client.target(uriBuilder).register(auth).request().header("X-Tenant", tenantId).post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));
		}

		return response.readEntity(resultType);

	}
}
