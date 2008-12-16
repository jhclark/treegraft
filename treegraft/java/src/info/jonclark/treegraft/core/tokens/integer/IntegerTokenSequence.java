package info.jonclark.treegraft.core.tokens.integer;

import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.util.DebugUtils;
import info.jonclark.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class IntegerTokenSequence implements TokenSequence<IntegerToken> {

	private static final boolean DEBUG = false;
	private static final List<IntegerToken> EMPTY_LIST = new ArrayList<IntegerToken>(0);

	private int hash = -1;
	private final long[] seqs;
	private final int length;

	private static final int BITS_PER_WORD = 21;
	private static int WORDS_PER_LONG = 3;
	private static long[] masks;
	static {
		masks = new long[WORDS_PER_LONG + 1];

		long value = 0;
		for (int i = 0; i < masks.length; i++) {
			masks[i] = value;
			for (int j = 0; j < BITS_PER_WORD; j++) {
				value <<= 1;
				value |= 1;
			}
		}
	}

	protected IntegerTokenSequence(long[] seqs, int length) {
		assert length > 0;
		this.seqs = seqs;
		this.length = length;
	}

	public IntegerTokenSequence(List<IntegerToken> tokens) {

		// yes they can: packing strings
//		if (DebugUtils.isAssertEnabled()) {
//			for (IntegerToken tok : tokens) {
//				assert tok.isTerminal() : "non-terminals cannot be members of token sequences";
//			}
//		}

		this.length = tokens.size();
		int nLongsRequired =
				tokens.size() / WORDS_PER_LONG + (tokens.size() % WORDS_PER_LONG > 0 ? 1 : 0);
		this.seqs = new long[nLongsRequired];

		// if (DEBUG) {
		// System.out.print("CREATE: ");
		// for (long x : seqs)
		// System.out.print(str(x) + " - ");
		// System.out.println();
		// }

		// store tokens in array of longs so that first token is left-most bit
		// sequence
		// this enables the operation "keep right-most" to complete quickly

		for (int k = tokens.size() - 1; k >= 0; k--) {
			int i = k / WORDS_PER_LONG;
			long thisID = tokens.get(k).id;
			seqs[i] <<= BITS_PER_WORD;
			seqs[i] |= thisID;

			// if (DEBUG) {
			// System.out.print("BUILD: ");
			// for (long x : seqs)
			// System.out.print(str(x) + " - ");
			// System.out.println();
			// }
		}
	}

	public List<IntegerToken> getTokens() {

		if (length == 0) {
			return EMPTY_LIST;
		}

		List<IntegerToken> tokens = new ArrayList<IntegerToken>(length);

		// now all except last long
		for (int i = 0; i < seqs.length - 1; i++) {
			long seqID = seqs[i];
			for (int k = 0; k < WORDS_PER_LONG; k++) {
				int tokID = (int) (seqID & masks[1]);
				tokens.add(new IntegerToken(tokID, true));
				seqID >>= BITS_PER_WORD;
			}
		}

		// handle last long as a special case
		// since it may have less than WORDS_PER_LONG in it
		int nToksInLastLong = length % WORDS_PER_LONG;
		if (nToksInLastLong == 0)
			nToksInLastLong = WORDS_PER_LONG;
		long seqID = seqs[seqs.length - 1];
		for (int k = 0; k < nToksInLastLong; k++) {
			int tokID = (int) (seqID & masks[1]);
			tokens.add(new IntegerToken(tokID, true));
			seqID >>= BITS_PER_WORD;
		}

		return tokens;
	}

	public int hashCode() {
		if (hash == -1) {
			for (int i = 0; i < seqs.length; i++) {

				// calculate tokenHash^i
				long seqHash = seqs[i];
				long pow = seqHash;
				for (int j = 1; j < i; j++) {
					pow *= seqHash;
				}
				hash += pow;
			}
		}
		return hash;
	}

	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		} else if (obj instanceof IntegerTokenSequence) {
			IntegerTokenSequence other = (IntegerTokenSequence) obj;
			if (this.length == other.length) {

				for (int i = 0; i < seqs.length; i++) {
					if (this.seqs[i] != other.seqs[i]) {
						return false;
					}
				}
				return true;

			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int size() {
		return length;
	}

	public TokenSequence<IntegerToken> keepNLeftMostTokens(int n) {
		if (length <= n) {
			return this;
		} else {

			// keep the "left tokens" which are the bit sequence on the right
			int modulus = n % WORDS_PER_LONG;
			int nRemainingInLastLong = modulus == 0 ? WORDS_PER_LONG : modulus;
			int nLongsRequired = n / WORDS_PER_LONG + (n % WORDS_PER_LONG == 0 ? 0 : 1);

			long[] newSeqs = new long[nLongsRequired];
			System.arraycopy(seqs, 0, newSeqs, 0, nLongsRequired);

			// just mask off the portion that got cut on the last long
			newSeqs[nLongsRequired - 1] &= masks[nRemainingInLastLong];

			return new IntegerTokenSequence(newSeqs, n);
		}
	}

	private static String str(long x) {
		String str = Long.toBinaryString(x);
		str = StringUtils.forceNumberLength(str, 64);
		StringBuilder builder = new StringBuilder();
		int i = 1;
		for (char c : str.toCharArray()) {
			if (i == 0) {
				builder.append(" ");
				i = BITS_PER_WORD;
			}
			builder.append(c);
			i--;
		}
		return builder.toString();
	}

	// TODO: Create special class for short integer token sequences
	public TokenSequence<IntegerToken> keepNRightMostTokens(int n) {

		if (length <= n) {
			return this;
		} else {

			// keep the "right tokens" which are the bit sequence on the left

			int modulus = n % WORDS_PER_LONG;
			int nLongsRequired = n / WORDS_PER_LONG + (modulus == 0 ? 0 : 1);
			long[] newSeqs = new long[nLongsRequired];

			int nCutPerLong = (length - n) % WORDS_PER_LONG;
			int nRemainingPerLong = WORDS_PER_LONG - nCutPerLong;
			int nOriginallyInLastLong = length - (seqs.length - 1) * WORDS_PER_LONG;

			if (DEBUG) {
				System.out.print("OLD: ");
				for (long x : seqs)
					System.out.print(str(x) + " - ");
				System.out.println();
			}

			if (nCutPerLong == 0) {

				// handle "no shifting" as a special case that can be sped up
				// with a
				// direct array copy

				int offset = seqs.length - newSeqs.length;
				System.arraycopy(seqs, offset, newSeqs, 0, nLongsRequired);
			} else {

				// the harder case: some tokens must "shift" from one array
				// element to another while other tokens will stay in their
				// current array element

				int offset = (length - n) / WORDS_PER_LONG;
				if (DEBUG) {
					System.out.println("OFFSET = " + offset);
				}
				for (int i = 0; i < newSeqs.length; i++) {

					if (i + offset >= 0) {
						newSeqs[i] = seqs[i + offset] >>> (BITS_PER_WORD * nCutPerLong);
					}

					if (DEBUG) {
						System.out.print("NEW1: ");
						for (long x : newSeqs)
							System.out.print(str(x) + " - ");
						System.out.println();
					}

					if (i + offset + 1 < seqs.length) {
						newSeqs[i] |=
								(seqs[i + offset + 1] & masks[nCutPerLong]) << (BITS_PER_WORD * nRemainingPerLong);

						if (DEBUG) {
							System.out.print("NEW2: ");
							for (long x : newSeqs)
								System.out.print(str(x) + " - ");
							System.out.println();
						}
					}
				}
			}

			if (DEBUG) {
				System.out.print("NEW: ");
				for (long x : newSeqs)
					System.out.print(str(x) + " - ");
				System.out.println();
			}

			return new IntegerTokenSequence(newSeqs, n);
		}

	}

	public TokenSequence<IntegerToken> subsequence(int nStart, int nEnd) {
		// TODO: Optimize this
		return this.keepNRightMostTokens(nEnd).keepNLeftMostTokens(nEnd - nStart);
	}

	public TokenSequence<IntegerToken> append(TokenSequence<IntegerToken> suffix) {
		// TODO: Optimize this!!!!
		List<IntegerToken> list = this.getTokens();
		list.addAll(suffix.getTokens());
		IntegerTokenSequence seq = new IntegerTokenSequence(list);
		return seq;
	}

	public static void getBitSequence(TokenSequence<IntegerToken> sequence, long[] bits) {
		IntegerTokenSequence seq = (IntegerTokenSequence) sequence;
		System.arraycopy(seq.seqs, 0, bits, 0, Math.min(seq.seqs.length, bits.length));
	}

	public IntegerToken get(int i) {
		// TODO: Maybe optimize this?
		// or just remove get method...
		return this.getTokens().get(i);
	}
}
