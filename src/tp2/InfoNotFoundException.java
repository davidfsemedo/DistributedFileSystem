package tp2;


/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The InfoNotFoundException implements an exception representing files/dirs not found.
 */
public class InfoNotFoundException
		extends Exception
{
	private static final long serialVersionUID = 4063033701940593855L;

	public InfoNotFoundException( String message) {
		super( message);
	}
}
