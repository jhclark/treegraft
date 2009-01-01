package info.jonclark.treegraft.parsing.merging;

import info.jonclark.lang.Option;
import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.log.LogUtils;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.scoring.FeatureScores;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.search.Beam;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.ParseFactory;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.parses.Tree;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.transduction.Transducer;
import info.jonclark.util.DebugUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

/**
 * Time critical!
 * 
 * @author jon
 * @param <R>
 * @param <T>
 */
@OptionsTarget(BeamSearchMerger.BeamSearchMergerOptions.class)
public class BeamSearchMerger<R extends GrammarRule<T>, T extends Token> implements Merger<R, T> {

	private final TokenFactory<T> tokenFactory;

	private final ProfilerTimer recombinationTimer;
	private final ProfilerTimer tokenCombinationTimer;
	private final ProfilerTimer featureCombinationTimer;

	public static class BeamSearchMergerOptions implements Options {
		@Option(name = "merger.recombinerClass", usage = "The fully-qualified Java class to be used for determining if two hypotheses can be recombined.")
		public Class<? extends Recombiner> recombiner;

		// TODO: Remove decoder recombination along with the DecoderHypothesis
		// class and the combineHypotheses method
		@Option(name = "decoder.doHypothesisRecombination", usage = "Should hypotheses that are equivalent according to the model be recombined during decoder search?")
		public boolean doHypothesisRecombination;

		@Option(name = "transfer.beamSize", usage = "Number of highest ranking hypotheses to return from the decoding process")
		public int transferBeam;

		@Option(name = "decoder.beamSize", usage = "Number of highest ranking hypotheses to return from the decoding process")
		public int decoderBeam;
	}
	private final BeamSearchMergerOptions opts;

	private static final Logger log = LogUtils.getLogger();

	public BeamSearchMerger(BeamSearchMergerOptions opts, TreegraftConfig<R, T> config) {

		this.opts = opts;

		this.tokenFactory = config.tokenFactory;

		ProfilerTimer parentTimer = config.profiler.treegraftTimer;
		this.recombinationTimer = ProfilerTimer.newTimer("recombination", parentTimer, true, false);
		this.tokenCombinationTimer =
				ProfilerTimer.newTimer("tokenCombination", parentTimer, true, false);
		this.featureCombinationTimer =
				ProfilerTimer.newTimer("featureCombination", parentTimer, true, false);
	}

	public List<PartialParse<T>> combineCrossProductOfChildParses(R parentRule,
			List<T> sourceInputTokens, int startIndex, int endIndex, Scorer<R, T> scorer,
			Transducer<R, T> transducer, ParseFactory<R, T> parseFactory,
			List<PartialParse<T>>... childParses) {

		// create a blank parse containing the LHS and any source terminals
		List<PartialParse<T>> currentResultParses = new Beam<PartialParse<T>>(opts.transferBeam);
		T sourceLhs = parentRule.getLhs();
		T[] sourceRhs = parentRule.getRhs();
		T targetLhs = transducer.transduceLhs(parentRule);
		T[] targetRhs = transducer.transduceRhs(parentRule);

		PartialParse<T> seedBlankParse =
				parseFactory.createParse(sourceInputTokens, startIndex, endIndex, sourceLhs,
						targetLhs, sourceRhs, targetRhs, scorer.getInitialFeatureScores());
		for (int sourceRhsIndex = 0; sourceRhsIndex < sourceRhs.length; sourceRhsIndex++) {
			if (sourceRhs[sourceRhsIndex].isTerminal()) {
				seedBlankParse.appendSourceTerminal(sourceRhsIndex, sourceRhs[sourceRhsIndex]);
			}
		}
		currentResultParses.add(seedBlankParse);

		// traverse left-to-right on the target side
		int[] targetToSourceAlignment = transducer.getTargetToSourceRhsAlignment(parentRule);

		// take the crossproduct of the current parse with the new parses
		for (int targetRhsIndex = 0; targetRhsIndex < childParses.length; targetRhsIndex++) {

			int sourceRhsIndex = targetToSourceAlignment[targetRhsIndex];
			List<PartialParse<T>> parseList = childParses[targetRhsIndex];

			currentResultParses =
					combine(scorer, sourceRhsIndex, targetRhsIndex, currentResultParses, parseList,
							sourceInputTokens);
		}

		checkSanity(parentRule, currentResultParses);

		return currentResultParses;
	}

	public static <R extends GrammarRule<T>, T extends Token> void checkSanity(R parentRule,
			List<PartialParse<T>> currentResultParses) {

		// check sanity of parse tree
		if (DebugUtils.isAssertEnabled()) {
			for (PartialParse<T> parse : currentResultParses) {

				Tree<T>[] sourceChildren = parse.getSourceTree().getChildren();
				for (int i = 0; i < sourceChildren.length; i++) {
					assert sourceChildren[i] != null : "null child at source RHS position " + i
							+ " for rule " + parentRule.toString();
				}
				Tree<T>[] targetChildren = parse.getTargetTree().getChildren();
				for (int i = 0; i < targetChildren.length; i++) {
					assert targetChildren[i] != null : "null child at target RHS position " + i
							+ " for rule " + parentRule.toString();
				}
			}
		}
	}

	private List<PartialParse<T>> combine(Scorer<R, T> scorer, int sourceRhsIndex,
			int targetRhsIndex, List<PartialParse<T>> currentParsesToBeCombined,
			List<PartialParse<T>> rightParsesToBeCombined, List<T> sourceTokens) {

		List<PartialParse<T>> combinedParses = new Beam<PartialParse<T>>(opts.transferBeam);

		Recombiner<R, T> recombiner =
				new BasicRecombiner<R, T>(currentParsesToBeCombined.size()
						* rightParsesToBeCombined.size(), scorer);

		for (PartialParse<T> parseFromBackpointer : rightParsesToBeCombined) {
			for (PartialParse<T> resultParse : currentParsesToBeCombined) {

				PartialParse<T> expandedParse = new PartialParse<T>(resultParse);
				expandedParse.appendParse(sourceRhsIndex, targetRhsIndex, parseFromBackpointer);

				// TODO: Add class for MarkovizedPartialParse (which can be
				// unpacked into PartialParses)
				// XXX: we don't have to re-calculte the LM for combined parses!
				if (!recombiner.recombine(expandedParse)) {

					// no recombination could be done, so we must continue with
					// a full scoring

					FeatureScores newScores =
							scorer.combineChildParseScores(parseFromBackpointer, resultParse,
									sourceTokens);
					expandedParse.setCurrentScore(newScores);
					combinedParses.add(expandedParse);
				}
			}
		}

		return combinedParses;
	}

	public void combineCrossProductOfHypotheses(Scorer<R, T> scorer,
			Beam<DecoderHypothesis<T>> beam1, Beam<DecoderHypothesis<T>> beam2,
			Beam<DecoderHypothesis<T>> outputBeam, List<T> sourceTokens) {

		// TODO: Only check for recombination if the score is good enough to get
		// it into the beam in the first place
		recombinationTimer.go();
		HashMap<TokenSequence<T>, DecoderHypothesis<T>> uniqueHypotheses =
				new HashMap<TokenSequence<T>, DecoderHypothesis<T>>(beam1.size() * beam2.size());
		recombinationTimer.pause();

		// TODO: Apply cube pruning right here
		for (DecoderHypothesis<T> hyp1 : beam1) {
			for (DecoderHypothesis<T> hyp2 : beam2) {

				// TODO: Combine sequences in more intelligent way
				tokenCombinationTimer.go();
				ArrayList<T> combinedTokens =
						new ArrayList<T>(hyp1.getTokens().size() + hyp2.getTokens().size());
				combinedTokens.addAll(hyp1.getTokens());
				combinedTokens.addAll(hyp2.getTokens());
				tokenCombinationTimer.pause();

				TokenSequence<T> combinedTokenSequence =
						tokenFactory.makeTokenSequence(combinedTokens);

				DecoderHypothesis<T> previousHypothesisWithSameYield = null;

				recombinationTimer.go();
				if (opts.doHypothesisRecombination)
					previousHypothesisWithSameYield = uniqueHypotheses.get(combinedTokenSequence);
				recombinationTimer.pause();

				// try to do hypothesis recomination -- any two hypotheses with
				// the same yield get lumped together
				if (!opts.doHypothesisRecombination || previousHypothesisWithSameYield == null) {

					// no previous hypothesis found
					DecoderHypothesis<T> combinedHyp =
							makeCombinedHypothesis(scorer, hyp1, hyp2, combinedTokens,
									combinedTokenSequence, sourceTokens);

					// HAAAAAAAACK
					// String strHyp =
					// StringUtils.untokenize(combinedHyp.getTokens());
					// if (combinedHyp.getSourceStartIndex() != 0
					// || LRStackDecoder.DESIRED_HYPOTHESIS.startsWith(strHyp))
					// {
					outputBeam.add(combinedHyp);
					// }
					// HAAAAAAAACK

					recombinationTimer.go();
					if (opts.doHypothesisRecombination)
						uniqueHypotheses.put(combinedTokenSequence, combinedHyp);
					recombinationTimer.pause();
				} else {

					// if we actually make the new hypothesis and keep scoring
					// it individually, we waste lots of time, but throwing it
					// away seems wrong too

					recombinationTimer.go();
					DecoderHypothesis<T> combinedHyp =
							makeCombinedHypothesis(scorer, hyp1, hyp2, combinedTokens,
									combinedTokenSequence, sourceTokens);

					if (combinedHyp.getLogProb() > previousHypothesisWithSameYield.getLogProb()) {
						combinedHyp.addRecombinedHypothesis(previousHypothesisWithSameYield);
						for (DecoderHypothesis<T> hyp : previousHypothesisWithSameYield.getRecombinedHypotheses()) {
							combinedHyp.addRecombinedHypothesis(hyp);
						}
						previousHypothesisWithSameYield.clearRecombinedHypotheses();
					} else {
						previousHypothesisWithSameYield.addRecombinedHypothesis(combinedHyp);
					}

					// FeatureScores recombinedScores =
					// scorer.recombineHypotheses(previousHypothesisWithSameYield
					// , combinedHyp);
					// previousHypothesisWithSameYield.setCurrentScore(
					// recombinedScores);
					// previousHypothesisWithSameYield.addRecombinedHypothesis(
					// combinedHyp);
					recombinationTimer.pause();
				}
			}
		}
	}

	private DecoderHypothesis<T> makeCombinedHypothesis(Scorer<R, T> scorer,
			DecoderHypothesis<T> hyp1, DecoderHypothesis<T> hyp2, ArrayList<T> combinedTokens,
			TokenSequence<T> combinedTokenSequence, List<T> sourceTokens) {

		featureCombinationTimer.go();
		FeatureScores combinedScore =
				scorer.combineHypotheses(hyp1, hyp2, combinedTokenSequence, sourceTokens);
		featureCombinationTimer.pause();

		ArrayList<PartialParse<T>> parses =
				new ArrayList<PartialParse<T>>(hyp1.getParses().size() + hyp2.getParses().size());
		parses.addAll(hyp1.getParses());
		parses.addAll(hyp2.getParses());

		// note: this span trick (hyp1.start, hyp2.end) only works
		// because we have a monotonic decoder
		DecoderHypothesis<T> combinedHyp =
				new DecoderHypothesis<T>(hyp1.getSourceStartIndex(), hyp2.getSourceEndIndex(),
						parses, combinedTokens, combinedScore);
		return combinedHyp;
	}
}
