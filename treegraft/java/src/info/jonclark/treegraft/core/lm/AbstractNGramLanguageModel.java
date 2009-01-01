package info.jonclark.treegraft.core.lm;

import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNGramLanguageModel<T extends Token> implements LanguageModel<T> {

	protected double oovProb;
	protected int order;
	protected TokenSequence<T> sentenceBegin;
	protected TokenSequence<T> sentenceEnd;
	protected final ProfilerTimer lmBoundaryTimer;
	protected final ProfilerTimer lmScoreTokenTimer;

	public AbstractNGramLanguageModel(T bos, T eos, TokenFactory<T> tokenFactory,
			ProfilerTimer parentTimer) {

		sentenceBegin = tokenFactory.makeTokenSequence(asList(bos));
		sentenceEnd = tokenFactory.makeTokenSequence(asList(eos));

		// loader.loadLM(this, tokenFactory, stream, encoding,
		// targetVocabulary);
		this.lmBoundaryTimer = ProfilerTimer.newTimer("lmBoundary", parentTimer, true, false);
		this.lmScoreTokenTimer = ProfilerTimer.newTimer("lmScoreToken", parentTimer, true, false);
	}

	private List<T> asList(T t) {
		List<T> list = new ArrayList<T>(1);
		list.add(t);
		return list;
	}

	public abstract void setOrder(int order, int[] expectedItems);

	public void setOOVProb(double logProb) {
		this.oovProb = logProb;
	}

	// TODO: Presize LM hashes
	public abstract void addEntry(TokenSequence<T> tokenSequence, double logProb,
			double backoffLogProb);

	public LanguageModelScore scoreSequence(TokenSequence<T> tokenSequence) {

		// score each token then combine
		double sequenceLogProb = 0.0;
		List<ScoredToken> scoredTokens = new ArrayList<ScoredToken>(tokenSequence.size());
		for (int i = 0; i < tokenSequence.size(); i++) {

			// keep a moving window of context, which has a maximum size of the
			// order of this ngram model
			int nStart = Math.max(0, i + 1 - this.order);
			int nEnd = i + 1;

			TokenSequence<T> subsequence = tokenSequence.subsequence(nStart, nEnd);
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
			LanguageModelScore scores1, TokenSequence<T> seq2, LanguageModelScore scores2,
			TokenSequence<T> combinedSequence) {

		lmBoundaryTimer.go();

		List<ScoredToken> scoredTokens = new ArrayList<ScoredToken>(combinedSequence.size());

		// no rescoring is necessary for the first sequence
		// provided that it has been scored before
		if (seq1.size() > 0 && scores1.getScoredTokens().get(0).ngramUsed == 0) {
			scores1 = scoreSequence(seq1);
		}
		double sequenceLogProb = scores1.getSequenceScore();
		scoredTokens.addAll(scores1.getScoredTokens());

		// rescore the first few items of seq2, if necessary
		for (int i = 0; i < seq2.size(); i++) {

			ScoredToken prevScore = scores2.getScoredTokens().get(i);

			// at most we should only have to rescore the first n tokens where n
			// is the order of this LM
			if (prevScore.ngramUsed < this.order && i < this.order) {

				int nStart = Math.max(0, seq1.size() - this.order + i + 1);
				int nEnd = seq1.size() + i + 1;
				TokenSequence<T> subsequence = combinedSequence.subsequence(nStart, nEnd);

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
		assert scoredSequence.getScoredTokens().size() == (seq1.size() + seq2.size()) : "Length mismatch";
		lmBoundaryTimer.pause();
		return scoredSequence;
	}

	// behold, some of the most time-critical code in the decoder
	protected abstract ScoredToken scoreToken(TokenSequence<T> originalSeq);
}
