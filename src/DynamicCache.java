import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class DynamicCache implements Cache {
	public enum Priority {
		BOTTOM, MIDDLE, TOP;
	}

	private int HISTORY_COUNT = 10;

	private HashMap<Priority, LinkedHashMap<String, cache_entry>> priority_cache;
	private HashMap<String, Priority> cache_priority;
	private LinkedHashMap<String, cache_entry> evicted_entries;

	private int total_size = 0;
	private int curr_size = 0;

	public DynamicCache(int size) {

		cache_priority = new HashMap<>();
		evicted_entries = new LinkedHashMap<>(HISTORY_COUNT, .75F, false);

		LinkedHashMap<String, cache_entry> top_cache;
		LinkedHashMap<String, cache_entry> middle_cache;
		LinkedHashMap<String, cache_entry> bottom_cache;

		top_cache = new LinkedHashMap<>(10000, .75F, true);
		middle_cache = new LinkedHashMap<>(10000, .75F, true);
		bottom_cache = new LinkedHashMap<>(10000, .75F, true);

		priority_cache = new HashMap<>();
		priority_cache.put(Priority.TOP, top_cache);
		priority_cache.put(Priority.MIDDLE, middle_cache);
		priority_cache.put(Priority.BOTTOM, bottom_cache);

		total_size = size;
	}

	@Override
	public accessState get(String filename, int size) {
		Priority entryPriority = Priority.MIDDLE;

		/* if the element is not already in the cache */
		if (cache_priority.containsKey(filename) == false) {
			cache_entry entry = null;

			if (evicted_entries.containsKey(filename) == true) {
				entry = evicted_entries.remove(filename);
				entryPriority = entry.priority;
			}

			if (entry == null) {
				entry = new cache_entry(size, entryPriority);
			}

			entry.AccessCount++;
			entry.lastaccess = System.currentTimeMillis();

			if (curr_size + size > total_size) {
				if (free_space_incache(entry) == false) {
					evicted_entries.put(filename, entry);
					return accessState.MISS;
				}
			}

			cache_priority.put(filename, entryPriority);

			priority_cache.get(entryPriority).put(filename, entry);
			curr_size += size;
			return accessState.MISS;
		}

		/* if the element is already in the cache */
		entryPriority = cache_priority.get(filename);

		if (priority_cache.get(entryPriority).get(filename).size != size) {
			deleteEntry(filename, entryPriority);

			if (size != 0) {
				get(filename, size);
			}

			return accessState.NONE;
		}

		priority_cache.get(entryPriority).get(filename).lastaccess = System
				.currentTimeMillis();
		priority_cache.get(entryPriority).get(filename).AccessCount++;

		System.err.println("Curr Size " + curr_size);
		return accessState.HIT;
	}

	private boolean free_space_incache(cache_entry entry) {
		while (entry.size + curr_size > total_size) {
			/* Look at the caches up priority, staring from bottom */
			if (priority_cache.get(Priority.BOTTOM).isEmpty() == false) {
				if (deleteEntry(entry, Priority.BOTTOM) == false) {
					break;
				}
				continue;
			} else if (priority_cache.get(Priority.MIDDLE).isEmpty() == false) {
				if (deleteEntry(entry, Priority.MIDDLE) == false) {
					break;
				}
				continue;
			} else if (priority_cache.get(Priority.TOP).isEmpty() == false) {
				if (deleteEntry(entry, Priority.TOP) == false) {
					break;
				}
				continue;
			}

			break;
		}

		if (entry.size + curr_size > total_size) {
			return false;
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	public boolean deleteEntry(cache_entry forentry, Priority entryPriority) {
		String filename = null;

		/**
		 * if the priority of being inserted is same as the eviction bucket,
		 * Only evict if the access count is more than the one being evicted
		 */
		if (forentry.priority == entryPriority) {
			LinkedHashMap<String, cache_entry> list2 = new LinkedHashMap<>();
			list2 = (LinkedHashMap<String, cache_entry>) priority_cache
					.get(entryPriority).clone();
			List<String> keylist = new ArrayList<>(list2.keySet());

			/* look for at least one which satisfies this requirement */
			for (String file : keylist) {
				if (forentry.AccessCount > list2.get(file).AccessCount) {
					filename = file;
					break;
				}
			}

			if (filename == null) {
				return false;
			}

			deleteEntry(filename, entryPriority);
		} /***
			 * if the eviction bucket is lower in priority than the new entries
			 * priority, or the priority is Bottom (where the insertion is by
			 * LRU), just evict he LRU entry
			 */
		else if (entryPriority == Priority.BOTTOM
				|| (entryPriority == Priority.MIDDLE
						&& forentry.priority == Priority.TOP)) {
			List<String> keylist = new ArrayList<>(
					priority_cache.get(entryPriority).keySet());

			deleteEntry(priority_cache.get(entryPriority).get(keylist.get(0)),
					entryPriority);
		}

		return true;
	}

	public void deleteEntry(String filename, Priority entryPriority) {
		cache_entry entry = priority_cache.get(entryPriority).remove(filename);
		curr_size -= entry.size;
		evicted_entries.put(filename, entry);
	}

	public class cache_entry {
		int size;
		Priority priority;
		long lastaccess;

		int AccessCount;

		public cache_entry(int size, Priority priority) {
			this.size = size;
			this.lastaccess = System.currentTimeMillis();
			this.priority = priority;
			this.AccessCount = 0;
		}
	}
}
