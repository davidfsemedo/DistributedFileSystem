package tp2;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Set;

import javax.xml.namespace.QName;

import trab1.both.ws.DirServerWSService;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The ServerConnectionManager represents a manager of server connections
 * It can establish connections to RMI and WS servers and returns it
 */
public class ServerConnectionManager {

	private static final String RMI = "rmi";
	private static final String WS = "ws";
	public static final String DROPBOX = "dropbox";
	public static final String GOOGLEDRIVE = "googledrive";
	public static final String FLICKR = "flickr";

	private ServerUnresponsiveHandler servHandler;
	protected Set<String> serverList;

	/**
	 * Constructs a ServerConnectionManager
	 * @param servHandler server handler to use
	 * @param serverList list of known servers
	 */
	public ServerConnectionManager(ServerUnresponsiveHandler servHandler, Set<String> serverList) {
		this.servHandler = servHandler;
		this.serverList = serverList;
	}

	/**
	 * Establishes a connection to a server and returns it
	 * @param url url of the server to connect
	 * @return a Pair object with connection to a server
	 */
	public Pair<String, Object> getConnectionToServer(String url){

		try{
			Pair<String,Object> p = null;
			if(url.regionMatches(0, RMI, 0, 3)){
				IFileServer fileServer = (IFileServer) Naming
						.lookup(url);
				p = new Pair<String, Object>(RMI, fileServer);
			}
			else{
				DirServerWSService service = new DirServerWSService( new URL( url + "?wsdl"), 
						new QName("http://tp2/", "DirServerWSService"));

				trab1.both.ws.DirServerWS fileServer = service.getDirServerWSPort();
				p = new Pair<String, Object>(WS, fileServer);
			}
			return p;
		}catch(javax.xml.ws.WebServiceException e){
			servHandler.treatServerException(url, serverList);
		}catch (RemoteException e) {
			servHandler.treatServerException(url, serverList);
		} catch (MalformedURLException e) {
			System.out.println("Malformed url!");
		} catch (NotBoundException e) {
			System.out.println("Not bound");
		}
		return null;
	}

	/**
	 * Updates the server list
	 * @param serverList new server list
	 */
	public void updateServerList(Set<String> serverList) {
		this.serverList = serverList;
	}

	/**
	 * Checks if the given url belongs to a proxy server
	 * @param url - url to be checked
	 * @return true if it belongs or false otherwise
	 */
	public boolean isService(String url){
		return url.toLowerCase().contains(DROPBOX) || url.toLowerCase().contains(GOOGLEDRIVE)
				|| url.toLowerCase().contains(FLICKR);
	}

	/**
	 * Extracts the type of service of server with the given url
	 * @param url - url of the server 
	 * @return the service that the server represents
	 */
	public String getService(String url) {
		String aux = url.toLowerCase();
		if(aux.contains(DROPBOX))
			return DROPBOX;
		else if(aux.contains(GOOGLEDRIVE))
			return GOOGLEDRIVE;
		else if(aux.contains(FLICKR))
			return FLICKR;
		else return null;
	}
}
