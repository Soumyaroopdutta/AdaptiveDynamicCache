import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * This implementation doesn't yet test parallel execution of the cache. For
 * this implementation we can assume that the execution would be 1 access per
 * sec or any unit desired and the Rebalancer Ticks can therefore be tweaked
 * accrodingly.
 *
 * But, the performance could be assumed to be similar, or better as the number
 * of accesses in the given time would be much better.
 */
public class DynamicCache implements Cache {
	public enum Priority {
		BOTTOM, MIDDLE, TOP;
	}

	private final int HISTORY_COUNT = 10; // number of entries in evicted table.

	private final int DEMOTION_CYCLE = 30; // 30sec or 30 accesses this case.
	private final int PROMOTION_CYCLE = 10; // 10sec or 10 accesses this case.
	private final int PROMOTION_CRITERIA = 3; // 3 asses in current cycle.
	private long fake_ticks = 0;

	private HashMap<Priority, LinkedHashMap<String, cache_entry>> priority_cache;
	private HashMap<String, Priority> cache_priority;
	private LinkedHashMap<String, cache_entry> evicted_entries;

	private int total_size = 0;
	private int curr_size = 0;

	public DynamicCache(int size) {

		cache_priority = new HashMap<>();
		evicted_entries = new LinkedHashMap<>(HISTORY_COUNT, .75F, true);

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

	@SuppressWarnings("unchecked")
	private void demoteEntries() {
		/* check the MIDDLE tier entries */
		LinkedHashMap<String, cache_entry> tempList = new LinkedHashMap<>();
		tempList = (LinkedHashMap<String, cache_entry>) priority_cache
				.get(Priority.MIDDLE).clone();
		List<String> keylist = new ArrayList<>(tempList.keySet());

		/* look for at least one which satisfies this requirement */
		for (String file : keylist) {
			if (fake_ticks - tempList.get(file).lastaccess < DEMOTION_CYCLE) {
				break;
			}

			cache_entry entry = priority_cache.get(Priority.MIDDLE)
					.remove(file);
			cache_priority.put(file, Priority.BOTTOM);
			entry.priority = Priority.BOTTOM;
			priority_cache.get(Priority.BOTTOM).put(file, entry);
		}

		/* check the TOP entries */
		tempList = new LinkedHashMap<>();
		tempList = (LinkedHashMap<String, cache_entry>) priority_cache
				.get(Priority.TOP).clone();
		keylist = new ArrayList<>(tempList.keySet());

		/* look for at least one which satisfies this requirement */
		for (String file : keylist) {
			if (fake_ticks - tempList.get(file).lastaccess < DEMOTION_CYCLE) {
				break;
			}

			cache_entry entry = priority_cache.get(Priority.TOP).remove(file);
			cache_priority.put(file, Priority.MIDDLE);
			entry.priority = Priority.MIDDLE;
			priority_cache.get(Priority.MIDDLE).put(file, entry);
		}

	}

	private void checkifPeriodEnd(String file, Priority priority) {
		if ((fake_ticks / PROMOTION_CYCLE) != (priority_cache.get(priority)
				.get(file).lastaccess / PROMOTION_CYCLE)) {
			System.err.println("Clearing out Acount of " + file);
			priority_cache.get(priority).get(file).AccessCount = 0;
		} else {
			System.err.println("last access = "
					+ (priority_cache.get(priority).get(file).lastaccess)
					+ " curr ticks " + fake_ticks);
		}
	}

	private void promoteEntry(String file, Priority priority) {
		/*
		 * check if the last access time was not in this Promotion Cycle. If
		 * not, Clear out the Access Count number.
		 */
		checkifPeriodEnd(file, priority);
		priority_cache.get(priority).get(file).lastaccess = fake_ticks;
		priority_cache.get(priority).get(file).AccessCount++;
		/*
		 * check if the Access Count is greater than the criteria for promotion.
		 * If it satisfies, then promote it to the higher level.
		 */
		if (priority_cache.get(priority)
				.get(file).AccessCount >= PROMOTION_CRITERIA) {
			if (priority == Priority.TOP) {
				return; // can't be promoted anymore.
			} else {
				cache_entry entry = priority_cache.get(priority).remove(file);
				System.err.println("Promoting Entry from " + priority
						+ " to higher bucket ");
				if (priority == Priority.MIDDLE) {
					priority_cache.get(Priority.TOP).put(file, entry);
					cache_priority.put(file, Priority.TOP);
				} else {
					priority_cache.get(Priority.MIDDLE).put(file, entry);
					cache_priority.put(file, Priority.MIDDLE);
				}
			}
		}
	}

	@Override
	public accessState get(String filename, int size) {
		Priority entryPriority = Priority.MIDDLE;

		/*
		 * Using fake timer instead of a real timer, for controlled execution.
		 * Given the single threaded usage in this case. It won't effect much
		 * with the continuous traces being run.
		 */
		fake_ticks++;
		if (fake_ticks % DEMOTION_CYCLE == 0) {
			demoteEntries();
		}

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

			entry.lastaccess = fake_ticks;
			entry.AccessCount++;

			if (curr_size + size > total_size) {
				if (free_space_incache(entry) == false) {
					if ((fake_ticks / PROMOTION_CYCLE) != (entry.lastaccess
							/ PROMOTION_CYCLE)) {
						System.err
								.println("Clearing out Acount of " + filename);
						entry.AccessCount = 0;
					}
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

		promoteEntry(filename, entryPriority);

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

		System.err.println("Freed up space in cache. Now size " + curr_size);
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
					System.err.println("evicting " + file + " with Acount "
							+ forentry.AccessCount + " > "
							+ list2.get(file).AccessCount);
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
		cache_priority.remove(filename);
		System.err.println("Removed  " + filename + " from cache");
	}

	public class cache_entry {
		int size;
		Priority priority;
		long lastaccess;

		int AccessCount;

		public cache_entry(int size, Priority priority) {
			this.size = size;
			this.lastaccess = fake_ticks;
			this.priority = priority;
			this.AccessCount = 0;
		}
	}
}
