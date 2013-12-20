package tp2;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
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
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.DropBoxApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import trab1.both.ws.IOException_Exception;


/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The DirServerDropBoxRMI class implements a proxy to the online service: DropBox
 * 
 * 
 */
public class DirServerDropBoxRMI extends UnicastRemoteObject implements IFileServer {

	private static final String USERNAME ="server";
	private static final String PASSWORD ="server";
	private static final long serialVersionUID = 1L;
	private static final int DOWNLOAD_CHUNK = 4000000;
	private static final String DROPBOX_KEY = "h509057ehl85phl";
	private static final String DROPBOX_SECRET = "g52w9d5pajxxosj";
	private static final String DROPBOX_SCOPE = "dropbox";
	private static final String DROPBOX_PROTECTED_RESOURCE_URL = "https://api.dropbox.com/1/metadata/dropbox/";
	private static final String DROPBOX_FILEOPS_CREATE_FOLDER = "https://api.dropbox.com/1/fileops/create_folder/?root=dropbox&path=";
	private static final String DROPBOX_FILEOPS_REMOVE_FILE_OR_FOLDER = "https://api.dropbox.com/1/fileops/delete/?root=dropbox&path=";
	private static final String DROPBOX_FILE_FOLDER_METADATA = "https://api.dropbox.com/1/metadata/dropbox/";
	private static final String DROPBOX_FILE_DOWNLOAD = "https://api-content.dropbox.com/1/files/dropbox/";
	private static final String DROPBOX_CHUNKED_UPLOAD = "https://api-content.dropbox.com/1/chunked_upload?";
	private static final String DROBPOX_COMMIT_CHUNKED_UPLOAD = "https://api-content.dropbox.com/1/commit_chunked_upload/dropox/";
	private static final String DROPBOX_COPY_DROP_DROP = "https://api.dropbox.com/1/fileops/copy/?root=dropbox&";

	private static OAuthService dropboxService;	
	private static Token dropboxToken;
	private static Token dropboxAccessToken;


	protected static final int CHUNK_SIZE =  524288;

	@SuppressWarnings("unused")
	private File basePath;
	private String id;
	private String url;
	private String community;
	private int nrIdsGenerated;
	private String baseId;

	private static final int DEFAULT_ATTEMPTS = 3;
	public static final String DEFAULT_MULTICAST_ADDRESS = "224.0.1.0";
	public static int DEFAULT_MULTICAST_PORT_RECEIVE = 9000;
	public static int DEFAULT_MULTICAST_PORT_SEND = 9001;
	private static String SERVERTYPE = "dropbox";
	private static final int BUFFER_SIZE = 65536;
	public static final String CLIENT_LOOKING_FOR_SERVER = "Looking For a Server in local network.";
	private static final String SERVER_MESSAGE = "SRV";
	private static final String CLIENT_MESSAGE = "CLT";
	private static final String WS = "ws";
	private static final String RMI = "rmi";
	private static final int WAITING_PERIOD = 5000;
	private static final String BASE_PATH = ".";

	protected Set<String> knownServers;
	protected Map<String,String> users;
	private Map<String, String> usersSessions;
	private ServerUnresponsiveHandler servHandler;
	private ServerConnectionManager servCManager;

	public static enum SERVICE { GOOGLE_DRIVE, DROPBOX };

	public DirServerDropBoxRMI(String community, String id, String ip,
			String basePath) throws Exception {

		super(0,new RMISSLClientSocketFactory(),new RMISSLServerSocketFactory());

		this.basePath = new File(basePath);
		this.knownServers = new HashSet<String>();
		this.community = community;
		this.nrIdsGenerated = 1;
		this.baseId = id;
		this.id = id;
		this.url = "rmi://" + ip + "/" + community + "-"+ SERVERTYPE+"-" + id;
		this.servHandler = new ServerUnresponsiveHandler(false,USERNAME,PASSWORD);
		servCManager = new ServerConnectionManager(servHandler, knownServers);
		this.users = new HashMap<String,String>();
		this.users.put(USERNAME, PASSWORD);
		this.usersSessions = new HashMap<String, String>();
		init();
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServer#getUrl(java.lang.String, java.lang.String)
	 */
	@Override
	public String getUrl(String username,String pw) throws RemoteException{
		return this.url;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServer#getName(java.lang.String, java.lang.String)
	 */
	@Override
	public String getName(String username,String pw) throws RemoteException{
		return "/" + community + "-" + id;
	}


	/* (non-Javadoc)
	 * @see tp2.IFileServer#getCommunity(java.lang.String, java.lang.String)
	 */
	@Override
	public String getCommunity(String username,String pw) throws RemoteException{
		return this.community;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServer#getServerList()
	 */
	@Override
	public List<String> getServerList() throws RemoteException{
		return new ArrayList<String>(knownServers);
	}


	/* (non-Javadoc)
	 * @see tp2.IFileServer#dir(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String[] dir(String path,String username,String pw) throws RemoteException,
	InfoNotFoundException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			path = path.replaceAll("^(\\.|\\./|/)", "");
			List<String> dir = new LinkedList<String>();
			int index = path.indexOf(".");
			String query = "" + DROPBOX_PROTECTED_RESOURCE_URL;
			if(index != -1)
				query += path.substring(index+1);
			else
				query+= path;
			OAuthRequest request = new OAuthRequest(Verb.GET, query);
			dropboxService.signRequest(dropboxAccessToken, request);

			Response response = request.send();

			String[] splittedPath;
			
			if (response.getCode() == 200) {
				JSONParser parser = new JSONParser();
				JSONObject res;
				try {
					res = (JSONObject) parser.parse(response.getBody());

					JSONArray items = (JSONArray) res.get("contents");
					Iterator<?> it = items.iterator();
					while (it.hasNext()) {
						
						JSONObject file = (JSONObject) it.next();
						splittedPath = ((String)file.get("path")).split("/");
						dir.add(splittedPath[splittedPath.length-1]);;
					}
				} catch (ParseException e) {
					System.out.println("Parse Error!");
				}
			}
			System.out.println(dir.size());
			String[] result = new String[dir.size()];
			dir.toArray(result);

			return result;
		}
		else
			throw new InvalidCredentialsException();
	}


	/* (non-Javadoc)
	 * @see tp2.IFileServer#getFileInfo(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public FileInfo getFileInfo(String path,String username,String pw) throws RemoteException,
	InfoNotFoundException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			path = path.replaceAll("^(\\.|\\./|/)", "");
			String result = null;
			result = "" + DROPBOX_FILE_FOLDER_METADATA +URLParamEncoder.encode(path);
			OAuthRequest request = null;
			request = new OAuthRequest(Verb.POST,result);

			dropboxService.signRequest(dropboxAccessToken, request);
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss Z",Locale.US);
			Response response = request.send();
			JSONObject res = null;
			if (response.getCode() == 200){
				try {
					JSONParser parser = new JSONParser();
					res = (JSONObject) parser.parse(response.getBody());
					Boolean deleted = (Boolean)res.get("is_deleted");
					if (deleted != null) {
						if (deleted)
							return null;
					}

					Date lastModifiedDate = simpleDateFormat.parse(((String) res.get("modified")).split(",")[1].trim());

					FileInfo file = new FileInfo((String)res.get("path"), (Long)res.get("bytes"),
							lastModifiedDate,
							!(Boolean)res.get("is_dir"));

					return file;
				} catch (ParseException e) {
					System.out.println("Parse Error!");
				} catch (java.text.ParseException e) {
					System.out.println("erro parsing");
					System.out.println((String)res.get("modified"));
				}
			}
			else if(response.getCode() == 404){
				System.out.println("Error : File or Folder \"" + path + "\" not found!" );
			}
			else if(response.getCode() == 406){
				System.out.println("Too many files would be involved in the operation for it to complete successfully." +
						" The limit is currently 10,000 files and folders." );
			}
			return null;
		}
		else
			throw new InvalidCredentialsException();


	}


	/*@Override
	public byte[] pullFile(String path, long bytesRead) throws RemoteException,
	InfoNotFoundException {

		File file = new File(basePath, path);
		System.out.println(file.getAbsolutePath());
		System.out.println("exists: "+file.exists());
		if(!file.exists()){
			if(!downloadFile(path))
				return null;
			else
				fileCache.incrementFileUsers(file.getAbsolutePath());
		}
		else {
			if(bytesRead == 0)
				fileCache.incrementFileUsers(file.getAbsolutePath());
		}
		System.out.println(fileCache.isAvailable(file.getAbsolutePath()));
		try {
			byte[] data = new byte[CHUNK_SIZE];
			FileInputStream fstream = new FileInputStream(basePath + "/" + path);
			BufferedInputStream in = new BufferedInputStream(fstream);
			in.skip(bytesRead);
			int result = in.read(data, 0, CHUNK_SIZE);
			in.close();
			System.out.println("bytesRead: "+ bytesRead);
			System.out.println("read: "+result);
			if(result == -1 || file.length() == (result+bytesRead))
				if(fileCache.decrementFileUsers(file.getAbsolutePath())){
					System.out.println(file.getAbsolutePath());
					if(file.delete())
						System.out.println("File Sucessfuly deleted.");
				}


			return data;
		} catch (Exception e) {
			;
		}

		return null;
	}*/

	
	
	/* (non-Javadoc)
	 * @see tp2.IFileServer#pullFile(java.lang.String, long, java.lang.String, java.lang.String)
	 */
	@Override
	public byte[] pullFile(String path, long bytesRead,String username,String pw) throws RemoteException,
	InfoNotFoundException , InvalidCredentialsException{
		if (validateUser(username, pw)){
			path = path.replaceAll("^(\\.|\\./|/)", "");
			return downloadFile(path,bytesRead);
		}
		else
			throw new InvalidCredentialsException();

	}

	/* (non-Javadoc)
	 * @see tp2.IFileServer#pushFile(java.lang.String, java.lang.String, byte[], long, long, long, java.lang.String, java.lang.String)
	 */
	@Override
	public void pushFile(String path, String name, byte[] fdata,
			long startingByte, long bytesWritten,long fileLength,String username,String pw) throws RemoteException,
			FileNotFoundException, IOException , InvalidCredentialsException{

		if (validateUser(username, pw)){
			path = path.replaceAll("^(\\.|\\./|/)", "");
			uploadFile(fdata,path,startingByte,bytesWritten,fileLength,username);
		}
		else
			throw new InvalidCredentialsException();

	}

	/* (non-Javadoc)
	 * @see tp2.IFileServer#copyFileServerToServer(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean copyFileServerToServer(String pathFrom, String serverTo,
			String pathTo,String username,String pw) throws RemoteException, InvalidCredentialsException {
		boolean result = false;
		if (validateUser(username, pw)) {

			if(this.url.equalsIgnoreCase(serverTo))
				result = copyFileDropDrop(pathFrom, pathTo);
			else {
				result = copyFileToNonServiceServer(pathFrom,serverTo,pathTo);

			}
			return result;
		}
		else
			throw new InvalidCredentialsException();
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServer#removeFile(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean removeFile(String path,String username,String pw) throws RemoteException, InvalidCredentialsException  {
		if (validateUser(username, pw)) {
			try {
				return rmDropBox(path);

			} catch (UnsupportedEncodingException e) {
				//ignore
			} catch (ParseException e) {
				System.out.println("Parse Error!");
			}
			return false;
		}
		else
			throw new InvalidCredentialsException();
	}

	/* (non-Javadoc)
	 * @see tp2.IFileServer#removeDirectory(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean removeDirectory(String path,String username,String pw) throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			try {
				return rmDropBox(path);

			} catch (UnsupportedEncodingException e) {
				//UnsuportedEncoding
			} catch (ParseException e) {
				System.out.println("Parse Error!");
			}
			return false;
		}
		else
			throw new InvalidCredentialsException();
	}

	/* (non-Javadoc)
	 * @see tp2.IFileServer#createDir(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean createDir(String path,String username,String pw) throws RemoteException,
	InfoNotFoundException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			OAuthRequest request = new OAuthRequest(Verb.POST, DROPBOX_FILEOPS_CREATE_FOLDER + path);
			dropboxService.signRequest(dropboxAccessToken, request);

			Response response = request.send();
			if (response.getCode() == 200)
				return true;

			
			return false;
		}
		else
			throw new InvalidCredentialsException();
	}


	/* (non-Javadoc)
	 * @see tp2.IFileServer#getUsers(java.lang.String, java.lang.String)
	 */
	@Override
	public Map<String,String> getUsers(String username,String pw)throws RemoteException,
	InvalidCredentialsException {
		if(validateUser(username,pw))
			return users;
		else
			throw new InvalidCredentialsException();
	}


	/* (non-Javadoc)
	 * @see tp2.IFileServer#updateUserDataBase(java.util.Map, java.lang.String, java.lang.String)
	 */
	@Override
	public void updateUserDataBase(Map<String, String> users,String username,String pw)
			throws RemoteException, InvalidCredentialsException {
		if(validateUser(username,pw)){
			this.users = users;
			System.out.println("User Database Updated!");
			System.out.println(users);
		}
		else
			throw new InvalidCredentialsException();
	}



	/* (non-Javadoc)
	 * @see tp2.IFileServer#newServerConnected(java.lang.String)
	 */
	@Override
	public void newServerConnected(String url) throws RemoteException,
	NotBoundException{
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
							anotherServer
							.updateKnownServerList(new ArrayList<String>(
									knownServers),USERNAME,PASSWORD);
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

	/* (non-Javadoc)
	 * @see tp2.IFileServer#registerClient(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean registerClient(String username, String pwd)
			throws RemoteException {
		String aux = users.get(username);
		if(aux != null)
			return false;
		else {
			users.put(username, pwd);
			userDatabaseChanged();
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see tp2.IFileServer#logout(java.lang.String, java.lang.String)
	 */
	@Override
	public void logout(String username, String pwd) throws RemoteException, InvalidCredentialsException{
		if (validateUser(username, pwd)) {
			users.remove(username);
			userDatabaseChanged();
		}
		else throw new InvalidCredentialsException();

	}


	/* (non-Javadoc)
	 * @see tp2.IFileServer#updateKnownServerList(java.util.List, java.lang.String, java.lang.String)
	 */
	@Override
	public void updateKnownServerList(List<String> servers,String username,String pw)
			throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			knownServers.clear();
			for (String s : servers){
				knownServers.add(s);
			}
			System.out.println("Server list Updated!\nTotal Servers known: "+knownServers.size());
		}
		else throw new InvalidCredentialsException();
	}


	/* (non-Javadoc)
	 * @see tp2.IFileServer#getNextId(java.lang.String, java.lang.String)
	 */
	@Override
	public String getNextId(String username,String pw) throws RemoteException, InvalidCredentialsException {
		return baseId + "-" + nrIdsGenerated++;
	}


	/* (non-Javadoc)
	 * @see tp2.IFileServer#receiveShutdownAnnouncement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public List<String> receiveShutdownAnnouncement(String name,String username,String pw)
			throws RemoteException, InvalidCredentialsException {
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
					anotherServer.updateKnownServerList(new ArrayList<String>(
							knownServers));
				} else {
					IFileServer anotherServer = (IFileServer) p.getSecond();
					anotherServer.updateKnownServerList(new ArrayList<String>(
							knownServers),USERNAME,PASSWORD);
				}
			} catch (javax.xml.ws.WebServiceException e) {
				servHandler.treatServerException(aux, knownServers);
			} catch (RemoteException e) {
				servHandler.treatServerException(aux, knownServers);
			}
		}
		return new ArrayList<String>(knownServers);
	}



	public static void main(String[] args) throws ParseException, UnsupportedEncodingException {
		try {

			String anotherServerUrl = "";
			String community = "";
			String ipAddress = "";
			String basePath = "";
			final DirServerDropBoxRMI server;

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
			System.out.println("ip: "+ipAddress);
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
			System.setProperty("javax.net.ssl.keyStore","server.ks");
			System.setProperty("javax.net.ssl.keyStorePassword","123456");

			System.setProperty("javax.net.ssl.trustStore", "cacerts");
			System.setProperty("javax.net.ssl.trustStorePassword", "changeit");


			if (anotherServerUrl != "") {
				System.out
				.println("SERVER STARTUP: Contacting community server with url: "
						+ anotherServerUrl + " ...");
				ServerConnectionManager servCManager = new ServerConnectionManager(
						new ServerUnresponsiveHandler(false,USERNAME,PASSWORD),
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
					id = anotherServerRMI.getNextId(USERNAME,PASSWORD);
				}
				System.out.println("SERVER STARTUP: Connection Estabilished!");


				server = new DirServerDropBoxRMI(community, id, ipAddress, basePath);
				Naming.rebind(server.getUrl(USERNAME,PASSWORD), server);
				if (p2.getFirst().equals(WS))
					anotherServerWS.newServerConnected(server.getUrl(USERNAME,PASSWORD));
				else {
					System.out
					.println("antes: " + server.url);
					anotherServerRMI.newServerConnected(server.getUrl(USERNAME,PASSWORD));
					server.getUserDataBase();
					System.out
					.println("depois: " + server.url);
				}
			} else {
				server = new DirServerDropBoxRMI(community, "1", ipAddress, basePath);
				Naming.rebind(server.getUrl(USERNAME,PASSWORD), server);

				server.knownServers.add(server.getUrl(USERNAME,PASSWORD));
			}

			System.out.println("SERVER STARTUP: DirServer bound in registry");
			System.out
			.println("SERVER STARTUP: Server URL: " + server.url);

			System.out.println("SERVER IP: " + ipAddress);
			System.out.println("BASE WORKING PATH: " + basePath);



			/*Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					server.announceShutdown();
				}
			});*/

			waitForClients(server.getUrl(USERNAME,PASSWORD));



		}
		catch (Exception e) {
			e.printStackTrace();
			System.out.println("It was not possible to launch the server. Please confirm your parameters.");
		}

	}

	/**
	 * Authenticates the proxy in the Dropbox Service.
	 */
	private void init() {
		dropboxService = new ServiceBuilder()
		.provider(DropBoxApi.class)
		.apiKey(DROPBOX_KEY)
		.apiSecret(DROPBOX_SECRET)
		.scope(DROPBOX_SCOPE)
		.build();

		dropboxToken = dropboxService.getRequestToken();
		Scanner in = new Scanner(System.in);
		System.out.println("You need to get the authorization:");
		System.out.println(dropboxService.getAuthorizationUrl(dropboxToken));
		System.out.println("After allowing the Dropbox application press ENTER");
		System.out.print(">>"); 
		Verifier dropboxVerifier = new Verifier(in.nextLine());
		System.out.println();
		System.out.println("Trading the Request Token for an Access Token...");
		dropboxAccessToken = dropboxService.getAccessToken(dropboxToken, dropboxVerifier);
		System.out.println("Got the Access Token!");
		System.out.println();
	}

	/**
	 * Copies a file from the remote directory to a non-online directory.
	 * @param pathFrom - origin
	 * @param serverTo - destination server ip
	 * @param pathTo -  destination
	 * @return true in case of success or false otherwise
	 */
	private boolean copyFileToNonServiceServer(String pathFrom,
			String serverTo, String pathTo) {

		servCManager.updateServerList(knownServers);
		Pair<String, Object> p = servCManager
				.getConnectionToServer(serverTo);
		if (p == null)
			return false;

		byte[] file = downloadWholeFile(pathFrom);
		if(file == null) return false;

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
					server.pushFile(pathTo, "", data, j, i,file.length, USERNAME, PASSWORD);
				}
				i -= CHUNK_SIZE;
				j += CHUNK_SIZE;
			}
			in.close();
			return true;
		} catch (IOException e) {
			System.out.println("IO error!");
		} catch (IOException_Exception e) {
			//
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		}
		return false;
	}

	/**
	 * Checks if the given username and password identify a valid user
	 * @param username - username
	 * @param pw - password
	 * @return true if it's a valid user , false otherwise
	 */
	private boolean validateUser(String username, String pw) {
		boolean result = false;
		String aux = users.get(username);
		if(aux != null)
			result = aux.equals(pw);
		return result;
	}
	
	/**
	 * When new user registers in the community the user's list is sent
	 * to the other servers.
	 */
	private void userDatabaseChanged(){
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
						IFileServer anotherServer = (IFileServer) p
								.getSecond();
						anotherServer.updateUserDataBase(users, USERNAME, PASSWORD);
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
		System.out.println(users);
	}
	
	/**
	 * Removes a file or folder from the remote directory
	 * @param path - path of the file/folder 
	 * @return true in case of success or false otherwise
	 * @throws ParseException
	 * @throws UnsupportedEncodingException
	 */
	private boolean rmDropBox(String path) throws ParseException, UnsupportedEncodingException {

		OAuthRequest request = new OAuthRequest(Verb.POST, DROPBOX_FILEOPS_REMOVE_FILE_OR_FOLDER + path);
		dropboxService.signRequest(dropboxAccessToken, request);

		Response response = request.send();
		if (response.getCode() == 200){
			return true;
		}
		else if(response.getCode() == 404){
			System.out.println("Error : File or Folder \"" + path + "\" not found!" );
		}
		else if(response.getCode() == 406){
			System.out.println("Too many files would be involved in the operation for it to complete successfully." +
					" The limit is currently 10,000 files and folders." );
		}
		return false;
	}


	/*private boolean downloadFile(String path){
		String result = "" + DROPBOX_FILE_DOWNLOAD + path;
		FileInfo info;
		long bytesLeft = 0;
		try {
			info = getFileInfo(path);
			bytesLeft = info.length;
		} catch (RemoteException e1) {
			///treat error
		} catch (InfoNotFoundException e) {
			//treat error
		}
		long bytesDownloaded = 0;
		int bytesToGet = 0;
		while(bytesLeft > 0){
			OAuthRequest request = new OAuthRequest(Verb.GET,result);
			dropboxService.signRequest(dropboxAccessToken, request);
			bytesToGet = (int) (bytesLeft >= DOWNLOAD_CHUNK ? DOWNLOAD_CHUNK : bytesLeft);
			request.addHeader("range", "bytes="+ bytesDownloaded+"-"+ (bytesDownloaded +bytesToGet));

			Response response = request.send();
			if (response.getCode() == 200 || response.getCode() == 206){
				bytesLeft -= (bytesToGet+1);
				bytesDownloaded += bytesToGet + 1;
				try {
					InputStream is = response.getStream();
					String[] splt = path.split("/");
					File file = new File(basePath,splt[splt.length-1]);
					FileOutputStream out;

					out = new FileOutputStream(file, file.exists()
							&& bytesDownloaded > 0);

					int numRead;
					byte[] data = new byte[65536];
					while ( (numRead = is.read(data) ) >= 0)
						out.write(data, 0, numRead);

					out.close();
					is.close();

				}catch (IOException e) {
					e.printStackTrace();
				}
			}
			else {
				System.out.println("Error downloading file!");
				return false;
			}
		}
		return true;
	}*/

	/**
	 * Downloads a file from the remote directory.
	 * @param path - path of the file
	 * @return the file data in a byte array or null otherwise
	 */
	private byte[] downloadWholeFile(String path){
		String result = "" + DROPBOX_FILE_DOWNLOAD + path;
		FileInfo info;
		long bytesLeft = 0;
		byte[] fileData = null;
		ByteBuffer target = null;
		try {
			info = getFileInfo(path,USERNAME,PASSWORD);

			bytesLeft = info.length;
			fileData = new byte[(int) bytesLeft];
			target = ByteBuffer.wrap(fileData);
		} catch (RemoteException e1) {
			///treat error
		} catch (InfoNotFoundException e) {
			System.out.println("File Not found!");
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		}
		long bytesDownloaded = 0;
		int bytesToGet = 0;
		while(bytesLeft > 0){
			OAuthRequest request = new OAuthRequest(Verb.GET,result);
			dropboxService.signRequest(dropboxAccessToken, request);
			bytesToGet = (int) (bytesLeft >= DOWNLOAD_CHUNK ? DOWNLOAD_CHUNK : bytesLeft);
			request.addHeader("range", "bytes="+ bytesDownloaded+"-"+ (bytesDownloaded +bytesToGet));

			Response response = request.send();
			if (response.getCode() == 200 || response.getCode() == 206){

				try {
					InputStream is = response.getStream();

					int numRead;
					byte[] data = new byte[65536];
					while ( (numRead = is.read(data) ) >= 0){
						target.put(data,0,numRead);
					}
					is.close();
					bytesLeft -= (bytesToGet+1);
					bytesDownloaded += bytesToGet + 1;

				}catch (IOException e) {
					System.out.println("IO Error!");
				}catch (Exception e){
					//
				}
			}
			else {
				System.out.println("Error downloading file!");
				return null;
			}
		}
		return fileData;
	}

	/**
	 * Downloads a block of a file in the remote directory
	 * @param path - path of the file
	 * @param bytesRead - total bytes read till the moment
	 * @return a block of data in a byte array or null in case of failure
	 */
	private byte[] downloadFile(String path,long bytesRead){
		String result = "" + DROPBOX_FILE_DOWNLOAD + path;
		FileInfo info;
		long bytesLeft = 0;
		byte[] fileData = null;
		ByteBuffer target = null;
		try {
			info = getFileInfo(path,USERNAME,PASSWORD);

			bytesLeft = info.length - bytesRead;
			fileData = new byte[CHUNK_SIZE];
			target = ByteBuffer.wrap(fileData);

		} catch (RemoteException e1) {
			return null;
		} catch (InfoNotFoundException e) {
			System.out.println("File Not found!");
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		}
		System.out.println("passou");


		int bytesToGet = (int) (bytesLeft % CHUNK_SIZE);

		OAuthRequest request = new OAuthRequest(Verb.GET,result);
		dropboxService.signRequest(dropboxAccessToken, request);
		bytesToGet = (int) (bytesLeft >= CHUNK_SIZE ? CHUNK_SIZE : bytesLeft);
		request.addHeader("range", "bytes="+ (bytesRead)+"-"+ (bytesRead +bytesToGet-1));

		Response response = request.send();
		if (response.getCode() == 200 || response.getCode() == 206){

			try {
				InputStream is = response.getStream();

				int numRead;
				byte[] data = new byte[CHUNK_SIZE];
				while ( (numRead = is.read(data) ) > 0){
					target.put(data,0,numRead);
				}
				is.close();
				bytesLeft -= (bytesToGet+1);

			}catch (IOException e) {
				System.out.println("IO error!");
			}catch (Exception e){
				//
			}
		}
		else {
			System.out.println("Error downloading file!");
			return null;
		}

		return fileData;
	}


	/*private boolean uploadFile(String path) {
		String result = "" + DROPBOX_CHUNKED_UPLOAD + "offset=0";

		long bytesLeft = 0;
		long bytesUploaded = 0;
		int bytesToUpload = 0;
		String uploadId = "";
		JSONParser parser = new JSONParser();
		JSONObject res = null;

		try {

			RandomAccessFile f = new RandomAccessFile(path, "r");
			bytesLeft = f.length();

			while(bytesLeft > 0){
				bytesToUpload = (int) (bytesLeft >= UPLOAD_CHUNK ? UPLOAD_CHUNK : bytesLeft);
				byte[] chunk = new byte[bytesToUpload];
				f.read(chunk,0, bytesToUpload);
				if(!uploadId.equals(""))
					result = "" + DROPBOX_CHUNKED_UPLOAD + "upload_id=" + uploadId+"&offset=" +bytesUploaded ;

				OAuthRequest request = new OAuthRequest(Verb.PUT,result);
				dropboxService.signRequest(dropboxAccessToken, request);
				request.addHeader("Content-Type","");
				request.addPayload(chunk);
				Response response = request.send();

				if (response.getCode() == 200 ){

					res = (JSONObject) parser.parse(response.getBody());
					bytesLeft -= bytesToUpload;

					if(uploadId.equals("")){
						uploadId = (String) res.get("upload_id");	
					}
					bytesUploaded = (Long) res.get("offset");
				}
				else {
					System.out.println("Error downloading file!");
					f.close();
					return false;
				}
			}
			f.close();
			//Commit upload
			result = "" + DROBPOX_COMMIT_CHUNKED_UPLOAD+path+"/?upload_id="+uploadId;
			OAuthRequest request = new OAuthRequest(Verb.POST,result);
			dropboxService.signRequest(dropboxAccessToken, request);
			Response response = request.send();

			if (response.getCode() == 200 ){
				System.out.println("Sucess");
				res = (JSONObject) parser.parse(response.getBody());
				return true;
			}
			else{
				System.out.println("Error on commit");
				System.out.println(response.getBody());
			}


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}*/

	/*private boolean uploadWholeFile(byte[] fdata, String path,String username) {
		String result = "" + DROPBOX_CHUNKED_UPLOAD + "offset=0";

		long bytesLeft = 0;
		long bytesUploaded = 0;
		int bytesToUpload = 0;
		String uploadId = "";
		JSONParser parser = new JSONParser();
		JSONObject res = null;

		try {
			bytesLeft = fdata.length;
			InputStream is = new ByteArrayInputStream(fdata);

			while(bytesLeft > 0){
				bytesToUpload = (int) (bytesLeft >= UPLOAD_CHUNK ? UPLOAD_CHUNK : bytesLeft);
				byte[] chunk = new byte[bytesToUpload];
				is.read(chunk);

				if(!uploadId.equals(""))
					result = "" + DROPBOX_CHUNKED_UPLOAD + "upload_id=" + uploadId+"&offset=" +bytesUploaded ;
				System.out.println(result);
				OAuthRequest request = new OAuthRequest(Verb.PUT,result);
				dropboxService.signRequest(dropboxAccessToken, request);
				request.addHeader("Content-Type","");
				request.addPayload(chunk);
				Response response = request.send();

				if (response.getCode() == 200 ){

					res = (JSONObject) parser.parse(response.getBody());
					bytesLeft -= bytesToUpload;

					if(uploadId.equals("")){
						uploadId = (String) res.get("upload_id");
					}
					bytesUploaded = (Long) res.get("offset");
				}
				else {
					System.out.println("Error downloading file!");
					is.close();
					return false;
				}
			}
			is.close();

			result = "" + DROBPOX_COMMIT_CHUNKED_UPLOAD+path+"/?upload_id="+uploadId;
			OAuthRequest request = new OAuthRequest(Verb.POST,result);
			dropboxService.signRequest(dropboxAccessToken, request);
			Response response = request.send();

			if (response.getCode() == 200 ){
				System.out.println("Sucess");
				res = (JSONObject) parser.parse(response.getBody());
				return true;
			}
			else{
				System.out.println("Error on commit");
				System.out.println(response.getBody());
			}


		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}*/

	/**
	 * Uploads a block of a file to the remote directory
	 * When all the blocks were sent, a commit message is sent.
	 * @param fdata - a block of data
	 * @param path - file path destination
	 * @param startingByte - number of the byte fdata[0] according to the total file length
	 * @param bytesWritten - total bytes writenn till the moment
	 * @param fileLength - total file length
	 * @param username - username of the user 
	 * @return true in case of success or false otherwise
	 */
	private boolean uploadFile(byte[] fdata, String path,long startingByte, long bytesWritten, long fileLength, String username) {
		String result = DROPBOX_CHUNKED_UPLOAD + "offset="+ startingByte;
		long bytesUploaded = 0;
		JSONParser parser = new JSONParser();
		JSONObject res = null;

		try {
			InputStream is = new ByteArrayInputStream(fdata);
			if(usersSessions.containsKey(username))
				result = "" + DROPBOX_CHUNKED_UPLOAD + "upload_id=" + usersSessions.get(username)+
				"&offset=" +startingByte ;
			OAuthRequest request = new OAuthRequest(Verb.PUT,result);
			dropboxService.signRequest(dropboxAccessToken, request);
			request.addHeader("Content-Type","");
			request.addPayload(fdata);
			Response response = request.send();

			if (response.getCode() == 200 ){

				res = (JSONObject) parser.parse(response.getBody());


				if(!usersSessions.containsKey(username)){
					usersSessions.put(username, (String) res.get("upload_id"));
				}
				bytesUploaded = (Long) res.get("offset");
			}
			else {
				System.out.println("Error downloading file!");
				is.close();
				return false;
			}

			is.close();

			if(bytesUploaded == fileLength){

				result = "" + DROBPOX_COMMIT_CHUNKED_UPLOAD+path+"/?upload_id="+usersSessions.get(username);
				request = new OAuthRequest(Verb.POST,result);
				dropboxService.signRequest(dropboxAccessToken, request);
				response = request.send();

				if (response.getCode() == 200 ){
					System.out.println("Sucess");
					usersSessions.remove(username);
					return true;
				}
				else{
					System.out.println("Error on commit");
					System.out.println(response.getBody());
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("File not found!");
		} catch (IOException e) {
			System.out.println("IO error!");
		} catch (ParseException e) {
			System.out.println("Parse Error!");
		}
		return false;

	}

	/**
	 * Copies a file from a path in the Remote directory to another
	 * path in the remote directory
	 * @param fromPath - origin path
	 * @param toPath - destination path
	 * @return true in sucess or false otherwise
	 */
	private boolean copyFileDropDrop(String fromPath, String toPath){
		String result = "" + DROPBOX_COPY_DROP_DROP + 
				"from_path="+ fromPath + "&to_path="+toPath;
		OAuthRequest request = new OAuthRequest(Verb.POST,result);
		dropboxService.signRequest(dropboxAccessToken, request);

		Response response = request.send();
		if (response.getCode() == 200){

			return true;
		}
		else if(response.getCode() == 403){
			System.out.println("Error : An invalid copy operation was attempted " +
					"(e.g. there is already a file at the given destination, or trying to copy a shared folder)." );
		}
		else if(response.getCode() == 404){
			System.out.println("The source file wasn't found at the specified path.");
		}else if(response.getCode() == 406){
			System.out.println("Too many files would be involved in the operation for it to complete " +
					"successfully. The limit is currently 10,000 files and folders.");
		}
		System.out.println(response.getBody());
		return false;
	}


	/**
	 * Asks for the user database from an available server.
	 */
	private void getUserDataBase() {
		Pair<String, Object> p;

		for( String s: knownServers){
			try {

				if(!s.equals(this.url) && s.split(":")[0].equalsIgnoreCase("rmi")){
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
	 * This method announces to all servers that this server will shutdown
	 */
	protected void announceShutdown() {
		boolean done = false;
		String aux = null;
		Pair<String, Object> p;
		if(knownServers.size() == 1){
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
						anotherServer
						.receiveShutdownAnnouncement(this.getUrl(USERNAME,PASSWORD));

					} else {
						System.out.println("RMI SERVER!");
						IFileServer anotherServer = (IFileServer) p.getSecond();
						anotherServer
						.receiveShutdownAnnouncement(this.getUrl(USERNAME,PASSWORD),USERNAME, PASSWORD);
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

	/**
	 * This method waits for client requests through multicast
	 * @param serverName name of the server
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
	 * @param socket multicast socket
	 * @param packet packet to process
	 * @param name name of the server
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