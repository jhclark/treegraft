package info.jonclark.treegraft.core.lm;

import info.jonclark.treegraft.core.scoring.FeatureScore;

public class LanguageModelMultiScore implements FeatureScore {

	public final LanguageModelScore[] scores;

	public LanguageModelMultiScore(LanguageModelScore[] scores) {
		this.scores = scores;
	}

	public double[] getFeatureProbVector() {
		double[] vec = new double[scores.length];
		for(int i=0; i<scores.length; i++) {
			vec[i] = scores[i].getSequenceScore();
		}
		return vec;
	}
}
