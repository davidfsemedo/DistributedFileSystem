package tp2;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 *         The FileClient class represents a DirServer client
 */
public class FileClient {
	String communityURL;
	public static final String DEFAULT_MULTICAST_ADDRESS = "224.0.1.0";
	public static final int CHUNK_SIZE = 32768;
	private static final int GOOGLE_DRIVE_CHUNK_SIZE = DirServerGoogleDriveRMI.GOOGLE_DRIVE_CHUNK_SIZE;
	private static final int DROPBOX_CHUNK_SIZE = DirServerDropBoxRMI.CHUNK_SIZE;
	public static int DEFAULT_MULTICAST_PORT_RECEIVE = 9001;
	public static int DEFAULT_MULTICAST_PORT_SEND = 9000;
	private static boolean needUpdate;
	private static int numberDown;
	private static Set<String> downSet;
	private static final String LOCAL = "local";
	private static final String PROMPT = "> ";
	public static final String CLIENT_LOOKING_FOR_SERVER = "Looking For a Server in local network.";
	private static final String CLIENT_MESSAGE = "CLT";

	// Date format of metainfo files
	private static final String DATE_FORMAT = "EEE MMM dd HH:mm:ss z yyyy";

	private Set<String> serverList;

	// Sync paths
	private String localSyncDirPath;
	private String remoteSyncDirPath;

	private boolean firstSyncRun = true;

	// Sync queues
	private Queue<Pair<String, Boolean>> queuedDownloadFiles;
	private Queue<Pair<String, Boolean>> queuedUploadFiles;
	private Queue<Pair<String, Boolean>> queuedRemoteDirRemovedFiles;
	private Queue<Pair<String, Boolean>> queuedLocalDirRemovedFiles;

	private ServerUnresponsiveHandler servHandler;
	private ServerConnectionManager servCManager;
	private IDirServer dirServer;

	public static final int USERNAME_ALREADY_TAKEN = 1;
	public static final int NO_RMI_SERVERS_AVAILABLE = 2;

	/**
	 * Constructs a FileClient
	 * 
	 * @param url
	 *            url of a server of the community to join
	 */
	protected FileClient(String url, String username, String pwd) {
		this.communityURL = url;
		numberDown = 0;
		downSet = new HashSet<String>();
		this.servHandler = new ServerUnresponsiveHandler(true, username, pwd);
		this.servCManager = new ServerConnectionManager(servHandler, serverList);
		this.dirServer = new DirServer(servCManager, servHandler, serverList,
				username, pwd);
		needUpdate = false;
		this.serverList = updateServerList(communityURL);
		queuedDownloadFiles = new LinkedList<Pair<String, Boolean>>();
		queuedUploadFiles = new LinkedList<Pair<String, Boolean>>();
		queuedRemoteDirRemovedFiles = new LinkedList<Pair<String, Boolean>>();
		queuedLocalDirRemovedFiles = new LinkedList<Pair<String, Boolean>>();
	}

	/**
	 * Updates the current known servers list of the file client asking a server
	 * for a updated list
	 * 
	 * @param serverUrl
	 *            - the server that will be asked for the updated list
	 * @return a Set<String> with all the server url's in the community
	 */
	private Set<String> updateServerList(String serverUrl) {
		List<String> ls = null;
		Set<String> set = null;
		Pair<String, Object> p = dirServer.getServer(serverUrl, serverList);
		if (p == null)
			return null;
		try {
			ls = dirServer.getServerList(serverUrl);
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);
		}
		set = new HashSet<String>(ls.size());
		for (String s : ls)
			set.add(s);
		return set;
	}

	/**
	 * Returns an array of servers which belongs to the client's community
	 * 
	 * @return array of servers' url
	 */
	protected String[] servers() {
		Iterator<String> it = serverList.iterator();
		Set<String> map = null;
		boolean done = false;
		while (!done && it.hasNext()) {
			map = updateServerList(it.next());
			if (map != null) {
				done = true;
			}
		}
		this.serverList = map;
		String[] aux = new String[serverList.size()];
		return serverList.toArray(aux);
	}

	/**
	 * This method informs the user that no servers are available
	 */
	private void noServersAvailable() {
		System.out.println("No Servers Available!!");
		System.out.println("Client Will Exit!");
		System.exit(1);
	}

	/**
	 * Returns an array with the files and directories that belongs to the
	 * directory dir on the server server If the specified server is null, it
	 * will return files and directories of all servers of the community
	 * 
	 * @param server
	 *            server to list content
	 * @param dir
	 *            directory to list content
	 * @return array with the files' and directories' names
	 */
	protected String[] dir(String server, String dir) {
		List<String> directoryList = null;
		try {
			Map<String, List<String>> files;
			String[] fileList = null;
			directoryList = new LinkedList<String>();
			if (server == null) {
				files = new TreeMap<String, List<String>>(new FilesComparator());
				for (String srv : serverList) {
					Pair<String, Object> p = dirServer.getServer(srv,
							serverList);
					if (p == null)
						continue;

					fileList = dirServer.dir(dir, srv);
					if (fileList != null && fileList.length >= 0) {
						List<String> l;
						for (String f : fileList) {
							if (!files.containsKey(f)) {
								files.put(f, l = new LinkedList<String>());
								l.add(srv);
							} else {
								files.get(f).add(srv);

							}
						}
					}
				}
				List<String> flist;
				for (String file : files.keySet()) {
					if ((flist = files.get(file)).size() == 1)
						directoryList.add(file);
					else {
						Iterator<String> it = flist.iterator();
						while (it.hasNext())
							directoryList.add(file + "@" + it.next());
					}
				}
			} else {
				Pair<String, Object> p = dirServer
						.getServer(server, serverList);

				if (p == null)
					return null;

				fileList = dirServer.dir(dir, server);

				if (fileList == null)
					return null;
				if (fileList.length == 0)
					return fileList;
				for (String file : fileList)
					directoryList.add(file);
			}
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (directoryList.size() >= 0) {
			String[] dlist = new String[directoryList.size()];
			if (directoryList.size() > 0)
				directoryList.toArray(dlist);
			return dlist;
		}

		return null;
	}

	/**
	 * Creates the directory dir on the server server
	 * 
	 * @param server
	 *            server to create a directory
	 * @param dir
	 *            directory name to create
	 * @return true if the directory was created; false otherwise
	 */
	protected boolean mkdir(String server, String dir) {
		try {
			boolean success = false;
			boolean aux = false;
			if (server == null) {
				for (String srv : serverList) {
					try {
						Pair<String, Object> p = dirServer.getServer(srv,
								serverList);
						if (p == null)
							continue;
						aux = dirServer.createDir(dir, srv);

						success = success || aux;

					} catch (InfoNotFoundException e) {
						;
					} catch (InvalidCredentialsException e) {
						System.out
						.println("Invalid Username or Password! Please login Again!");
						System.exit(1);
					} catch (UsernameAlreadyTaken e) {
						System.out
						.println("Username Already taken! Impossible to register!");
						System.exit(1);
					}
				}
				return success;
			}
			Pair<String, Object> p = dirServer.getServer(server, serverList);
			if (p == null)
				return false;
			return dirServer.createDir(dir, server);

		} catch (InfoNotFoundException e) {
			System.out
			.println("It was not possible to get information about the file");
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);
		}
		return false;
	}

	/**
	 * Removes the directory dir on the server server. If server is null then it
	 * will remove the directory in all known servers
	 * 
	 * @param server
	 *            server to remove the directory
	 * @param dir
	 *            relative directory path to remove
	 * @return true if the directory was removed; false otherwise
	 */
	protected boolean rmdir(String server, String dir) {
		boolean success = false;
		boolean aux = false;
		if (server == null) {
			for (String srv : serverList) {
				Pair<String, Object> p = dirServer.getServer(srv, serverList);
				if (p == null)
					continue;

				try {

					aux = dirServer.removeDirectory(dir, srv);

				} catch (InvalidCredentialsException e) {
					System.out
					.println("Invalid Username or Password! Please login Again!");
					System.exit(1);
				} catch (UsernameAlreadyTaken e) {
					System.out
					.println("Username Already taken! Impossible to register!");
					System.exit(1);
				}

				success = success || aux;

				return success;
			}
		}

		Pair<String, Object> p = dirServer.getServer(server, serverList);
		if (p == null)
			return false;
		try {
			return dirServer.removeDirectory(dir, server);
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);
		}
		return false;
	}

	/**
	 * Removes the file path on server server If server is null then it will
	 * remove the file in all known servers
	 * 
	 * @param server
	 *            server to remove file from
	 * @param path
	 *            relative path of the file to remove
	 * @return true if the file was removed; false otherwise
	 */
	protected boolean rm(String server, String path) {
		boolean success = false;
		boolean aux = false;
		if (server == null) {
			for (String srv : serverList) {

				Pair<String, Object> p = dirServer.getServer(srv, serverList);
				if (p == null)
					continue;

				try {
					aux = dirServer.removeFile(path, srv);
				} catch (InvalidCredentialsException e) {
					System.out
					.println("Invalid Username or Password! Please login Again!");
					System.exit(1);
				} catch (UsernameAlreadyTaken e) {
					System.out
					.println("Username Already taken! Impossible to register!");
					System.exit(1);
				}

				success = success || aux;

				return success;
			}
		}

		Pair<String, Object> p = dirServer.getServer(server, serverList);
		if (p == null)
			return false;
		try {
			return dirServer.removeFile(path, server);
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);
		}
		return false;
	}

	/**
	 * Returns information about the file/directory path on server server If
	 * server is null then it will return file info of a file/directory on an
	 * arbitrary server
	 * 
	 * @param server
	 *            server to get information of file/directory
	 * @param path
	 *            relative path of the file/directory
	 * @return information about the file/directory
	 */
	protected FileInfo getAttr(String server, String path) {
		try {
			FileInfo info = null;
			if (server == null) {
				for (String srv : serverList) {
					Pair<String, Object> p = dirServer.getServer(srv,
							serverList);
					if (p == null)
						continue;

					info = dirServer.getFileInfo(path, srv);

					if (info != null)
						return info;
				}
				return null;
			}

			Pair<String, Object> p = dirServer.getServer(server, serverList);
			if (p == null)
				return null;
			info = dirServer.getFileInfo(path, server);
			return info;
		} catch (InfoNotFoundException e) {
			System.out.println("File Not Found!");
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);
		}
		return null;
	}

	/**
	 * Copies a file from relative path pathFrom from serverFrom to relative
	 * path pathTo of serverTo If server is local then it copies from the client
	 * to a server If server is null then the file is copied from an existing
	 * server
	 * 
	 * @param serverFrom
	 *            server to copy file from
	 * @param pathFrom
	 *            relative path to copy file from
	 * @param serverTo
	 *            destination server to copy the file
	 * @param pathTo
	 *            relative path of the file on the destination server
	 * @return true if the file was copied; false otherwise
	 */
	protected boolean cp(String serverFrom, String pathFrom, String serverTo,
			String pathTo) {
		String server = null;
		if (serverTo == null)
			return false;
		if (serverFrom == null) {
			server = findFileServerWithFile(pathFrom, serverTo);
			if (server == null)
				return false;
		} else
			server = serverFrom;

		if (serverList != null) {
			if (server.equals(LOCAL) && !serverTo.equals(LOCAL)
					&& serverList.contains(serverTo))
				return sendFileToServer(pathFrom, serverTo, pathTo);
			else if (!server.equals(LOCAL) && serverTo.equals(LOCAL)
					&& serverList.contains(server))
				return getFileFromServer(server, pathFrom, pathTo);
			else if (!server.equals(LOCAL) && !serverTo.equals(LOCAL)
					&& serverList.contains(server)
					&& serverList.contains(serverTo))
				return transferFileFromServerToServer(server, pathFrom,
						serverTo, pathTo);
			else if (server.equals(LOCAL) && serverTo.equals(LOCAL))
				return copyFile(pathFrom, pathTo);
		}
		return false;

	}

	/**
	 * Reads a fixed chunk size of a local file and writes it to another local
	 * file
	 * 
	 * @param pathFrom
	 *            relative path of the file to copy
	 * @param pathTo
	 *            relative destination path of the copy
	 * @return true if the file was copied; false otherwise
	 */
	private boolean copyFile(String pathFrom, String pathTo) {
		File dir = new File(pathFrom);
		try {
			if (dir.exists()) {
				byte[] data = new byte[CHUNK_SIZE];
				FileInputStream fstream = new FileInputStream(pathFrom);
				BufferedInputStream in = new BufferedInputStream(fstream);
				FileOutputStream fileout = new FileOutputStream(pathTo);
				BufferedOutputStream out = new BufferedOutputStream(fileout);
				long i = dir.length();
				System.out.println("Transfering file " + dir.getAbsolutePath()
						+ ". \nPlease wait... ");
				while (i > 0) {
					
					if (i < CHUNK_SIZE) {
						in.read(data, 0, (int) i);
						out.write(data, 0, (int) i);
						i = 0;
					} else {
						in.read(data, 0, CHUNK_SIZE);
						out.write(data, 0, CHUNK_SIZE);
					}

					i -= CHUNK_SIZE;
				}
				in.close();
				out.close();
				return true;
			}
		} catch (java.io.IOException e) {
			return false;
		}
		return false;
	}

	/**
	 * Searches for a server with the file pathFrom
	 * 
	 * @param pathFrom
	 *            file to search on the server
	 * @param serverTo
	 *            server to send the file
	 * @return url of the server with the desired file
	 */
	private String findFileServerWithFile(String pathFrom, String serverTo) {

		Iterator<String> it = serverList.iterator();
		boolean done = false;
		String aux = null;
		FileInfo info = null;
		while (!done && it.hasNext()) {
			try {
				aux = it.next();
				if (!aux.equals(serverTo)) {
					Pair<String, Object> p = dirServer.getServer(aux,
							serverList);
					if (p == null)
						continue;

					info = dirServer.getFileInfo(pathFrom, aux);

					if (info != null)
						return aux;
				}
			} catch (InfoNotFoundException e) {
				System.out
				.println("It was not possible to get information about the file");
			} catch (InvalidCredentialsException e) {
				System.out
				.println("Invalid Username or Password! Please login Again!");
				System.exit(1);
			} catch (UsernameAlreadyTaken e) {
				System.out
				.println("Username Already taken! Impossible to register!");
				System.exit(1);
			}
		}
		return null;
	}

	/**
	 * Transfers a file from a server to another server
	 * 
	 * @param serverFrom
	 *            server to transfer file from
	 * @param pathFrom
	 *            relative path of the file on the serverFrom
	 * @param serverTo
	 *            server to transfer the file
	 * @param pathTo
	 *            relative path of the file on the serverTo
	 * @return true if the file was transfered; false otherwise
	 */
	private boolean transferFileFromServerToServer(String serverFrom,
			String pathFrom, String serverTo, String pathTo) {
		Pair<String, Object> p = dirServer.getServer(serverFrom, serverList);
		if (p == null)
			return false;
		System.out.println("Transfering file " + pathFrom + " from server "
				+ serverFrom + " to server " + serverTo + "\nPlease wait... ");
		try {
			return dirServer.copyFileServerToServer(pathFrom, serverTo, pathTo,
					serverFrom);
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);
		}
		return false;
	}

	/**
	 * Sends a file to a server
	 * 
	 * @param pathFrom
	 *            relative path of the file
	 * @param serverTo
	 *            server to transfer the file
	 * @param pathTo
	 *            relative path of the file on the serverTo server
	 * @return true if the file was sent; false otherwise
	 */
	private boolean sendFileToServer(String pathFrom, String serverTo,
			String pathTo) {
		try {
			Pair<String, Object> p = dirServer.getServer(serverTo, serverList);
			if (p == null)
				return false;
			int chunkSize = CHUNK_SIZE;
			File dir = new File(pathFrom);
			if (dir.exists()) {
				byte[] data;
				if(servCManager.isService(serverTo)) {
					String service = servCManager.getService(serverTo);
					if (service.equals(ServerConnectionManager.GOOGLEDRIVE))
						chunkSize = GOOGLE_DRIVE_CHUNK_SIZE;
					else if(service.equals(ServerConnectionManager.DROPBOX))
						chunkSize = DROPBOX_CHUNK_SIZE;
				}
	
				data = new byte[chunkSize];
				FileInputStream fstream = new FileInputStream(pathFrom);
				BufferedInputStream in = new BufferedInputStream(fstream);
				long i = dir.length();
				long j = 0;
				while (i > 0) {
					if (i < chunkSize) {
						data = new byte[(int) i];
						in.read(data, 0, (int) i);
					} else
						in.read(data, 0, chunkSize);
					
					dirServer.pushFile(pathTo, dir.getName(), data, j, i, dir.length(),
							serverTo);
					i -= chunkSize;
					j += chunkSize;
					
				}
				in.close();
				return true;
			}

		} catch (java.io.IOException e) {
			System.out
			.println("It was not possible to get information about the file");
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);
		}
		return false;
	}

	@SuppressWarnings("unused")
	private void sendFileInOnce(String pathFrom, File dir, String serverTo,
			String pathTo) {
		byte[] data = new byte[(int) dir.length()];
		FileInputStream fstream;
		try {
			fstream = new FileInputStream(pathFrom);

			BufferedInputStream in = new BufferedInputStream(fstream);
			in.read(data, 0, (int) dir.length());
			dirServer.pushFile(pathTo, "", data, 0, dir.length(), dir.length(),
					serverTo);
			in.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);
		}
	}

	/**
	 * Gets a file from a server
	 * 
	 * @param serverFrom
	 *            server of the file
	 * @param pathFrom
	 *            relative path of the file on the serverFrom server
	 * @param pathTo
	 *            relative path of the copied file
	 * @return true if the file was copied; false otherwise
	 */
	private boolean getFileFromServer(String serverFrom, String pathFrom,
			String pathTo) {
		try {
			FileInfo info = null;
			Pair<String, Object> p = dirServer
					.getServer(serverFrom, serverList);
			if (p == null)
				return false;

			File dir = new File(pathTo);
			info = dirServer.getFileInfo(pathFrom, serverFrom);

			if (info == null) {
				System.out.println("File \"" + pathFrom
						+ "\" doesn't exist on server " + serverFrom + " !");
				return false;
			}

			byte[] fdata;
			long i = info.length;
			FileOutputStream fileout = new FileOutputStream(dir);
			BufferedOutputStream out = new BufferedOutputStream(fileout);
			long j = 0;
			System.out.println("Transfering file " + dir.getAbsolutePath()
					+ ". Please wait... ");

			while (i > 0) {
				fdata = dirServer.pullFile(pathFrom, j, serverFrom);
				if(servCManager.isService(serverFrom)) {
					String service = servCManager.getService(serverFrom);
					if (service.equals(ServerConnectionManager.FLICKR))
						i = fdata.length;
				}

				if (i < fdata.length) {
					out.write(fdata, 0, (int) i);
					i = 0;
				} else
					out.write(fdata);
				i -= fdata.length;
				j += fdata.length;
			}
			out.close();
			return true;

			// return false;
		} catch (java.io.IOException e) {
			e.printStackTrace();
			System.out.println("There was a problem while copying the file");
		} catch (InfoNotFoundException e) {
			System.out
			.println("It was not possible to get information about the file");
		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);
		}
		return false;
	}

	/**
	 * Returns a String array with files' name obtained from an escaped string,
	 * i. e. file\ name will be file name
	 * 
	 * @param line
	 *            command written by the user
	 * @param n
	 *            index of the file. e.g. With two files file\ name\ 1 file\
	 *            name\ 2 the index 1 will be file name 1 and index 2 will be
	 *            file name 2
	 * @return a String array with files' name
	 */
	private String[] getEscapedFiles(String line, int n) {
		String[] cmd = line.split(" ");
		String t = line.split(cmd[0] + " ")[1].replace("\\", "");
		String[] content = new String[2];

		if (n == 1) {
			if (t.contains("@")) {
				content[0] = t.split("@")[0];
				content[1] = t.split("@")[1].split(" ")[0];
			} else
				content[0] = t;
		} else if (n == 2) {
			if (t.contains("@")) {
				content[0] = t.split("@")[1].replaceAll(
						"local\\s|\\w+://.*/\\w+.*?\\s", "");

				content[1] = t.split("@")[2];
			} else
				content[0] = t;
		}
		return content;
	}

	/**
	 * Command interpreter
	 * 
	 * @throws java.io.IOException
	 */
	protected void doit() throws java.io.IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				System.in));

		for (;;) {

			if (needUpdate) {
				if (numberDown == serverList.size()) {
					if (downSet.containsAll(serverList))
						noServersAvailable();
				}
				this.serverList = attemptToGetUpdatedServerList();
				if (serverList == null)
					noServersAvailable();
				needUpdate = false;
				numberDown = 0;
				downSet.clear();
			}

			System.out.print(PROMPT);
			String line = reader.readLine();
			if (line == null)
				break;
			String[] cmd = line.split(" ");

			if (cmd[0].equalsIgnoreCase("servers")) {
				String[] s = servers();
				if (s == null)
					System.out.println("error");
				else {
					for (int i = 0; i < s.length; i++)
						System.out.println(s[i]);
				}
			} else if (cmd[0].equalsIgnoreCase("ls")) {
				String[] dir;
				if (cmd.length > 1)
					dir = getEscapedFiles(line, 1);
				else {
					dir = new String[1];
					dir[0] = ".";
				}

				String[] res = dir(dir.length == 1 ? null : dir[1], dir[0]);
				if (res != null) {
					System.out.println(res.length);
					for (int i = 0; i < res.length; i++)
						System.out.println(res[i]);
				} else
					System.out.println("error");
			} else if (cmd[0].equalsIgnoreCase("mkdir") && cmd.length > 1) {
				String[] dir = getEscapedFiles(line, 1);
				boolean b = mkdir(dir.length == 1 ? null : dir[1], dir[0]);
				if (b)
					System.out.println("success");
				else
					System.out.println("error");
			} else if (cmd[0].equalsIgnoreCase("rmdir") && cmd.length > 1) {
				String[] dir = getEscapedFiles(line, 1);
				boolean b = rmdir(dir.length == 1 ? null : dir[1], dir[0]);
				if (b)
					System.out.println("success");
				else
					System.out.println("error");
			} else if (cmd[0].equalsIgnoreCase("rm") && cmd.length > 1) {
				String[] dir = getEscapedFiles(line, 1);
				boolean b = rm(dir.length == 1 ? null : dir[1], dir[0]);
				if (b)
					System.out.println("success");
				else
					System.out.println("error");
			} else if (cmd[0].equalsIgnoreCase("getattr") && cmd.length > 1) {
				String[] dir = getEscapedFiles(line, 1);
				FileInfo info = getAttr(dir.length == 1 ? null : dir[1], dir[0]);
				if (info != null) {
					System.out.println(info);
					System.out.println("success");
				} else
					System.out.println("error");
			} else if (cmd[0].equalsIgnoreCase("cp") && cmd.length > 1) {
				String[] dir1 = getEscapedFiles(line, 1);
				String[] dir2 = getEscapedFiles(line, 2);
				boolean b = cp(dir1.length == 1 ? null : dir1[1], dir1[0],
						dir2.length == 1 ? null : dir2[1], dir2[0]);
				if (b)
					System.out.println("success");
				else
					System.out.println("error");
			} else if (cmd[0].equalsIgnoreCase("help")) {
				System.out
				.println("servers - lists URL of servers that belongs to the community");
				System.out
				.println("ls dir - lists files/directories that belongs to the directory dir (. and .. have the usual meaning). If there's files with the same name they appear as name@server");
				System.out
				.println("mkdir dir@server - creates directory dir on server server");
				System.out
				.println("rmdir dir@server - removes directory dir on server server");
				System.out
				.println("cp path1 path2 - copies file path1 to path2; when path represents a file on a server it must have the pattern path@server. When it represents a local file to the client it must have the pattern path@local");
				System.out.println("rm path - removes file path");
				System.out
				.println("getattr path - shows information about the file/directory path. It includes name, a boolean showing if it is a file, creation date, last modified date");
			} else if (cmd[0].equalsIgnoreCase("exit"))
				break;
			else if (cmd[0].equalsIgnoreCase("sync") && cmd.length > 1) {
				String[] dir1 = getEscapedFiles(line, 1);
				String[] dir2 = getEscapedFiles(line, 2);
				sync(dir1[0], dir2[0], dir2.length == 1 ? null : dir2[1]);
			} else
				System.out
				.println("Command unavailable. For a list of commands enter: help");
		}
	}

	private Set<String> attemptToGetUpdatedServerList() {
		try {
			List<String> slist = dirServer.getServerListFromRandomServer();
			Set<String> set = new HashSet<String>(slist.size());
			for (String s : slist)
				set.add(s);
			return set;

		} catch (InvalidCredentialsException e) {
			System.out
			.println("Invalid Username or Password! Please login Again!");
			System.exit(1);
		} catch (UsernameAlreadyTaken e) {
			System.out
			.println("Username Already taken! Impossible to register!");
			System.exit(1);
		}
		return null;
	}

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String url;

		if (args.length > 1) {
			System.out
			.println("Use: java trab1.FileClient <URL>\n<URL> Opcional");
			return;
		} else if (args.length == 1) {
			url = args[0];
		} else {
			url = findServersOnLocalNetwork();
		}
		System.setProperty("javax.net.ssl.trustStore", "clientcacerts.ks");
		System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
		Scanner in = new Scanner(System.in);
		System.out.println("Please Insert a Username:");
		String username = in.nextLine();
		System.out.println("Please Insert a Password:");
		String pwd = in.nextLine();

		try {
			FileClient client = new FileClient(url, username, pwd);
			switch (client.register()) {
			case USERNAME_ALREADY_TAKEN:
				System.out.println("Username already Taken!");
				return;
			case NO_RMI_SERVERS_AVAILABLE:
				System.out
				.println("No RMI Servers Available. Impossible to register at the moment.");
				break;
			default:
				System.out.println("Client Registered with Sucess!");
			}

			client.doit();
			client.logout();

		} catch (java.io.IOException e) {
			System.out.println("There was a problem while copying the file");
		}

	}

	private int register() {

		return dirServer.registerClient(serverList);
	}

	private void logout() {
		if (dirServer.isRegistered()) {
			try {
				dirServer.logout(serverList);
				System.out.println("Logged out!");
			} catch (InvalidCredentialsException e) {
				System.out
				.println("Invalid Username or Password! Please login Again!");
				System.exit(1);
			}
		}
	}

	/**
	 * Searches for a server on local network using multicast
	 * 
	 * @return url of a found server; null if no server was found
	 */
	private static String findServersOnLocalNetwork() {
		String message = null;
		String[] tokens = null;
		try {
			// create multicast socket
			final InetAddress address = InetAddress
					.getByName(FileClient.DEFAULT_MULTICAST_ADDRESS);
			MulticastSocket socket = new MulticastSocket(
					FileClient.DEFAULT_MULTICAST_PORT_RECEIVE);
			socket.joinGroup(address);

			// send packet
			message = FileClient.CLIENT_MESSAGE + "\n"
					+ FileClient.CLIENT_LOOKING_FOR_SERVER + "\n";
			DatagramPacket packet = new DatagramPacket(message.getBytes(),
					message.getBytes().length);
			packet.setAddress(address);
			packet.setPort(FileClient.DEFAULT_MULTICAST_PORT_SEND);
			socket.send(packet);

			System.out.println("Searching for a server...");

			// receive packet
			byte[] buffer = new byte[65536];
			DatagramPacket rpacket = new DatagramPacket(buffer, buffer.length);
			socket.receive(rpacket);
			System.out.println("Server found!");
			message = new String(rpacket.getData(), 0, rpacket.getLength());
			socket.leaveGroup(address);
			socket.close();

			tokens = message.split("\n");

		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
		return tokens[1];
	}

	/**
	 * This method is used to inform the client that is serverList needs to be
	 * updated. Every server url from an offline server is stored. When the
	 * client performs the update it uses the servers down set
	 * 
	 * @param serverDown
	 *            - the server that is down server is stored. When the client
	 *            performs the update it uses the servers down set
	 */
	public static void needUpdate(String serverDown) {
		needUpdate = true;
		numberDown++;
		downSet.add(serverDown);
	}

	/**
	 * Returns the attributes of a local file
	 * 
	 * @param path
	 *            path of file
	 * @return attributes of a local file
	 */
	private FileInfo getLocalFileAttr(String path) {
		File f = new File(path);
		if (f.exists())
			return new FileInfo(f.getName(), f.length(), new Date(
					f.lastModified()), f.isFile());
		return null;
	}

	/**
	 * Lists files of a local directory
	 * 
	 * @param path
	 *            path of local directory
	 * @return files of a local directory
	 */
	private String[] dirLocal(String path) {
		File f = new File(path);
		List<String> fileList;
		String[] content;
		if (f.exists()) {
			Map<String, String> children = new TreeMap<String, String>(
					new FilesComparator());
			for (String s : f.list())
				children.put(s, s);
			fileList = new LinkedList<String>();
			content = new String[children.size()];
			for (String s : children.keySet()) {
				fileList.add(s);
			}
			fileList.toArray(content);
			return content;
		}
		return null;
	}

	/**
	 * Gets the data of all files inside a directory in a local path or remote
	 * path
	 * 
	 * @param server
	 *            server url if it is getting data of remote path
	 * @param path
	 *            path to get files data
	 * @param fileList
	 *            List object which will have the files path list
	 * @param fileInfos
	 *            Map object which will have FileInfo objects about each file
	 * @param local
	 *            should be true if it will get data of a local directory; false
	 *            if remote
	 */
	private void recursiveGetFileData(String server, String path,
			List<String> fileList, Map<String, FileInfo> fileInfos,
			boolean local) {

		FileInfo info;
		if (!path.equals(".sync")) { // ignore .sync dir
			if (local)
				info = getLocalFileAttr(path);
			else
				info = getAttr(server, path);
			if (!path.equals("/")) {
				fileList.add(path);
				fileInfos.put(path, info);
			} else
				path = "";
			if (!info.isFile) {
				String[] children;
				if (local)
					children = dirLocal(path);
				else
					children = dir(server, path);
				for (String s : children) {
					if (!s.equals(".sync")) { // ignore .sync dir
						if (local)
							info = getLocalFileAttr(path + "/" + s);
						else
							info = getAttr(server, path + "/" + s);
						if (!info.isFile) {
							recursiveGetFileData(server, path + "/" + s,
									fileList, fileInfos, local);
							fileInfos.put(path + "/" + s, info);
						} else {
							fileList.add(path + "/" + s);
							fileInfos.put(path + "/" + s, info);
						}
					}
				}
			} else {
				fileList.add(path);
				fileInfos.put(path, info);
			}
		}
	}

	/**
	 * Removes a directory and its content
	 * 
	 * @param dir
	 *            File object initialized with the directory path
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

	/**
	 * Removes the metainfo file of a specified file
	 * 
	 * @param filePath
	 *            path of local file that metainfo will be returned
	 * @param syncDir
	 *            File instance of .sync dir
	 * @param isFile
	 *            if it is a file; should be false if it is a directory
	 */
	private void removeFileMetaInfo(String filePath, File syncDir,
			boolean isFile) {
		// initial setup
		File dirInSyncDir = null;
		File fileInSyncDir = null;

		if (!isFile) {
			dirInSyncDir = new File(syncDir.getAbsolutePath() + "/" + filePath);
			if (!dirInSyncDir.exists()) {
				dirInSyncDir.mkdir();
			}
		} else {
			fileInSyncDir = new File(syncDir.getAbsolutePath() + "/" + filePath);
		}
		File metaInfoFile;
		if (!isFile) {
			metaInfoFile = new File(dirInSyncDir.getAbsolutePath());
			recursiveFilesRemove(metaInfoFile);
		} else {
			metaInfoFile = new File(fileInSyncDir.getAbsolutePath()
					+ ".metainfo");
			metaInfoFile.delete();
		}
	}

	/**
	 * Writes the metainfo of a file
	 * 
	 * @param filePath
	 *            path of local file which is the owner of the metainfo
	 * @param syncDir
	 *            File instance of .sync dir
	 * @param fileName
	 *            name of the file
	 * @param serverModifiedDate
	 *            last modified date of file in remote path
	 * @param localModifiedDate
	 *            last modified date if file in local path
	 * @param isFile
	 *            if it is a file; should be false if it is a directory
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	private void writeFileMetaInfo(String filePath, File syncDir,
			String fileName, String serverModifiedDate,
			String localModifiedDate, Boolean isFile) throws IOException {
		// initial setup
		File dirInSyncDir = null;
		File fileInSyncDir = null;

		if (!isFile) {
			dirInSyncDir = new File(syncDir.getAbsolutePath() + "/" + filePath);
			if (!dirInSyncDir.exists()) {
				dirInSyncDir.mkdir();
			}
		} else {
			fileInSyncDir = new File(syncDir.getAbsolutePath() + "/" + filePath);
		}

		// write metainfo
		File metaInfoFile;

		if (!isFile)
			metaInfoFile = new File(dirInSyncDir.getAbsolutePath()
					+ "/.metainfo");
		else
			metaInfoFile = new File(fileInSyncDir.getAbsolutePath()
					+ ".metainfo");

		FileOutputStream os = new FileOutputStream(metaInfoFile);
		JSONObject fileMetaInfoJSON = new JSONObject();
		// Metainfo to write
		fileMetaInfoJSON.put("title", fileName);
		fileMetaInfoJSON.put("serverLastModifiedDate", serverModifiedDate);
		fileMetaInfoJSON.put("localLastModifiedDate", localModifiedDate);
		fileMetaInfoJSON.put("isFile", isFile.toString());
		os.write(fileMetaInfoJSON.toJSONString().getBytes());
		os.flush();
		os.close();
	}

	/**
	 * Initializes the synchronization i.e. writes the initial meta info of
	 * files and gets the list of files in local and remote directory that will
	 * be synchronized
	 * 
	 * @param server
	 *            server to sync
	 * @param localPath
	 *            local path to sync
	 * @param remotePath
	 *            remote path to sync
	 * @return true if the initial setup was successfull; false otherwise
	 */
	private boolean syncInit(String server, String localPath, String remotePath) {
		File localDir = new File(localPath);
		if (!localDir.exists()) {
			localDir.mkdirs();
		}

		localSyncDirPath = localPath;
		remoteSyncDirPath = remotePath;

		if (localDir.list().length == 0) { // empty local sync dir
			File syncDir = new File(localPath + "/.sync");
			if (syncDir.exists()) {
				recursiveFilesRemove(syncDir); // remove old .sync
				syncDir.mkdirs();
			} else if (!syncDir.exists())
				syncDir.mkdirs();

			List<String> fileList = new LinkedList<String>();
			Map<String, FileInfo> fileInfos = new HashMap<String, FileInfo>();

			recursiveGetFileData(server, remotePath, fileList, fileInfos, false); // get
																					// server
																					// files
																					// data

			FileInfo tmpFileInfo;
			Date currentDate;
			try {
				File dir;
				for (String s : fileList) {
					if (!fileInfos.get(s).isFile) { // create a server directory
													// in local path
						dir = new File(localPath + "/" + s);
						dir.mkdirs();
						tmpFileInfo = fileInfos.get(s);
						currentDate = new Date();
						writeFileMetaInfo(s, syncDir, tmpFileInfo.name,
								tmpFileInfo.modified, currentDate.toString(),
								false);
					} else {
						currentDate = new Date();
						getFileFromServer(server, s, localPath + "/" + s); // download
																			// file
						tmpFileInfo = fileInfos.get(s);
						writeFileMetaInfo(s, syncDir, tmpFileInfo.name,
								tmpFileInfo.modified, currentDate.toString(),
								true);
					}
				}
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		else if (dir(server, remotePath).length == 0) { // empty remote sync dir
			File syncDir = new File(localPath + "/.sync");

			if (!syncDir.exists())
				syncDir.mkdirs();

			mkdir(server, remotePath);

			List<String> fileList = new LinkedList<String>();
			Map<String, FileInfo> fileInfos = new HashMap<String, FileInfo>();

			recursiveGetFileData(server, localPath, fileList, fileInfos, true); // get
																				// local
																				// files
																				// data

			FileInfo tmpFileInfo;
			FileInfo remoteTmpFileInfo;
			try {
				for (String s : fileList) {
					if (!fileInfos.get(s).isFile) {
						mkdir(server, remotePath + "/" + s); // create a local
																// directory in
																// remote path
						tmpFileInfo = fileInfos.get(s);
						remoteTmpFileInfo = getAttr(server, remotePath + "/"
								+ s);
						writeFileMetaInfo(s, syncDir, tmpFileInfo.name,
								remoteTmpFileInfo.modified,
								tmpFileInfo.modified, false);
					} else {
						sendFileToServer(s, server, remotePath + "/" + s); // upload
																			// file
						tmpFileInfo = fileInfos.get(s);
						remoteTmpFileInfo = getAttr(server, remotePath + "/"
								+ s);
						writeFileMetaInfo(s, syncDir, tmpFileInfo.name,
								remoteTmpFileInfo.modified,
								tmpFileInfo.modified, true);
					}
				}
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		} else {
			System.out.println("One of the dirs must be empty or don't exist!");
			return false;
		}
	}

	/**
	 * Returns the metainfo file of a local sync dir file
	 * 
	 * @param path
	 *            path of metainfo owner
	 * @param isFile
	 *            if it is a file; must be false if is a directory
	 * @return metainfo file of a local sync dir file
	 */
	private File getMetainfo(String path, boolean isFile) {
		File f;
		if (isFile)
			f = new File(localSyncDirPath + "/.sync/" + "/" + path
					+ ".metainfo");
		else
			f = new File(localSyncDirPath + "/.sync/" + path + "/.metainfo");
		return f;

	}

	/**
	 * Returns the files that had metainfo which were removed
	 * 
	 * @param metaInfoFileList
	 *            list of .metainfo files
	 * @param metaInfoFileInfos
	 *            list of .metainfo files FileInfo
	 * @return files that had metainfo which were removed
	 */
	private Set<Pair<String, Boolean>> localFilesOfMetaInfoRemoved(
			List<String> metaInfoFileList,
			Map<String, FileInfo> metaInfoFileInfos) {
		File f;
		String tmpDir;
		Set<Pair<String, Boolean>> removedFiles = new HashSet<Pair<String, Boolean>>();
		String p;
		for (String s : metaInfoFileList) {
			p = s.replaceAll("^" + localSyncDirPath + "/?", "");
			tmpDir = p.split(".sync\\\\")[1];
			tmpDir = localSyncDirPath + "/" + tmpDir.replaceAll("(/.metainfo|.metainfo)$", "");
			f = new File(tmpDir);
			if (!f.exists())
				removedFiles.add(new Pair<String, Boolean>(tmpDir.replaceAll("^" + localSyncDirPath + "/?", ""),
						metaInfoFileInfos.get(s).isFile));
		}

		return removedFiles;
	}

	/**
	 * Synchronizes the files that are queued
	 * 
	 * @param server
	 *            server to synchronize with
	 * @param fileInfos
	 *            FileInfo Map of local files
	 * @throws IOException
	 */
	private void syncQueuedFiles(String server, Map<String, FileInfo> fileInfos)
			throws IOException {
		Pair<String, Boolean> next;
		FileInfo tmpFileInfo;
		FileInfo remoteTmpFileInfo;
		File syncDir = new File(localSyncDirPath + "/.sync");

		// Download files
		File f;
		
		File tmp;
		boolean b;
		while (!queuedDownloadFiles.isEmpty()) {
			next = queuedDownloadFiles.poll();
			tmp = new File(localSyncDirPath + "/" + next.getFirst());
			b = tmp.exists();
			if (next.getSecond())
				getFileFromServer(server,
						!b ? remoteSyncDirPath + "/" + next.getFirst() : next.getFirst(),
						localSyncDirPath + "/" + (b ? "" : remoteSyncDirPath + "/") + next.getFirst());
			else {
				f = new File(localSyncDirPath + "/" + (b ? "" : remoteSyncDirPath + "/") + next.getFirst());
				f.mkdir();
			}
			Date currentDate = new Date();
			remoteTmpFileInfo = getAttr(server,
					!b ? remoteSyncDirPath + "/" + next.getFirst() : next.getFirst());
			writeFileMetaInfo(!b ? remoteSyncDirPath + "/" + next.getFirst() : next.getFirst(), syncDir, remoteTmpFileInfo.name,
					remoteTmpFileInfo.modified, currentDate.toString(),
					next.getSecond());
		}

		// Upload files
		while (!queuedUploadFiles.isEmpty()) {
			next = queuedUploadFiles.poll();
			if (!next.getFirst().equals("")) {
				if (next.getSecond())
					sendFileToServer(localSyncDirPath + "/" + next.getFirst(), server, next.getFirst());
				else
					mkdir(server, next.getFirst());
				
				tmpFileInfo = fileInfos.get(localSyncDirPath + "/" + next.getFirst());
				remoteTmpFileInfo = getAttr(server,
						next.getFirst());
				writeFileMetaInfo(next.getFirst(), syncDir, tmpFileInfo.name,
						remoteTmpFileInfo.modified, tmpFileInfo.modified,
						tmpFileInfo.isFile);
			}
		}

		// Removed local files
		while (!queuedLocalDirRemovedFiles.isEmpty()) {
			next = queuedLocalDirRemovedFiles.poll();
			if (next.getSecond())
				rm(server, next.getFirst());
			else
				rmdir(server, next.getFirst());
			removeFileMetaInfo(next.getFirst(), syncDir, next.getSecond());
		}

		// Removed remote files
		while (!queuedRemoteDirRemovedFiles.isEmpty()) {
			next = queuedRemoteDirRemovedFiles.poll();
			f = new File(localSyncDirPath + "/" + next.getFirst());
			if (next.getSecond())
				f.delete();
			else
				recursiveFilesRemove(f);
		
			removeFileMetaInfo(next.getFirst(), syncDir, next.getSecond());
		}

	}

	/**
	 * Checks if changes were made to the files in sync directories and puts the
	 * changes in queues
	 * 
	 * @param server
	 *            server to sync
	 * @throws java.text.ParseException
	 * @throws FileNotFoundException
	 * @throws ParseException
	 */
	private void checkChanges(String server) throws java.text.ParseException,
			FileNotFoundException, ParseException {
		File localDir = new File(localSyncDirPath);
		if (localDir.exists()) {
			File metaInfoDir = new File(localDir + "/.sync/" + remoteSyncDirPath);
			if (metaInfoDir.exists()) {
				// Get data of metainfo files
				List<String> metaInfoFileList = new LinkedList<String>();
				Map<String, FileInfo> metaInfoFileInfos = new HashMap<String, FileInfo>();
				recursiveGetFileData(null, metaInfoDir.getAbsolutePath(),
						metaInfoFileList, metaInfoFileInfos, true);

				// Get data of local sync dir files
				List<String> localFileList = new LinkedList<String>();
				Map<String, FileInfo> localFileInfos = new HashMap<String, FileInfo>();
				recursiveGetFileData(null, localSyncDirPath, localFileList,
						localFileInfos, true);

				// Get data of remote sync dir files
				List<String> remoteFileList = new LinkedList<String>();
				Map<String, FileInfo> remoteFileInfos = new HashMap<String, FileInfo>();
				recursiveGetFileData(server, remoteSyncDirPath, remoteFileList,
						remoteFileInfos, false);

				FileInfo serverFileInfo;

				JSONParser parser = new JSONParser();
				JSONObject jobj;
				Date metaInfoLocalModifiedDate;
				Date metaInfoRemoteModifiedDate;
				Date fileModifiedDate;
				SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT,
						Locale.US);
				Boolean isFile = false;

				File f;
				Scanner in;
				if (localFileList.size() < metaInfoFileList.size()) { // a local
																		// file
																		// was
																		// removed
					Set<Pair<String, Boolean>> removedFiles = new HashSet<Pair<String, Boolean>>();
					// get removed files
					removedFiles = localFilesOfMetaInfoRemoved(
							metaInfoFileList, metaInfoFileInfos);

					// add removed files to queued removed files
					queuedLocalDirRemovedFiles.addAll(removedFiles);
				}
				String p;
				for (String s : localFileList) {
					p = s.replaceAll("^" + localSyncDirPath + "/?", "");
					if ((f = getMetainfo(p, localFileInfos.get(s).isFile))
							.exists()) { // get metainfo file

						in = new Scanner(f);
						jobj = (JSONObject) parser.parse(in.nextLine());

						// Get local metainfo, remote metainfo and file modified
						// dates
						metaInfoLocalModifiedDate = dateFormat
								.parse((String) jobj
										.get("localLastModifiedDate"));
						metaInfoRemoteModifiedDate = dateFormat
								.parse((String) jobj
										.get("serverLastModifiedDate"));
						fileModifiedDate = dateFormat.parse(localFileInfos
								.get(s).modified);
						isFile = Boolean.valueOf((String) jobj.get("isFile"));
						// Get server file info
						serverFileInfo = getAttr(server, p);

						if (serverFileInfo == null) { // file was removed from
														// server
							queuedRemoteDirRemovedFiles
									.add(new Pair<String, Boolean>(p, isFile));
						} else {
							// check which file is newer
							if (dateFormat.parse(serverFileInfo.modified)
									.compareTo(metaInfoRemoteModifiedDate) != 0) {
						
								// file is newer in remote path
								queuedDownloadFiles
										.add(new Pair<String, Boolean>(p,
												isFile));
							} else if (metaInfoLocalModifiedDate
									.compareTo(fileModifiedDate) != 0) {
								// file is newer in local path
								queuedUploadFiles
										.add(new Pair<String, Boolean>(p,
												isFile));
							}
						}
						in.close();
					} else { // there's no metainfo so it is a new file
						queuedUploadFiles.add(new Pair<String, Boolean>(p,
								localFileInfos.get(s).isFile));
					}
				}

				// List new files in remote path and put it in download queue

				for (String s : remoteFileList) {
					if (!s.equals(remoteSyncDirPath)) {
						p = s.replaceAll("^" + remoteSyncDirPath + "/", "");
					
						if (!(f = getMetainfo(remoteSyncDirPath + "/" + p, remoteFileInfos.get(s).isFile))
								.exists()) { // get metainfo file
							queuedDownloadFiles.add(new Pair<String, Boolean>(
									p, remoteFileInfos.get(s).isFile));
						}
					}
				}
			}
		}
	}

	/**
	 * Synchronizes a local path with a remote path
	 * 
	 * @param localPath
	 *            local path to sync
	 * @param remotePath
	 *            remote path to sync
	 * @param server
	 *            server to sync
	 */
	public void sync(String localPath, String remotePath, String server) {
		try {
			File f = new File(localPath);
			if (!f.exists()) // If local sync dir was removed setup the sync
								// again
				firstSyncRun = true;
			else if (dir(server, remotePath).length == 0)
				firstSyncRun = true;

			if (firstSyncRun) {
				System.out.println("Starting synchronization of local dir "
						+ localPath + " with remote dir " + remotePath
						+ " on server " + server + "...");
				firstSyncRun = !syncInit(server, localPath, remotePath);
				if (!firstSyncRun) {
					localSyncDirPath = localPath;
					remoteSyncDirPath = remotePath;
					List<String> localFileList = new LinkedList<String>();
					Map<String, FileInfo> localFileInfos = new HashMap<String, FileInfo>();
					recursiveGetFileData(null, localSyncDirPath, localFileList,
							localFileInfos, true);
					checkChanges(server);
					syncQueuedFiles(server, localFileInfos);
					System.out.println("All files synched");
				}
			} else {

				System.out.println("Starting synchronization of local dir "
						+ localPath + " with remote dir " + remotePath
						+ " on server " + server + "...");
				localSyncDirPath = localPath;
				remoteSyncDirPath = remotePath;
				List<String> localFileList = new LinkedList<String>();
				Map<String, FileInfo> localFileInfos = new HashMap<String, FileInfo>();

				recursiveGetFileData(null, localSyncDirPath, localFileList,
						localFileInfos, true);
				checkChanges(server);
				syncQueuedFiles(server, localFileInfos);
				System.out.println("All files synched");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (java.text.ParseException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			System.out.println("There was an error while synching. Try again");
			firstSyncRun = true;
		}
	}
}
