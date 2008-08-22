package info.jonclark.treegraft.core.lm;

public class ScoredToken {

	public final double lmScore;
	public final int ngramAvailable;
	public final int ngramUsed;

	public ScoredToken(double lmScore, int ngramAvailable, int ngramUsed) {
		this.lmScore = lmScore;
		this.ngramAvailable = ngramAvailable;
		this.ngramUsed = ngramUsed;
	}
}
