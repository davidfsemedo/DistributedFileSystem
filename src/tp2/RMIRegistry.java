package tp2;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * Class RMIRegistry. Launches a RMI registry in por 1099
 */
public class RMIRegistry {

	
	public static void main(String[] args) throws InterruptedException, RemoteException {
		LocateRegistry.createRegistry(1099);
		System.out.println("RMI registry running on port 1099");
		// Sleep forever
		Thread.sleep(Long.MAX_VALUE);

	}

}
