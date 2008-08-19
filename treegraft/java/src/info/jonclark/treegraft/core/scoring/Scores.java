package info.jonclark.treegraft.core.scoring;

public class Scores implements Scored {
	
	private double[] scores;
	private double aggregateScore;
	
	public Scores(double aggregateScore) {
		this.aggregateScore = aggregateScore;
	}
	
	public Scores(Scores other) {
		this.scores = other.scores;
		this.aggregateScore = other.aggregateScore;
	}
	
//	public double getComponentLogProb(int nFeature) {
//		return scores[nFeature];
//	}
	
	public double getLogProb() {
		return aggregateScore;
	}
}
