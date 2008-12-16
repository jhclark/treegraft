package info.jonclark.lang.trie;

import info.jonclark.util.StringUtils;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class TrieTest {

	private static class Key implements Gettable<String> {
		private List<String> a;

		public Key(List<String> a) {
			this.a = a;
		}

		public String get(int i) {
			return a.get(i);
		}

		public int size() {
			return a.size();
		}
	}

	@Test
	public void testTrie() {
		Trie<Key,String, Integer> trie = new Trie<Key, String, Integer>(100, 3);
		
		List<String> a = StringUtils.tokenizeToList("a");
		List<String> ab = StringUtils.tokenizeToList("a b");
		List<String> ax = StringUtils.tokenizeToList("a x");
		List<String> abc = StringUtils.tokenizeToList("a b c");
		List<String> abx = StringUtils.tokenizeToList("a b x");
		trie.put(new Key(a), 1);
		trie.put(new Key(ab), 2);
		trie.put(new Key(ax), 3);
		trie.put(new Key(abc), 4);
		trie.put(new Key(abx), 5);
		
		List<String> xxx = StringUtils.tokenizeToList("x x x");
		Integer value = trie.get(new Key(xxx));
		Assert.assertEquals(null, value);
		value = trie.get(new Key(a));
		Assert.assertEquals(new Integer(1), value);
		value = trie.get(new Key(ab));
		Assert.assertEquals(new Integer(2), value);
		value = trie.get(new Key(ax));
		Assert.assertEquals(new Integer(3), value);
		value = trie.get(new Key(abc));
		Assert.assertEquals(new Integer(4), value);
		value = trie.get(new Key(abx));
		Assert.assertEquals(new Integer(5), value);
	}
}
