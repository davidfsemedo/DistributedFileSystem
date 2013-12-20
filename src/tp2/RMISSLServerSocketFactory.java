package tp2;


import java.io.*;
import java.net.*;
import java.rmi.server.*;
import javax.net.ssl.*;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The RMISSLServerSocketFactory class implements a RMIClientSocketFactory to support
 * comunications over SSL.
 */
public class RMISSLServerSocketFactory implements RMIServerSocketFactory {


	private SSLServerSocketFactory ssf = null;

	public RMISSLServerSocketFactory() throws Exception {
		try {
			ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public ServerSocket createServerSocket(int port) throws IOException {
		return ssf.createServerSocket(port);
	}

	public int hashCode() {
		return getClass().hashCode();
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return true;
	}
}
