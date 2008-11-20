package info.jonclark.lang.hash;

import junit.framework.Assert;

import org.junit.Test;

public class WideHashMapTest {
	@Test
	public void testHashMap() {
		WideHashMap map = new WideHashMap(10, 0.75f, Float.NEGATIVE_INFINITY);

		// test for no value being there
		Assert.assertEquals(Float.NEGATIVE_INFINITY, map.getValue1(5, 7));

		map.put(5, 7, 0.5f, 0.7f);
		Assert.assertEquals(0.5f, map.getValue1(5, 7));
		Assert.assertEquals(0.7f, map.getValue2(5, 7));

		// TODO: Try to cause collision
		// TODO: Detect when too many entries have been added
	}
}
