package info.jonclark.lang.hash;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * A hash map that can store wide keys (2 longs) and 2 values (floats) per table
 * entry. It assumes the user knows how many entries will be put into the hash
 * at time of creation.
 */
public class WideHashMap {

	// index 1 = hash value (even-odd rows form pairs)
	// index 2 = indicies of entries falling into the same bucket (collisions)
	private final boolean[] occupied;
	private final boolean[] collision;
	private final long[] keys;
	private final float[] values;
	private final float outOfHashValue;

	private int nElements;
	private long nCollisions = 0;

	public WideHashMap(int nElementsToPut, float loadFactor, float outOfHashValue) {

		this.outOfHashValue = outOfHashValue;

		int tableSize = (int) ((float) nElementsToPut / loadFactor);
		this.keys = new long[tableSize * 2];
		this.values = new float[tableSize * 2];
		this.occupied = new boolean[tableSize * 2];
		this.collision = new boolean[tableSize * 2];

		// now we have to pass around a parameter that tells whether
		// to do "alternate indexing"
		// alternate indexing is the process of pairing even and odd
		// entries (adjoining array entries offset by the low order bit)
		// so that we can store longer keys
	}

	// private static byte get8bits(byte[] data, int ptr) {
	// return data[ptr];
	// }
	//
	// private static short get16bits(byte[] data, int ptr) {
	// return (short) (data[ptr] | (data[ptr + 1] << 8));
	// }
	//
	// private static byte[] toBytes(long a, long b) {
	//
	// byte[] c = new byte[8 * 2];
	//
	// for (int j = 0; j < 4; j++) {
	// c[3 - j] = (byte) (a >>> (j * 8));
	// }
	// for (int j = 0; j < 4; j++) {
	// c[3 - j] = (byte) (b >>> (j * 8));
	// }
	//
	// return c;
	// }
	//
	// public int hash(long a, long b) {
	//
	// byte[] data = toBytes(a, b);
	// int ptr = 0;
	// int len = data.length;
	//
	// int hash = len, tmp;
	// int rem;
	//
	// if (len <= 0)
	// return 0;
	//
	// rem = len & 3;
	// len >>= 2;
	//
	// /* Main loop */
	// for (; len > 0; len--) {
	// hash += get16bits(data, ptr);
	// tmp = (get16bits(data, ptr + 2) << 11) ^ hash;
	// hash = (hash << 16) ^ tmp;
	// ptr += 2 * 2;
	// hash += hash >> 11;
	// }
	//
	// /* Handle end cases */
	// switch (rem) {
	// case 3:
	// hash += get16bits(data, ptr);
	// hash ^= hash << 16;
	// hash ^= get8bits(data, ptr + 2) << 18;
	// hash += hash >> 11;
	// break;
	// case 2:
	// hash += get16bits(data, ptr);
	// hash ^= hash << 11;
	// hash += hash >> 17;
	// break;
	// case 1:
	// hash += get8bits(data, ptr);
	// hash ^= hash << 10;
	// hash += hash >> 1;
	// }
	//
	// /* Force "avalanching" of final 127 bits */
	// hash ^= hash << 3;
	// hash += hash >> 5;
	// hash ^= hash << 4;
	// hash += hash >> 17;
	// hash ^= hash << 25;
	// hash += hash >> 6;
	//
	// return hash;
	//
	// }

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

	// public static int hash(long a, long b) {
	// int h = (int) (a ^ (a >>> 32 + 7) ^ b >>> 5 ^ (b >>> 32));
	// return h;
	// }

	private static int indexFor(int h, int keyArrayLength) {

		int nMaxEntries = keyArrayLength / 2;
		int inBoundsHash = h % nMaxEntries;

		// make sure we start on an even boundary and that the index is positive
		int firstIndex = inBoundsHash * 2;
		firstIndex = Math.abs(firstIndex);

		return firstIndex;
	}

	// static PrintWriter out;
	// static {
	// try {
	// out = new PrintWriter("/usr12/jhclark/log");
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// }
	// }

	public void put(long keyPart1, long keyPart2, float value1, float value2) {

		nElements++;

		int hash = hash(keyPart1, keyPart2);
		int firstIndex = indexFor(hash, keys.length);

		// out.println(keyPart1 + "" + keyPart2 + " " + hash + " " +
		// inBoundsHash + " " + firstIndex);

		// do linear probing, skipping over wide entries (pairs of positions)
		int tableIndex = firstIndex;
		while (occupied[tableIndex]) {

			collision[tableIndex] = true;
			nCollisions++;

			tableIndex += 2;
			tableIndex %= keys.length;
			if (tableIndex == firstIndex) {
				throw new RuntimeException(
						"Table full. User did not correctly predict number of entries.");
			}
		}

		occupied[tableIndex] = true;
		keys[tableIndex] = keyPart1;
		keys[tableIndex + 1] = keyPart2;
		values[tableIndex] = value1;
		values[tableIndex + 1] = value2;
	}

	public long getCollisionCount() {
		return nCollisions;
	}

	public int size() {
		return nElements;
	}

	private float get(long keyPart1, long keyPart2, int offset) {
		int hash = hash(keyPart1, keyPart2);
		int tableIndex = indexFor(hash, keys.length);

		while (occupied[tableIndex] && collision[tableIndex]) {
			if (keys[tableIndex] == keyPart1 && keys[tableIndex + 1] == keyPart2) {
				return values[tableIndex + offset];
			} else {
				tableIndex += 2;
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
