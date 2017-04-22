public interface Cache {
	public enum accessState {
		HIT, MISS, NONE;
	}

	public accessState get(String filename, int size);
}