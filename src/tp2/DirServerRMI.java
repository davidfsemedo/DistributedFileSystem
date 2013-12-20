package tp2;




import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;

import trab1.both.ws.IOException_Exception;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The DirServerRMI class represents a DirServer implemented in RMI
 */
public class DirServerRMI extends UnicastRemoteObject implements IFileServer {

	/**
	 * Constructs a DirServerRMI
	 * @throws RemoteException
	 */

	private static final long serialVersionUID = 1L;
	protected static final int CHUNK_SIZE = 524288;

	private File basePath;
	private String id;
	private String url;
	private String community;
	private int nrIdsGenerated;
	private String baseId;

	private static final String USERNAME ="server";
	private static final String PASSWORD ="server";
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

	protected Set<String> knownServers;
	protected Map<String,String> users;
	private ServerUnresponsiveHandler servHandler;
	private ServerConnectionManager servCManager;

	/**
	 * Constructs a DirServerRMI
	 * @param community community of servers to connect
	 * @param id id of the server
	 * @param ip ip in which the server will be host
	 * @param port port in which the server will be bind
	 * @param basePath base working directory
	 * @throws Exception 
	 */
	protected DirServerRMI(String community, String id, String ip,
			String basePath) throws Exception {
		super(0,new RMISSLClientSocketFactory(),new RMISSLServerSocketFactory());
		

		this.basePath = new File(basePath);
		this.knownServers = new HashSet<String>();

		this.community = community;
		this.nrIdsGenerated = 1;
		this.baseId = id;
		this.id = id;
		this.url = "rmi://" + ip + "/" + community + "-" + id;
		this.servHandler = new ServerUnresponsiveHandler(false,USERNAME,PASSWORD);
		servCManager = new ServerConnectionManager(servHandler, knownServers);
		this.users = new HashMap<String,String>();
		this.users.put(USERNAME, PASSWORD);
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
			File f = new File(basePath, path);
			System.out.println(f.getAbsolutePath());
			if (f.exists())
				return f.list();
			else
				return null;
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
			File f = new File(basePath, path);
			if (f.exists())
				return false;
			else
				return f.mkdir();
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
			File f = new File(basePath, path);
			if (f.exists()) {
				return new FileInfo(f.getName(), f.length(), new Date(
						f.lastModified()), f.isFile());
			}
			return null;
		}
		else
			throw new InvalidCredentialsException();
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServer#pullFile(java.lang.String, long, java.lang.String, java.lang.String)
	 */
	@Override
	public byte[] pullFile(String path, long bytesRead,String username,String pw) throws RemoteException,
	InfoNotFoundException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			File dir = new File(basePath, path);
			if (dir.exists()) {
				try {
					byte[] data = new byte[CHUNK_SIZE];
					FileInputStream fstream = new FileInputStream(basePath + "/" + path);
					BufferedInputStream in = new BufferedInputStream(fstream);
					in.skip(bytesRead);
					in.read(data, 0, CHUNK_SIZE);
					in.close();
					return data;
				} catch (Exception e) {
					return null;
				}
			}
			return null;
		}
		else
			throw new InvalidCredentialsException();
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServer#pushFile(java.lang.String, java.lang.String, byte[], long, long, long, java.lang.String, java.lang.String)
	 */
	@Override
	public void pushFile(String path, String name, byte[] fdata,
			long startingByte, long bytesToWrite, long fileLength, String username,String pw) throws IOException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			File f = new File(basePath, path + "/" + name);

			FileOutputStream fileout = new FileOutputStream(f, f.exists()
					&& startingByte > 0);

			BufferedOutputStream out = new BufferedOutputStream(fileout);
			if (bytesToWrite < CHUNK_SIZE)
				out.write(fdata, 0, (int) bytesToWrite);
			else
				out.write(fdata, 0, CHUNK_SIZE);
			out.close();
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
		try {
			if (validateUser(username, pw)) {
				servCManager.updateServerList(knownServers);
				Pair<String, Object> p = servCManager
						.getConnectionToServer(serverTo);
				System.out.println(p == null);
				if (p == null)
					return false;
				File dir = new File(basePath,pathFrom);


				System.out.println(dir.exists());
				if (dir.exists()) {
					System.out.println("aqui");
					if(servCManager.isService(serverTo))
						sendFileInOnce(pathFrom,dir,serverTo,pathTo,p);
					else {
						byte[] data = new byte[CHUNK_SIZE];
						FileInputStream fstream = new FileInputStream(pathFrom);
						BufferedInputStream in = new BufferedInputStream(fstream);
						long i = dir.length();
						long j = 0;
						while (i >= 0) {
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
								server.pushFile(pathTo, "", data, j, i, dir.length(), USERNAME, PASSWORD);
							}
							i -= CHUNK_SIZE;
							j += CHUNK_SIZE;
						}
						in.close();
					}
					return true;
				}
			}
			else throw new InvalidCredentialsException();

		} catch (MalformedURLException e) {
			return false;
		} catch (javax.xml.ws.WebServiceException e) {
			servHandler.treatServerException(serverTo, knownServers);
		} catch (RemoteException e) {
			servHandler.treatServerException(serverTo, knownServers);
		} catch (java.io.IOException e) {
			return false;
		} catch (IOException_Exception e) {
			return false;
		}
		return false;
	}


	private void sendFileInOnce(String pathFrom, File dir, String serverTo, String pathTo, Pair<String, Object> p) {
		byte[] data = new byte[(int) dir.length()];
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(dir.getAbsolutePath());

			BufferedInputStream in = new BufferedInputStream(fstream);
			System.out.println("Uploading file " + dir.getAbsolutePath() + ". \nPlease wait... ");
			in.read(data, 0, (int)dir.length());
			IFileServer server = (IFileServer) p.getSecond();
			server.pushFile(pathTo, "", data, 0, dir.length(), dir.length(), USERNAME, PASSWORD);
			in.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		}
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServer#removeFile(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean removeFile(String path,String username,String pw) throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			File f = new File(basePath, path);
			if (f.exists())
				return f.delete();
			return false;
		}
		else throw new InvalidCredentialsException();
	}

	/**
	 * This method recursively removes a directory i.e. it removes all the containing files and directories
	 * @param dir directory to remove
	 */
	private void recursiveFilesRemove(File dir) {
		int dirLength = dir.isDirectory() ? dir.list().length : 0;
		if (dir.exists()) {
			if (dir.isFile()) 
				dir.delete();
			else if (dirLength == 0)
				dir.delete();
			else {
				for (String fileName : dir.list()) {
					File f = new File(dir.getAbsolutePath(), fileName);
					recursiveFilesRemove(f);
				}
				dir.delete();
			}
		}
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServer#removeDirectory(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean removeDirectory(String path,String username,String pw) throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw)) {
			File dir = new File(basePath, path);
			if (dir.exists())
				recursiveFilesRemove(dir);
			else
				return false;
			return true;
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
	 * @see tp2.IFileServer#getUsers(java.lang.String, java.lang.String)
	 */
	@Override
	public Map<String,String> getUsers(String username,String pw) throws InvalidCredentialsException{
		if(validateUser(username,pw))
			return users;
		else throw new InvalidCredentialsException();
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
	 * @see tp2.IFileServer#getNextId(java.lang.String, java.lang.String)
	 */
	@Override
	public String getNextId(String username,String pw) throws RemoteException, InvalidCredentialsException {
		if (validateUser(username, pw))
			return baseId + "-" + nrIdsGenerated++;
		else throw new InvalidCredentialsException();
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServer#receiveShutdownAnnouncement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public List<String> receiveShutdownAnnouncement(String name,String username,String pw)
			throws RemoteException, InvalidCredentialsException {
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
		else throw new InvalidCredentialsException();
		
	}

	/**
	 * Main method
	 * @param args
	 */
	public static void main(String args[]){
		try {

			String anotherServerUrl = "";
			String community = "";
			String ipAddress = "";
			String basePath = "";
			final DirServerRMI server;

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

			Matcher m = p.matcher(ipAddress);
			if (!m.matches()) {
				System.out.println("Invalid IP address!");
				return;
			}

			System.out.println(basePath);
			System.getProperties().put("java.security.policy", "policy.all");



			if (System.getSecurityManager() == null) {
				System.setSecurityManager(new RMISecurityManager());
			}



			System.setProperty("javax.net.ssl.keyStore","server.ks");
			System.setProperty("javax.net.ssl.keyStorePassword","123456");

			System.setProperty("javax.net.ssl.trustStore", "cacerts");
			System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

			try {
				LocateRegistry.createRegistry(1099);
			} catch (RemoteException e) {
				// if not start it
				// do nothing - already started with rmiregistry
			}
			
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


				server = new DirServerRMI(community, id, ipAddress, basePath);
				Naming.rebind(server.getUrl(USERNAME,PASSWORD), server);
				if (p2.getFirst().equals(WS))
					anotherServerWS.newServerConnected(server.getUrl(USERNAME,PASSWORD));
				else {
					anotherServerRMI.newServerConnected(server.getUrl(USERNAME,PASSWORD));
					server.getUserDataBase();
				}
			} else {
				server = new DirServerRMI(community, "1", ipAddress, basePath);
				Naming.rebind(server.getUrl(USERNAME,PASSWORD), server);

				server.knownServers.add(server.getUrl(USERNAME,PASSWORD));
			}



			System.out.println("SERVER STARTUP: DirServer bound in registry");
			System.out
			.println("SERVER STARTUP: Server URL: " + server.getUrl(USERNAME,PASSWORD));

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
					//do nothing
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
