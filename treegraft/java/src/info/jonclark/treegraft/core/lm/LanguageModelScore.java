package info.jonclark.treegraft.core.lm;

import info.jonclark.treegraft.core.scoring.FeatureScore;

import java.util.List;

public class LanguageModelScore implements FeatureScore {

	private final List<ScoredToken> scoredTokens;
	private final double sequenceScore;

	public LanguageModelScore(List<ScoredToken> scoredTokens, double sequenceScore) {
		this.scoredTokens = scoredTokens;
		this.sequenceScore = sequenceScore;
	}

	public List<ScoredToken> getScoredTokens() {
		return scoredTokens;
	}

	public double getSequenceScore() {
		return sequenceScore;
	}

	public double[] getFeatureProbVector() {
		return new double[] { sequenceScore };
	}
}
