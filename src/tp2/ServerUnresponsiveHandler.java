
package tp2;

import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;

import trab1.both.ws.DirServerWSService;
/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The ServerUnresponsiveHandler represents a handler to manage problems due to unresponsive servers
 */
public class ServerUnresponsiveHandler {

	private static final int MAX_FAILURES = 3;
	private static final String RMI = "rmi";
	private static final String WS = "ws";

	private Map<String,Integer> connectionsRefused;
	private Set<String> srvList;
	private Set<String> serverListReceived;
	private boolean client;
	private String username;
	private String pw;

	/**
	 * Constructs a ServerUnresponsiveHandler
	 * @param client 
	 */
	public ServerUnresponsiveHandler(boolean client,String username,String pw) {
		this.client = client;
		this.username = username;
		this.pw = pw;
		connectionsRefused = new HashMap<String, Integer>();
		srvList = null;
	}
	
	/**
	 * This method informs the user that no servers are available
	 */
	private void noServersAvailable(){
		System.out.println("No Servers Available!!");
		System.out.println("Client Will Exit!");
		System.exit(1);
	}

	/**
	 * This method receives a URL of a server and a list of servers and
	 * updates the total of failed attempts. When it reaches the 
	 * <code>MAX_FAILURES</code> value the method will try to warn other
	 * server that the server with url serverDown is down.
	 * @param serverDown - a url from a server that couldn't be contacted
	 * @param serverList - a list of servers known
	 */
	public void treatServerException(String serverDown, Set<String> serverList){
		System.out.println("Server with url \""+ serverDown+"\" not available!");
		if(!serverList.contains(serverDown)){
			System.out.println("This is an Unknown Server!");
			return;
		}
		if(serverList.size() == 1)
			noServersAvailable();
		this.serverListReceived = serverList;
		int val =1;
		if(connectionsRefused.containsKey(serverDown)){
			val= connectionsRefused.get(serverDown) + 1;
		}
		connectionsRefused.put(serverDown, val);
		System.out.println("Total Attempts: "+val);
		if(val == MAX_FAILURES){
			announceServerShutDown(serverDown, serverList);
			if(client)
				FileClient.needUpdate(serverDown);
		}
	}

	public Set<String> getList() {
		return srvList;
	}

	/**
	 * This method announces a server that this server is down
	 */
	private void announceServerShutDown(String serverUrl, Set<String> serverList) {

		connectionsRefused.remove(serverUrl);
		//this line could be useful because the method can be recursively called
		if(serverList.size() == 1 )
			noServersAvailable();

		Iterator<String> it = serverList.iterator();
		boolean done = false;
		String aux = null;
		List<String> ls = null;
		while(!done && it.hasNext()){
			try {
				aux = it.next();
				if(!aux.equals(serverUrl) || (connectionsRefused.containsKey(aux) && connectionsRefused.get(aux) != 3)){
					Pair<String,Object> p = getConnectionToServer(aux);
					if(p == null) continue;
					if(p.getFirst().equals("ws")){
						trab1.both.ws.DirServerWS server = (trab1.both.ws.DirServerWS) p.getSecond();
						ls = server.receiveShutdownAnnouncement(serverUrl);
					}
					else{
						IFileServer server = (IFileServer) p.getSecond();
						ls = server.receiveShutdownAnnouncement(serverUrl,username,pw);
					}

					srvList = new HashSet<String>();
					for(String s: ls){
						srvList.add(s);
					}
					done = true;
				}

			} catch(javax.xml.ws.WebServiceException e){
				treatServerException(aux, serverList);
			} catch (RemoteException e) {
				treatServerException(aux, serverList);
			} catch (InvalidCredentialsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	/**
	 * Establishes a connection to a server and returns it
	 * @param url url of the server to connect
	 * @return a Pair object with connection to a server
	 */
	private Pair<String, Object> getConnectionToServer(String url){
		try{
			Pair<String,Object> p = null;
			if(url.regionMatches(0, RMI, 0, 3)){
				IFileServer fileServer = (IFileServer) Naming
						.lookup(url);
				p = new Pair<String, Object>(RMI, fileServer);
			}
			else{
				DirServerWSService service = new DirServerWSService( new URL( url + "?wsdl"), 
						new QName("http://both.trab1/", "DirServerWSService"));
				trab1.both.ws.DirServerWS fileServer = service.getDirServerWSPort();
				p = new Pair<String, Object>(WS, fileServer);
			}
			return p;
		}catch(javax.xml.ws.WebServiceException e){
			treatServerException(url, serverListReceived);
		}catch (RemoteException e) {
			treatServerException(url, serverListReceived);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}
