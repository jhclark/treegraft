package info.jonclark.treegraft.core;

import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.core.tokens.integer.IntegerToken;
import info.jonclark.treegraft.core.tokens.integer.IntegerTokenFactory;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class IntegerTokenTest {

	public void testTokenFactory() {
		// TODO: test for same tokens getting same ID
		// TODO: test for nonterminals and terminals getting different IDs
	}
	
	@Test
	public void testIntegerTokenSequence() {

		IntegerTokenFactory tokenFactory = new IntegerTokenFactory();
		List<String> start = Arrays.asList(new String[] { "a", "b", "c", "d", "e", "f", "g" });

		while (start.size() > 0) {

			TokenSequence<IntegerToken> startSequence = makeSeq(tokenFactory, start);
			String[] result = tokenFactory.getTokensAsStrings(startSequence.getTokens());
//			System.out.println("INIT: " + Arrays.toString(result));
			Assert.assertArrayEquals("Could not reproduce starting sequence", start.toArray(),
					result);

			for (int i = start.size(); i > 0; i--) {
				String[] expectedLeft = start.subList(0, i).toArray(new String[i]);
				String[] actualLeft =
						tokenFactory.getTokensAsStrings(startSequence.keepNLeftMostTokens(i).getTokens());
//				System.out.println("LEFT: " + Arrays.toString(actualLeft));
				Assert.assertArrayEquals(expectedLeft, actualLeft);

				String[] expectedRight =
						start.subList(start.size() - i, start.size()).toArray(new String[i]);
				String[] actualRight =
						tokenFactory.getTokensAsStrings(startSequence.keepNRightMostTokens(i).getTokens());
//				System.out.println("RIGHT: " + Arrays.toString(actualRight));
				Assert.assertArrayEquals(expectedRight, actualRight);
			}

			start = start.subList(0, start.size() - 1);
		}
	}

	private TokenSequence<IntegerToken> makeSeq(IntegerTokenFactory tokenFactory, List<String> s) {
		IntegerToken[] tokens = tokenFactory.makeTokens(s.toArray(new String[s.size()]), true);
		TokenSequence<IntegerToken> sequence =
				tokenFactory.makeTokenSequence(Arrays.asList(tokens));
		return sequence;
	}
}
