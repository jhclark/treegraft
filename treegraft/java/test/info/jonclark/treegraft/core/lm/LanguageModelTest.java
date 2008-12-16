package info.jonclark.treegraft.core.lm;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.core.tokens.integer.IntegerToken;
import info.jonclark.treegraft.core.tokens.integer.IntegerTokenFactory;
import info.jonclark.treegraft.core.tokens.string.StringToken;
import info.jonclark.treegraft.core.tokens.string.StringTokenFactory;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import org.junit.Test;

public class LanguageModelTest {

	public void testSimpleLM() throws Exception {
		TokenFactory<StringToken> tokenFactory = new StringTokenFactory();
		LanguageModel<StringToken> lm = new SimpleNGramLanguageModel<StringToken>(null);
		testLM(tokenFactory, lm);
	}

	@Test
	public void testEfficientLM() throws Exception {
		TokenFactory<IntegerToken> tokenFactory = new IntegerTokenFactory();
		LanguageModel<IntegerToken> lm = new EfficientNGramLanguageModel(null);
		testLM(tokenFactory, lm);
	}

	public <T extends Token> void testLM(TokenFactory<T> tokenFactory, LanguageModel<T> lm)
			throws Exception {

		File file = new File("/Users/jon/Documents/Data/news-parl-1M/news-parl-1M-5gram.arpa");
		String sent = "<s> rammallah </s>";
		String a = StringUtils.substringBefore(sent, " ");
		String b = StringUtils.substringAfter(sent, " ");

		ARPALanguageModelLoader<T> loader = new ARPALanguageModelLoader<T>();
		loader.loadLM(lm, tokenFactory, new FileInputStream(file), "UTF8", null, null);

		System.gc();
		long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		long kb = used / 1000;
		long mb = kb / 1000;
		System.out.println("Loaded LM using " + mb + "MB");

		TokenSequence<T> seq1 =
				tokenFactory.makeTokenSequence(Arrays.asList(tokenFactory.makeTokens(
						StringUtils.tokenize(a), true)));
		TokenSequence<T> seq2 =
				tokenFactory.makeTokenSequence(Arrays.asList(tokenFactory.makeTokens(
						StringUtils.tokenize(b), true)));
		TokenSequence<T> seqAll = seq1.append(seq2);

		LanguageModelScore scoreAll = lm.scoreSequence(seqAll);
		System.out.println(scoreAll.getSequenceScore());
	}
}
