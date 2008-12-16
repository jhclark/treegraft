package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.treegraft.core.scoring.FeatureScore;

public class LengthScore implements FeatureScore {
	
	private double score;

	public LengthScore(double score) {
		this.score = score;
	}

	public double[] getFeatureProbVector() {
		return new double[] { score };
	}

}
