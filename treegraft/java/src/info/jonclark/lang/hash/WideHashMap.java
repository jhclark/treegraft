package info.jonclark.lang.hash;

/**
 * A hash map that can store wide keys (2 longs) and 2 values (floats) per table
 * entry. It assumes the user knows how many entries will be put into the hash
 * at time of creation.
 */
public class WideHashMap {

	// index 1 = hash value (even-odd rows form pairs)
	// index 2 = indicies of entries falling into the same bucket (collisions)
	private final boolean[] occupied;
	private final long[] keys;
	private final float[] values;
	private final float outOfHashValue;

	public WideHashMap(int nElementsToPut, float loadFactor, float outOfHashValue) {

		this.outOfHashValue = outOfHashValue;

		int tableSize = (int) ((float) nElementsToPut / loadFactor);
		this.keys = new long[tableSize * 2];
		this.values = new float[tableSize * 2];
		this.occupied = new boolean[tableSize * 2];

		// now we have to pass around a parameter that tells whether
		// to do "alternate indexing"
		// alternate indexing is the process of pairing even and odd
		// entries (adjoining array entries offset by the low order bit)
		// so that we can store longer keys
	}

	private static int hash(long a, long b) {
		int h = (int) (a ^ (a >>> 32) ^ b ^ (b >>> 32));

		// taken from java.util.HashMap:
		// This function ensures that hashCodes that differ only by
		// constant multiples at each bit position have a bounded
		// number of collisions (approximately 8 at default load factor).
		h ^= (h >>> 20) ^ (h >>> 12);
		h ^= (h >>> 7) ^ (h >>> 4);

		// take the absolute value
		h = (h < 0) ? -h : h;

		return h;
	}

	public void put(long keyPart1, long keyPart2, float value1, float value2) {

		int hash = hash(keyPart1, keyPart2);
		int inBoundsHash = hash % keys.length;

		// make sure we start on an even boundary
		int firstBucket = inBoundsHash & 0xFFFFFFFE;

		// do linear probing, skipping over wide entries (pairs of positions)
		int bucket = firstBucket;
		while (occupied[bucket]) {
			bucket += 2;
			bucket %= keys.length;
			if (bucket == firstBucket) {
				throw new RuntimeException(
						"Table full. User did not correctly predict number of entries.");
			}
		}

		occupied[bucket] = true;
		keys[bucket] = keyPart1;
		keys[bucket + 1] = keyPart2;
		values[bucket] = value1;
		values[bucket + 1] = value2;
	}

	private float get(long keyPart1, long keyPart2, int offset) {
		int bucket = hash(keyPart1, keyPart2) % keys.length;

		// make sure we start on an even boundary
		bucket &= 0xFFFFFFFE;

		while (occupied[bucket]) {
			if (keys[bucket] == keyPart1 && keys[bucket + 1] == keyPart2) {
				return values[bucket + offset];
			} else {
				bucket += 2;
			}
		}
		return outOfHashValue;
	}

	public float getValue1(long keyPart1, long keyPart2) {
		return get(keyPart1, keyPart2, 0);
	}

	public float getValue2(long keyPart1, long keyPart2) {
		return get(keyPart1, keyPart2, 1);
	}
}
