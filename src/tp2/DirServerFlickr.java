package tp2;


import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
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
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.FlickrApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The DirServerFlickr class implements a proxy to an online service: Flickr.
 * 
 * Flickr is an online service that allows an user to host and share photos.
 * An album contains one ore more pictures.
 * In the DirServer context, photos are treated like a file and albuns like a folder.
 * 
 */
public class DirServerFlickr extends UnicastRemoteObject implements IFileServer {



	private static final String USERNAME ="server";
	private static final String PASSWORD ="server";
	private static final long serialVersionUID = 1L;
	/*private static final String FLICKR_KEY = "b5e43234058a7cbf8952c411dd5cb8bc";
	private static final String FLICKR_SECRET = "2c45fe40eaa70d0e";*/
	private static final String FLICKR_KEY = "7d6f00db080c83d4e905b5575f0bb2e6";
	private static final String FLICKR_SECRET = "53c8de713b621490";

	private static final String PROTECTED_RESOURCE_URL = "http://api.flickr.com/services/rest/";
	private static final String FLICKR_FORMAT= "json";
	private static final String FLICKR_ALBUNS = "flickr.photosets.getList";
	private static final String FLICKR_USER_PHOTOS = "flickr.people.getPhotos";
	private static final String FLICKR_LIST_PHOTOS_FROM_ALBUM = "flickr.photosets.getPhotos";
	private static final String FLICKR_DELETE_PHOTO = "flickr.photos.delete";
	private static final String FLICKR_DELETE_ALBUM = "flickr.photosets.delete";
	private static final String FLICKR_CREATE_ALBUM = "flickr.photosets.create";

	private static OAuthService flickrService;	
	private static Token flickrToken;
	private static Token flickrAccessToken;


	protected static int CHUNK_SIZE = 32768;

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
	private static String SERVERTYPE = "flickr";
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

	public DirServerFlickr(String community, String id, String ip,
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

	/**
	 * Downloads an Image from a URL.
	 * @param path - Path of the Image.
	 * @param username - client username
	 * @return returns the downloaded file in a byte array in case of sucess or null otherwise
	 */
	private byte[] downloadFile(String path,String username){
		byte[] data = null;
		ImageDownloader img = new ImageDownloader();
		Pair<String,String> parsedPath = parserPath(path);
		Map<String,String> photos = getPhotos();
		if(photos.containsKey(parsedPath.getSecond())){
			String photoUrl = getPhotoURL(photos.get(parsedPath.getSecond()));

			data = img.saveImage(photoUrl, basePath, username, parsedPath.getSecond());
			
			if(data != null)
				CHUNK_SIZE = data.length;
		}
		return data;
	}



	/**
	 * Uploads a file
	 * @param fdata - the file to be uploaded in bytes
	 * @param path - the path of the file
	 * @param f - the File to be uploaded
	 * @param name - name of the file
	 * @return true on sucess or false otherwise
	 */
	private boolean uploadFile(byte[] fdata, String path,File f, String name) {

		/*Pair<String,String> parsedPath = parserPath(path);
		String id = null;

		OAuthRequest request = new OAuthRequest(Verb.POST, FLICKR_UPLOAD_PHOTO);
		request.addQuerystringParameter("title", "photo1");
		request.addBodyParameter("Content-length",""+f.length());
		request.addBodyParameter("Content-Type", "image/jpeg");
		System.out.println(request.getBodyContents());
		try {
			request.addPayload(IOUtil.readFile(f));
		} catch (IOException e) {
			return false;
		}
		flickrService.signRequest(flickrAccessToken, request);
		Response response = request.send();
		System.out.println(response.getHeaders());
		String jsonString = parseJsonResponse(response.getBody());
		System.out.println();
		if (response.getCode() == 200) {

		}*/

		System.out.println("Feature not Available!");
		return false;

	}

	/**
	 * Parses a json response from Flickr so that it can be used with the JSONParser
	 * @param jsonString - string in JSON format to be parsed
	 * @return the string parsed.
	 */
	private String parseJsonResponse(String jsonString){
		return jsonString.substring("jsonFlickrApi(".length(),jsonString.length()-1);
	}


	/**
	 * Parses a path given by the user. A pair (a,f) is constructed where
	 * a is a album (if the path has one directory) and f photo (it can be a album too).
	 * @param path - path to be parsed
	 * @return the resulting pair acoording to the description in case of sucess of
	 * null otherwise
	 */
	private Pair<String, String> parserPath(String path){
		String[] pathSplitted = path.split("/");
		Pair<String,String> pair;
		if(pathSplitted.length == 1){
			if(pathSplitted[0].equals(".") || pathSplitted[0].equals("") )
				pair = new Pair<String, String>(".", "");
			else
				pair = new Pair<String, String>("", pathSplitted[0]);
		}
		else if(pathSplitted.length == 2){
			pair = new Pair<String, String>(pathSplitted[0], pathSplitted[1]);
		}
		else if(pathSplitted.length == 3){
			pair = new Pair<String, String>(pathSplitted[1], pathSplitted[2]);
		}
		else
			return null;

		return pair;

	}

	/**
	 * Gets all the albums of the user.
	 * @return a Map <Name of Album, Metadata> with all albuns
	 */
	private Map<String,String> getAlbuns() {
		Map<String,String> albuns= new HashMap<String,String>();


		OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
		request.addQuerystringParameter("method", FLICKR_ALBUNS);
		request.addQuerystringParameter("format", FLICKR_FORMAT);
		flickrService.signRequest(flickrAccessToken, request);
		Response response = request.send();
		String jsonString = parseJsonResponse(response.getBody());
		if (response.getCode() == 200) {
			JSONParser parser = new JSONParser();
			JSONObject res;

			try {
				res = (JSONObject) parser.parse(jsonString);
				JSONArray items =(JSONArray) ((JSONObject) parser.parse(res.get("photosets").toString())).get("photoset");
				Iterator<?> it = items.iterator();
				while (it.hasNext()) {
					JSONObject file = (JSONObject) it.next();
					res = (JSONObject) parser.parse(file.toJSONString());
					String name = ((JSONObject)parser.parse(res.get("title").toString())).get("_content").toString();
					albuns.put(name,file.toJSONString());
				}
			} catch (ParseException e) {
				System.out.println("Parse Error!");
			}
			return albuns;
		}
		return null;
	}

	/**
	 * Gets all the photos of the user.
	 * @return a Map <Name of Photo, Metadata> with all the photos
	 */
	private Map<String,String> getPhotos() {
		Map<String,String> photos = new HashMap<String,String>();


		OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
		request.addQuerystringParameter("method", FLICKR_USER_PHOTOS);
		request.addQuerystringParameter("format", FLICKR_FORMAT);
		request.addQuerystringParameter("user_id", "me");
		request.addQuerystringParameter("extras", "last_update,url_l");
		flickrService.signRequest(flickrAccessToken, request);
		Response response = request.send();
		String jsonString = parseJsonResponse(response.getBody());
		if (response.getCode() == 200) {
			JSONParser parser = new JSONParser();
			JSONObject res;
			try {
				res = (JSONObject) parser.parse(jsonString);
				JSONArray items =(JSONArray) ((JSONObject) parser.parse(res.get("photos").toString())).get("photo");
				Iterator<?> it = items.iterator();
				while (it.hasNext()) {
					JSONObject file = (JSONObject) it.next();
					String s = file.toString();
					res = (JSONObject) parser.parse(s);
					photos.put(res.get("title").toString(), s);
				}
			} catch (ParseException e) {
				System.out.println("Parse Error!");
			}
			return photos;
		}
		return null;
	}


	/**
	 * Gets all the photos from a album
	 * @param albumId - the id of the album 
	 * @return a Map<Photoname, Metadata> with all the photos in the album
	 * with id albumId or null otherwise
	 */
	private Map<String,String> getPhotosFromAlbum(String albumId) {

		Map<String,String> photos = new HashMap<String,String>();

		OAuthRequest request = new OAuthRequest(Verb.GET, PROTECTED_RESOURCE_URL);
		request.addQuerystringParameter("method",FLICKR_LIST_PHOTOS_FROM_ALBUM);
		request.addQuerystringParameter("format", FLICKR_FORMAT);
		request.addQuerystringParameter("photoset_id", albumId);
		request.addQuerystringParameter("user_id", "me");
		request.addQuerystringParameter("extras", "last_update,url_l");
		flickrService.signRequest(flickrAccessToken, request);
		Response response = request.send();
		String jsonString = parseJsonResponse(response.getBody());
		if (response.getCode() == 200) {
			JSONParser parser = new JSONParser();
			JSONObject res;
			try {
				res = (JSONObject) parser.parse(jsonString);
				JSONArray items =(JSONArray) ((JSONObject) parser.parse(res.get("photoset").toString())).get("photo");
				Iterator<?> it = items.iterator();
				while (it.hasNext()) {
					JSONObject file = (JSONObject) it.next();
					String s = file.toString();
					res = (JSONObject) parser.parse(s);
					photos.put(res.get("title").toString(), s);
				}
			} catch (ParseException e) {
				System.out.println("Parse Error!");
			}
			return photos;
		}
		return null;
	}


	/**
	 * Builds a File info object from a string formatted in JSON
	 * @param name - Name of the file
	 * @param result - string formatted in JSON with file's metadata
	 * @param isPhoto - flag for photo
	 * @return A FileInfo objective of the file with name name or null
	 * otherwise
	 */
	private FileInfo buildFileInfo(String name ,String result,boolean isPhoto) {
		JSONParser parser = new JSONParser();
		JSONObject res;
		long length;
		String date;

		try {
			res = (JSONObject) parser.parse(result);
			
			if(!isPhoto){
				String epoch = res.get("date_update").toString();
				date =  new SimpleDateFormat("d MMM yyyy HH:mm:ss Z",Locale.US).format(new Date(Long.parseLong(epoch)*1000));
				length = Long.parseLong(res.get("photos").toString());
			}
			else{
				String epoch = res.get("lastupdate").toString();
				date =  new SimpleDateFormat("d MMM yyyy HH:mm:ss Z",Locale.US).format(new Date(Long.parseLong(epoch)*1000));
				length = 1;
			}

			FileInfo f = new FileInfo(name, length, date, isPhoto);
			return f;
		} catch (ParseException e) {
			System.out.println("Parse Error!");
		}
		return null;
	}


	/**
	 * Get a id of a album or photo from a json string
	 * @param data - a string in JSON
	 * @return the id in sucess or null otherwise
	 */
	private String getId(String data){
		JSONParser parser = new JSONParser();
		JSONObject res;
		try {
			res = (JSONObject) parser.parse(data);
			return res.get("id").toString();
		} catch (ParseException e) {
		}
		return null;
	}

	/**
	 * Get a url of a photo from a json string
	 * @param data - a string in JSON
	 * @return the url in sucess or null otherwise
	 */
	private String getPhotoURL(String data){
		JSONParser parser = new JSONParser();
		JSONObject res;
		try {
			res = (JSONObject) parser.parse(data);
			return res.get("url_l").toString();
		} catch (ParseException e) {
		}
		return null;
	}





	/* (non-Javadoc)
	 * @see tp2.IFileServer#dir(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public String[] dir(String path,String username,String pw) throws RemoteException,
	InfoNotFoundException, InvalidCredentialsException {

		if (validateUser(username, pw)) {
			List<String> dir = new LinkedList<String>();
			String result = null;
			String name = "";
			Map<String,String> albuns = null;
			Map<String,String> photos= null;
			Pair<String,String> parsedPath = parserPath(path);

			if(parsedPath.getFirst().equals(".") && parsedPath.getSecond().equals("")){
				albuns = getAlbuns();
				if(albuns != null){
					for(Entry<String, String> s: albuns.entrySet()){
						dir.add("Album: "+ s.getKey());
					}
				}
				photos= getPhotos();
				if(photos != null){
					for(Entry<String, String> s: photos.entrySet()){
						dir.add("Photo: "+s.getKey());
					}
				}
			}
			else if(parsedPath.getFirst().equals("")){
				albuns = getAlbuns();
				if(albuns.containsKey(parsedPath.getSecond())){
					name = parsedPath.getSecond();
					result = albuns.get(parsedPath.getSecond());
					String id = getId(result);
					Map<String, String> photoAlbum= getPhotosFromAlbum(id);
					if(photoAlbum != null){
						for(Entry<String, String> s: photoAlbum.entrySet()){
							dir.add("Photo: "+ s.getKey());
						}
					}
				}
				if(result == null){
					photos = getPhotos();
					if(photos.containsKey(parsedPath.getSecond())){
						name = parsedPath.getSecond();
						result = photos.get(parsedPath.getSecond());
						dir.add("Photo: " +name);
					}
				}
			}
			else {
				if(albuns == null)
					albuns = getAlbuns();
				String id = getId(albuns.get(parsedPath.getFirst()));
				
				Map<String, String> photoAlbum= getPhotosFromAlbum(id);
				if(photoAlbum.containsKey(parsedPath.getSecond())){
					name = parsedPath.getSecond();
					result = photoAlbum.get(parsedPath.getSecond());
				}

			}
			String[] listArray = new String[dir.size()];
			dir.toArray(listArray);

			return listArray;
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
			String result = null;
			String name = "";
			boolean isPhoto = false;
			Map<String,String> albuns = null;
			Map<String,String> photos= null;
			Pair<String,String> parsedPath = parserPath(path);

			if(parsedPath.getFirst().equals("")){
				albuns = getAlbuns();
				if(albuns.containsKey(parsedPath.getSecond())){
					name = parsedPath.getSecond();
					result = albuns.get(parsedPath.getSecond());
					isPhoto = false;
				}
				if(result == null){
					photos = getPhotos();
					if(photos.containsKey(parsedPath.getSecond())){
						name = parsedPath.getSecond();
						result = photos.get(parsedPath.getSecond());
						isPhoto= true;
					}
				}
			}
			else {
				if(albuns == null)
					albuns = getAlbuns();
				String id = getId(albuns.get(parsedPath.getFirst()));
				Map<String, String> photoAlbum= getPhotosFromAlbum(id);
				if(photoAlbum.containsKey(parsedPath.getSecond())){
					name = parsedPath.getSecond();
					result = photoAlbum.get(parsedPath.getSecond());
					isPhoto = true;
				}

			}

			if(result != null) {
				return buildFileInfo(name,result,isPhoto);
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
	InfoNotFoundException , InvalidCredentialsException{
		if (validateUser(username, pw)) 
			return downloadFile(path,username);
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
			File dir = new File(basePath,username);
			if(!dir.exists())
				dir.mkdir();

			File f = new File(dir.getAbsolutePath(),name);

			FileOutputStream fileout = new FileOutputStream(f, f.exists()
					&& startingByte > 0);

			BufferedOutputStream out = new BufferedOutputStream(fileout);
			out.write(fdata, 0, (int) fdata.length);
			out.close();
			if(f.length() == fileLength){
				uploadFile(fdata,path,f,name);
				f.delete();
				dir.delete();
			}
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
		System.out.println("Feature not Available!");
		return false;
	}




	/* (non-Javadoc)
	 * @see tp2.IFileServer#removeFile(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public boolean removeFile(String path,String username,String pw) throws RemoteException, InvalidCredentialsException  {
		if (validateUser(username, pw)) {

			Pair<String,String> parsedPath = parserPath(path);
			String id = null;
			if(parsedPath.getFirst().equals("")){
				Map<String,String> photos = getPhotos();
				if(photos.containsKey(parsedPath.getSecond())){
					id = getId(photos.get(parsedPath.getSecond()));
				}
			}
			else{
				Map<String,String> albuns = getAlbuns();
				if(albuns.containsKey(parsedPath.getFirst())){
					String idAlbum = getId(albuns.get(parsedPath.getFirst()));
					Map<String, String> photoAlbum= getPhotosFromAlbum(idAlbum);
					if(photoAlbum != null){
						if(photoAlbum.containsKey(parsedPath.getSecond()))
							id = getId(photoAlbum.get(parsedPath.getSecond()));
					}
				}
			}


			if(id!= null){
				OAuthRequest request = new OAuthRequest(Verb.POST, PROTECTED_RESOURCE_URL);
				request.addQuerystringParameter("method", FLICKR_DELETE_PHOTO);
				request.addQuerystringParameter("format", FLICKR_FORMAT);
				request.addQuerystringParameter("photo_id",id);
				flickrService.signRequest(flickrAccessToken, request);
				Response response = request.send();
				if (response.getCode() == 200) {
					return true;
				}
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

			Pair<String,String> parsedPath = parserPath(path);
			String id = null;
			Map<String,String> albuns = getAlbuns();
			if(albuns.containsKey(parsedPath.getSecond())){
				id = getId(albuns.get(parsedPath.getSecond()));
			}


			if(id!= null){
				OAuthRequest request = new OAuthRequest(Verb.POST, PROTECTED_RESOURCE_URL);
				request.addQuerystringParameter("method", FLICKR_DELETE_ALBUM);
				request.addQuerystringParameter("format", FLICKR_FORMAT);
				request.addQuerystringParameter("photoset_id",id);
				flickrService.signRequest(flickrAccessToken, request);
				Response response = request.send();
				if (response.getCode() == 200) {
					return true;
				}
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
			Pair<String,String> parsedPath = parserPath(path);

			String photoId = null;
			Map<String,String> photos = getPhotos();
			if(photos.size() > 0) 
				photoId = getId(photos.values().iterator().next());
			else return false;


			OAuthRequest request = new OAuthRequest(Verb.POST, PROTECTED_RESOURCE_URL);
			request.addQuerystringParameter("method", FLICKR_CREATE_ALBUM);
			request.addQuerystringParameter("format", FLICKR_FORMAT);
			request.addQuerystringParameter("title",parsedPath.getSecond());
			request.addQuerystringParameter("primary_photo_id",photoId);
			flickrService.signRequest(flickrAccessToken, request);
			Response response = request.send();
			if (response.getCode() == 200) {
				return true;
			}
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
		}
		else
			throw new InvalidCredentialsException();
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
			final DirServerFlickr server;

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



			flickrService = new ServiceBuilder()
			.provider(FlickrApi.class)
			.apiKey(FLICKR_KEY)
			.apiSecret(FLICKR_SECRET)
			.build();

			flickrToken = flickrService.getRequestToken();
			Scanner in = new Scanner(System.in);
			System.out.println("You need to get the authorization:");
			System.out.println(flickrService.getAuthorizationUrl(flickrToken)+ "&perms=delete");
			System.out.println("Paste the verifier here");
			System.out.print(">>");
			Verifier verifier = new Verifier(in.nextLine());
			System.out.println();
			System.out.println("Trading the Request Token for an Access Token...");
			flickrAccessToken = flickrService.getAccessToken(flickrToken, verifier);
			System.out.println("Got the Access Token!");
			System.out.println();


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


				server = new DirServerFlickr(community, id, ipAddress, basePath);
				Naming.rebind(server.getUrl(USERNAME,PASSWORD), server);
				if (p2.getFirst().equals(WS))
					anotherServerWS.newServerConnected(server.getUrl(USERNAME,PASSWORD));
				else {
					anotherServerRMI.newServerConnected(server.getUrl(USERNAME,PASSWORD));
					server.getUserDataBase();
				}
			} else {
				server = new DirServerFlickr(community, "1", ipAddress, basePath);
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
			//
		} catch (IOException e) {
			//
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