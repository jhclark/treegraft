package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.treegraft.core.scoring.FeatureScore;

public class LexicalProbsScore implements FeatureScore {

	public final double sgt;
	public final double tgs;

	public LexicalProbsScore(double sgt, double tgs) {
		this.sgt = sgt;
		this.tgs = tgs;
	}

	public double[] getFeatureProbVector() {
		return new double[] { sgt, tgs };
	}
}
