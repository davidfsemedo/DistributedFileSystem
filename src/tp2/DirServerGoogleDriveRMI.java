package tp2;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scribe.builder.api.*;

import trab1.both.ws.IOException_Exception;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 *         The DirServerGoogleDriveRMI class represents a DirServer proxy to
 *         Google Drive service
 */
public class DirServerGoogleDriveRMI extends UnicastRemoteObject implements
		IFileServer {

	private static final long serialVersionUID = 1L;

	private static final String CLIENT_ID = "348940569341-lc48ftmu1jdi901lqdfffgghifqm6cip.apps.googleusercontent.com";
	private static final String API_SECRET = "MJovI584acasz04EW3iZBaGB";

	private static final String SCOPE = "https://www.googleapis.com/auth/drive";
	private static final String DRIVE_FILES_URL = "https://www.googleapis.com/drive/v2/files";
	private static final String DRIVE_UPLOAD_FILE_URL = "https://www.googleapis.com/upload/drive/v2/files?uploadType=resumable";
	private static final String DRIVE_FOLDER = "application/vnd.google-apps.folder";
	private static final String DEFAULT_MIME_TYPE = "text/plain";
	private static final String DATE_FORMAT = "yyyy-MM-dd'T'hh:mm:ss.SSS'Z'";

	private static final String REQUEST_CONTENT_TYPE = "application/json";
	protected static final int GOOGLE_DRIVE_CHUNK_SIZE = 524288;

	private static final String USERNAME = "server";
	private static final String PASSWORD = "server";

	private OAuthService googleDriveService;
	private Token googleDriveToken;
	private Token googleDriveAccessToken;

	protected static final int CHUNK_SIZE = 32768;

	@SuppressWarnings("unused")
	private File basePath;
	private String id;
	private String url;
	private String community;
	private int nrIdsGenerated;
	private String baseId;

	private Map<String, String> usersSessions;

	private static final int DEFAULT_ATTEMPTS = 3;
	public static final String DEFAULT_MULTICAST_ADDRESS = "224.0.1.0";
	public static int DEFAULT_MULTICAST_PORT_RECEIVE = 9000;
	public static int DEFAULT_MULTICAST_PORT_SEND = 9001;
	private static final int BUFFER_SIZE = 65536;
	public static final String CLIENT_LOOKING_FOR_SERVER = "Looking For a Server in local network.";
	private static final String SERVER_MESSAGE = "SRV";
	private static final String CLIENT_MESSAGE = "CLT";
	private static final String WS = "ws";
	private static final String RMI = "rmi";
	private static final int WAITING_PERIOD = 5000;
	private static final String BASE_PATH = ".";
	private static String SERVERTYPE = "googledrive";

	protected Set<String> knownServers;
	protected Map<String, String> users;
	private ServerUnresponsiveHandler servHandler;
	private ServerConnectionManager servCManager;

	/**
	 * Constructs a DirServerGoogleDriveRMI
	 * 
	 * @param community
	 *            community of servers to connect
	 * @param id
	 *            id of the server
	 * @param ip
	 *            ip in which the server will be host
	 * @param port
	 *            port in which the server will be bind
	 * @param basePath
	 *            base working directory
	 * @throws Exception
	 */
	public DirServerGoogleDriveRMI(String community, String id, String ip,
			String basePath) throws Exception {

		super(0, new RMISSLClientSocketFactory(),
				new RMISSLServerSocketFactory());

		this.basePath = new File(basePath);
		this.knownServers = new HashSet<String>();
		this.community = community;
		this.nrIdsGenerated = 1;
		this.baseId = id;
		this.id = id;
		this.url = "rmi://" + ip + "/" + community + "-" + SERVERTYPE + "-"
				+ id;
		this.servHandler = new ServerUnresponsiveHandler(false, USERNAME,
				PASSWORD);
		this.users = new HashMap<String, String>();
		this.users.put(USERNAME, PASSWORD);
		servCManager = new ServerConnectionManager(servHandler, knownServers);

		googleDriveService = new ServiceBuilder().provider(GoogleApi.class)
				.apiKey(CLIENT_ID).apiSecret(API_SECRET).scope(SCOPE).build();

		googleDriveToken = googleDriveService.getRequestToken();
		usersSessions = new HashMap<String, String>();
		init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see trab1.both.IFileServer#getUrl()
	 */
	@Override
	public String getUrl(String username, String pw) throws RemoteException {
		return this.url;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see trab1.both.IFileServer#getName()
	 */
	@Override
	public String getName(String username, String pw) throws RemoteException {
		return "/" + community + "-" + id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see trab1.both.IFileServer#getCommunity()
	 */
	@Override
	public String getCommunity(String username, String pw)
			throws RemoteException {
		return this.community;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see trab1.both.IFileServer#getServerList()
	 */
	@Override
	public List<String> getServerList() throws RemoteException {
		return new ArrayList<String>(knownServers);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#dir(java.lang.String, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public String[] dir(String path, String username, String pw)
			throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			path = path.replaceAll("^(\\.|\\./|/)", "");
			List<String> dir = new LinkedList<String>();
			OAuthRequest request;
			Response response;
			JSONObject res;
			JSONArray items;
			JSONParser parser = new JSONParser();
			Iterator<?> it;
			if (path.equals(".") || path.equals(""))
				request = new OAuthRequest(Verb.GET, DRIVE_FILES_URL
						+ "?q=%22root%22+in+parents");
			else
				request = new OAuthRequest(Verb.GET, DRIVE_FILES_URL + "?q=%22"
						+ getResourceID(path) + "%22+in+parents");

			googleDriveService.signRequest(googleDriveAccessToken, request);
			response = request.send();
			try {
				if (response.getCode() == 200) {
					res = (JSONObject) parser.parse(response.getBody());
					items = (JSONArray) res.get("items");
					if (items != null) {
						it = items.iterator();
						while (it.hasNext()) {
							dir.add((String) ((JSONObject) it.next())
									.get("title"));

						}
					} else
						return null;
				}
				String[] result = new String[dir.size()];
				dir.toArray(result);

				return result;
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return null;
		} else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#createDir(java.lang.String, java.lang.String,
	 * java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean createDir(String path, String username, String pw)
			throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			path = path.replaceAll("^(\\.|\\./|/)", "");
			String id = getResourceID(path);
			if (id != null)
				return false;

			String[] splittedPath = path.split("/");
			String p = "";
			if (splittedPath.length > 0) {
				for (int i = 0; i < splittedPath.length - 1; i++) {
					p += splittedPath[i]
							+ (i + 1 < splittedPath.length - 1 ? "/" : "");
				}
			} else
				p = path;
			String parentId = getResourceID(p);
			String title = splittedPath.length > 0 ? splittedPath[splittedPath.length - 1]
					: path;

			OAuthRequest request = new OAuthRequest(Verb.POST, DRIVE_FILES_URL);

			request.addHeader("Content-Type", REQUEST_CONTENT_TYPE);

			JSONObject bodyParameters = new JSONObject();

			bodyParameters.put("title", title);
			bodyParameters.put("mimeType", DRIVE_FOLDER);
			JSONArray parents = new JSONArray();
			JSONObject val = new JSONObject();
			val.put("id", parentId == null ? "root" : parentId);
			parents.add(val);
			bodyParameters.put("parents", parents);

			request.addPayload(bodyParameters.toJSONString());
			googleDriveService.signRequest(googleDriveAccessToken, request);

			Response response = request.send();
			return (response.getCode() == 200);
		} else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#newServerConnected(java.lang.String)
	 */
	@Override
	public void newServerConnected(String url) throws RemoteException,
			NotBoundException {
		String aux = null;

		if (!this.knownServers.contains(url)) {
			knownServers.add(url);
			Iterator<String> it = knownServers.iterator();
			Pair<String, Object> p;

			while (it.hasNext()) {
				try {
					aux = it.next();

					if (!aux.equals(this.url)) {

						p = servCManager.getConnectionToServer(aux);
						if (p == null)
							continue;
						if (p.getFirst().equals(WS)) {
							trab1.both.ws.DirServerWS anotherServer = (trab1.both.ws.DirServerWS) p
									.getSecond();
							anotherServer
									.updateKnownServerList(new ArrayList<String>(
											knownServers));
						}

						else {
							IFileServer anotherServer = (IFileServer) p
									.getSecond();
							anotherServer.updateKnownServerList(
									new ArrayList<String>(knownServers),
									USERNAME, PASSWORD);
						}
					}

				} catch (javax.xml.ws.WebServiceException e) {
					servHandler.treatServerException(aux, knownServers);
				} catch (RemoteException e) {
					servHandler.treatServerException(aux, knownServers);
				} catch (InvalidCredentialsException e) {
					System.out
					.println("Invalid Username or Password! Please login Again!");
					System.exit(1);
				}
			}
		}
		System.out.println("New server connected. Known Servers List updated");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#removeFile(java.lang.String)
	 */
	@Override
	public boolean removeFile(String path, String username, String pw)
			throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw))
			return removeContent(path, false);
		else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#removeDirectory(java.lang.String)
	 */
	@Override
	public boolean removeDirectory(String path, String username, String pw)
			throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw))
			return removeContent(path, true);
		else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#getFileInfo(java.lang.String, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public FileInfo getFileInfo(String path, String username, String pw)
			throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			path = path.replaceAll("^(\\.|\\./|/)", "");
			String id = getResourceID(path);
			OAuthRequest request;
			if (id == null)
				return null;
			if (path.equals(".") || path.equals(""))
				request = new OAuthRequest(Verb.GET, DRIVE_FILES_URL + "/root");
			else
				request = new OAuthRequest(Verb.GET, DRIVE_FILES_URL + "/" + id);
			googleDriveService.signRequest(googleDriveAccessToken, request);
			Response response = request.send();
			try {
				if (response.getCode() == 200) {
					JSONParser parser = new JSONParser();
					JSONObject res = (JSONObject) parser.parse(response
							.getBody());
					String title;
					long length = 0;
					Date lastModifiedDate = null;
					boolean isFile = false;
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
							DATE_FORMAT);
					isFile = !((String) res.get("mimeType"))
							.equals(DRIVE_FOLDER);
					title = (String) res.get("title");
					String fileSize = (String) res.get("fileSize");
					if (fileSize != null) // a folder has this field with null
											// value
						length = Long.valueOf(fileSize);
					else
						length = 0;
					lastModifiedDate = simpleDateFormat.parse((String) res
							.get("modifiedDate"));
					return new FileInfo(title, length, lastModifiedDate, isFile);
				}
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (java.text.ParseException e) {
				e.printStackTrace();
			}
			return null;
		} else
			throw new InvalidCredentialsException();
	}

	public static void main(String[] args) throws ParseException,
			UnsupportedEncodingException {
		try {

			String anotherServerUrl = "";
			String community = "";
			String ipAddress = "";
			String basePath = "";
			final DirServerGoogleDriveRMI server;

			Pattern p = Pattern.compile("([0-9]{1,3}[.]){3}[0-9]{1,3}");

			if (args.length == 5) {
				if (args[0].equals("-bp")) {
					basePath = args[1];
					community = args[2];
					ipAddress = args[4];
					anotherServerUrl = args[3];
				}
			}

			else if (args.length == 4) {
				if (args[0].equals("-bp")) {
					basePath = args[1];
					community = args[2];
					ipAddress = args[3];
					anotherServerUrl = "";
				} else {
					basePath = BASE_PATH;
					community = args[0];
					ipAddress = args[1];
					anotherServerUrl = args[2];
				}
			} else if (args.length == 2 | args.length == 3) {
				basePath = BASE_PATH;
				community = args[0];
				anotherServerUrl = args.length == 3 ? args[1] : "";
				ipAddress = args.length == 3 ? args[2] : args[1];

			} else {
				System.out
						.println("Use: java DirServerRMI [-bp BasePath] comunnity [URL] serverIPAddress");
				return;
			}
			System.out.println("ip: " + ipAddress);
			Matcher m = p.matcher(ipAddress);
			if (!m.matches()) {
				System.out.println("Invalid IP address!");
				return;
			}

			System.out.println(basePath);

			System.getProperties().put("java.security.policy", "policy.all");

			try {
				LocateRegistry.createRegistry(1099);
			} catch (RemoteException e) {
				// if not start it
				// do nothing - already started with rmiregistry
			}

			if (System.getSecurityManager() == null) {
				System.setSecurityManager(new RMISecurityManager());
			}
			System.setProperty("javax.net.ssl.keyStore", "server.ks");
			System.setProperty("javax.net.ssl.keyStorePassword", "123456");

			System.setProperty("javax.net.ssl.trustStore", "cacerts");
			System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

			if (anotherServerUrl != "") {
				System.out
						.println("SERVER STARTUP: Contacting community server with url: "
								+ anotherServerUrl + " ...");
				ServerConnectionManager servCManager = new ServerConnectionManager(
						new ServerUnresponsiveHandler(false, USERNAME, PASSWORD),
						new HashSet<String>());
				Pair<String, Object> p2 = servCManager
						.getConnectionToServer(anotherServerUrl);
				String id;
				trab1.both.ws.DirServerWS anotherServerWS = null;
				IFileServer anotherServerRMI = null;
				if (p2.getFirst().equals(WS)) {
					anotherServerWS = (trab1.both.ws.DirServerWS) p2
							.getSecond();
					id = anotherServerWS.getNextId();
				} else {
					anotherServerRMI = (IFileServer) p2.getSecond();
					id = anotherServerRMI.getNextId(USERNAME, PASSWORD);
				}
				System.out.println("SERVER STARTUP: Connection Estabilished!");

				server = new DirServerGoogleDriveRMI(community, id, ipAddress,
						basePath);
				Naming.rebind(server.getUrl(USERNAME, PASSWORD), server);
				if (p2.getFirst().equals(WS))
					anotherServerWS.newServerConnected(server.getUrl(USERNAME,
							PASSWORD));
				else {
					anotherServerRMI.newServerConnected(server.getUrl(USERNAME,
							PASSWORD));
					server.getUserDataBase();
				}
			} else {
				server = new DirServerGoogleDriveRMI(community, "1", ipAddress,
						basePath);
				Naming.rebind(server.getUrl(USERNAME, PASSWORD), server);

				server.knownServers.add(server.getUrl(USERNAME, PASSWORD));
			}

			System.out.println("SERVER STARTUP: DirServer bound in registry");
			System.out.println("SERVER STARTUP: Server URL: "
					+ server.getUrl(USERNAME, PASSWORD));

			System.out.println("SERVER IP: " + ipAddress);
			System.out.println("BASE WORKING PATH: " + basePath);

			waitForClients(server.getUrl(USERNAME, PASSWORD));

		} catch (Exception e) {
			e.printStackTrace();
			System.out
					.println("It was not possible to launch the server. Please confirm your parameters.");
		}

	}

	/**
	 * This method announces to all servers that this server will shutdown
	 */
	protected void announceShutdown() {
		boolean done = false;
		String aux = null;
		Pair<String, Object> p;
		if (knownServers.size() == 1) {
			System.out.println("No servers Online to warn!");
			return;
		}
		for (int i = 0; !done && i < DEFAULT_ATTEMPTS; i++) {
			System.out.println("SERVER SHUTDOWN: Attempt " + (i + 1) + " ...");
			Iterator<String> it = knownServers.iterator();
			while (!done && it.hasNext()) {
				try {
					aux = it.next();
					servCManager.updateServerList(knownServers);
					p = servCManager.getConnectionToServer(aux);
					if (p == null)
						continue;
					if (p.getFirst().equals(WS)) {
						trab1.both.ws.DirServerWS anotherServer = (trab1.both.ws.DirServerWS) p
								.getSecond();
						anotherServer.receiveShutdownAnnouncement(this.getUrl(
								USERNAME, PASSWORD));

					} else {
						IFileServer anotherServer = (IFileServer) p.getSecond();
						anotherServer.receiveShutdownAnnouncement(
								this.getUrl(USERNAME, PASSWORD), USERNAME,
								PASSWORD);
					}
					done = true;
				} catch (javax.xml.ws.WebServiceException e) {
					servHandler.treatServerException(aux, knownServers);
				} catch (RemoteException e) {
					servHandler.treatServerException(aux, knownServers);
				} catch (InvalidCredentialsException e) {
					System.out
					.println("Invalid Username or Password! Please login Again!");
					System.exit(1);
				}
				try {
					Thread.sleep(WAITING_PERIOD);
				} catch (InterruptedException e) {
				}
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#pullFile(java.lang.String, long, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public byte[] pullFile(String path, long bytesRead, String username,
			String pw) throws RemoteException, InfoNotFoundException,
			InvalidCredentialsException {
		if (validateUser(username, pw)) {
			path = path.replaceAll("^(\\.|\\./|/)", "");
			return downloadFile(path, bytesRead);
		} else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#pushFile(java.lang.String, java.lang.String, byte[],
	 * long, long)
	 */
	@Override
	public void pushFile(String path, String name, byte[] fdata,
			long startingByte, long bytesWritten, long fileLength,
			String username, String pw) throws RemoteException,
			FileNotFoundException, IOException, InvalidCredentialsException {
		if (validateUser(username, pw))
			uploadFile(fdata, path, startingByte, bytesWritten, fileLength,
					username);
		else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#updateKnownServerList(java.util.List,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void updateKnownServerList(List<String> servers, String username,
			String pw) throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			knownServers.clear();
			for (String s : servers) {
				knownServers.add(s);
			}

			System.out.println("Server list Updated!\nTotal Servers known: "
					+ knownServers.size());
		} else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#getNextId(java.lang.String, java.lang.String)
	 */
	@Override
	public String getNextId(String username, String pw) throws RemoteException,
			InvalidCredentialsException {
		if (validateUser(username, pw))
			return baseId + "-" + nrIdsGenerated++;
		else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see trab1.both.IFileServer#receiveShutdownAnnouncement(java.lang.String)
	 */
	@Override
	public List<String> receiveShutdownAnnouncement(String name,
			String username, String pw) throws RemoteException,
			InvalidCredentialsException {
		if (validateUser(username, pw)) {
			String aux;
			knownServers.remove(name);
			Iterator<String> it = knownServers.iterator();
			Pair<String, Object> p;
			while (it.hasNext()) {
				aux = it.next();
				try {
					p = servCManager.getConnectionToServer(aux);
					if (p == null)
						continue;
					if (p.getFirst().equals(WS)) {
						trab1.both.ws.DirServerWS anotherServer = (trab1.both.ws.DirServerWS) p
								.getSecond();
						anotherServer
								.updateKnownServerList(new ArrayList<String>(
										knownServers));
					} else {
						IFileServer anotherServer = (IFileServer) p.getSecond();
						anotherServer.updateKnownServerList(
								new ArrayList<String>(knownServers), USERNAME,
								PASSWORD);
					}

				} catch (javax.xml.ws.WebServiceException e) {
					servHandler.treatServerException(aux, knownServers);
				} catch (RemoteException e) {
					servHandler.treatServerException(aux, knownServers);
				}
			}
			return new ArrayList<String>(knownServers);
		} else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#logout(java.lang.String, java.lang.String)
	 */
	@Override
	public void logout(String username, String pwd) throws RemoteException,
			InvalidCredentialsException {
		if (validateUser(username, pwd)) {
			users.remove(username);
			userDatabaseChanged();
		} else
			throw new InvalidCredentialsException();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#copyFileServerToServer(java.lang.String,
	 * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean copyFileServerToServer(String pathFrom, String serverTo,
			String pathTo, String username, String pw) throws RemoteException,
			InvalidCredentialsException {
		if (validateUser(username, pw)) {
			boolean result = false;
			if (this.url.equalsIgnoreCase(serverTo))
				result = copyFileDriveDrive(pathFrom, pathTo);
			else {
				result = copyFileToNonServiceServer(pathFrom, serverTo, pathTo);

			}
			return result;
		} else
			throw new InvalidCredentialsException();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#getUsers(java.lang.String, java.lang.String)
	 */
	@Override
	public Map<String, String> getUsers(String username, String pw)
			throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw))
			return users;
		else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#updateUserDataBase(java.util.Map, java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public void updateUserDataBase(Map<String, String> users, String username,
			String pw) throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			this.users = users;
			System.out.println("User Database Updated!");
		} else
			throw new InvalidCredentialsException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see tp2.IFileServer#registerClient(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean registerClient(String username, String pwd)
			throws RemoteException {
		String aux = users.get(username);
		if (aux != null)
			return false;
		else {
			users.put(username, pwd);
			userDatabaseChanged();
			return true;
		}
	}

	/**
	 * Initial setup
	 */
	private void init() {
		Scanner in = new Scanner(System.in);
		System.out.println("You need to get the authorization:");
		System.out.println(googleDriveService
				.getAuthorizationUrl(googleDriveToken));
		System.out
				.println("After allowing the GoogleDrive application insert the code");
		System.out.print(">>");
		Verifier googleDriveVerifier = new Verifier(in.nextLine());

		System.out.println();
		// Trade the Request Token and Verfier for the Access Token
		System.out.println("Trading the Request Token for an Access Token...");
		googleDriveAccessToken = googleDriveService.getAccessToken(
				googleDriveToken, googleDriveVerifier);
		System.out.println("Got the Access Token!");
		System.out.println();
	}

	/**
	 * Retrieve the users database
	 */
	private void getUserDataBase() {
		Pair<String, Object> p;

		for (String s : knownServers) {
			try {

				if (!s.equals(this.url)
						&& s.split(":")[0].equalsIgnoreCase("rmi")) {
					p = servCManager.getConnectionToServer(s);
					IFileServer anotherServer = (IFileServer) p.getSecond();
					users = anotherServer.getUsers(USERNAME, PASSWORD);
					break;
				}
			} catch (RemoteException e) {
				servHandler.treatServerException(s, knownServers);
			} catch (InvalidCredentialsException e) {
				System.out
				.println("Invalid Username or Password! Please login Again!");
				System.exit(1);
			}
		}
	}

	/**
	 * This method returns the Google Drive's interal id of a resource
	 * 
	 * @param path
	 *            path of the resource
	 * @return id of the resource
	 * @throws UnsupportedEncodingException
	 * @throws ParseException
	 */
	private String getResourceID(String path) {
		String[] splittedPath = null;
		OAuthRequest request;
		Response response;
		JSONParser parser = new JSONParser();
		JSONArray items;
		JSONObject res;
		String parentID = null;
		int i = 0;
		if (path.equals(".") || path.equals(""))
			path = "/";
		try {
			if (path.contains("/")) {
				splittedPath = path.split("/");
				if (splittedPath.length > 0) {
					request = new OAuthRequest(Verb.GET, DRIVE_FILES_URL
							+ "?q=%22root%22+in+parents+and+title%3D%22"
							+ URLEncoder.encode(splittedPath[i], "UTF-8")
							+ "%22");
				} else
					return "root";
			} else
				request = new OAuthRequest(Verb.GET, DRIVE_FILES_URL
						+ "?q=%22root%22+in+parents+and+title%3D%22"
						+ URLEncoder.encode(path, "UTF-8") + "%22");

			googleDriveService.signRequest(googleDriveAccessToken, request);
			response = request.send();

			if (response.getCode() == 200) {
				res = (JSONObject) parser.parse(response.getBody());
				items = (JSONArray) res.get("items");
				if (items.size() == 0) // if the dir is empty
					return null;
				parentID = (String) ((JSONObject) (items).get(0)).get("id");
				if (splittedPath != null) {
					for (i = 1; i < splittedPath.length; i++) {
						request = new OAuthRequest(Verb.GET, DRIVE_FILES_URL
								+ "?q=%22" + parentID
								+ "%22+in+parents+and+title%3D%22"
								+ URLEncoder.encode(splittedPath[i], "UTF-8")
								+ "%22");
						googleDriveService.signRequest(googleDriveAccessToken,
								request);
						response = request.send();
						if (response.getCode() == 200) {
							res = (JSONObject) parser.parse(response.getBody());
							items = (JSONArray) res.get("items");
							if (items.size() > 0)
								parentID = (String) ((JSONObject) (items)
										.get(0)).get("id");
							else
								parentID = null;
						}
					}
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return parentID;
	}

	/**
	 * Sends a file to a server which isnt a service
	 * 
	 * @param pathFrom
	 *            path of the file
	 * @param serverTo
	 *            detination server
	 * @param pathTo
	 *            path of the file on the remote server
	 * @return true if it was copied; false otherwise
	 */
	private boolean copyFileToNonServiceServer(String pathFrom,
			String serverTo, String pathTo) {
		servCManager.updateServerList(knownServers);
		Pair<String, Object> p = servCManager.getConnectionToServer(serverTo);
		if (p == null)
			return false;

		byte[] file = downloadFile(pathFrom, 0);
		if (file == null)
			return false;

		InputStream byteStream = new ByteArrayInputStream(file);
		BufferedInputStream in = new BufferedInputStream(byteStream);
		long i = file.length;
		long j = 0;

		byte[] data = new byte[CHUNK_SIZE];
		try {
			while (i > 0) {
				if (i < CHUNK_SIZE)
					in.read(data, 0, (int) i);
				else
					in.read(data, 0, CHUNK_SIZE);

				if (p.getFirst().equals(WS)) {
					trab1.both.ws.DirServerWS server = (trab1.both.ws.DirServerWS) p
							.getSecond();
					server.pushFile(pathTo, "", data, j, i);
				} else {
					IFileServer server = (IFileServer) p.getSecond();
					server.pushFile(pathTo, "", data, j, i, file.length, USERNAME, PASSWORD);
				}
				i -= CHUNK_SIZE;
				j += CHUNK_SIZE;
			}
			in.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IOException_Exception e) {
			e.printStackTrace();
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		}
		return false;
	}

	/**
	 * Copies a file from a path to another path in GoogleDrive
	 * 
	 * @param fromPath
	 *            origin path
	 * @param toPath
	 *            destination path
	 * @return true if it was copied; false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean copyFileDriveDrive(String fromPath, String toPath) {
		String title = "";
		String[] splittedPath = fromPath.split("/");
		title = splittedPath.length > 0 ? splittedPath[splittedPath.length - 1]
				: fromPath;

		String fromID = getResourceID(fromPath);
		String toID = getResourceID(toPath);
		String toPathTitle = toPath + "/" + title;
		String destParentID = "";
		boolean isDir = false;
		if (toID != null) {
			JSONObject jobj = getItemWithID(toID);
			String mimeType = (String) jobj.get("mimeType");
			isDir = mimeType.equals(DRIVE_FOLDER);
			JSONArray jarray = (JSONArray) jobj.get("parents");
			destParentID = (String) ((JSONObject) (jarray.get(0))).get("id");
		} else {
			if (!toPath.contains("/")) {
				isDir = true;
				toID = "root";
				title = toPath;
			}
		}
		removeContent(toPathTitle, false);
		if (fromID != null && toID != null) {
			OAuthRequest request = new OAuthRequest(Verb.POST, DRIVE_FILES_URL
					+ "/" + fromID + "/copy");
			request.addHeader("Content-type", REQUEST_CONTENT_TYPE);

			JSONObject bodyParameters = new JSONObject();
			JSONArray parents = new JSONArray();
			JSONObject val = new JSONObject();

			if (isDir) {
				bodyParameters.put("title", title);
				val.put("id", toID);

			} else {
				splittedPath = toPath.split("/");
				title = splittedPath.length > 0 ? splittedPath[splittedPath.length - 1]
						: fromPath;
				val.put("id", destParentID);
			}

			parents.add(val);
			bodyParameters.put("parents", parents);
			request.addPayload(bodyParameters.toJSONString());

			googleDriveService.signRequest(googleDriveAccessToken, request);

			Response response = request.send();
			if (response.getCode() == 200) {

				return true;
			}

		}
		return false;
	}

	/**
	 * This method removes a resource from the Google Drive using the resource
	 * path
	 * 
	 * @param path
	 *            path of the resource to remove
	 * @param isDir
	 *            if the file to remove is a directory
	 * @return true if the resource was removed; false otherwise
	 */
	private boolean removeContent(String path, boolean isDir) {

		String id = getResourceID(path);

		if (id == null)
			return false;

		JSONObject item = getItemWithID(id);
		if (isDir)
			if (!((String) item.get("mimeType")).equals(DRIVE_FOLDER))
				return false;

		return removeContent(id);
	}

	/**
	 * This method removes a resource from the Google Drive using the resource
	 * id
	 * 
	 * @param id
	 *            the id of the resource in Google Drive
	 * @return true if the resource was removed; false otherwise
	 */
	private boolean removeContent(String id) {

		if (id == null || id == "")
			return false;

		OAuthRequest request = new OAuthRequest(Verb.DELETE, DRIVE_FILES_URL
				+ "/" + id);
		googleDriveService.signRequest(googleDriveAccessToken, request);

		Response response = request.send();
		return (response.getCode() == 204);
	}

	/**
	 * This method returns a JSON item
	 * 
	 * @param id
	 *            the resource id in Google Drive
	 * @return a JSON item
	 */
	private JSONObject getItemWithID(String id) {
		OAuthRequest request = new OAuthRequest(Verb.GET, DRIVE_FILES_URL + "/"
				+ id);
		googleDriveService.signRequest(googleDriveAccessToken, request);
		Response response = request.send();
		JSONParser parser = new JSONParser();
		try {
			if (response.getCode() == 200)
				return (JSONObject) parser.parse(response.getBody());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * Uploads a chunk of a file to Google Drive
	 * 
	 * @param fdata
	 *            byte array
	 * @param path
	 *            destination path
	 * @param startingByte
	 *            initial byte
	 * @param bytesWritten
	 *            total bytes written
	 * @param fileLength
	 *            length of file
	 * @param username
	 * @return true if it was uploaded; false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean uploadFile(byte[] fdata, String path, long startingByte,
			long bytesWritten, long fileLength, String username) {

		String mimeType = URLConnection.guessContentTypeFromName(path);
		mimeType = mimeType == null ? DEFAULT_MIME_TYPE : mimeType;
		String parentId = "";
		String parentPath = "";
		OAuthRequest request;
		Response response;

		/*
		 * if (path.contains("/")) { String[] splittedPath = path.split("/");
		 * path = splittedPath[splittedPath.length - 1]; }
		 */
		try {
			if (startingByte == 0) {
				if (!usersSessions.containsKey(username)) {
					String[] splittedPath = path.split("/");
					if (splittedPath.length > 0) {
						for (int i = 0; i < splittedPath.length - 1; i++) {
							parentPath += splittedPath[i]
									+ (i + 1 < splittedPath.length - 1 ? "/"
											: "");
						}
						parentId = getResourceID(parentPath);
					} else
						parentId = "root";
					String id = getResourceID(path);
					if (id != null)
						removeContent(id);

					request = new OAuthRequest(Verb.POST, DRIVE_UPLOAD_FILE_URL);

					request.addHeader("Content-Type", REQUEST_CONTENT_TYPE);
					request.addHeader("X-Upload-Content-Type", mimeType);
					request.addHeader("X-Upload-Content-Length",
							String.valueOf(fileLength));

					splittedPath = path.split("/");
					String title = (splittedPath.length > 0 ? splittedPath[splittedPath.length - 1]
							: path);
					JSONObject jobj = new JSONObject();
					jobj.put("title", title);
					JSONArray jsonarr = new JSONArray();
					JSONObject parent = new JSONObject();
					parent.put("id", parentId);
					jsonarr.add(parent);
					jobj.put("parents", jsonarr);
					request.addHeader("Content-Length",
							String.valueOf(jobj.toJSONString().length()));
					request.addPayload(jobj.toJSONString());
					googleDriveService.signRequest(googleDriveAccessToken,
							request);
					response = request.send();
					if (response.getCode() == 200) {
						JSONParser parser = new JSONParser();
						JSONObject res;
						res = (JSONObject) parser.parse(new JSONObject(response
								.getHeaders()).toJSONString());
						String location = (String) res.get("Location");
						usersSessions.put(username, location);
					} else
						return false;
				}
			}
			String location = usersSessions.get(username);
			long endingByte = startingByte + fdata.length;

			request = new OAuthRequest(Verb.PUT, location);
			request.addHeader("Content-Length", String.valueOf(fdata.length));
			request.addHeader("Content-Type", mimeType);
			request.addHeader("Content-Range", "bytes " + startingByte + "-"
					+ (endingByte > 0 ? (endingByte - 1) : endingByte) + "/"
					+ fileLength);
			request.addPayload(fdata);
			googleDriveService.signRequest(googleDriveAccessToken, request);

			response = request.send();
			if (response.getCode() == 308) {
				return true;
			} else if (response.getCode() == 200) {
				usersSessions.remove(username);
				return true;
			} else
				return false;

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return false;

	}

	/**
	 * This method downloads a file from Google Drive to local filesystem
	 * 
	 * @param path
	 *            file's path in Google Drive
	 * @param bytesRead
	 *            bytes already read
	 * @return true if the file was downloaded; false otherwise
	 * @throws ParseException
	 * @throws IOException
	 */
	private byte[] downloadFile(String path, long bytesRead) {
		InputStream is;
		byte[] fileData = null;
		ByteBuffer target = null;

		String id = getResourceID(path);
		String downloadUrl = "";
		long fileSize = 0;
		byte[] b = null;
		if (id != null) {
			JSONObject jobj = getItemWithID(id);
			downloadUrl = (String) jobj.get("downloadUrl");
			fileSize = Long.valueOf((String) jobj.get("fileSize"));

			long i = bytesRead;
			int len = (int) i;
			OAuthRequest request;
			Response response;
			try {
				request = new OAuthRequest(Verb.GET, downloadUrl);
				if ((fileSize - len) < GOOGLE_DRIVE_CHUNK_SIZE) {
					len += (int) (fileSize - i);
					b = new byte[(int) (fileSize - i)];
					fileData = new byte[(int) (fileSize - i)];
					target = ByteBuffer.wrap(fileData);

					request.addHeader("Range", "bytes=" + i + "-" + (len - 1));
				} else {
					len += GOOGLE_DRIVE_CHUNK_SIZE;
					b = new byte[(int) GOOGLE_DRIVE_CHUNK_SIZE];
					fileData = new byte[(int) GOOGLE_DRIVE_CHUNK_SIZE];
					target = ByteBuffer.wrap(fileData);

					request.addHeader("Range", "bytes=" + i + "-" + (len - 1));
				}

				googleDriveService.signRequest(googleDriveAccessToken, request);

				response = request.send();
				int readByte = 0;
				is = response.getStream();
				while ((readByte = is.read(b)) != -1) {
					target.put(b, 0, readByte);
				}
				is.close();
				i = len;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return fileData;
	}

	/**
	 * Checks if a user is valid
	 * 
	 * @param username
	 * @param pw
	 * @return true if username is valid; false otherwise
	 */
	private boolean validateUser(String username, String pw) {
		boolean result = false;
		String aux = users.get(username);
		if (aux != null)
			result = aux.equals(pw);
		return result;
	}

	/**
	 * Sends the user database to other servers
	 */
	private void userDatabaseChanged() {
		String aux = null;

		Iterator<String> it = knownServers.iterator();
		Pair<String, Object> p;

		while (it.hasNext()) {
			try {
				aux = it.next();
				if (!aux.equals(this.url)) {
					p = servCManager.getConnectionToServer(aux);
					if (p == null)
						continue;
					if (p.getFirst().equals(RMI)) {
						IFileServer anotherServer = (IFileServer) p.getSecond();
						anotherServer.updateUserDataBase(users, USERNAME,
								PASSWORD);
					}
				}

			} catch (javax.xml.ws.WebServiceException e) {
				servHandler.treatServerException(aux, knownServers);
			} catch (RemoteException e) {
				servHandler.treatServerException(aux, knownServers);
			} catch (InvalidCredentialsException e) {
				System.out
				.println("Invalid Username or Password! Please login Again!");
				System.exit(1);
			}
		}

		System.out.println("User Database updated");
	}

	/**
	 * This method waits for client requests through multicast
	 * 
	 * @param serverName
	 *            name of the server
	 */
	private static void waitForClients(String serverName) {
		System.out.println("\nWaiting for client requests...");
		try {
			InetAddress multicastAddress = InetAddress
					.getByName(DEFAULT_MULTICAST_ADDRESS);
			MulticastSocket socket = new MulticastSocket(
					DEFAULT_MULTICAST_PORT_RECEIVE);
			socket.joinGroup(multicastAddress);

			for (;;) {

				byte[] buffer = new byte[BUFFER_SIZE];
				DatagramPacket packet = new DatagramPacket(buffer,
						buffer.length);
				socket.receive(packet);
				processPacket(socket, packet, serverName);
			}

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * This method processes a packet received through multicast
	 * 
	 * @param socket
	 *            multicast socket
	 * @param packet
	 *            packet to process
	 * @param name
	 *            name of the server
	 * @throws java.io.IOException
	 */
	private static void processPacket(MulticastSocket socket,
			DatagramPacket packet, String name) throws IOException {
		String message = new String(packet.getData(), 0, packet.getLength());
		Scanner in = new Scanner(message);
		String origin = in.nextLine();
		if (origin.equals(CLIENT_MESSAGE)) {
			System.out.println("Received message from client..");
			origin = in.nextLine();
			if (origin.equals(CLIENT_LOOKING_FOR_SERVER)) {
				message = SERVER_MESSAGE + "\n" + name + "\n";
				DatagramPacket responsePacket = new DatagramPacket(
						message.getBytes(), message.getBytes().length);
				responsePacket.setAddress(InetAddress
						.getByName(DEFAULT_MULTICAST_ADDRESS));
				responsePacket.setPort(DEFAULT_MULTICAST_PORT_SEND);
				socket.send(responsePacket);
				System.out.println("Response to Client Sent!");
			}
		}
		// else : Mensagem de um servidor. Ignorar.
		in.close();
	}

}