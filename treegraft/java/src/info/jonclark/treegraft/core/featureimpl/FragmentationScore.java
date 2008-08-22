package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.treegraft.core.scoring.FeatureScore;

public class FragmentationScore implements FeatureScore {
	
	public final int fragments;
	
	public FragmentationScore(int fragments) {
		this.fragments = fragments;
	}

	public double[] getFeatureProbVector() {
		return new double[] {fragments};
	}
	
	
}
