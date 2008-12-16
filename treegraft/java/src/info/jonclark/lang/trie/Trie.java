package info.jonclark.lang.trie;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Trie<K extends Gettable<T>, T extends Comparable<? super T>, V> implements TrieChild {

	// within comparable:
	// 1) navigate for first token
	// 2) navigate for second token
	// 4) navigate for nth token

	private final List<T> keys;
	private final List<Object> values;
	
	private static int defaultSize;
	private static int maxKeyLength;
	private V internalValue;

	/**
	 * HACK HACK HACK HACK
	 * XXX: maxKeyLength is STATIC
	 * 
	 * @param listSize
	 * @param maxKeyLength
	 */
	public Trie(int listSize, int maxKeyLength) {
		Trie.maxKeyLength = maxKeyLength;
		Trie.defaultSize = listSize;
		this.keys = new ArrayList<T>(listSize);
		this.values = new ArrayList<Object>(listSize);
	}

	public void put(K key, V value) {
		assert key.size() > 0 : "Key must contain at least one element.";
		put(key, value, 0);
	}

	// place elements by recursive descent
	private void put(K key, V value, int i) {
		T elem = key.get(i);
		int pos = Collections.binarySearch(keys, elem);
		if (i == key.size() - 1) {

			// we are at a key leaf

			if (pos >= 0) {
				if (i == maxKeyLength - 1) {
					// just add value as leaf to save memory
					// it's already in the list, so just reset it
					values.set(pos, value);
				} else {
					// otherwise we need to reset the internal value of the trie
					// containing its
					Trie<K, T, V> child = new Trie<K, T, V>(defaultSize, maxKeyLength);
					child.internalValue = value;
				}
			} else {
				// not in list yet
				// get the insertion point
				pos = -pos - 1;
				keys.add(pos, elem);

				// are we at a maximal leaf?
				if (i == maxKeyLength - 1) {
					// just add value to save memory
					values.add(pos, value);
				} else {
					// otherwise put it inside a trie
					Trie<K, T, V> child = new Trie<K, T, V>(defaultSize, maxKeyLength);
					child.internalValue = value;
					values.add(pos, child);
				}
			}

		} else {

			// we are not at a key leaf yet

			Trie<K, T, V> child;
			if (pos >= 0) {
				// found it
				child = (Trie) values.get(pos);
			} else {
				// not in list yet
				child = new Trie<K, T, V>(defaultSize, maxKeyLength);
				// get the insertion point
				pos = -pos - 1;
				keys.add(pos, elem);
				values.add(pos, child);
			}
			child.put(key, value, i + 1);
		}
	}

	public V get(K key) {
		return get(key, 0);
	}

	private V get(K key, int i) {
		T elem = key.get(i);
		int pos = Collections.binarySearch(keys, elem);
		if (pos < 0) {
			// not found
			return null;
		} else {
			if (i == key.size() - 1) {

				// we are at a key leaf
				if (i == maxKeyLength - 1) {
					// we are at a maximal leaf
					// the value is stored directly
					// in the table to save memory
					V value = (V) values.get(pos);
					return value;
				} else {
					// the value is stored inside the trie
					Trie<K, T, V> child = (Trie) values.get(pos);
					return child.internalValue;
				}
			} else {
				// we are not yet at a key leaf
				Trie<K, T, V> child = (Trie) values.get(pos);
				return child.get(key, i + 1);
			}
		}
	}
}
