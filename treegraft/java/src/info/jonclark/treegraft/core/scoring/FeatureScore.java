package info.jonclark.treegraft.core.scoring;

// FEATURE SCORES MUST BE IMMUTABLE!!!
// IS THERE ANY WAY TO REMOVE THIS REQUIREMENT???
public interface FeatureScore {

	/**
	 * The returned array must have the same length as the parent feature's
	 * getFeatureProbVectorLabels() array.
	 * 
	 * @return
	 */
	public double[] getFeatureProbVector();
}
