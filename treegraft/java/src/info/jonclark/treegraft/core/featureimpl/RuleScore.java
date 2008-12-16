package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.treegraft.core.scoring.FeatureScore;
import info.jonclark.util.FormatUtils;

public class RuleScore implements FeatureScore {

	public final double sgt;
	public final double tgs;
	public static final RuleScore DEFAULT_RULE_SCORE = new RuleScore(0.0, 0.0);

	public RuleScore(double sgt, double tgs) {
		assert sgt <= 0 : "Rule scores should be in log domain.";
		assert tgs <= 0 : "Rule scores should be in log domain.";
		this.sgt = sgt;
		this.tgs = tgs;
	}

	public double[] getFeatureProbVector() {
		return new double[] { sgt, tgs };
	}

	public String toString() {
		return FormatUtils.formatDouble4(sgt) + " " + FormatUtils.formatDouble4(tgs);
	}
}
