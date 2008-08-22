package info.jonclark.treegraft.core.scoring;

// immutable (required to Parse to retain sanity)
public class FeatureScores implements Scored {
	
	protected FeatureScore[] metadata;
	private double aggregateScore;
	private boolean isRecombined = false;
	
	// for use only by the FeatureScorer
	protected FeatureScores(int nFeatures) {
		this.metadata = new FeatureScore[nFeatures];
	}
	
//	public double getComponentLogProb(int nFeature) {
//		return scores[nFeature];
//	}
	
	public double getLogProb() {
		return aggregateScore;
	}
	
	public boolean isRecombined() {
		return isRecombined;
	}
	
	protected void setInterpolatedLogProb(double logProb) {
		aggregateScore = logProb;
	}
	
	protected void setRecombined(boolean recombined) {
		this.isRecombined = recombined;
	}
}
