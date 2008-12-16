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
	}

	@Test
	public void testHashMap2() {

		int nEntries = 1000;

		WideHashMap map = new WideHashMap(nEntries, 1.0f, Float.NEGATIVE_INFINITY);
		for (int i = 0; i < nEntries; i++) {
			int a = i;
			int b = i * 2;
			float c = i / 10.0f;
			float d = b / 10.0f;

			// test for no value being there
			Assert.assertEquals(Float.NEGATIVE_INFINITY, map.getValue1(5, 7));

			map.put(a, b, c, d);
			float value1 = map.getValue1(a, b);
			Assert.assertEquals(c, value1);
			float value2 = map.getValue2(a, b);
			Assert.assertEquals(d, value2);
//			System.out.println(a + " " + b + " " + c + " " + d);
		}

		// make sure we didn't overwrite any values
		for (int i = 0; i < nEntries; i++) {
			int a = i;
			int b = i * 2;
			float c = i / 10.0f;
			float d = b / 10.0f;

			float value1 = map.getValue1(a, b);
			Assert.assertEquals(c, value1);
			float value2 = map.getValue2(a, b);
			Assert.assertEquals(d, value2);
		}

		// now cause one too many entries and check for failure
		boolean except = false;
		try {
			map.put(nEntries, nEntries * 2, 0, 0);
		} catch (RuntimeException e) {
			except = true;
		}
		Assert.assertTrue("Exception was not fired when adding one too many entries.", except);
	}
}
