package com.brif.nix.gdrive;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.brif.nix.model.User;
import com.brif.nix.oauth2.OAuth2Configuration;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentReference;

public class DriveManager {

	final static DriveManager driveManager = new DriveManager();

	private Drive service;
	private List<ParentReference> rootFolder = null;
	
	private User user;

	public static DriveManager getSingelton() {
		return driveManager;
	}

	public void setUser(User currentUser) throws IOException {
		this.user = currentUser;
		
		HttpTransport httpTransport = new NetHttpTransport();
		JsonFactory jsonFactory = new JacksonFactory();
		
		TokenResponse tr = new TokenResponse();
		tr.setAccessToken(currentUser.access_token);
		tr.setRefreshToken(currentUser.refresh_token);

		OAuth2Configuration conf = OAuth2Configuration
				.getConfiguration(currentUser.origin);

		final GoogleCredential credentials = new GoogleCredential.Builder()
				.setTransport(httpTransport)
				.setJsonFactory(jsonFactory)
				.setClientSecrets(conf.get("client_id"),
						conf.get("client_secret")).build()
				.setFromTokenResponse(tr);

		service = new Drive.Builder(httpTransport, jsonFactory, credentials)
				.build();

	}

	public List<ParentReference> getRootFolder() throws IOException {
		if (user == null) {
			throw new IllegalArgumentException();
		}
		
		if (rootFolder == null) {

			final FileList atts = service
					.files()
					.list()
					.setQ("title = 'brif-files' and mimeType = 'application/vnd.google-apps.folder'")
					.execute();
			File folder;

			if (atts.getItems().isEmpty()) {
				File body = new File();
				body.setTitle("brif-files");
				body.setMimeType("application/vnd.google-apps.folder");
				body.setDescription("Brif's attachments directory");
				folder = service.files().insert(body).execute();
			} else {
				folder = atts.getItems().get(0);
			}
			ParentReference newParent = new ParentReference();
			newParent.setId(folder.getId());
			rootFolder = new ArrayList<ParentReference>(1);
			rootFolder.add(newParent);
		}
		return rootFolder;
	}

	public String uploadStream(String name, String type, InputStream is)
			throws IOException {
		if (user == null) {
			throw new IllegalArgumentException();
		}
		
		System.out.println("new attachment " + name + " " + type);
		
	    java.io.File f = new java.io.File("/tmp/tmp_" + user.objectId + name);
	    FileOutputStream fos = new FileOutputStream(f);
	    byte[] buf = new byte[1024*50];
	    int bytesRead;
	    while((bytesRead = is.read(buf))!=-1) {
	        fos.write(buf, 0, bytesRead);
	    }
	    fos.close();
		
		File body = new File();
		body.setTitle(name);
		body.setMimeType(type);
		body.setParents(getRootFolder());
		FileContent fc = new FileContent(type, f);
		File uf = service.files().insert(body, fc).execute();
		
		f.delete();
		
		return uf.getId();
		
	}

}
