package com.dropbox.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

public class DropboxClientHelper {

	public static final String REQUEST_TOKEN_URL = "http://api.getdropbox.com/0/oauth/request_token";
	public static final String ACCESS_TOKEN_URL = "http://api.getdropbox.com/0/oauth/access_token";
	public static final String AUTHORIZATION_URL = "http://api.getdropbox.com/0/oauth/authorize";

	private final static String ROOT = "sandbox";

	public static DropboxClient newClient(String consumerKey,
			String consumerSecret, String username, String password)
			throws OAuthMessageSignerException, OAuthNotAuthorizedException,
			OAuthExpectationFailedException, OAuthCommunicationException,
			IOException {

		CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(
				consumerKey, consumerSecret);
		DefaultOAuthProvider provider = new DefaultOAuthProvider(
				REQUEST_TOKEN_URL, ACCESS_TOKEN_URL, AUTHORIZATION_URL);

		String url = provider.retrieveRequestToken(consumer, null);
		log("Url is: " + url);
		authorizeForm(url, username, password);

		provider.retrieveAccessToken(consumer, "");
		return new DropboxClient(consumer);
	}

	public static void putFile(DropboxClient client, String remotedir,
			String localfile) throws DropboxException {
		File lfile = new File(localfile);
		putFile(client, remotedir, lfile);
	}

	public static void putFile(DropboxClient client, String remotedir,
			File localfile) throws DropboxException {
		HttpResponse resp = client.putFile(ROOT, remotedir, localfile);
		assertValidResponse(resp);
	}

	public static InputStream getFileStream(DropboxClient client, String remotefile)
			throws DropboxException, IllegalStateException, IOException {
		HttpResponse resp = client.getFile(ROOT, remotefile);
		assertValidResponse(resp);
		return resp.getEntity().getContent();
	}

	public static String getFileContents(DropboxClient client, String remotefile)
			throws DropboxException, IllegalStateException, IOException {
		InputStream is = getFileStream(client, remotefile);
		return readStream(is);
	}

	public static void fileDelete(DropboxClient client, String remotefile)
			throws DropboxException, JSONException {
		JSONObject resp = client.fileDelete(ROOT, remotefile, null);
		assertValidResponse(resp);
	}

	public static void fileCreateFolder(DropboxClient client, String remotedir)
			throws DropboxException, JSONException {
		JSONObject resp = client.fileCreateFolder(ROOT, remotedir, null);
		assertValidResponse(resp);
	}

	public static void assertValidResponse(JSONObject resp)
			throws DropboxException, JSONException {
		if (resp == null) {
			throw new DropboxException("Should always get a response.");
		}
		log(resp.toString());
	}

	public static void assertValidResponse(HttpResponse resp)
			throws DropboxException {
		if (resp == null) {
			throw new DropboxException("Should always get a response.");
		}
		int status = resp.getStatusLine().getStatusCode();
		if (status != 200) {
			throw new DropboxException("Invalid status: " + status);
		}
	}

	private DropboxClientHelper() {
	}

	// Util below...

	public static void authorizeForm(String url, String username,
			String password) throws IOException {

		log("AUTHORIZING: " + url);

		InputStream is = null;
		try {
			is = getStreamHttpClient(url, null);
			String str = readStream(is);
			// log("auth res: "+str);
			if (str.contains("Log in")) {
				log("log in");
				List<NameValuePair> pairs = new ArrayList<NameValuePair>();
				// pairs.add(new BasicNameValuePair("t", "791206fc33"));
				pairs.add(new BasicNameValuePair("cont", url));
				pairs.add(new BasicNameValuePair("login_email", username));
				pairs.add(new BasicNameValuePair("login_password", password));
				// pairs.add(new BasicNameValuePair("remember_me", "false"));
				// pairs.add(new BasicNameValuePair("login_submit", "Log in"));
				InputStream stream = getStreamHttpClient(
						"https://www.dropbox.com/login", pairs);
				String res = readStream(stream);
				// log("login res: "+res);
				if (str.contains("Success")) {
					log("SUCCESS: Logged in!");
				} else {
					log(res);
					log("ERROR: Failed to log in!");
				}
			} else {
				log("already logged in");
			}
		} catch (Exception e1) {
			log("Exception - " + e1);
			e1.printStackTrace();
		} finally {
			closeStream(is);
		}
	}

	private static void log(String msg) {
		System.out.println(msg);
	}

	public static String readStream(InputStream is) {
		if (is == null) {
			return null;
		}
		try {
			int c;
			byte[] buffer = new byte[8192];
			StringBuilder sb = new StringBuilder();
			while ((c = is.read(buffer)) != -1) {
				sb.append(new String(buffer, 0, c));
			}
			return sb.toString();
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			closeStream(is);
		}
		return null;
	}

	public static void closeStream(Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
				stream = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static InputStream getStreamHttpClient(String url,
			List<NameValuePair> pairs) throws Exception {
		try {
			DefaultHttpClient client = new DefaultHttpClient();
			HttpRequestBase request;
			if (pairs != null) {
				request = new HttpPost(url);
				((HttpPost) request).setEntity(new UrlEncodedFormEntity(pairs));
			} else {
				request = new HttpGet(url);
			}
			HttpResponse response = client.execute(request);
			int statusCode = response.getStatusLine().getStatusCode();
			if (statusCode != 200 && statusCode != 206) {
				String err = "Invalid status code: " + statusCode + " phrase: "
						+ response.getStatusLine().getReasonPhrase() + " url: "
						+ url;
				throw new Exception(err);
			}
			HttpEntity entity = response.getEntity();
			return entity.getContent();
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}

}
