package info.jonclark.treegraft.core.lm;

import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.lang.trie.Trie;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.core.tokens.string.StringToken;
import info.jonclark.treegraft.core.tokens.string.StringTokenFactory;
import info.jonclark.treegraft.core.tokens.string.StringTokenSequence;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@OptionsTarget(SimpleNGramLanguageModel.SimpleNGramLanguageModelOptions.class)
public class SimpleNGramLanguageModel<T extends Token> extends AbstractNGramLanguageModel<T> {

	private static class LMEntry {
		public final float logProb;
		public final float backoffLogProb;

		// probabilities don't need to be very exact
		public LMEntry(double logProb, double backoffLogProb) {
			this.logProb = (float) logProb;
			this.backoffLogProb = (float) backoffLogProb;
		}
	}

	public static final float LOAD_FACTOR = 0.75f;
	public static final boolean TRIE_MODE = false;
	private Map<TokenSequence<T>, LMEntry>[] probs;
	private Trie<TokenSequence<T>, T, LMEntry> trie;
	
	public static class SimpleNGramLanguageModelOptions implements Options {

	}

	public SimpleNGramLanguageModel(SimpleNGramLanguageModelOptions opts,
			TreegraftConfig<?, T> config) {
		super(config.bos, config.eos, config.tokenFactory, config.profiler.featureTimer);
	}

	public void setOrder(int order, int[] expectedItems) {
		this.order = order;
		if (TRIE_MODE) {
			this.trie = new Trie<TokenSequence<T>, T, LMEntry>(1, order);
		} else {
			this.probs = new HashMap[order];
			for (int i = 0; i < order; i++) {
				this.probs[i] = new HashMap<TokenSequence<T>, LMEntry>(expectedItems[i], LOAD_FACTOR);
			}
		}
	}

	public void addEntry(TokenSequence<T> tokenSequence, double logProb, double backoffLogProb) {
		LMEntry entry = new LMEntry(logProb, backoffLogProb);
		if (TRIE_MODE) {
			trie.put(tokenSequence, entry);
		} else {
			int order = tokenSequence.size();
			probs[order - 1].put(tokenSequence, entry);
		}
	}

	// behold, some of the most time-critical code in the decoder
	protected ScoredToken scoreToken(TokenSequence<T> originalSeq) {

		// TODO: Make this faster or unnecessary
		if (originalSeq.equals(sentenceBegin)
		// || originalSeq.keepNRightMostTokens(1).equals(sentenceEnd)
		) {
			return new ScoredToken(0.0, 1, 1);
		}

		// System.out.println("Scoring: " + originalSeq);

		lmScoreTokenTimer.go();

		if (trie == null && probs == null)
			throw new RuntimeException("No language model loaded.");

		TokenSequence<T> trimmedSeq = originalSeq;
		int nGram = trimmedSeq.size();
		assert nGram <= this.order : "N-gram is longer than order of LM";

		// int order = originalSeq.length();
		// if (order > probs.length) {
		// order = probs.length;
		// trimmedSeq = originalSeq.keepNRightMostTokens(order);
		// }

		double score = 0.0;
		LMEntry entry;
		if (TRIE_MODE) {
			entry = trie.get(trimmedSeq);
		} else {
			entry = probs[nGram - 1].get(trimmedSeq);
		}

		ScoredToken scoredToken;
		if (entry != null) {

			// no backoff required
			scoredToken = new ScoredToken(entry.logProb, originalSeq.size(), trimmedSeq.size());

		} else {

			// do backoff
			while (entry == null && nGram > 1) {
				nGram--;

				TokenSequence<T> backoffSeq = trimmedSeq.keepNLeftMostTokens(nGram);
				LMEntry backoffEntry;
				if (TRIE_MODE) {
					backoffEntry = trie.get(backoffSeq);
				} else {
					backoffEntry = probs[nGram - 1].get(backoffSeq);
				}
				if (backoffEntry != null && backoffEntry.backoffLogProb != Double.NEGATIVE_INFINITY) {
					score += backoffEntry.backoffLogProb;
					// System.out.println("Backoff mass for " + backoffSeq +
					// ": "
					// + backoffEntry.backoffLogProb);
				}

				trimmedSeq = trimmedSeq.keepNRightMostTokens(nGram);
				// System.out.println("Backing off to: " + trimmedSeq + "(" +
				// score + ")");
				if (TRIE_MODE) {
					entry = trie.get(trimmedSeq);
				} else {
					entry = probs[nGram - 1].get(backoffSeq);
				}
			}

			if (entry != null && nGram > 0) {
				score += entry.logProb;
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

//	public static void main(String[] args) throws Exception {
//
////		File file = new File(args[0]);
//		File file = new File("/Users/jon/Downloads/2-trainA.txt(3).arpa");
//		String sent = args[1];
//		String a = StringUtils.substringBefore(sent, " ");
//		String b = StringUtils.substringAfter(sent, " ");
//
//		StringTokenFactory tokenFactory = new StringTokenFactory();
//		ARPALanguageModelLoader<StringToken> loader = new ARPALanguageModelLoader<StringToken>();
//		SimpleNGramLanguageModel<StringToken> lm = new SimpleNGramLanguageModel<StringToken>();
//		loader.loadLM(lm, tokenFactory, new FileInputStream(file), "UTF8", null, null);
//
//		// String a =
//		// ", sheikh mohammad confirmed that , in the interview that that focused on two files of iraq and"
//		// ;
//		// String b =
//		// "arab - israeli conflict \" importance of resorting to dispute resolution . and peacefully resolving problems away from violence and the arms race and double standards a guarantee to reinforce the right , providing justice and freedom of all peoples \" according to the emirates news agency ."
//		// ;
//
//		// String a = "given";
//		// String b =
//		// "award zayed international environment that fired year 1998 name state founder united arab emirates sheik zayed bin sultan of nahyan , two people known in defense on environment . total value لجوائزها million dollars ."
//		// ;
//
//		StringTokenSequence seq1 =
//				new StringTokenSequence(Arrays.asList(tokenFactory.makeTokens(
//						StringUtils.tokenize(a), true)));
//		StringTokenSequence seq2 =
//				new StringTokenSequence(Arrays.asList(tokenFactory.makeTokens(
//						StringUtils.tokenize(b), true)));
//		TokenSequence<StringToken> seqAll = seq1.append(seq2);
//
//		// LanguageModelScore score1 = lm.scoreSequence(seq1);
//		// System.out.println(score1.getSequenceScore());
//		//
//		// LanguageModelScore score2 = lm.scoreSequence(seq2);
//		// System.out.println(score2.getSequenceScore());
//		//
//		// LanguageModelScore scoreCombined =
//		// lm.scoreBoundaryAndCombine(seq1, score1, seq2, score2, seqAll);
//		// System.out.println(scoreCombined.getSequenceScore());
//
//		LanguageModelScore scoreAll = lm.scoreSequence(seqAll);
//		System.out.println(scoreAll.getSequenceScore());
//	}

	public String getMetaInfo() {
		return "";
	}
}
