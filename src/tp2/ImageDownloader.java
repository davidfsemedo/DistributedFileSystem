package tp2;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * @author Alexandre Martins Garcia, no. 34625
 * @author David Fernandes Semedo, no. 35133
 * 
 * The ImageDownloader class downloads an image from a URL using a temporary folder. The
 * name of the folder is the username to avoid concorrency problems.
 */
public class ImageDownloader
{      

	/**
	 * Downloads an image from a URL
	 * @param imageUrl - url of the image
	 * @param basePath - basepath of the server
	 * @param username - username of the user
	 * @param fileName - filename to save the picture
	 * @return a byte array with the image data
	 */
	public byte[] saveImage(String imageUrl,File basePath,String username,String fileName) {
		try {
			File dir = new File(basePath,username);
			if(!dir.exists())
				dir.mkdir();
			System.out.println(dir.canWrite());
			URL url;
			File f = new File(dir.getAbsolutePath(),fileName);
			url = new URL(imageUrl);

			InputStream is = url.openStream();
			OutputStream os = new FileOutputStream(f);

			byte[] b = new byte[DirServerFlickr.CHUNK_SIZE];
			int length;

			while ((length = is.read(b)) != -1) {
				os.write(b, 0, length);
			}

			is.close();
			os.close();
			
			
			
			byte[] fdata = IOUtil.readFile(f);
			f.delete();
			dir.delete();

			return fdata;
			
		} catch (MalformedURLException e) {
			//
		} catch (IOException e) {
			//
		}
		return null;
	}

    

}
