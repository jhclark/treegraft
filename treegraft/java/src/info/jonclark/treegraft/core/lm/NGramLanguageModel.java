package info.jonclark.treegraft.core.lm;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class NGramLanguageModel<T extends Token> implements LanguageModel<T> {

	private static class LMEntry {
		public final double logProb;
		public final double backoffLogProb;

		public LMEntry(double logProb, double backoffLogProb) {
			this.logProb = logProb;
			this.backoffLogProb = backoffLogProb;
		}
	}

	private HashMap<TokenSequence<T>, LMEntry>[] probs;
	private double oovProb;
	private int order;

	public NGramLanguageModel() throws IOException {

		// loader.loadLM(this, tokenFactory, stream, encoding,
		// targetVocabulary);
	}

	public void setOrder(int order) {
		this.order = order;
		this.probs = new HashMap[order];
		for (int i = 0; i < order; i++) {
			probs[i] = new HashMap<TokenSequence<T>, LMEntry>();
		}
	}

	public void setOOVProb(double logProb) {
		this.oovProb = logProb;
	}

	public void addEntry(TokenSequence<T> tokenSequence, double logProb, double backoffLogProb) {
		int order = tokenSequence.getTokens().size();
		LMEntry entry = new LMEntry(logProb, backoffLogProb);
		probs[order - 1].put(tokenSequence, entry);
	}

	public LanguageModelScore scoreSequence(TokenSequence<T> tokenSequence) {

		// score each token then combine
		double sequenceLogProb = 1.0;
		List<ScoredToken> scoredTokens =
				new ArrayList<ScoredToken>(tokenSequence.getTokens().size());
		for (int i = 0; i < tokenSequence.getTokens().size(); i++) {
			TokenSequence<T> subsequence = tokenSequence.keepNLeftMostTokens(i + 1);
			ScoredToken scoredToken = scoreToken(subsequence);
			sequenceLogProb += scoredToken.lmScore;
			scoredTokens.add(scoredToken);
		}

		// TODO: Cache LM results? (use external caching class (e.g.
		// ObjectCache)

		LanguageModelScore scoredSequence = new LanguageModelScore(scoredTokens, sequenceLogProb);
		return scoredSequence;
	}

	public LanguageModelScore scoreBoundaryAndCombine(TokenSequence<T> seq1,
			LanguageModelScore scores1, TokenSequence<T> seq2, LanguageModelScore scores2) {

		List<ScoredToken> scoredTokens = new ArrayList<ScoredToken>(seq1.length() + seq2.length());

		// no rescoring is necessary for the first sequence
		if (seq1.length() > 0 && scores1.getScoredTokens().get(0).ngramUsed == 0) {
			scores1 = scoreSequence(seq1);
		}
		double sequenceLogProb = scores1.getSequenceScore();
		scoredTokens.addAll(scores1.getScoredTokens());

		// rescore the first few items of seq2, if necessary
		for (int i = 0; i < seq2.getTokens().size(); i++) {

			ScoredToken prevScore = scores2.getScoredTokens().get(i);

			// at most we should only have to rescore the first n tokens where n
			// is the order of this LM
			if (prevScore.ngramUsed < this.order && i < this.order) {

				TokenSequence<T> subsequence = seq2.keepNLeftMostTokens(i + 1);

				// prepend tokens from the prev sequence
				TokenSequence<T> additionalLeftContext =
						seq1.keepNRightMostTokens(this.order - i - 1);
				subsequence = subsequence.prepend(additionalLeftContext);

				ScoredToken scoredToken = scoreToken(subsequence);
				sequenceLogProb += scoredToken.lmScore;
				scoredTokens.add(scoredToken);

				// TODO: We might get some time savings if we give lower-order
				// n-grams information about if higher-level n-grams ending with
				// their sequence exist

			} else {
				sequenceLogProb += prevScore.lmScore;
				scoredTokens.add(prevScore);
			}
		}

		LanguageModelScore scoredSequence = new LanguageModelScore(scoredTokens, sequenceLogProb);
		assert scoredSequence.getScoredTokens().size() == (seq1.length() + seq2.length()) : "Length mismatch";
		return scoredSequence;
	}

	public ScoredToken scoreToken(TokenSequence<T> originalSeq) {
		if (probs == null)
			throw new RuntimeException("No language model loaded.");

		TokenSequence<T> trimmedSeq = originalSeq;
		int order = originalSeq.length();
		if (order > probs.length) {
			order = probs.length;
			trimmedSeq = originalSeq.keepNRightMostTokens(order);
		}

		double score;
		LMEntry entry = probs[order - 1].get(trimmedSeq);
		if (entry != null) {

			// no backoff required
			ScoredToken scoredToken =
					new ScoredToken(entry.logProb, originalSeq.getTokens().size(),
							trimmedSeq.getTokens().size());
			return scoredToken;

		} else {

			// do backoff
			while (entry == null && order > 1) {
				order--;
				trimmedSeq = trimmedSeq.keepNRightMostTokens(order);
				entry = probs[order - 1].get(trimmedSeq);
			}
			if (order > 1) {
				score = entry.logProb;
				if (entry.backoffLogProb != Double.NEGATIVE_INFINITY) {
					score += entry.backoffLogProb;
				}
			} else {
				// word was not in vocabulary
				score = oovProb;
			}

			ScoredToken scoredToken =
					new ScoredToken(score, originalSeq.getTokens().size(),
							trimmedSeq.getTokens().size());
			return scoredToken;
		}
	}
}
