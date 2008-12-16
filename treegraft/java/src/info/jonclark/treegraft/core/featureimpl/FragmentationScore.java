package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.treegraft.core.scoring.FeatureScore;

public class FragmentationScore implements FeatureScore {
	
	public final double logFragBonus;
	public final int fragments;
	public final int sourceWords;
	
	public FragmentationScore(double logFragBonus, int fragments, int sourceWords) {
		this.logFragBonus = logFragBonus;
		this.fragments = fragments;
		this.sourceWords = sourceWords;
	}

	public double[] getFeatureProbVector() {
		return new double[] {logFragBonus};
	}
	
	
}
