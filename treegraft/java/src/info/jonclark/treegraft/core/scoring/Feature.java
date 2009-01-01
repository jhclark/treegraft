package info.jonclark.treegraft.core.scoring;

import info.jonclark.treegraft.core.Plugin;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.List;

// TODO: Divide this into Feature and MultiFeature where MultiFeature can return a feature vector
// TODO: How do we deal with parsing features from rules? Just require the rule type to deal with it?

/**
 * Parses begin with the creation of "terminal parses" if you need to do scoring
 * only once we know what the source association is, consider returning a dummy
 * score object (a singleton) and then doing a reference comparison during
 * combination to see if a score has been generated yet.
 * <p>
 * All implementations of Feature must specify the OptionsTarget annotation,
 * which corresponds to the type of the first constructor argument.
 */
public interface Feature<R extends GrammarRule<T>, T extends Token, S extends FeatureScore> extends
		Plugin<R, T> {

	public S combineRuleScoreWithChildren(PartialParse<T> parse, S parseScore, R ruleToAppend,
			List<T> inputSentence);

	public S getInitialScore();

	public S combineChildParseScores(PartialParse<T> accumulatedParse,
			TokenSequence<T> accumulatedSeq, S accumulatedScore, PartialParse<T> addedChild,
			TokenSequence<T> addedSeq, S addedScore, TokenSequence<T> combinedSeq,
			List<T> inputSentence);

	public S scoreTerminalParse(PartialParse<T> terminalParse, TokenSequence<T> seq);

	public S combineHypotheses(DecoderHypothesis<T> hyp1, TokenSequence<T> tokensFromHyp1,
			S scoreFromHyp1, DecoderHypothesis<T> hyp2, TokenSequence<T> tokensFromHyp2,
			S scoreFromHyp2, TokenSequence<T> combinedTokenSequence, List<T> inputSentence);

	public S recombine(S a, S b);

	public String getFeatureName();

	/**
	 * Gets the mnemonic label names used for these (used for reading in lambda
	 * weights from properties file)
	 * 
	 * @return
	 */
	public String[] getFeatureProbVectorLabels();

	public double[] getFeatureWeightVector();

	public void setFeatureWeightVector(double[] lambdas);
}
