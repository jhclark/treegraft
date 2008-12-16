package info.jonclark.treegraft.core.scoring;

import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.log.LogUtils;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.List;
import java.util.logging.Logger;

// TODO: Address the issue of interpolation and each feature having multiple weights
@OptionsTarget(LogLinearScorer.LogLinearScorerOptions.class)
public class LogLinearScorer<R extends GrammarRule<T>, T extends Token> implements Scorer<R, T> {

	private static final double ZERO = 0.0;
	private final Feature[] features;
	private final TokenFactory<T> tokenFactory;
	private final FeatureScores initialScores;

	private static final Logger log = LogUtils.getLogger();

	private ProfilerTimer[] featureHypCombineTimers;
	
	public static class LogLinearScorerOptions implements Options {
		
	}

	public LogLinearScorer(LogLinearScorerOptions opts, TreegraftConfig config) {
		
//		Feature[] decoderFeatures, TokenFactory<T> tokenFactory,
//		ProfilerTimer parent
		
		this.features = config.features;
		this.tokenFactory = config.tokenFactory;

		this.initialScores = new FeatureScores(features.length);
		this.featureHypCombineTimers = new ProfilerTimer[features.length];
		for (int i = 0; i < features.length; i++) {
			initialScores.metadata[i] = features[i].getInitialScore();
			this.featureHypCombineTimers[i] =
					ProfilerTimer.newTimer(features[i].getFeatureName() + "-combineHyps",
							config.profiler.processingTimer, true, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see info.jonclark.treegraft.core.scoring.Scorer#scoreTerminalToken(T)
	 */
	public FeatureScores scoreTerminalParse(Parse<T> terminalParse) {

		TokenSequence<T> seq = tokenFactory.makeTokenSequence(terminalParse.getTargetTokens());

		FeatureScores combinedScores = new FeatureScores(features.length);
		double interpolated = ZERO;
		for (int i = 0; i < features.length; i++) {
			combinedScores.metadata[i] = features[i].scoreTerminalParse(terminalParse, seq);

			double[] probVector = combinedScores.metadata[i].getFeatureProbVector();
			double[] weights = features[i].getFeatureWeightVector();
			assert probVector.length == weights.length;
			for (int j = 0; j < weights.length; j++) {
				interpolated += probVector[j] * weights[j];
			}
		}
		combinedScores.setInterpolatedLogProb(interpolated);
		return combinedScores;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * info.jonclark.treegraft.core.scoring.Scorer#combineRuleScoreWithChildren
	 * (info.jonclark.treegraft.core.parses.Parse, R)
	 */
	public FeatureScores combineRuleScoreWithChildren(Parse<T> currentLogProb, R ruleToAppend,
			List<T> inputSentence) {

		FeatureScores combinedScores = new FeatureScores(features.length);
		double interpolated = ZERO;
		for (int i = 0; i < features.length; i++) {
			combinedScores.metadata[i] =
					features[i].combineRuleScoreWithChildren(currentLogProb,
							currentLogProb.getScores().metadata[i], ruleToAppend, inputSentence);

			double[] probVector = combinedScores.metadata[i].getFeatureProbVector();
			double[] weights = features[i].getFeatureWeightVector();
			assert probVector.length == weights.length;
			for (int j = 0; j < weights.length; j++) {
				interpolated += probVector[j] * weights[j];
			}
		}
		combinedScores.setInterpolatedLogProb(interpolated);
		return combinedScores;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * info.jonclark.treegraft.core.scoring.Scorer#combineChildParseScores(info
	 * .jonclark.treegraft.core.parses.Parse,
	 * info.jonclark.treegraft.core.parses.Parse)
	 */
	public FeatureScores combineChildParseScores(Parse<T> accumulatedParse, Parse<T> addedChild,
			List<T> inputSentence) {

		TokenSequence<T> accumulatedSeq =
				tokenFactory.makeTokenSequence(accumulatedParse.getTargetTokens());
		TokenSequence<T> addedSeq = tokenFactory.makeTokenSequence(addedChild.getTargetTokens());

		TokenSequence<T> combinedSeq = accumulatedSeq.append(addedSeq);

		FeatureScores combinedScores = new FeatureScores(features.length);
		double interpolated = ZERO;
		for (int i = 0; i < features.length; i++) {

			combinedScores.metadata[i] =
					features[i].combineChildParseScores(accumulatedParse, accumulatedSeq,
							accumulatedParse.getScores().metadata[i], addedChild, addedSeq,
							addedChild.getScores().metadata[i], combinedSeq, inputSentence);

			double[] probVector = combinedScores.metadata[i].getFeatureProbVector();
			double[] weights = features[i].getFeatureWeightVector();
			assert probVector.length == weights.length;
			for (int j = 0; j < weights.length; j++) {
				interpolated += probVector[j] * weights[j];
			}
		}
		combinedScores.setInterpolatedLogProb(interpolated);
		return combinedScores;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * info.jonclark.treegraft.core.scoring.Scorer#recombineParses(info.jonclark
	 * .treegraft.core.parses.Parse, info.jonclark.treegraft.core.parses.Parse)
	 */
	public FeatureScores recombineParses(Parse<T> a, Parse<T> b) {

		// take sum of recombined parses IFF we kept both
		// the ParseRecombiner has the option the throw away
		// the lower scoring parse, in which case we'll never get here

		// double sum =
		// ProbUtils.unlog(a.getScores().getLogProb())
		// + ProbUtils.unlog(b.getScores().getLogProb());

		FeatureScores recombinedScores = new FeatureScores(features.length);
		double interpolated = ZERO;
		for (int i = 0; i < features.length; i++) {
			recombinedScores.metadata[i] =
					features[i].recombine(a.getScores().metadata[i], b.getScores().metadata[i]);

			double[] probVector = recombinedScores.metadata[i].getFeatureProbVector();
			double[] weights = features[i].getFeatureWeightVector();
			assert probVector.length == weights.length;
			for (int j = 0; j < weights.length; j++) {
				interpolated += probVector[j] * weights[j];
			}
		}
		recombinedScores.setRecombined(true);
		recombinedScores.setInterpolatedLogProb(interpolated);
		return recombinedScores;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * info.jonclark.treegraft.core.scoring.Scorer#combineHypotheses(info.jonclark
	 * .treegraft.decoder.DecoderHypothesis,
	 * info.jonclark.treegraft.decoder.DecoderHypothesis,
	 * info.jonclark.treegraft.core.tokens.TokenSequence)
	 */
	public FeatureScores combineHypotheses(DecoderHypothesis<T> hyp1, DecoderHypothesis<T> hyp2,
			TokenSequence<T> combinedTokenSequence, List<T> inputSentence) {

		TokenSequence<T> seq1 = tokenFactory.makeTokenSequence(hyp1.getTokens());
		TokenSequence<T> seq2 = tokenFactory.makeTokenSequence(hyp2.getTokens());

		FeatureScores scores = new FeatureScores(features.length);
		double interpolated = ZERO;
		for (int i = 0; i < features.length; i++) {

			FeatureScore prevMetadata1 = hyp1.getScores().metadata[i];
			FeatureScore prevMetadata2 = hyp2.getScores().metadata[i];
			this.featureHypCombineTimers[i].go();
			scores.metadata[i] =
					features[i].combineHypotheses(hyp1, seq1, prevMetadata1, hyp2, seq2,
							prevMetadata2, combinedTokenSequence, inputSentence);
			this.featureHypCombineTimers[i].pause();

			double[] probVector = scores.metadata[i].getFeatureProbVector();
			double[] weights = features[i].getFeatureWeightVector();
			assert probVector.length == weights.length;
			for (int j = 0; j < weights.length; j++) {
				interpolated += probVector[j] * weights[j];
			}
		}
		scores.setInterpolatedLogProb(interpolated);
		return scores;
	}

	public FeatureScores recombineHypotheses(DecoderHypothesis<T> a, DecoderHypothesis<T> b) {

		// take sum of recombined hypotheses IFF we kept both
		// the Merger has the option the throw away
		// the lower scoring parse, in which case we'll never get here

		// double sum =
		// ProbUtils.unlog(a.getScores().getLogProb())
		// + ProbUtils.unlog(b.getScores().getLogProb());

		FeatureScores recombinedScores = new FeatureScores(features.length);
		double interpolated = ZERO;
		for (int i = 0; i < features.length; i++) {
			recombinedScores.metadata[i] =
					features[i].recombine(a.getScores().metadata[i], b.getScores().metadata[i]);

			double[] probVector = recombinedScores.metadata[i].getFeatureProbVector();
			double[] weights = features[i].getFeatureWeightVector();
			assert probVector.length == weights.length;
			for (int j = 0; j < weights.length; j++) {
				interpolated += probVector[j] * weights[j];
			}
		}
		recombinedScores.setRecombined(true);
		recombinedScores.setInterpolatedLogProb(interpolated);
		return recombinedScores;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * info.jonclark.treegraft.core.scoring.Scorer#getInitialFeatureScores()
	 */
	public FeatureScores getInitialFeatureScores() {
		return initialScores;
	}

	public String[] getFeatureProbVectorLabels() {

		int nFeatures = 0;
		String[][] vectors = new String[features.length][];
		for (int i = 0; i < features.length; i++) {
			vectors[i] = features[i].getFeatureProbVectorLabels();
			nFeatures += vectors[i].length;
		}

		// now concatenate arrays
		String[] result = new String[nFeatures];
		int k = 0;
		for (int i = 0; i < vectors.length; i++) {
			for (int j = 0; j < vectors[i].length; j++) {
				result[k] = features[i].getFeatureName() + "." + vectors[i][j];
				k++;
			}
		}
		return result;
	}

	public double[] getFeatureWeightVector() {

		int nFeatures = 0;
		double[][] vectors = new double[features.length][];
		for (int i = 0; i < features.length; i++) {
			vectors[i] = features[i].getFeatureWeightVector();
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

	public void setFeatureWeightVector(double[] lambdas) {

		if (lambdas.length != getFeatureWeightVector().length) {
			throw new RuntimeException("New lambda vector has incorrect length " + lambdas.length
					+ "previous size was " + getFeatureWeightVector().length);
		}

		int iLambda = 0;
		for (int iFeature = 0; iFeature < features.length; iFeature++) {

			String[] names = features[iFeature].getFeatureProbVectorLabels();
			int nWeights = features[iFeature].getFeatureWeightVector().length;

			double[] weightVectorForFeature = new double[nWeights];
			for (int iWeight = 0; iWeight < nWeights; iWeight++) {
				weightVectorForFeature[iWeight] = lambdas[iLambda];
				log.info("Setting " + features[iFeature].getFeatureName() + "." + names[iWeight]
						+ " = " + lambdas[iLambda]);
				iLambda++;
			}
			features[iFeature].setFeatureWeightVector(weightVectorForFeature);
		}
	}
}
