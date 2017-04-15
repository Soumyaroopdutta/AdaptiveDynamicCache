import java.util.LinkedHashMap;

public class BaseLRU implements Cache {
	private int size;
	private LinkedHashMap<String, Integer> cache;

	public BaseLRU(int size) {
		cache = new LinkedHashMap<>();
		this.size = size;
	}

	@Override
	public accessState get(String filename, int size) {
		// TODO Auto-generated method stub
		return Cache.accessState.HIT;
	}

	@Override
	public void deleteEntry(String filename) {
		// TODO Auto-generated method stub

	}
}
