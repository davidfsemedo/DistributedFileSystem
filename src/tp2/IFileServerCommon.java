package tp2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The IFileServerCommon interface represents an interface for a FileServer implemented in WS
 */
public interface IFileServerCommon  {
	/**
	 * Returns an array of String that contains a list of files
	 * @param path relative path of the file
	 * @return an array of String
	 * @throws InfoNotFoundException
	 */
	String[] dir(String path) throws InfoNotFoundException;

	/**
	 * Returns information about a directory
	 * @param path relative path of the directory
	 * @return a FileInfo object
	 * @throws InfoNotFoundException
	 */
	FileInfo getFileInfo(String path) throws InfoNotFoundException;

	/**
	 * Pulls a file from the server
	 * @param path relative path of the file
	 * @param bytesRead total bytes read until the current call
	 * @return an array of byte which contains a chunk of file data
	 * @throws InfoNotFoundException
	 */
	byte[] pullFile(String path, long bytesRead)
			throws InfoNotFoundException;

	/**
	 * Pushes a file to the server
	 * @param path absolute path of the file
	 * @param name name of the file
	 * @param fdata an array of byte which contains a chunk of file data
	 * @param startingByte the starting byte of the current sent chunk
	 * @param bytesWritten the total bytes number of the current chunk 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	void pushFile(String path,String name, byte[] fdata, long startingByte,
			long bytesWritten) throws FileNotFoundException, IOException;
	
	/**
	 * Removes a file from the server
	 * @param path relative path of the file
	 * @return true if the file was removed; false otherwise
	 */
	boolean removeFile(String path);
	
	/**
	 * Removes a directory from the server
	 * @param path relative path of the directory
	 * @return true if the directory was removed; false otherwise
	 */
	boolean removeDirectory(String path);
	
	/**
	 * Creates a directory on the server
	 * @param path relative path of the directory
	 * @return true if the directory was created; false otherwise
	 * @throws InfoNotFoundException
	 */
	boolean createDir(String path) throws InfoNotFoundException;
	

	/**
	 * Accepts a connection from a server, then it updates the list of known servers 
	 * and sends it to all of the known servers
	 * @param url url of the new connected server
	 * @throws NotBoundException
	 */
	void newServerConnected(String url);

	/**
	 * Updates the list of known servers
	 * @param servers list of servers to push to current known servers
	 */
	void updateKnownServerList(List<String> list);

	/**
	 * Returns the server URL
	 * @return server URL
	 */
	String getUrl();

	/**
	 * Returns the community of the server
	 * @return community of the server
	 */
	String getCommunity();

	/**
	 * Generates a new id for a server
	 * @return new id
	 */
	String getNextId();

	/**
	 * Returns the name of the server
	 * @return name of the server
	 */
	String getName() ;

	/**
	 * Returns the current known servers by this server
	 * @return current known servers list
	 */
	List<String> getServerList();

	/**
	 * Receives a shutdown announcement of another server
	 * @param name name of the server
	 * @return a list without the server name
	 */
	List<String> receiveShutdownAnnouncement(String name) ;
	
	/**
	 * Copies a file from a server to another server
	 * @param pathFrom path of the file on the source server
	 * @param serverTo url of the destination server
	 * @param pathTo path of the file on the destination server
	 * @return true if the file was copied; false otherwise
	 */
	boolean copyFileServerToServer(String pathFrom,String serverTo,String pathTo);


}
