package info.jonclark.treegraft.core.lm;

import info.jonclark.treegraft.core.parses.Parse;
import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.ArrayList;
import java.util.Collections;

// TODO: Handle beginning of sentence markers and summing over all
// possibilities if we don't know what token actually comes to the left.
public class LanguageModelFeature<R extends GrammarRule<T>, T extends Token> implements
		Feature<R, T, LanguageModelScore> {

	private static final double FLOOR = -100;
	private final LanguageModel<T> lm;
	private final double[] weights;

	public LanguageModelFeature(LanguageModel<T> lm, double weight) {
		this.lm = lm;
		this.weights = new double[] { weight };

		ScoredToken dummy = new ScoredToken(-100, 1, 0);
		ArrayList<ScoredToken> list = new ArrayList<ScoredToken>(1);
		list.add(dummy);
	}

	public LanguageModelScore scoreParse(Parse<T> parse, TokenSequence<T> seq) {
		LanguageModelScore lmScore = lm.scoreSequence(seq);
		return lmScore;
	}

	public LanguageModelScore combineHypotheses(TokenSequence<T> tokensFromHyp1,
			LanguageModelScore scoreFromHyp1, TokenSequence<T> tokensFromHyp2,
			LanguageModelScore scoreFromHyp2, TokenSequence<T> combinedTokenSequence) {

		LanguageModelScore lmScore =
				lm.scoreBoundaryAndCombine(tokensFromHyp1, scoreFromHyp1, tokensFromHyp2,
						scoreFromHyp2);
		return lmScore;
	}

	public LanguageModelScore combineChildParseScores(Parse<T> accumulatedParse,
			TokenSequence<T> accumulatedSeq, LanguageModelScore accumulatedScore,
			Parse<T> addedChild, TokenSequence<T> addedSeq, LanguageModelScore addedScore) {

		LanguageModelScore lmScore =
				lm.scoreBoundaryAndCombine(accumulatedSeq, accumulatedScore, addedSeq, addedScore);
		return lmScore;
	}

	public LanguageModelScore scoreTerminalToken(TokenSequence<T> singleToken) {
		LanguageModelScore lmScore = lm.scoreSequence(singleToken);
		return lmScore;
	}

	public LanguageModelScore getInitialScore() {
		return new LanguageModelScore(Collections.EMPTY_LIST, 0.0);
	}

	public LanguageModelScore combineRuleScoreWithChildren(Parse<T> parse,
			LanguageModelScore parseScore, R ruleToAppend) {

		// This won't affect terminals, therefore the LM doesn't care
		return parseScore;
	}

	public String getFeatureName() {
		return "lm";
	}

	public String[] getFeatureProbVectorLabels() {
		return new String[] { "score" };
	}

	public double[] getFeatureWeightVector() {
		return weights;
	}
}
