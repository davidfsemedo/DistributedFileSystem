package tp2;

import java.util.HashMap;
import java.util.Map;

public class FileMaintenance {
	
	private Map<String,Integer> filesBeingUsed;
	
	public FileMaintenance() {
		this.filesBeingUsed = new HashMap<String,Integer>();
	}
	
	public void incrementFileUsers(String key){
		if(filesBeingUsed.containsKey(key))
			filesBeingUsed.put(key, filesBeingUsed.get(key)+1);
		else {
			filesBeingUsed.put(key, 1);
		}
	}
	
	public boolean decrementFileUsers(String key){
		filesBeingUsed.put(key, filesBeingUsed.get(key)-1);
		if(filesBeingUsed.get(key) == 0){
			filesBeingUsed.remove(key);
			return true;
		}
		return false;
	}
	
	public int isAvailable(String key){
		//return !filesBeingUsed.containsKey(key);
		return filesBeingUsed.get(key);
	}

}
