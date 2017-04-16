import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class TestCaches {
	String trace;

	float hits;
	float miss;

	public TestCaches(String trace) {
		this.trace = trace;
	}

	public void printInstructions() {
		System.err.println("The Traces are in the Format <FileName>,<Size>");
		System.err.println(
				"If a file comes in for the first time. It is a Cache Miss."
						+ "The Cache then Inserts the File  into the cache");
		System.err.println(
				"Next time the same file is accessed and the file is still is in Cache. "
						+ "Its a Cache HIT, else a Cache MISS");
		System.err.println(
				"IMPORTANT: 1) If the file is accessed with the same size -  Its a Normal Read.\n"
						+ "2)If Used with 0 - Delete the file from Cache \n"
						+ "3)If used with a different size. Delete the Previous Copy and Update with a new size. "
						+ "Consider it a HIT if in Cache.\n\n");
	}

	public void run_trace(Cache cacheobject) throws InterruptedException {
		BufferedReader br = null;
		FileReader fr = null;

		hits = 0;
		miss = 0;
		System.err.print("Starting the Tests Now.");
		Thread.sleep(300);
		System.err.print("...");
		Thread.sleep(300);
		System.err.print("...");
		Thread.sleep(300);
		System.err.println("...");
		Cache.accessState isHit;

		try {

			fr = new FileReader(trace);
			br = new BufferedReader(fr);

			String sCurrentLine;

			br = new BufferedReader(new FileReader(trace));

			while ((sCurrentLine = br.readLine()) != null) {
				String file = sCurrentLine.substring(0,
						sCurrentLine.indexOf(','));
				String size = sCurrentLine.substring(
						sCurrentLine.indexOf(',') + 1, sCurrentLine.length());

				System.err.println(file + " " + size);
				if ((isHit = cacheobject.get(file,
						Integer.parseInt(size))) == Cache.accessState.HIT) {
					hits++;
					System.out.println("HIT");
				} else if (isHit == Cache.accessState.MISS) {
					miss++;
					System.out.println("MISS");
				}

				// System.out.println(
				// file + " " + Integer.parseInt(size) + "-->" + isHit);
				Thread.sleep(500);
			}

			System.err
					.println("TEST ENDED----> HITS " + hits + " MISS " + miss);
			System.err.println("HIT Ratio --->" + hits / miss);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();

				if (fr != null)
					fr.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws InterruptedException {
		Scanner sc_input = new Scanner(System.in);
		System.out.println("Enter Size of Cache");
		int size = sc_input.nextInt();

		System.out.println("Enter trace name to run");
		String tracepath = null;
		while (sc_input.hasNextLine()) {
			tracepath = sc_input.nextLine();
			if (tracepath.equals("") == false)
				break;
		}

		System.err.println("trace path : " + tracepath);

		TestCaches test = new TestCaches(tracepath);
		test.printInstructions();
		Thread.sleep(500);

		BaseLRU cache = new BaseLRU(size);
		System.err.println("Running LRU FIRST");
		test.run_trace(cache);
		System.err.println("LRU RESULTS ^^^^^\n\n");

		System.err
				.println("Enter N to stop, anything else to measure Dynamic\n");
		while (sc_input.hasNextLine()) {
			tracepath = sc_input.nextLine();
			if (tracepath.equals("") == false)
				break;
		}
		sc_input.close();

		if (tracepath.equals("N") == true) {
			System.exit(0);
		}

		BaseLRU cache_dynamic = new BaseLRU(size);
		System.err.println("Running Dynamic Caching Performance");
		test.run_trace(cache_dynamic);
		System.err.println("Dynamic RESULTS ^^^^^\n\n");
	}
}
