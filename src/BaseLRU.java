import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class BaseLRU implements Cache {
	private int total_size;
	private int curr_usage;

	private LinkedHashMap<String, Integer> cache;

	public BaseLRU(int size) {
		cache = new LinkedHashMap<>(10000, .75F, false);

		total_size = size;
		curr_usage = 0;
	}

	@Override
	public accessState get(String filename, int size) {

		if (cache.containsKey(filename) == true) {
			int file_size = cache.get(filename);

			if (size != file_size) {
				curr_usage -= file_size;
				cache.remove(filename);

				if (size == 0) {
					return Cache.accessState.NONE;
				} else {
					get(filename, size);
				}
			}

			return Cache.accessState.HIT;
		}

		/* make space if the cache size is smaller */
		if (curr_usage + size > total_size) {
			free_space_incache(size);
		}

		cache.put(filename, size);
		curr_usage += size;

		return Cache.accessState.MISS;
	}

	private void free_space_incache(int size) {
		List<String> keylist = new ArrayList<>(this.cache.keySet());
		int i = 0;

		while (curr_usage + size > total_size) {
			if (cache.isEmpty() == true) {
				break;
			}

			int remove_size = cache.remove(keylist.get(i));

			curr_usage -= remove_size;
			System.err.println("removed entry " + keylist.get(0) + " of size "
					+ remove_size);
			System.err.println("Current Size " + curr_usage + ". present size "
					+ total_size);
			i++;
		}
	}

	@Override
	public void deleteEntry(String filename) {
		// TODO Auto-generated method stub

	}
}
