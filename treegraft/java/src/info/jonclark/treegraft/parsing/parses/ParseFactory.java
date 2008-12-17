package info.jonclark.treegraft.parsing.parses;

import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.ArrayList;
import java.util.List;

/**
 * Responsible for creating parses and appending <s> and </s> markers to the
 * target tokens where appropriate.
 * 
 * @author jon
 */
public class ParseFactory<R extends GrammarRule<T>, T extends Token> {

	private final T bos;
	private final T eos;
	private final int inputLength;
	private final Transducer<R, T> transducer;

	public ParseFactory(TokenFactory<T> tokenFactory, Transducer<R, T> transducer, int inputLength) {
		this.bos = tokenFactory.makeToken("<s>", true);
		this.eos = tokenFactory.makeToken("</s>", true);
		this.inputLength = inputLength;
		this.transducer = transducer;
	}

	/**
	 * Create a new <code>Parse</code> from a non-terminal node
	 * 
	 * @param sourceInputTokens
	 *            The FULL source input sequence
	 * @param lispTree
	 */
	public Parse<T> createParse(List<T> sourceInputTokens, int startIndex, int endIndex,
			T sourceLhs, T targetLhs, T[] sourceRhs, T[] targetRhs, FeatureScores scores) {

		return new Parse<T>(sourceInputTokens, startIndex, endIndex, sourceLhs, targetLhs,
				sourceRhs, targetRhs, scores);
	}

	// /**
	// * Create a new <code>Parse</code> from a source terminal.
	// *
	// * @param sourceInputTokens
	// * @param startIndex
	// * @param endIndex
	// * @param sourceTerminal
	// * @param scores
	// */
	// public Parse<T> createParse(List<T> sourceInputTokens, int startIndex,
	// int endIndex,
	// T sourceTerminal, FeatureScores scores) {
	//
	// return new Parse<T>(sourceInputTokens, startIndex, endIndex,
	// sourceTerminal, scores);
	// }

	/**
	 * Create a new <code>Parse</code> from a target terminal.
	 * 
	 * @param sourceInputTokens
	 * @param targetTerminal
	 * @param scores
	 */
	public Parse<T> createParse(int sourceTokenStart,
			int sourceTokenEnd, List<T> sourceInputTokens, T targetTerminal, R parentRule,
			int targetRhsIndex) {

		List<T> targetTokens = new ArrayList<T>(2);

		boolean firstTerminal = targetRhsIndex == 0;
		boolean lastTerminal = transducer.transduceRhs(parentRule).length - 1 == targetRhsIndex;

		// add beginning of sentence marker
		if (sourceTokenStart == 0 && firstTerminal) {
			targetTokens.add(bos);
		}

		// HACK HACK HACK HACK HACK HACK HACK HACK
		// String strTok = targetTerminal.toString();
		// if(Character.isUpperCase(strTok.charAt(0))) {
		// targetTerminal = tokenFactory.makeToken(strTok.toLowerCase(), true);
		// }
		// HACK HACK HACK HACK HACK HACK HACK HACK

		targetTokens.add(targetTerminal);

		// add end of sentence marker
		if (sourceTokenEnd == inputLength && lastTerminal) {
			targetTokens.add(eos);
		}

//		System.out.println("CREATED PARSE: " + sourceTokenStart + " " + sourceTokenEnd + ": "
//				+ Arrays.toString(tokenFactory.getTokensAsStrings(targetTokens)));

		return new Parse<T>(sourceInputTokens, targetTokens, targetTerminal);
	}
}
