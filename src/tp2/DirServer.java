package tp2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Set;

import trab1.both.ws.IOException_Exception;
import trab1.both.ws.InfoNotFoundException_Exception;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The DirServer class represents a top level layer of DirServerRMI and DirServerWS
 * It is used by the FileClient class to contact a server without knowing which type of server it is
 */
public class DirServer implements IDirServer {

	private static String RMI = "rmi";
	private static String WS = "ws";

	private ServerConnectionManager servCManager;
	private ServerUnresponsiveHandler servHandler;
	private Set<String> serverList;
	private Pair<String, Object> srv;
	private IFileServer servRMI;
	private trab1.both.ws.DirServerWS servWS;
	private String username;
	private String pwd;
	private boolean registered;

	/**
	 * Constructs a DirServer
	 * @param servCManager the connection manager to be used
	 * @param servHandler the server connection manager to be used
	 * @param serverList the server list to be used
	 */
	public DirServer(ServerConnectionManager servCManager,
			ServerUnresponsiveHandler servHandler, Set<String> serverList,String username,
			String pwd) {
		this.registered = false;
		this.username = username;
		this.pwd = pwd;
		this.servCManager = servCManager;
		this.servHandler = servHandler;
		this.serverList = serverList;
		this.servRMI = null;
		this.servWS = null;
		this.srv = null;
	}


	/* (non-Javadoc)
	 * @see tp2.IDirServer#getServer(java.lang.String, java.util.Set)
	 */
	public Pair<String, Object> getServer(String url,Set<String> serverList) {
		this.serverList = serverList;
		servCManager.updateServerList(serverList);
		return servCManager.getConnectionToServer(url);
	}


	/* (non-Javadoc)
	 * @see tp2.IDirServer#dir(java.lang.String, java.lang.String)
	 */
	@Override
	public String[] dir(String path, String url) throws InfoNotFoundException,
	InvalidCredentialsException  , UsernameAlreadyTaken{
		srv = servCManager.getConnectionToServer(url);
		try {
			if (srv.getFirst().equals(WS)) {
				servWS = (trab1.both.ws.DirServerWS) srv.getSecond();
				List<String> fl = servWS.dir(path);
				String[] fileList = new String[fl.size()];
				servWS.dir(path).toArray(fileList);
				return fileList;
			} else if (srv.getFirst().equals(RMI)) {
				if(!registered)
					registerIfPossible(serverList);
				servRMI = (IFileServer) srv.getSecond();
				return servRMI.dir(path,username,pwd);
			}
		} catch (InfoNotFoundException_Exception e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			servHandler.treatServerException(url, serverList);
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IDirServer#getFileInfo(java.lang.String, java.lang.String)
	 */
	@Override
	public FileInfo getFileInfo(String path, String url)
			throws InfoNotFoundException, InvalidCredentialsException , UsernameAlreadyTaken {
		srv = servCManager.getConnectionToServer(url);
		FileInfo info = null;

		try {
			if (srv.getFirst().equals(WS)) {
				servWS = (trab1.both.ws.DirServerWS) srv.getSecond();
				trab1.both.ws.FileInfo infoWS = servWS.getFileInfo(path);
				if (infoWS != null)

					info = new FileInfo(infoWS.getName(), infoWS.getLength(),
							infoWS.getModified(), infoWS.isIsFile());
				return info;
			} else if (srv.getFirst().equals(RMI)) {
				if(!registered)
					registerIfPossible(serverList);
				servRMI = (IFileServer) srv.getSecond();
				return servRMI.getFileInfo(path,username,pwd);
			}
		} catch (InfoNotFoundException_Exception e) {
			e.printStackTrace();
		} catch (javax.xml.ws.WebServiceException e) {
			servHandler.treatServerException(url, serverList);
		} catch (RemoteException e) {
			servHandler.treatServerException(url, serverList);
		}
		return null;
	}


	/* (non-Javadoc)
	 * @see tp2.IDirServer#pullFile(java.lang.String, long, java.lang.String)
	 */
	@Override
	public byte[] pullFile(String path, long bytesRead, String url)
			throws InfoNotFoundException, InvalidCredentialsException , UsernameAlreadyTaken {
		srv = servCManager.getConnectionToServer(url);
		byte[] data = null;
		try {
			if (srv.getFirst().equals(WS)) {
				servWS = (trab1.both.ws.DirServerWS) srv.getSecond();
				data = servWS.pullFile(path, bytesRead);
				return data;
			} else if (srv.getFirst().equals(RMI)) {
				if(!registered)
					registerIfPossible(serverList);
				servRMI = (IFileServer) srv.getSecond();
				data = servRMI.pullFile(path, bytesRead,username,pwd);
				return data;
			}
		} catch (javax.xml.ws.WebServiceException e) {
			servHandler.treatServerException(url, serverList);
		} catch (RemoteException e) {
			servHandler.treatServerException(url, serverList);
			e.printStackTrace();
		} catch (InfoNotFoundException_Exception e) {
			e.printStackTrace();
		}
		return null;
	}


	/* (non-Javadoc)
	 * @see tp2.IDirServer#pushFile(java.lang.String, java.lang.String, byte[], long, long, long, java.lang.String)
	 */
	@Override
	public void pushFile(String path, String name, byte[] fdata,
			long startingByte, long bytesWritten, long fileLength, String url)
					throws FileNotFoundException, IOException, InvalidCredentialsException , UsernameAlreadyTaken {
		srv = servCManager.getConnectionToServer(url);
		try {
			if (srv.getFirst().equals(WS)) {
				servWS = (trab1.both.ws.DirServerWS) srv.getSecond();
				servWS.pushFile(path, name, fdata, startingByte, bytesWritten);
			} else if (srv.getFirst().equals(RMI)) {
				if(!registered)
					registerIfPossible(serverList);
				servRMI = (IFileServer) srv.getSecond();
				servRMI.pushFile(path, name, fdata, startingByte, bytesWritten, fileLength, username,pwd);
			}
		} catch (javax.xml.ws.WebServiceException e) {
			servHandler.treatServerException(url, serverList);
		} catch (RemoteException e) {
			servHandler.treatServerException(url, serverList);
		} catch (MalformedURLException e) {
			System.out.println("Invalid URL: " + url);
		} catch (java.io.IOException e) {
			;
		} catch (IOException_Exception e) {
			e.printStackTrace();
		}

	}


	/* (non-Javadoc)
	 * @see tp2.IDirServer#removeFile(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean removeFile(String path, String url) throws InvalidCredentialsException , UsernameAlreadyTaken {
		srv = servCManager.getConnectionToServer(url);
		boolean removed = false;
		try {
			if (srv.getFirst().equals(WS)) {
				servWS = (trab1.both.ws.DirServerWS) srv.getSecond();
				removed = servWS.removeFile(path);
				return removed;
			} else if (srv.getFirst().equals(RMI)) {
				if(!registered)
					registerIfPossible(serverList);
				servRMI = (IFileServer) srv.getSecond();
				removed = servRMI.removeFile(path,username,pwd);
				return removed;
			}
		} catch (javax.xml.ws.WebServiceException e) {
			servHandler.treatServerException(url, serverList);
		} catch (RemoteException e) {
			servHandler.treatServerException(url, serverList);
		}
		return removed;
	}


	/* (non-Javadoc)
	 * @see tp2.IDirServer#removeDirectory(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean removeDirectory(String path, String url) 
			throws InvalidCredentialsException  , UsernameAlreadyTaken{
		srv = servCManager.getConnectionToServer(url);
		boolean removed = false;
		try {
			if (srv.getFirst().equals(WS)) {
				servWS = (trab1.both.ws.DirServerWS) srv.getSecond();
				removed = servWS.removeDirectory(path);
				return removed;
			} else if (srv.getFirst().equals(RMI)) {
				if(!registered)
					registerIfPossible(serverList);
				servRMI = (IFileServer) srv.getSecond();
				removed = servRMI.removeDirectory(path,username,pwd);
				return removed;
			}
		} catch (javax.xml.ws.WebServiceException e) {
			servHandler.treatServerException(url, serverList);
		} catch (RemoteException e) {
			servHandler.treatServerException(url, serverList);
		}
		return removed;
	}


	/* (non-Javadoc)
	 * @see tp2.IDirServer#createDir(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean createDir(String path, String url) throws InfoNotFoundException, 
	InvalidCredentialsException , UsernameAlreadyTaken {
		srv = servCManager.getConnectionToServer(url);
		boolean created = false;
		try {
			if (srv.getFirst().equals(WS)) {
				servWS = (trab1.both.ws.DirServerWS) srv.getSecond();
				created = servWS.createDir(path);
				return created;
			} else if (srv.getFirst().equals(RMI)) {
				if(!registered)
					registerIfPossible(serverList);
				servRMI = (IFileServer) srv.getSecond();
				created = servRMI.createDir(path,username,pwd);
				return created;
			}
		} catch (javax.xml.ws.WebServiceException e) {
			servHandler.treatServerException(url, serverList);
		} catch (RemoteException e) {
			servHandler.treatServerException(url, serverList);
		} catch (InfoNotFoundException_Exception e) {
			e.printStackTrace();
		}
		return created;
	}


	/* (non-Javadoc)
	 * @see tp2.IDirServer#getServerList(java.lang.String)
	 */
	@Override
	public List<String> getServerList(String url) throws InvalidCredentialsException , UsernameAlreadyTaken {

		srv = servCManager.getConnectionToServer(url);
		if(srv == null)
			return null;
		try {
			if (srv.getFirst().equals(WS)) {
				servWS = (trab1.both.ws.DirServerWS) srv.getSecond();
				return servWS.getServerList();
			} else if (srv.getFirst().equals(RMI)) {
				if(!registered)
					registerIfPossible(serverList);
				servRMI = (IFileServer) srv.getSecond();
				return servRMI.getServerList();
			}
		} catch (javax.xml.ws.WebServiceException e) {
			servHandler.treatServerException(url, serverList);
		} catch (RemoteException e) {
			servHandler.treatServerException(url, serverList);
		} 
		return null;
	}

	/* (non-Javadoc)
	 * @see tp2.IDirServer#getServerListFromRandomServer()
	 */
	@Override
	public List<String> getServerListFromRandomServer() throws InvalidCredentialsException , UsernameAlreadyTaken {
		List<String> slist;
		for(String s: serverList){
			slist = getServerList(s);
			if(slist != null)
				return slist;
		}
		return null;
	}

	
	/* (non-Javadoc)
	 * @see tp2.IDirServer#copyFileServerToServer(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean copyFileServerToServer(String pathFrom, String serverTo,
			String pathTo, String url) throws InvalidCredentialsException , UsernameAlreadyTaken{
		srv = servCManager.getConnectionToServer(url);
		boolean success = false;
		try {
			if (srv.getFirst().equals(WS)) {
				servWS = (trab1.both.ws.DirServerWS) srv.getSecond();
				success = servWS.copyFileServerToServer(pathFrom, serverTo, pathTo);
				return success;
			} else if (srv.getFirst().equals(RMI)) {
				if(!registered)
					registerIfPossible(serverList);
				servRMI = (IFileServer) srv.getSecond();
				success = servRMI.copyFileServerToServer(pathFrom, serverTo, pathTo,username,pwd);
				return success;
			}
		} catch (javax.xml.ws.WebServiceException e) {
			servHandler.treatServerException(url, serverList);
		} catch (RemoteException e) {
			servHandler.treatServerException(url, serverList);
		} 
		return false;
	}

	/* (non-Javadoc)
	 * @see tp2.IDirServer#registerClient(java.util.Set)
	 */
	@Override
	public int registerClient(Set<String> serverList) {
		String url = null;
		Pair<String, Object> p;
		for( String s: serverList){
			try {
				if(s.split(":")[0].equalsIgnoreCase("rmi")){
					url = s;
					p = servCManager.getConnectionToServer(url);
					IFileServer anotherServer = (IFileServer) p.getSecond();
					if(anotherServer.registerClient(username,pwd)){
						registered = true;
						return 0;
					}
					else
						return FileClient.USERNAME_ALREADY_TAKEN;
				}
			} catch (RemoteException e) {
				servHandler.treatServerException(s, serverList);
			}
		}

		return FileClient.NO_RMI_SERVERS_AVAILABLE;
	}

	/* (non-Javadoc)
	 * @see tp2.IDirServer#logout(java.util.Set)
	 */
	@Override
	public void logout(Set<String> serverList) throws InvalidCredentialsException {
		String url = null;
		Pair<String, Object> p;
		for( String s: serverList){
			try {
				if(s.split(":")[0].equalsIgnoreCase("rmi")){
					url = s;
					p = servCManager.getConnectionToServer(url);
					IFileServer anotherServer = (IFileServer) p.getSecond();
					anotherServer.logout(username,pwd);
					return;
				}
			} catch (RemoteException e) {
				servHandler.treatServerException(s, serverList);
			}
		}

	}

	/* (non-Javadoc)
	 * @see tp2.IDirServer#isRegistered()
	 */
	@Override
	public boolean isRegistered() {
		return registered;
	}

	/**
	 * Attempts to register a user in a RMI server.
	 * @param serverList - list of servers known
	 * @throws UsernameAlreadyTaken
	 */
	private void registerIfPossible(Set<String> serverList) throws UsernameAlreadyTaken{
		if(serverList != null){
			int result = registerClient(serverList);
			if(result == FileClient.USERNAME_ALREADY_TAKEN)
				throw new UsernameAlreadyTaken();
		}
	}


}
