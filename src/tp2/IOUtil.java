package tp2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;



/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The IOUtil class is an auxiliary class to read a file from the local directory to a byte array.
 */
public class IOUtil {

	/**
	 * Reads a file to a byte array
	 * @param file - file to be read
	 * @return an array of bytes with the file data or null otherwise
	 * @throws IOException
	 */
	public static byte[] readFile (File file) throws IOException {

		RandomAccessFile f = new RandomAccessFile(file, "r");

		try {

			long longlength = f.length();
			int length = (int) longlength;
			if (length != longlength) throw new IOException("File size >= 2 GB");


			byte[] data = new byte[length];
			f.readFully(data);
			return data;
		}
		finally {
			f.close();
		}
	}

}
