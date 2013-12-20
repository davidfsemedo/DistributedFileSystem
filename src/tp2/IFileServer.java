package tp2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.*;
import java.util.List;
import java.util.Map;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The IFileServer interface represents an interface for the RMI FileServer 
 */
public interface IFileServer extends Remote {
	/**
	 * Returns an array of String that contains a list of files
	 * @param path relative path of the file
	 * @return an array of String
	 * @throws RemoteException
	 * @throws InfoNotFoundException
	 */
	String[] dir(String path,String username,String pw) throws RemoteException, InfoNotFoundException, InvalidCredentialsException;

	/**
	 * Returns information about a directory
	 * @param path relative path of the directory
	 * @return a FileInfo object
	 * @throws RemoteException
	 * @throws InfoNotFoundException
	 */
	FileInfo getFileInfo(String path,String username,String pw) throws RemoteException,
			InfoNotFoundException, InvalidCredentialsException;

	/**
	 * Pulls a file from the server
	 * @param path relative path of the file
	 * @param bytesRead total bytes read until the current call
	 * @return an array of byte which contains a chunk of file data
	 * @throws RemoteException
	 * @throws InfoNotFoundException
	 */
	byte[] pullFile(String path, long bytesRead,String username,String pw)
			throws RemoteException, InfoNotFoundException, InvalidCredentialsException;

	/**
	 * Pushes a file to the server
	 * @param path absolute path of the file
	 * @param name name of the file
	 * @param fdata an array of byte which contains a chunk of file data
	 * @param startingByte the starting byte of the current sent chunk
	 * @param bytesWritten the total bytes number of the current chunk 
	 * @throws RemoteException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	void pushFile(String path,String name, byte[] fdata, long startingByte,
			long bytesWritten,long fileLength,String username,String pw) throws RemoteException,FileNotFoundException, IOException, InvalidCredentialsException;
	
	/**
	 * Removes a file from the server
	 * @param path relative path of the file
	 * @return true if the file was removed; false otherwise
	 * @throws RemoteException
	 */
	boolean removeFile(String path,String username,String pw) throws RemoteException, InvalidCredentialsException;
	
	/**
	 * Removes a directory from the server
	 * @param path relative path of the directory
	 * @return true if the directory was removed; false otherwise
	 * @throws RemoteException
	 */
	boolean removeDirectory(String path,String username,String pw) throws RemoteException, InvalidCredentialsException;
	
	/**
	 * Creates a directory on the server
	 * @param path relative path of the directory
	 * @return true if the directory was created; false otherwise
	 * @throws RemoteException
	 * @throws InfoNotFoundException
	 */
	public boolean createDir(String path,String username,String pw) throws RemoteException,
	InfoNotFoundException, InvalidCredentialsException;
	

	/**
	 * Aceitar ligação de um novo servidor. Actualizar lista de servidores
	 * conhecidos e espalhar lista de servidores por todos.
	 * 
	 * @throws NotBoundException
	 */
	/**
	 * Accepts a connection from a server, then it updates the list of known servers 
	 * and sends it to all of the known servers
	 * @param url url of the new connected server
	 * @throws RemoteException
	 * @throws NotBoundException
	 */
	void newServerConnected(String url) throws RemoteException,NotBoundException;

	/**
	 * Updates the list of known servers
	 * @param servers list of servers to push to current known servers
	 * @throws RemoteException
	 */
	void updateKnownServerList(List<String> servers,String username,String pw)
			throws RemoteException, InvalidCredentialsException;

	/**
	 * Returns the server URL
	 * @return server URL
	 * @throws RemoteException
	 */
	String getUrl(String username,String pw) throws RemoteException, InvalidCredentialsException;

	/**
	 * Returns the community of the server
	 * @return community of the server
	 * @throws RemoteException
	 */
	String getCommunity(String username,String pw) throws RemoteException, InvalidCredentialsException;

	/**
	 * Generates a new id for a server
	 * @return new id
	 * @throws RemoteException
	 */
	String getNextId(String username,String pw) throws RemoteException, InvalidCredentialsException;

	/**
	 * Returns the name of the server
	 * @return name of the server
	 * @throws RemoteException
	 */
	String getName(String username,String pw) throws RemoteException, InvalidCredentialsException;

	/**
	 * Returns the current known servers by this server
	 * @return current known servers list
	 * @throws RemoteException
	 */
	List<String> getServerList() throws RemoteException;
	
	/**
	 * Receives a shutdown announcement of another server
	 * @param name name of the server
	 * @return a list without the server name
	 * @throws RemoteException
	 */
	List<String> receiveShutdownAnnouncement(String name,String username,String pw) throws RemoteException, InvalidCredentialsException;
	
	/**
	 * Copies a file from a server to another server
	 * @param pathFrom path of the file on the source server
	 * @param serverTo url of the destination server
	 * @param pathTo path of the file on the destination server
	 * @return true if the file was copied; false otherwise
	 * @throws RemoteException
	 */
	boolean copyFileServerToServer(String pathFrom,String serverTo,String pathTo,String username,String pw) throws RemoteException, InvalidCredentialsException;
	
	
	/**
	 * Gets the user database
	 * @param username - username
	 * @param pw - password
	 * @return a Map with the user database
	 * @throws RemoteException
	 * @throws InvalidCredentialsException
	 */
	Map<String,String> getUsers(String username,String pw) throws RemoteException, InvalidCredentialsException;

	/**
	 * Registers a client in a RMI server and warns the other servers
	 * @param username - username
	 * @param pw - password
	 * @return true in case of success or false otherwise
	 * @throws RemoteException
	 */
	boolean registerClient(String username, String pwd) throws RemoteException;

	/**
	 * Updates the server user database
	 * @param users - updated user database
	 * @param username - username 
	 * @param pw - password
	 * @throws RemoteException
	 * @throws InvalidCredentialsException
	 */
	void updateUserDataBase(Map<String, String> users,String username,String pw) throws RemoteException, InvalidCredentialsException;

	/**
	 * Logs out the client with the given username
	 * @param users - updated user database
	 * @param username - username 
	 * @throws RemoteException
	 * @throws InvalidCredentialsException
	 */
	void logout(String username, String pwd) throws RemoteException, InvalidCredentialsException;

}
