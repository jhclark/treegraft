package info.jonclark.treegraft.core.scoring;

import info.jonclark.util.FormatUtils;

// immutable (required to Parse to retain sanity)
// document where to find names of features
public class FeatureScores implements Scored {

	protected FeatureScore[] metadata;
	private double aggregateScore;
	private boolean isRecombined = false;

	// for use only by the FeatureScorer
	protected FeatureScores(int nFeatures) {
		this.metadata = new FeatureScore[nFeatures];
	}

	// public double getComponentLogProb(int nFeature) {
	// return scores[nFeature];
	// }

	// SLOW; not recommended
	public double[] getFeatureLogProbVector() {

		int nFeatures = 0;
		double[][] vectors = new double[metadata.length][];
		for (int i = 0; i < metadata.length; i++) {
			vectors[i] = metadata[i].getFeatureProbVector();
			nFeatures += vectors[i].length;
		}

		// now concatenate arrays
		double[] result = new double[nFeatures];
		int k = 0;
		for (int i = 0; i < vectors.length; i++) {
			for (int j = 0; j < vectors[i].length; j++) {
				result[k] = vectors[i][j];
				k++;
			}
		}
		return result;
	}

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
	
	public String toString() {
		StringBuilder builder = new StringBuilder(" ** " + FormatUtils.formatDouble2(aggregateScore) + " ** (");
		for(FeatureScore score : metadata) {
			for(double d : score.getFeatureProbVector()) {
				builder.append(FormatUtils.formatDouble2(d) + ", ");
			}
		}
		builder.append(")");
		return builder.toString();
	}
}
