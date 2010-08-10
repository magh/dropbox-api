package com.dropbox.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import oauth.signpost.basic.DefaultOAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

import org.apache.http.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class DropboxClientHelper {

	public static final String REQUEST_TOKEN_URL = "https://www.dropbox.com/0/oauth/request_token";
	public static final String ACCESS_TOKEN_URL = "https://www.dropbox.com/0/oauth/access_token";
	public static final String AUTHORIZATION_URL = "https://www.dropbox.com/0/oauth/authorize";
//	public static final String REQUEST_TOKEN_URL = "http://api.getdropbox.com/0/oauth/request_token";
//	public static final String ACCESS_TOKEN_URL = "http://api.getdropbox.com/0/oauth/access_token";
//	public static final String AUTHORIZATION_URL = "http://api.getdropbox.com/0/oauth/authorize";
	
	private final static String ROOT = "sandbox";

	public static DropboxClient newClient(String consumerKey,
			String consumerSecret, String username, String password)
			throws OAuthMessageSignerException, OAuthNotAuthorizedException,
			OAuthExpectationFailedException, OAuthCommunicationException,
			IllegalStateException, IOException {

		CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(
				consumerKey, consumerSecret);
		DefaultOAuthProvider provider = new DefaultOAuthProvider(
				REQUEST_TOKEN_URL, ACCESS_TOKEN_URL, AUTHORIZATION_URL);

		String url = provider.retrieveRequestToken(consumer, null);
		String token = consumer.getToken();
		Util.authorizeDropbox(url, username, password, token);
		Util.log("retrieveAccessToken");
		provider.retrieveAccessToken(consumer, "");
		return new DropboxClient(consumer);
	}

	public static DropboxClient newAuthenticatedClient(String consumerKey,
			String consumerSecret, String accessToken, String accessTokenSecret) {
		CommonsHttpOAuthConsumer consumer = new CommonsHttpOAuthConsumer(
				consumerKey, consumerSecret);
		consumer.setTokenWithSecret(accessToken, accessTokenSecret);
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

	public static void putFile(DropboxClient client, String remotedir,
			String remotename, InputStream stream, long length) throws DropboxException {
		HttpResponse resp = client.putFileStream(ROOT, remotedir, remotename, stream, length);
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
		return Util.readStream(is);
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

}
