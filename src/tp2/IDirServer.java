package tp2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The IDirServer interface represents a top level layer of DirServerRMI and DirServerWS
 * It is used by the FileClient class to contact a server without knowing which type of server it is
 */
public interface IDirServer {
	
	/**
	 * Returns a Pair object with a String which is the server url and an Object which is a server reference
	 * @param url url of the server to get
	 * @param serverList list of currently known servers
	 * @return a Pair object
	 */
	Pair<String, Object> getServer(String url,Set<String> serverList);
	
	/**
	 * Returns an array of String that contains a list of files
	 * @param path relative path of the file
	 * @param url url of the server to invoke
	 * @return an array of String
	 * @throws InfoNotFoundException
	 * @throws InvalidCredentialsException 
	 * @throws UsernameAlreadyTaken 
	 */
	String[] dir(String path, String url) throws InfoNotFoundException, InvalidCredentialsException, UsernameAlreadyTaken;

	/**
	 * Returns information about a directory
	 * @param path relative path of the directory
	 * @param url url of the server to invoke
	 * @return a FileInfo object
	 * @throws InfoNotFoundException
	 * @throws InvalidCredentialsException 
	 */
	FileInfo getFileInfo(String path, String url) throws InfoNotFoundException, InvalidCredentialsException , UsernameAlreadyTaken;

	/**
	 * Pulls a file from a server
	 * @param path relative path of the file
	 * @param bytesRead total bytes read until the current call
	 * @param url url of the server to invoke
	 * @return an array of byte which contains a chunk of file data
	 * @throws InfoNotFoundException
	 * @throws InvalidCredentialsException 
	 * @throws UsernameAlreadyTaken 
	 */
	byte[] pullFile(String path, long bytesRead, String url) throws InfoNotFoundException, InvalidCredentialsException, UsernameAlreadyTaken;

	/**
	 * Pushes a file to a server
	 * @param path absolute path of the file , UsernameAlreadyTaken
	 * @param name name of the file
	 * @param fdata an array of byte which contains a chunk of file data
	 * @param startingByte the starting byte of the current sent chunk
	 * @param bytesWritten the total bytes number of the current chunk 
	 * @param url url of the server to invoke
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws InvalidCredentialsException 
	 */
	void pushFile(String path, String name, byte[] fdata, long startingByte,
			long bytesWritten, long fileLength, String url) throws FileNotFoundException, IOException, InvalidCredentialsException , UsernameAlreadyTaken;

	/**
	 * Removes a file from a server
	 * @param path relative path of the file
	 * @param url url of the server to invoke
	 * @return true if the file was removed; false otherwise
	 * @throws InvalidCredentialsException 
	 */
	boolean removeFile(String path, String url) throws InvalidCredentialsException , UsernameAlreadyTaken;

	/**
	 * Removes a directory from a server
	 * @param path relative path of the directory
	 * @param url url of the server to invoke
	 * @return true if the directory was removed; false otherwise
	 * @throws InvalidCredentialsException 
	 */
	boolean removeDirectory(String path, String url) throws InvalidCredentialsException , UsernameAlreadyTaken;

	/**
	 * Creates a directory on a server
	 * @param path relative path of the directory
	 * @param url url of the server to invoke
	 * @return true if the directory was created; false otherwise
	 * @throws InfoNotFoundException
	 * @throws InvalidCredentialsException 
	 */
	public boolean createDir(String path, String url) throws InfoNotFoundException, InvalidCredentialsException , UsernameAlreadyTaken;

	/**
	 * Returns a list of current known servers
	 * @param url url of the server to invoke
	 * @return list of current known server
	 * @throws InvalidCredentialsException 
	 */
	List<String> getServerList(String url) throws InvalidCredentialsException , UsernameAlreadyTaken;

	/**
	 * Copies a file from a server to another server
	 * @param pathFrom path of the file on the source server
	 * @param serverTo url of the destination server
	 * @param pathTo path of the file on the destination server
	 * @param url url of the server to invoke
	 * @return true if the file was copied; false otherwise
	 * @throws InvalidCredentialsException 
	 */
	boolean copyFileServerToServer(String pathFrom, String serverTo,
			String pathTo, String url) throws InvalidCredentialsException , UsernameAlreadyTaken;
	
	/**
	 * Registers a client in one RMI server.
	 * @param serverList - list of known servers
	 * @return 0 in case of success, FileClient.USERNAME_ALREADY_TAKEN if the username already exists,
	 * FileClient.NO_RMI_SERVERS_AVAILABLE if no RMI servers are available
	 * 
	 */
	int registerClient(Set<String> serverList);

	/**
	 * Logs out an user
	 * @param serverList - known server list
	 * @throws InvalidCredentialsException
	 */
	void logout(Set<String> serverList) throws InvalidCredentialsException;

	/**
	 * Checks if the user is registered.
	 * @return
	 */
	boolean isRegistered();

	/**
	 * Attempts to get the server list from an available server.
	 * @return
	 * @throws InvalidCredentialsException
	 * @throws UsernameAlreadyTaken
	 */
	List<String> getServerListFromRandomServer()
			throws InvalidCredentialsException, UsernameAlreadyTaken;

}
