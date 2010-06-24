package com.dropbox.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

public class RESTUtility {
	protected static String defaultProtocol = "http";
	protected static String secureProtocol = "https";

	protected static int BUFFER_SIZE = 2048;
	protected static HttpParams sParams;
	protected static ThreadSafeClientConnManager sManager;

	static {
		// sets up parameters
		sParams = new BasicHttpParams();
		HttpProtocolParams.setVersion(sParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(sParams, "utf-8");
		sParams.setBooleanParameter("http.protocol.expect-continue", false);

		// registers schemes for both http and https
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory
				.getSocketFactory(), 80));
		final SSLSocketFactory sslSocketFactory = SSLSocketFactory
				.getSocketFactory();
		sslSocketFactory
				.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

		registry.register(new Scheme("https", sslSocketFactory, 443));
		sManager = new ThreadSafeClientConnManager(sParams, registry);
	}

	static public String defaultProtocol() {
		return defaultProtocol;
	}

	static public String secureProtocol() {
		return secureProtocol;
	}

	static public HttpClient getClient() {
		// DefaultHttpClient ret = new DefaultHttpClient(sManager, sParams);
		DefaultHttpClient ret = new DefaultHttpClient();
		return ret;
	}

	/**
	 * Used internally to URL encode a list of parameters, which makes it easier
	 * to do params than with a map. If you want to use this, the params are
	 * organized as key=value pairs in a row, and should convert to Strings.
	 */
	static public String urlencode(Object[] params) {
		String result = "";
		try {
			boolean firstTime = true;
			for (int i = 0; i < params.length; i += 2) {
				if (params[i + 1] != null) {
					if (firstTime) {
						firstTime = false;
					} else {
						result += "&";
					}
					result += URLEncoder.encode("" + params[i], "UTF-8") + "="
							+ URLEncoder.encode("" + params[i + 1], "UTF-8");
				}
			}

		} catch (UnsupportedEncodingException e) {
			// This shouldn't show up, since UTF-8 should always be supported.
		}
		return result;
	}

	/**
	 * Used internally to simplify reading a response in buffered lines.
	 */
	static public String readResponse(HttpResponse response) throws IOException {
		HttpEntity ent = response.getEntity();
		if (ent != null) {
			BufferedReader in = new BufferedReader(new InputStreamReader(ent
					.getContent()), BUFFER_SIZE);
			String inputLine = null;
			String result = "";

			while ((inputLine = in.readLine()) != null) {
				result += inputLine;
			}

			response.getEntity().consumeContent();
			return result;
		} else {
			return "";
		}
	}

	static public JSONObject parseAsJSON(HttpResponse response)
			throws IOException {
		String body = readResponse(response);

		int statusCode = response.getStatusLine().getStatusCode();
		if (statusCode != 200) {
			throw new IOException("Invalid status code: " + statusCode + " "
					+ response.getStatusLine());
		}
		try {
			return new JSONObject(body);
		} catch (JSONException e) {
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Used internally to execute the Apache HTTP Components request and pull
	 * out either JSON or a raw response. A Map is always returned that has
	 * either the real data from your call, or an "ERROR" or "RESULT" key. If
	 * "ERROR" is given then you have "RESULT" as the body of the HTTP response
	 * for informational purposes. If you have "RESULT" only then the body
	 * couldn't be parsed as JSON and is just a raw string.
	 */
	static public JSONObject executeRequest(HttpUriRequest req)
			throws IOException {
		HttpClient client = getClient();
		HttpResponse response = client.execute(req);
		JSONObject result = parseAsJSON(response);
		if (result == null) {
			throw new RuntimeException("Invariant: result can't be null.");
		}
		return result;
	}

	/**
	 * Used internally to construct a complete URL to a given host, which can
	 * sometimes be the "API host" or the "content host" depending on the type
	 * of call.
	 */
	static public String buildFullURL(String protocol, String host, int port,
			String target) {
		String port_string = (port == 80) ? "" : (":" + port);
		return protocol + "://" + host + port_string + target;
	}

	/**
	 * Used internally to build a URL path + params (if given) according to the
	 * API_VERSION.
	 */
	static public String buildURL(String url, int apiVersion, Object[] params) {
		try {
			// we have to encode the whole line, then remove + and / encoding to
			// get a good oauth url
			url = URLEncoder.encode("/" + apiVersion + url, "UTF-8");
			url = url.replace("%2F", "/").replace("+", "%20");

			if (params != null && params.length > 0) {
				url += "?" + urlencode(params);
			}
		} catch (UnsupportedEncodingException uce) {
			// This shouldn't show up, since UTF-8 should always be supported.
		}

		return url;
	}

	/**
	 * Used internally to simplify making requests to the services, and handling
	 * an error, parsing JSON, or returning a non-JSON body. See executeRequest
	 * to see what's in the returned Map.
	 */
	static public JSONObject request(String method, String protocol,
			String host, int port, String url, int apiVersion, Object params[],
			CommonsHttpOAuthConsumer consumer) throws DropboxException {

		if (!method.equals("GET") && !method.equals("POST")) {
			throw new DropboxException("Only GET and POST supported.");
		}
		HttpUriRequest req = null;
		String target = null;

		try {
			if (method.equals("GET")) {
				target = buildFullURL(protocol, host, port, buildURL(url,
						apiVersion, params));
				req = new HttpGet(target);
			} else {
				target = buildFullURL(protocol, host, port, buildURL(url,
						apiVersion, null));
				HttpPost post = new HttpPost(target);

				if (params != null && params.length > 2) {
					if (params.length % 2 != 0) {
						throw new Exception(
								"Params must have an even number of elements.");
					}
					List<NameValuePair> nvps = new ArrayList<NameValuePair>();
					for (int i = 0; i < params.length; i += 2) {
						if (params[i + 1] != null) {
							nvps.add(new BasicNameValuePair("" + params[i], ""
									+ params[i + 1]));
						}
					}

					post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
					req = post;
				}
			}

			consumer.sign(req);

			return executeRequest(req);
		} catch (Exception e) {
			throw new DropboxException(e);
		}
	}

}
