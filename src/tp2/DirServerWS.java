package tp2;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.*;
import java.io.*;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import trab1.both.ws.*;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The DirServerWS class represents a DirServer implemented in WebServices
 */
@WebService
public class DirServerWS implements IFileServerCommon {
	public DirServerWS() {
		super();
	}

	protected static final int CHUNK_SIZE = 32768;

	private File basePath;
	private String id;
	private String url;
	private String community;
	private int nrIdsGenerated;
	private String baseId;

	private static final String USERNAME ="server";
	private static final String PASSWORD ="server";
	private static final int DEFAULT_ATTEMPTS = 3;
	private static final int WAITING_PERIOD = 5000;
	public static final String DEFAULT_MULTICAST_ADDRESS = "224.0.1.0";
	private static final int DEFAULT_PORT =8080;
	public static int DEFAULT_MULTICAST_PORT_RECEIVE = 9000;
	public static int DEFAULT_MULTICAST_PORT_SEND = 9001;
	private static final int BUFFER_SIZE = 65536;
	public static final String CLIENT_LOOKING_FOR_SERVER = "Looking For a Server in local network.";
	private static final String SERVER_MESSAGE = "SRV";
	private static final String CLIENT_MESSAGE = "CLT";
	private static final String WS = "ws";
	private static final String BASE_PATH = ".";

	private Set<String> knownServers;
	private ServerUnresponsiveHandler servHandler;
	private ServerConnectionManager servCManager;

	/**
	 * Constructs a DirServerWS
	 * @param community community of servers to connect
	 * @param id id of the server
	 * @param ip ip in which the server will be host
	 * @param port port in which the server will be bind
	 * @param basePath base working directory
	 */
	public DirServerWS(String community, String id, String ip,int port, String basePath) {
		super();
		this.basePath = new File(basePath);
		this.knownServers = new HashSet<String>();
		this.community = community;
		this.nrIdsGenerated = 1;
		this.baseId = id;
		this.id = id;
		this.url = "http://" +ip+":"+port+"/" + community + "-" + id;
		this.servHandler = new ServerUnresponsiveHandler(false,null,null);
		servCManager = new ServerConnectionManager(servHandler, knownServers);
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#getUrl()
	 */
	@WebMethod
	@Override
	public String getUrl() {
		return this.url;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#getName()
	 */
	@WebMethod
	@Override
	public String getName() {
		return "/" + community + "-" + id;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#getCommunity()
	 */
	@WebMethod
	@Override
	public String getCommunity() {
		return this.community;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#getServerList()
	 */
	@WebMethod
	@Override
	public List<String> getServerList() {
		return new ArrayList<String>(knownServers);
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#dir(java.lang.String)
	 */
	@WebMethod
	@Override
	public String[] dir(String path) throws InfoNotFoundException {
		File f = new File(basePath, path);
		if (f.exists())
			return f.list();
		else
			return null;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#createDir(java.lang.String)
	 */
	@WebMethod
	@Override
	public boolean createDir(String path) throws InfoNotFoundException {
		File f = new File(basePath, path);
		if (f.exists())
			return false;
		else
			return f.mkdir();
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#getFileInfo(java.lang.String)
	 */
	@WebMethod
	@Override
	public FileInfo getFileInfo(String path) throws InfoNotFoundException {
		File f = new File(basePath, path);
		if (f.exists()) {
			return new FileInfo(f.getName(), f.length(), new Date(f
					.lastModified()), f.isFile());
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#pullFile(java.lang.String, long)
	 */
	@WebMethod
	@Override
	public byte[] pullFile(String path, long bytesRead)
			throws InfoNotFoundException {
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
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#pushFile(java.lang.String, java.lang.String, byte[], long, long)
	 */
	@WebMethod
	@Override
	public void pushFile(String path, String name, byte[] fdata,
			long startingByte, long bytesToWrite) throws java.io.IOException {
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

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#copyFileServerToServer(java.lang.String, java.lang.String, java.lang.String)
	 */
	@WebMethod
	@Override
	public boolean copyFileServerToServer(String pathFrom, String serverTo,
			String pathTo) {
		try {

			Pair<String, Object> p = servCManager
					.getConnectionToServer(serverTo);
			if (p == null)
				return false;
			File dir = new File(basePath, pathFrom);

			if (dir.exists()) {
				byte[] data = new byte[CHUNK_SIZE];
				FileInputStream fstream = new FileInputStream(basePath + "/" + pathFrom);
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
						server.pushFile(pathTo, "", data, j, i, dir.length(), USERNAME,PASSWORD);
					}
					i -= CHUNK_SIZE;
					j += CHUNK_SIZE;
				}
				in.close();
				return true;
			}

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
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		}
		return false;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#removeFile(java.lang.String)
	 */
	@WebMethod
	@Override
	public boolean removeFile(String path) {
		File f = new File(basePath, path);
		if (f.exists())
			return f.delete();
		return false;
	}

	/**
	 * This method recursively removes a directory i.e. it removes all the containing files and directories
	 * @param dir directory to remove
	 */
	private void recursiveFilesRemove(File dir) {
		int dirLength = dir.isDirectory() ? dir.list().length : 0;
		if (dir.exists()) {
			if (dir.isFile()) {
				dir.delete();
			} else if (dirLength == 0)
				dir.delete();
			else {
				for (String fileName : dir.list()) {
					File f = new File(dir.getAbsolutePath() + "/" + fileName);
					recursiveFilesRemove(f);
				}
				dir.delete();
			}
		}
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#removeDirectory(java.lang.String)
	 */
	@WebMethod
	@Override
	public boolean removeDirectory(String path) {
		File dir = new File(basePath, path);
		if (dir.exists())
			recursiveFilesRemove(dir);
		else
			return false;
		return true;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#updateKnownServerList(java.util.List)
	 */
	@WebMethod
	@Override
	public void updateKnownServerList(List<String> list) {
		knownServers.clear();
		for (String s : list)
			knownServers.add(s);
		System.out.println("Server list Updated!\nTotal Servers known: "+knownServers.size());
	}

	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#newServerConnected(java.lang.String)
	 */
	@WebMethod
	@Override
	public void newServerConnected(String url) {
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
	 * @see tp2.IFileServerCommon#getNextId()
	 */
	@WebMethod
	public String getNextId() {
		return baseId + "-" + nrIdsGenerated++;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IFileServerCommon#receiveShutdownAnnouncement(java.lang.String)
	 */
	@WebMethod
	@Override
	public List<String> receiveShutdownAnnouncement(String name) {
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
			} catch (InvalidCredentialsException e) {
				System.out
				.println("Invalid Username or Password! Please login Again!");
				System.exit(1);
			}
		}
		return new ArrayList<String>(knownServers);
	}

	/**
	 * Main method of DirServerWS
	 * @param args
	 * @throws Exception
	 */
	@WebMethod(exclude = true)
	public static void main(String args[]) throws Exception {
		try {

			String anotherServerUrl = "";
			String community;
			String ipAddress;
			String basePath = "";
			int port;
			final DirServerWS server;

			Pattern p = Pattern
					.compile("([0-9]{1,3}[.]){3}[0-9]{1,3}");
			if (args.length >= 2 || args.length == 7) {
				
				if(args[0].equals("-p") && args[2].equals("-bp")){
					port = Integer.parseInt(args[1]);
					basePath = args[3];
					community = args[4];
					anotherServerUrl = args.length == 7 ? args[5] : "";
					ipAddress = args.length == 7 ? args[6] : args[5];
				}
				else if (args[0].equals("-p")) {
					port = Integer.parseInt(args[1]);
					basePath = BASE_PATH;
					community = args[2];
					anotherServerUrl = args.length == 5 ? args[3] : "";
					ipAddress = args.length == 5 ? args[4] : args[3];
				}
				else if (args[0].equals("-bp")) {
					port = DEFAULT_PORT;
					basePath = args[1];
					community = args[2];
					anotherServerUrl = args.length == 5 ? args[3] : "";
					ipAddress = args.length == 5 ? args[4] : args[3];
				}
				else{
					community = args[0];
					anotherServerUrl = args.length == 3 ? args[1] : "";
					ipAddress = args.length == 3 ? args[2] : args[1];
					port= DEFAULT_PORT;
					basePath = BASE_PATH;
				}
				
				  Matcher m = p.matcher(ipAddress); 
				  if (!m.matches()) {
				  System.out.println("Invalid IP address!"); 
				  return ; 
				  }
			} else {
				System.out
						.println("Use: java DirServerWS [-p Port] [-bp BasePath] community [URL] serverIPAddress");
				return;
			}

			System.getProperties().put("java.security.policy", "policy.all");
			System.setProperty("javax.net.ssl.trustStore", "cacerts");
			System.setProperty("javax.net.ssl.trustStorePassword", "changeit");

			if (anotherServerUrl != "") {
				System.out
						.println("SERVER STARTUP: Contacting comunity server with url: "
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

				server = new DirServerWS(community, id, ipAddress,port, basePath);
				Endpoint.publish(server.getUrl(), server);
				if (p2.getFirst().equals(WS))
					anotherServerWS.newServerConnected(server.getUrl());
				else
					anotherServerRMI.newServerConnected(server.getUrl());

			} else {
				server = new DirServerWS(community, "1", ipAddress,port, basePath);
				Endpoint.publish(server.getUrl(), server);

				server.knownServers.add(server.getUrl());
			}

			System.out.println("SERVER STARTUP: DirServer bound in registry");
			System.out
					.println("SERVER STARTUP: Server URL: " + server.getUrl());

			System.out.println("SERVER IP: " + ipAddress);
			System.out.println("BASE WORKING PATH: " + basePath);
			
			/*Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					server.announceShutdown();
				}
			});*/
			
			waitForClients(server.getUrl());

		}
		catch (Exception e) {
			System.out.println("It was not possible to launch the server. Please confirm your parameters.");
		}
	}

	/**
	 * This method announces to all servers that this server will shutdown
	 */
	@WebMethod(exclude = true)
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
					p = servCManager.getConnectionToServer(aux);
					if (p == null)
						continue;
					if (p.getFirst().equals(WS)) {
						trab1.both.ws.DirServerWS anotherServer = (trab1.both.ws.DirServerWS) p
								.getSecond();
						anotherServer
								.receiveShutdownAnnouncement(this.getUrl());

					} else {
						IFileServer anotherServer = (IFileServer) p.getSecond();
						anotherServer
								.receiveShutdownAnnouncement(this.getUrl(),USERNAME,PASSWORD);
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
	@WebMethod(exclude = true)
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
		} catch (java.io.IOException e) {
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
	@WebMethod(exclude = true)
	private static void processPacket(MulticastSocket socket,
			DatagramPacket packet, String name) throws java.io.IOException {
		String message = new String(packet.getData(), 0, packet.getLength());
		Scanner in = new Scanner(message);
		String origin = in.nextLine();
		if (origin.equals(CLIENT_MESSAGE)) {
			System.out.println("Received message from client..");
			origin = in.nextLine();
			if (origin.equals(CLIENT_LOOKING_FOR_SERVER)) {
				message = SERVER_MESSAGE + "\n" + name + "\n";
				DatagramPacket responsePacket = new DatagramPacket(message
						.getBytes(), message.getBytes().length);
				responsePacket.setAddress(InetAddress
						.getByName(DEFAULT_MULTICAST_ADDRESS));
				responsePacket.setPort(DEFAULT_MULTICAST_PORT_SEND);
				socket.send(responsePacket);
				System.out.println("Response to Client Sent!");
			}
		}
		// else : Message from server. Ignore
		in.close();
	}
}
