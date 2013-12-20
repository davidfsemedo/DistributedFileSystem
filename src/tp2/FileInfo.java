package tp2;

import java.util.*;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The FileInfo class represents information about a file
 */
public class FileInfo implements java.io.Serializable
{
	private static final long serialVersionUID = -4498079336259690561L;

	public String name;
	public long length;
	public String modified;
	public boolean isFile;
	
	/**
	 * No args constructor because of WS
	 */
	public FileInfo() {
		
	}
	
	/**
	 * Constructs a FileInfo
	 * @param name name of the file
	 * @param length length of the file
	 * @param modified last modified date
	 * @param isFile if it is a file
	 */
	public FileInfo( String name, long length, Date modified, boolean isFile) {
		this.name = name;
		this.length = length;
		this.modified = modified.toString();
		this.isFile = isFile;
	}
	
	/**
	 * Constructs a FileInfo
	 * @param name name of the file
	 * @param length length of the file
	 * @param modified last modified date
	 * @param isFile if it is a file
	 */
	public FileInfo( String name, long length, String modified, boolean isFile) {
		this.name = name;
		this.length = length;
		this.modified = modified;
		this.isFile = isFile;
	}
	
	
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Name : " + name + "\nLength: " + length +"\nModified date: " + modified + "\nisFile : " + isFile; 
	}
}
