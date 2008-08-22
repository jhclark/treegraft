package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.treegraft.core.scoring.FeatureScore;

public class RuleScore implements FeatureScore {
	
	public final double sgt;
	
	public RuleScore(double sgt) {
		this.sgt = sgt;
	}

	public double[] getFeatureProbVector() {
		return new double[] {sgt};
	}
	
	
}
