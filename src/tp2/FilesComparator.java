package tp2;

import java.util.Comparator;

/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The FilesComparator class represents a comparator to sort files/directories by name
 */
public class FilesComparator implements Comparator<String> {


	@Override
	public int compare(String o1, String o2) {
		if (o1.compareTo(o2) > 0)
			return 1;
		else if (o1.compareTo(o2) == 0)
			return 0;
		else
			return -1;
	}

}
