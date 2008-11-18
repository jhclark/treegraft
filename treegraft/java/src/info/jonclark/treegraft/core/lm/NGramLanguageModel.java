package info.jonclark.treegraft.core.lm;

import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.core.tokens.string.StringToken;
import info.jonclark.treegraft.core.tokens.string.StringTokenFactory;
import info.jonclark.treegraft.core.tokens.string.StringTokenSequence;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class NGramLanguageModel<T extends Token> implements LanguageModel<T> {

	private static class LMEntry {
		public final float logProb;
		public final float backoffLogProb;

		// probabilities don't need to be very exact
		public LMEntry(double logProb, double backoffLogProb) {
			this.logProb = (float) logProb;
			this.backoffLogProb = (float) backoffLogProb;
		}
	}

	private HashMap<TokenSequence<T>, LMEntry>[] probs;
	private double oovProb;
	private int order;
	private TokenSequence<T> sentenceBegin;
	private TokenSequence<T> sentenceEnd;
	private final ProfilerTimer lmBoundaryTimer;
	private final ProfilerTimer lmScoreTokenTimer;

	public NGramLanguageModel(ProfilerTimer parentTimer) {

		// loader.loadLM(this, tokenFactory, stream, encoding,
		// targetVocabulary);
		this.lmBoundaryTimer = ProfilerTimer.newTimer("lmBoundary", parentTimer, true, false);
		this.lmScoreTokenTimer = ProfilerTimer.newTimer("lmScoreToken", parentTimer, true, false);
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

	public void setSentenceBeginMarker(TokenSequence<T> bos) {
		this.sentenceBegin = bos;
	}

	public void setSentenceEndMarker(TokenSequence<T> eos) {
		this.sentenceEnd = eos;
	}

	// TODO: Presize LM hashes
	public void addEntry(TokenSequence<T> tokenSequence, double logProb, double backoffLogProb) {
		int order = tokenSequence.getTokens().size();
		LMEntry entry = new LMEntry(logProb, backoffLogProb);
		probs[order - 1].put(tokenSequence, entry);
	}

	public LanguageModelScore scoreSequence(TokenSequence<T> tokenSequence) {

		// score each token then combine
		double sequenceLogProb = 0.0;
		List<ScoredToken> scoredTokens = new ArrayList<ScoredToken>(tokenSequence.length());
		for (int i = 0; i < tokenSequence.length(); i++) {

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

		List<ScoredToken> scoredTokens = new ArrayList<ScoredToken>(combinedSequence.length());

		// no rescoring is necessary for the first sequence
		// provided that it has been scored before
		if (seq1.length() > 0 && scores1.getScoredTokens().get(0).ngramUsed == 0) {
			scores1 = scoreSequence(seq1);
		}
		double sequenceLogProb = scores1.getSequenceScore();
		scoredTokens.addAll(scores1.getScoredTokens());

		// rescore the first few items of seq2, if necessary
		for (int i = 0; i < seq2.length(); i++) {

			ScoredToken prevScore = scores2.getScoredTokens().get(i);

			// at most we should only have to rescore the first n tokens where n
			// is the order of this LM
			if (prevScore.ngramUsed < this.order && i < this.order) {

				int nStart = Math.max(0, seq1.length() - this.order + i + 1);
				int nEnd = seq1.length() + i + 1;
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
		assert scoredSequence.getScoredTokens().size() == (seq1.length() + seq2.length()) : "Length mismatch";
		lmBoundaryTimer.pause();
		return scoredSequence;
	}

	// behold, some of the most time-critical code in the decoder
	private ScoredToken scoreToken(TokenSequence<T> originalSeq) {

		// TODO: Make this faster or unnecessary
		if (originalSeq.equals(sentenceBegin)
		// || originalSeq.keepNRightMostTokens(1).equals(sentenceEnd)
		) {
			return new ScoredToken(0.0, 1, 1);
		}

//		System.out.println("Scoring: " + originalSeq);

		lmScoreTokenTimer.go();

		if (probs == null)
			throw new RuntimeException("No language model loaded.");

		TokenSequence<T> trimmedSeq = originalSeq;
		int nGram = trimmedSeq.length();
		assert nGram <= probs.length : "N-gram is longer than order of LM";

		// int order = originalSeq.length();
		// if (order > probs.length) {
		// order = probs.length;
		// trimmedSeq = originalSeq.keepNRightMostTokens(order);
		// }

		double score = 0.0;
		LMEntry entry = probs[nGram - 1].get(trimmedSeq);
		ScoredToken scoredToken;
		if (entry != null) {

			// no backoff required
			scoredToken = new ScoredToken(entry.logProb, originalSeq.length(), trimmedSeq.length());

		} else {

			// do backoff
			while (entry == null && nGram > 1) {
				nGram--;

				TokenSequence<T> backoffSeq = trimmedSeq.keepNLeftMostTokens(nGram);
				LMEntry backoffEntry = probs[nGram - 1].get(backoffSeq);
				if (backoffEntry != null && backoffEntry.backoffLogProb != Double.NEGATIVE_INFINITY) {
					score += backoffEntry.backoffLogProb;
//					System.out.println("Backoff mass for " + backoffSeq + ": "
//							+ backoffEntry.backoffLogProb);
				}

				trimmedSeq = trimmedSeq.keepNRightMostTokens(nGram);
//				System.out.println("Backing off to: " + trimmedSeq + "(" + score + ")");
				entry = probs[nGram - 1].get(trimmedSeq);
			}

			if (entry != null && nGram > 0) {
				score += entry.logProb;
			} else {
				// word was not in vocabulary
				score += oovProb;
//				System.out.println("OOV");
			}

//			System.out.println("Score: " + score);

			scoredToken = new ScoredToken(score, originalSeq.length(), trimmedSeq.length());
		}
		lmScoreTokenTimer.pause();
		return scoredToken;
	}

	public static void main(String[] args) throws Exception {

		File file = new File(args[0]);
		String sent = args[1];
		String a = StringUtils.substringBefore(sent, " ");
		String b = StringUtils.substringAfter(sent, " ");

		StringTokenFactory tokenFactory = new StringTokenFactory();
		ARPALanguageModelLoader<StringToken> loader = new ARPALanguageModelLoader<StringToken>();
		NGramLanguageModel<StringToken> lm = new NGramLanguageModel<StringToken>(null);
		loader.loadLM(lm, tokenFactory, new FileInputStream(file), "UTF8", null, null);

		// String a =
		// ", sheikh mohammad confirmed that , in the interview that that focused on two files of iraq and"
		// ;
		// String b =
		// "arab - israeli conflict \" importance of resorting to dispute resolution . and peacefully resolving problems away from violence and the arms race and double standards a guarantee to reinforce the right , providing justice and freedom of all peoples \" according to the emirates news agency ."
		// ;

		// String a = "given";
		// String b =
		// "award zayed international environment that fired year 1998 name state founder united arab emirates sheik zayed bin sultan of nahyan , two people known in defense on environment . total value لجوائزها million dollars ."
		// ;

		StringTokenSequence seq1 =
				new StringTokenSequence(Arrays.asList(tokenFactory.makeTokens(
						StringUtils.tokenize(a), true)));
		StringTokenSequence seq2 =
				new StringTokenSequence(Arrays.asList(tokenFactory.makeTokens(
						StringUtils.tokenize(b), true)));
		TokenSequence<StringToken> seqAll = seq1.append(seq2);

		// LanguageModelScore score1 = lm.scoreSequence(seq1);
		// System.out.println(score1.getSequenceScore());
		//
		// LanguageModelScore score2 = lm.scoreSequence(seq2);
		// System.out.println(score2.getSequenceScore());
		//
		// LanguageModelScore scoreCombined =
		// lm.scoreBoundaryAndCombine(seq1, score1, seq2, score2, seqAll);
		// System.out.println(scoreCombined.getSequenceScore());

		LanguageModelScore scoreAll = lm.scoreSequence(seqAll);
		System.out.println(scoreAll.getSequenceScore());
	}
}
