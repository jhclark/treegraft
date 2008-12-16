package info.jonclark.treegraft.core.scoring;

/**
 * A class of static methods that handles how probabilities are handled.
 * 
 * @author Jonathan Clark
 */
public class ProbUtils {
	
	public static final double FLOOR = -100;
	public static final double EPSILON = 0.00001;

	/**
	 * Gets the log probability of the given number.
	 * 
	 * @param prob
	 *            a number in the probability domain.
	 * @return the corresponding number in the log probability domain
	 */
	public static double logProb(double prob) {
		return Math.log10(prob);
	}

	public static double unlog(double logProb) {
		// return Math.pow(logProb, 10); // faster, please
		return logProb * logProb * logProb * logProb * logProb * logProb * logProb * logProb
				* logProb * logProb;
	}

	public static double sumInNonLogSpace(double logProbA, double logProbB) {
		double sum = unlog(logProbA) + unlog(logProbB);
		return logProb(sum);
	}
}
