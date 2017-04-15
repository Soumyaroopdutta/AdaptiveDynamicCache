public interface Cache {
	// public enum priority {
	// LOW, MEDIUM, HIGH;
	// }

	public enum accessState {
		HIT, MISS, NONE;
	}

	public accessState get(String filename, int size);

	public void deleteEntry(String filename);
}