package com.dropbox.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * High-level API for Dropbox functions that wraps the calls in DropboxClient.
 * 
 * @author tom
 * 
 */
public class DropboxAPI {
	protected DropboxClient mClient;

	final public static int AUTH_STATUS_NONE = -1;
	final public static int AUTH_STATUS_FAILURE = 0;
	final public static int AUTH_STATUS_SUCCESS = 1;
	final public static int AUTH_STATUS_NETWORK_ERROR = 2;

	public abstract class DropboxReturn {
		public int httpCode;
		public String httpReason;
		public String httpBody;
		protected boolean hasError = false;

		public DropboxReturn(HttpResponse resp) {
			StatusLine status = resp.getStatusLine();
			httpCode = status.getStatusCode();
			httpReason = status.getReasonPhrase();
		}

		public DropboxReturn(JSONObject map) {
			if (map == null) {
				hasError = true;
			} else {
				httpCode = 200;
			}
		}

		public boolean isError() {
			return (httpCode >= 300);
		}
	}

	public class Account extends DropboxReturn {
		public String country;
		public String displayName;
		public long quotaQuota;
		public long quotaNormal;
		public long quotaShared;
		public long uid;
		public String email; // For internal Dropbox use; not normally set.

		public Account(JSONObject jo) throws JSONException {
			super(jo);

			jo = jo.getJSONObject("body");
			if (jo != null) {
				country = jo.getString("country");
				displayName = jo.getString("display_name");
				uid = jo.getLong("uid");
				email = jo.getString("email");

				JSONObject quotaInfo = jo.getJSONObject("quota_info");
				if (quotaInfo != null) {
					quotaQuota = quotaInfo.getLong("quota");
					quotaNormal = quotaInfo.getLong("normal");
					quotaShared = quotaInfo.getLong("shared");
				}
			}
		}
	}

	public class Entry extends DropboxReturn {
		public long bytes;
		public String hash;
		public String icon;
		public boolean is_dir;
		public String modified;
		public String path;
		public String root;
		public String size;
		public String mime_type;
		public long revision;
		public boolean thumb_exists;

		public ArrayList<Entry> contents;

		public Entry(JSONObject jo) throws JSONException {
			super(jo);

			if (!hasError) {
				JSONObject bmap = jo.getJSONObject("body");

				if (bmap != null) {
					bytes = bmap.getLong("bytes");

					hash = bmap.getString("hash");
					icon = bmap.getString("icon");

					is_dir = bmap.getBoolean("is_dir");

					modified = bmap.getString("modified");
					path = bmap.getString("path");
					root = bmap.getString("root");
					size = bmap.getString("size");
					mime_type = bmap.getString("mime_type");

					revision = bmap.getLong("revision");

					thumb_exists = bmap.getBoolean("thumb_exists");

					JSONArray json_contents = bmap.getJSONArray("contents");
					if (json_contents != null) {
						contents = new ArrayList<Entry>();

						int arraylength = json_contents.length();
						for (int i = 0; i < arraylength; i++) {
							JSONObject jo2 = json_contents.getJSONObject(i);
							contents.add(new Entry(jo2));
						}
					}
				}
			}
		}

		public String fileName() {
			String[] dirs = path.split("/");
			return dirs[dirs.length - 1];
		}

		public String parentPath() {
			int ind = path.lastIndexOf('/');
			return path.substring(0, ind + 1);
		}

		public String pathNoInitialSlash() {
			if (path.startsWith("/")) {
				return path.substring(1);
			} else {
				return path;
			}
		}
	}

	public class FileDownload extends DropboxReturn {
		public InputStream is;
		public HttpEntity entity;
		public String mimeType;
		public String etag;
		public long length;

		public FileDownload(HttpResponse resp) {
			super(resp);

			Header mime = resp.getFirstHeader("mime-type");
			if (mime != null) {
				mimeType = mime.getValue();
			}
			Header et = resp.getFirstHeader("etag");
			if (et != null) {
				etag = et.getValue();
			}

			entity = resp.getEntity();
			if (entity != null) {
				length = entity.getContentLength();
				try {
					is = entity.getContent();
				} catch (IOException ioe) {
					System.out.println(ioe.toString());
				}
			}
		}
	}

	public void deauthenticate() {
		mClient = null;
	}

	public boolean isAuthenticated() {
		if (mClient != null) {
			return true;
		} else {
			return false;
		}
	}

	public Entry metadata(String root, String path, int file_limit,
			String hash, boolean list) {
		try {
			JSONObject dirinfo = mClient.metadata(root, path, file_limit, hash,
					list, false, null);
			return new Entry(dirinfo);
		} catch (DropboxException de) {
			System.out.println(de.toString());
		} catch (JSONException e) {
			System.out.println(e.toString());
		}
		return null;
	}

	public Account accountInfo() {
		try {
			JSONObject accountInfo = mClient.accountInfo(false, null);
			return new Account(accountInfo);
		} catch (DropboxException de) {
			System.out.println(de.toString());
		} catch (JSONException e) {
			System.out.println(e.toString());
		}
		return null;
	}

	public FileDownload getFileStream(String root, String dbPath, String etag) {
		try {
			HttpResponse response = mClient.getFileWithVersion(root, dbPath,
					etag);
			return new FileDownload(response);
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public File getFile(String root, String dbPath, File localFile, String etag) {
		FileOutputStream fos = null;
		HttpEntity entity = null;
		String result = "";
		try {
			FileDownload response = getFileStream(root, dbPath, etag);

			entity = response.entity;

			// localFile = openNewLocalFile(root, dbPath);
			try {
				fos = new FileOutputStream(localFile);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			entity.writeTo(fos);
		} catch (SocketException se) {
			System.out.println(se.toString());
		} catch (IOException ioe) {
			System.out.println(ioe.toString());
		} finally {
			System.out.println("Result: " + result);
			try {
				if (entity != null)
					entity.consumeContent();
			} catch (IOException ioe) {
			}
			try {
				if (fos != null)
					fos.close();
			} catch (IOException ioe) {
			}

		}
		return localFile;
	}

	public void putFile(String root, String dbPath, File localFile) {
		try {
			HttpResponse resp = mClient.putFile(root, dbPath, localFile);
		} catch (DropboxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
