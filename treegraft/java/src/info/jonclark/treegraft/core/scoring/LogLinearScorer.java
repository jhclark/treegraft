package info.jonclark.treegraft.core.scoring;

import info.jonclark.treegraft.core.parses.Parse;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.ArrayList;

// TODO: Address the issue of interpolation and each feature having multiple weights
public class LogLinearScorer<R extends GrammarRule<T>, T extends Token> implements Scorer<R, T> {

	private static final double ZERO_IN_LOG_SPACE = 1.0;
	private final Feature[] features;
	private final TokenFactory<T> tokenFactory;
	private final FeatureScores initialScores;

	public LogLinearScorer(Feature[] decoderFeatures, TokenFactory<T> tokenFactory) {
		this.features = decoderFeatures;
		this.tokenFactory = tokenFactory;

		this.initialScores = new FeatureScores(decoderFeatures.length);
		for (int i = 0; i < decoderFeatures.length; i++) {
			initialScores.metadata[i] = decoderFeatures[i].getInitialScore();
		}
	}

	/* (non-Javadoc)
	 * @see info.jonclark.treegraft.core.scoring.Scorer#scoreTerminalToken(T)
	 */
	public FeatureScores scoreTerminalToken(T token) {

		ArrayList<T> list = new ArrayList<T>(1);
		list.add(token);
		TokenSequence<T> singleToken = tokenFactory.makeTokenSequence(list);

		FeatureScores combinedScores = new FeatureScores(features.length);
		double interpolated = ZERO_IN_LOG_SPACE;
		for (int i = 0; i < features.length; i++) {
			combinedScores.metadata[i] = features[i].scoreTerminalToken(singleToken);

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

	/* (non-Javadoc)
	 * @see info.jonclark.treegraft.core.scoring.Scorer#combineRuleScoreWithChildren(info.jonclark.treegraft.core.parses.Parse, R)
	 */
	public FeatureScores combineRuleScoreWithChildren(Parse<T> currentLogProb, R ruleToAppend) {

		FeatureScores combinedScores = new FeatureScores(features.length);
		double interpolated = ZERO_IN_LOG_SPACE;
		for (int i = 0; i < features.length; i++) {
			combinedScores.metadata[i] =
					features[i].combineRuleScoreWithChildren(currentLogProb,
							currentLogProb.getScores().metadata[i], ruleToAppend);

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

	/* (non-Javadoc)
	 * @see info.jonclark.treegraft.core.scoring.Scorer#combineChildParseScores(info.jonclark.treegraft.core.parses.Parse, info.jonclark.treegraft.core.parses.Parse)
	 */
	public FeatureScores combineChildParseScores(Parse<T> accumulatedParse, Parse<T> addedChild) {

		TokenSequence<T> accumulatedSeq =
				tokenFactory.makeTokenSequence(accumulatedParse.getTargetTokens());
		TokenSequence<T> addedSeq = tokenFactory.makeTokenSequence(addedChild.getTargetTokens());

		FeatureScores combinedScores = new FeatureScores(features.length);
		double interpolated = ZERO_IN_LOG_SPACE;
		for (int i = 0; i < features.length; i++) {

			combinedScores.metadata[i] =
					features[i].combineChildParseScores(accumulatedParse, accumulatedSeq,
							accumulatedParse.getScores().metadata[i], addedChild, addedSeq,
							addedChild.getScores().metadata[i]);

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

	/* (non-Javadoc)
	 * @see info.jonclark.treegraft.core.scoring.Scorer#recombineParses(info.jonclark.treegraft.core.parses.Parse, info.jonclark.treegraft.core.parses.Parse)
	 */
	public FeatureScores recombineParses(Parse<T> a, Parse<T> b) {

		// take sum of recombined parses IFF we kept both
		// the ParseRecombiner has the option the throw away
		// the lower scoring parse, in which case we'll never get here

		double sum =
				ProbUtils.unlog(a.getScores().getLogProb())
						+ ProbUtils.unlog(b.getScores().getLogProb());

		FeatureScores recombinedScores = new FeatureScores(features.length);
		recombinedScores.setRecombined(true);
		recombinedScores.setInterpolatedLogProb(sum);
		return recombinedScores;
	}

	/* (non-Javadoc)
	 * @see info.jonclark.treegraft.core.scoring.Scorer#combineHypotheses(info.jonclark.treegraft.decoder.DecoderHypothesis, info.jonclark.treegraft.decoder.DecoderHypothesis, info.jonclark.treegraft.core.tokens.TokenSequence)
	 */
	public FeatureScores combineHypotheses(DecoderHypothesis<T> hyp1, DecoderHypothesis<T> hyp2,
			TokenSequence<T> combinedTokenSequence) {

		TokenSequence<T> seq1 = tokenFactory.makeTokenSequence(hyp1.getTokens());
		TokenSequence<T> seq2 = tokenFactory.makeTokenSequence(hyp2.getTokens());

		FeatureScores scores = new FeatureScores(features.length);
		double interpolated = ZERO_IN_LOG_SPACE;
		for (int i = 0; i < features.length; i++) {

			FeatureScore prevMetadata1 = hyp1.getScores().metadata[i];
			FeatureScore prevMetadata2 = hyp2.getScores().metadata[i];
			scores.metadata[i] =
					features[i].combineHypotheses(seq1, prevMetadata1, seq2, prevMetadata2,
							combinedTokenSequence);

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

	/* (non-Javadoc)
	 * @see info.jonclark.treegraft.core.scoring.Scorer#getInitialFeatureScores()
	 */
	public FeatureScores getInitialFeatureScores() {
		return initialScores;
	}
}
