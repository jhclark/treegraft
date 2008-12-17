package info.jonclark.treegraft.core.tokens.integer;

import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;

import java.util.HashMap;
import java.util.List;

/**
 * A <code>TokenFactory</code> implementation for <code>IntegerTokens</code>.
 * 
 * @author Jonathan Clark
 */
public class IntegerTokenFactory extends TokenFactory<IntegerToken> {

	private static final int DEFAULT_VOCAB_SIZE = 10000;
	private final HashMap<String, IntegerToken> str2tok =
			new HashMap<String, IntegerToken>(DEFAULT_VOCAB_SIZE);
	private final HashMap<IntegerToken, String> int2str = new HashMap<IntegerToken, String>(DEFAULT_VOCAB_SIZE);

	private final int maxVocabSize;

	private final int[] counterStartValues;
	private final int[] counters;
	private int whichCounter = 0;

	private static final int N_COUNTERS = 100;

	public IntegerTokenFactory() {

		// optimize token ID's for faster hashing
		// by trying to more evenly distribute the ID's
		// that are assigned among bits
		this.counters = new int[N_COUNTERS];
		this.counterStartValues = new int[N_COUNTERS];
		this.maxVocabSize = (int) Math.pow(2, IntegerTokenSequence.BITS_PER_WORD);
		int idsPerCounter = maxVocabSize / N_COUNTERS;

		// define starting values for each counter
		for (int i = 0; i < counters.length; i++) {
			this.counters[i] = idsPerCounter * i;
			this.counterStartValues[i] = counters[i];
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getTokenAsString(IntegerToken tok) {
		return int2str.get(tok);
	}

	/**
	 * {@inheritDoc}
	 */
	public IntegerToken makeToken(String str, boolean terminal) {

		String key;
		if (terminal) {
			key = str;
		} else {
			key = NON_TERMINAL_PREFIX + str + NON_TERMINAL_SUFFIX;
		}

		IntegerToken tok = str2tok.get(key);
		if (tok == null) {

			if (int2str.size() == maxVocabSize) {
				throw new RuntimeException("Vocabulary size exceeded: " + (int2str.size() + 1));
			}

			int id = nextId();
			tok = new IntegerToken(id, terminal);
			str2tok.put(key, tok);
			int2str.put(tok, str);
		}
		return tok;
	}

	private int nextId() {

		int id = counters[whichCounter];

		// advance counters
		do {
			counters[whichCounter]++;
			whichCounter++;
			whichCounter %= counters.length;
		} while (counters[whichCounter] == counterStartValues[(whichCounter + 1) % N_COUNTERS]);
		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TokenSequence<IntegerToken> makeTokenSequence(List<IntegerToken> tokens) {
		return new IntegerTokenSequence(tokens);
	}

	/**
	 * {@inheritDoc}
	 */
	public IntegerToken[] makeTokens(String[] strs, boolean terminals) {
		IntegerToken[] toks = new IntegerToken[strs.length];
		for (int i = 0; i < toks.length; i++)
			toks[i] = makeToken(strs[i], terminals);
		return toks;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IntegerToken[] newTokenArray(int length) {
		return new IntegerToken[length];
	}

	public static void main(String[] args) throws Exception {

		IntegerTokenFactory f = new IntegerTokenFactory();
		for (int i = 0; i < 1000; i++) {
			System.out.println(f.nextId());
		}
	}
}
