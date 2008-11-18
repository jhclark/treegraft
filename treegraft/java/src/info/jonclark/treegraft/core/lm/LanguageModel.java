package info.jonclark.treegraft.core.lm;

import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;

/**
 * Each language model implementation should take in a LanguageModelLoader into
 * its constructor.
 * 
 * @author jon
 * @param <T>
 */
public interface LanguageModel<T extends Token> {

	/**
	 * For use only by language model loader
	 * 
	 * @param tokenSequence
	 * @param logProb
	 * @param backoffLogProb
	 *            Double.NEGATIVE_INFINITY indicates no weight is available
	 */
	public void addEntry(TokenSequence<T> tokenSequence, double logProb, double backoffLogProb);

	public LanguageModelScore scoreSequence(TokenSequence<T> tokens);

	/**
	 * Much faster than rescoring the whole sequence
	 * 
	 * @param tokens1
	 * @param score1
	 * @param tokens2
	 * @param score2
	 * @return
	 */
	public LanguageModelScore scoreBoundaryAndCombine(TokenSequence<T> seq1,
			LanguageModelScore scores1, TokenSequence<T> seq2, LanguageModelScore scores2,
			TokenSequence<T> combinedSequence);

	public void setOrder(int n);

	public void setOOVProb(double lobProb);
	
	public void setSentenceBeginMarker(TokenSequence<T> bos);
	
	public void setSentenceEndMarker(TokenSequence<T> eos);
}
