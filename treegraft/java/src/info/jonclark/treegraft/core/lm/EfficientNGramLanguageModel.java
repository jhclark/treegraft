package info.jonclark.treegraft.core.lm;

import info.jonclark.lang.hash.WideHashMap;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.core.tokens.integer.IntegerToken;
import info.jonclark.treegraft.core.tokens.integer.IntegerTokenSequence;

// supports up to a 6-gram LM w/ a vocabulary of 2M words
public class EfficientNGramLanguageModel extends AbstractNGramLanguageModel<IntegerToken> {

	private WideHashMap[] probs;
	private final float outOfHashValue = Float.MIN_VALUE;
	private final float LOAD_FACTOR = 0.75f;
	private final long[] bitBuffer = new long[2];

	public EfficientNGramLanguageModel(ProfilerTimer parentTimer) {
		super(parentTimer);
	}

	public void setOrder(int order, int[] expectedItems) {
		this.order = order;
		this.probs = new WideHashMap[order];
		for (int i = 0; i < order; i++) {
			this.probs[i] = new WideHashMap(expectedItems[i], LOAD_FACTOR, outOfHashValue);
		}
	}

	// TODO: Presize LM hashes
	public void addEntry(TokenSequence<IntegerToken> tokenSequence, double logProb,
			double backoffLogProb) {

		IntegerTokenSequence.getBitSequence(tokenSequence, bitBuffer);

		int order = tokenSequence.size();
		probs[order - 1].put(bitBuffer[0], bitBuffer[1], (float) logProb, (float) backoffLogProb);
	}

	// behold, some of the most time-critical code in the decoder
	protected ScoredToken scoreToken(TokenSequence<IntegerToken> originalSeq) {

		// TODO: Make this faster or unnecessary
		if (originalSeq.equals(sentenceBegin)
		// || originalSeq.keepNRightMostTokens(1).equals(sentenceEnd)
		) {
			return new ScoredToken(0.0, 1, 1);
		}

		// System.out.println("Scoring: " + originalSeq);

		lmScoreTokenTimer.go();

		if (probs == null)
			throw new RuntimeException("No language model loaded.");

		TokenSequence<IntegerToken> trimmedSeq = originalSeq;
		int nGram = trimmedSeq.size();
		assert nGram <= this.order : "N-gram is longer than order of LM";

		// int order = originalSeq.length();
		// if (order > probs.length) {
		// order = probs.length;
		// trimmedSeq = originalSeq.keepNRightMostTokens(order);
		// }

		float logProb = probs[nGram - 1].getValue1(bitBuffer[0], bitBuffer[1]);
		double score = 0.0;

		ScoredToken scoredToken;
		if (logProb != outOfHashValue) {

			// no backoff required
			scoredToken = new ScoredToken(logProb, originalSeq.size(), trimmedSeq.size());

		} else {

			// do backoff
			while (logProb == outOfHashValue && nGram > 1) {
				nGram--;

				TokenSequence<IntegerToken> backoffSeq = trimmedSeq.keepNLeftMostTokens(nGram);
				IntegerTokenSequence.getBitSequence(backoffSeq, bitBuffer);

				float backoffLogProb = probs[nGram - 1].getValue2(bitBuffer[0], bitBuffer[1]);
				if (backoffLogProb != outOfHashValue && backoffLogProb != Double.NEGATIVE_INFINITY) {
					score += backoffLogProb;
					// System.out.println("Backoff mass for " + backoffSeq +
					// ": "
					// + backoffEntry.backoffLogProb);
				}

				trimmedSeq = trimmedSeq.keepNRightMostTokens(nGram);
				IntegerTokenSequence.getBitSequence(trimmedSeq, bitBuffer);
				// System.out.println("Backing off to: " + trimmedSeq + "(" +
				// score + ")");

				logProb = probs[nGram - 1].getValue1(bitBuffer[0], bitBuffer[1]);
			}

			if (logProb != outOfHashValue && nGram > 0) {
				score += logProb;
			} else {
				// word was not in vocabulary
				score += oovProb;
				// System.out.println("OOV");
			}

			// System.out.println("Score: " + score);

			scoredToken = new ScoredToken(score, originalSeq.size(), trimmedSeq.size());
		}
		lmScoreTokenTimer.pause();
		return scoredToken;
	}
}
