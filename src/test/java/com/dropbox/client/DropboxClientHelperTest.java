package com.dropbox.client;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.json.JSONException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class DropboxClientHelperTest {

	private static final String CONSUMER_KEY = "24uvvbp09jcgkv0";
	private static final String CONSUMER_SECRET = "arl24x8l1jgqy7p";
	private static final String USERNAME = "todotxt@hotmail.com";
	private static final String PASSWORD = "somepass";
	private static final String ACCESSTOKEN = "jglhstnw5oq661a";
	private static final String ACCESSTOKEN_SECRET = "sc1g1426pzpaqlr";
	private static final String REMOTEFILE = "/test.txt";
	private static final String REMOTEDIR = "/";
	private static final String TESTDATA = "test task 1";

	private static DropboxClient mClient;

	@BeforeClass
	public static void runBeforeClass() {
		// run for one time before all test cases
		mClient = DropboxClientHelper.newAuthenticatedClient(CONSUMER_KEY,
				CONSUMER_SECRET, ACCESSTOKEN, ACCESSTOKEN_SECRET);
	}

	@AfterClass
	public static void runAfterClass() {
		// run for one time after all test cases
	}

	@Before
	public void runBeforeEveryTest() {
		try {
			mClient.fileDelete(REMOTEDIR, REMOTEFILE, null);
		} catch (DropboxException e) {
			// System.out.println("runBeforeEveryTest: "+e.getMessage());
			// e.printStackTrace();
		}
	}

	@After
	public void runAfterEveryTest() {
		try {
			mClient.fileDelete(REMOTEDIR, REMOTEFILE, null);
		} catch (DropboxException e) {
			// System.out.println("runAfterEveryTest: "+e.getMessage());
			// e.printStackTrace();
		}
	}

	@Ignore
	@Test
	public void testNewClient() throws Exception {
		DropboxClient client = DropboxClientHelper.newClient(CONSUMER_KEY,
				CONSUMER_SECRET, USERNAME, PASSWORD);
		assertEquals("Could not instantiate client!", true, client != null);
	}

	@Ignore
	@Test
	public void testNewAuthenticatedClient() throws Exception {
		DropboxClient client = DropboxClientHelper.newAuthenticatedClient(
				CONSUMER_KEY, CONSUMER_SECRET, ACCESSTOKEN, ACCESSTOKEN_SECRET);
		assertEquals("Could not instantiate client!", true, client != null);
	}

	@Test
	public void testPutGetFile() throws DropboxException,
			IllegalStateException, IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(TESTDATA.getBytes());
		DropboxClientHelper.putFile(mClient, REMOTEDIR, REMOTEFILE, is,
				TESTDATA.length());
		String content = DropboxClientHelper.getFileContents(mClient,
				REMOTEFILE);
		assertEquals(TESTDATA, content);
	}

	@Test
	public void testPutDeleteFile() throws IllegalStateException, DropboxException,
			IOException, JSONException {
		ByteArrayInputStream is = new ByteArrayInputStream(TESTDATA.getBytes());
		DropboxClientHelper.putFile(mClient, REMOTEDIR, REMOTEFILE, is,
				TESTDATA.length());
		DropboxClientHelper.fileDelete(mClient, REMOTEFILE);
	}

	@Ignore("Ignore test case")
	@Test(expected = Exception.class)
	public void testException() throws Exception {
		throw new Exception();
	}

	@Ignore("Ignore test case")
	@Test
	public void testDummy() {
	}

	@Ignore("Ignore test case")
	@Test(timeout = 500)
	public void testTimeout() {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
	}

}
