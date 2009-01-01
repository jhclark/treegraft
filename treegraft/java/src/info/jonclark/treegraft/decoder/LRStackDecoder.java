package info.jonclark.treegraft.decoder;

import info.jonclark.lang.Option;
import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.log.LogUtils;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.search.Beam;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.merging.Merger;
import info.jonclark.treegraft.parsing.parses.ParseFactory;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleFactory;
import info.jonclark.treegraft.parsing.transduction.Transducer;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@OptionsTarget(LRStackDecoder.LRStackDecoderOptions.class)
public class LRStackDecoder<R extends GrammarRule<T>, T extends Token> implements Decoder<R, T> {

	// public static final String DESIRED_HYPOTHESIS =
	// "given award zayed international environment that fired year 1998 " +
	// "name state founder united arab emirates sheik zayed bin sultan of nahyan , "
	// +
	// "two people known in defense on environment . total value لجوائزها million dollars .";

	private final Scorer<R, T> scorer;
	private final Merger<R, T> merger;
	private final T glueLabel;
	private final ProfilerTimer decoderTimer;
	private final ProfilerTimer combineTimer;

	private static final Logger log = LogUtils.getLogger();

	public static class LRStackDecoderOptions implements Options {

		@Option(name = "decoder.nBest", usage = "Number of highest ranking hypotheses to return from the decoding process")
		public int nBest;
	}
	private final LRStackDecoderOptions opts;
	private final RuleFactory<R, T> ruleFactory;

	private final R decoderGlueRule;

	private final TreegraftConfig<R, T> config;

	public LRStackDecoder(LRStackDecoderOptions opts, TreegraftConfig<R, T> config) {

		this.config = config;
		this.opts = opts;
		this.ruleFactory = config.ruleFactory;

		this.scorer = config.scorer;
		this.merger = config.merger;
		this.glueLabel = config.tokenFactory.makeToken("DECODER", false);

		T[] glueRhs = config.tokenFactory.makeTokens(new String[] { "*", "*" }, false);
		this.decoderGlueRule = config.ruleFactory.makeGlueRule(glueLabel, glueRhs, glueRhs);

		this.decoderTimer = config.profiler.decoderTimer;
		this.combineTimer = ProfilerTimer.newTimer("combine-hypotheses", decoderTimer, true, false);
	}

	public List<PartialParse<T>> getKBest(Lattice<R, T> targetLattice,
			ParseFactory<R, T> parseFactory) {

		decoderTimer.go();

		int N = targetLattice.getInputLength();

		// create one beam (stack) for each number of source words covered
		// NOTE: The List actually uses a Beam implementation
		List<PartialParse<T>>[] decoderBeams = createEmptyDecoderBeams(opts.nBest, N);

		// append sentence markers for the LM
		for (int i = 1; i <= N; i++) {
			for (PartialParse<T> parse : targetLattice.getPartialParses(0, i)) {
				parse.prependTargetTerminal(config.bos);
			}
		}
		for (int i = 0; i < N; i++) {
			for (PartialParse<T> parse : targetLattice.getPartialParses(i, N)) {
				parse.appendTargetTerminal(config.eos);
			}
		}

		// i corresponds to # of src words covered - 1
		for (int i = 0; i < N; i++) {

			// List<PartialParse<T>> outputBeam = decoderBeams[i];

			// copy hypotheses from the transfer beam that covers this whole
			// span transferBeams[0][i + 1]
			decoderBeams[i].addAll(targetLattice.getPartialParses(0, i + 1));

			// TODO: Add BOS here
			// ----------------------------------------------------------

			// now merge transfer hypotheses ending at position i, but not
			// covering the whole input span, with decoder hypotheses starting
			// at 0
			// here, you can think of j as a "midpoint" splitting the
			// combination
			for (int j = 0; j < i; j++) {
				List<PartialParse<T>> decoderInputBeamLeft = decoderBeams[j];

				// transferBeams[j + 1][i + 1]
				List<PartialParse<T>> transferInputBeamRight = targetLattice.getPartialParses(j + 1, i + 1);
				if (log.isLoggable(Level.FINE)) {
					log.fine("Combining the " + decoderInputBeamLeft.size()
							+ " decoder hypotheses from 0 to " + j + " with the "
							+ transferInputBeamRight.size() + " transfer hypotheses of from "
							+ (j + 1) + " to " + (i + 1));
				}
				combineTimer.go();

				// public List<PartialParse<T>>
				// combineCrossProductOfChildParses(R parentRule,
				// List<T> sourceInputTokens, int sourceInputStartIndex, int
				// sourceInputEndIndex,
				// Scorer<R, T> scorer, Transducer<R, T> transducer,
				// ParseFactory<R, T> parseFactory,
				// List<PartialParse<T>>... childParses);

				// TODO: Add EOS here
				// ----------------------------------------------------------
				// if we're covering the whole input span

				decoderBeams[i] =
						merger.combineCrossProductOfChildParses(decoderGlueRule,
								targetLattice.getSourceInputTokens(), j + 1, i + 1, scorer,
								ruleFactory.getTransducer(), parseFactory, decoderInputBeamLeft,
								transferInputBeamRight);
				combineTimer.pause();
			}

			// make sure we still have a desireable prefix somewhere in this new
			// shiny combined beam

			// log.info(outputBeam.toString());
			//
			// boolean found = false;
			// Beam<DecoderHypothesis<T>> newOutputBeam =
			// new Beam<DecoderHypothesis<T>>(outputBeam.maxSize());
			// for (DecoderHypothesis<T> hyp : outputBeam) {
			// String strHyp = StringUtils.untokenize(hyp.getTokens());
			// if (DESIRED_HYPOTHESIS.startsWith(strHyp)) {
			// found = true;
			// System.out.println("Got " + strHyp + " from " +
			// hyp.getSourceStartIndex()
			// + " to " + hyp.getSourceEndIndex());
			// newOutputBeam.add(hyp);
			// }
			// }
			// decoderBeams[i] = newOutputBeam;
			// if (!found) {
			// log.warning("It go bye bye.");
			// // TODO: Look at transfer beams here and see where our
			// // hypothesis dropped out
			// }

			if (log.isLoggable(Level.FINE)) {
				log.fine("Created " + decoderBeams[i].size() + " hypotheses of length " + (i + 1));
			}
		}

		decoderTimer.pause();

		// TODO: Fix this completely screwed up method of combining LM scores
		// TODO: Hypothesis recombination
		// TODO: Support translation of lattices

		// TODO: Where do we store this LM score along with the hypothesis?
		// TODO: How do we fit the LM cleanly into this feature architecture?
		// Can each feature in the FeatureScore object have its own meta-data
		// component?

		// return the hypotheses that cover the whole input
		return decoderBeams[N - 1];
	}

	private Beam<PartialParse<T>>[] createEmptyDecoderBeams(int k, final int N) {
		Beam<PartialParse<T>>[] beams = new Beam[N];
		for (int i = 0; i < beams.length; i++) {
			beams[i] = new Beam<PartialParse<T>>(k);
		}
		return beams;
	}
}
