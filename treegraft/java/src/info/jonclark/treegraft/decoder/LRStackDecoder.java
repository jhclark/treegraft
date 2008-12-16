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
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.chartparser.Key;
import info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.parsing.merging.Merger;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.ArrayList;
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
	private final ProfilerTimer decoderTimer;
	private final ProfilerTimer combineTimer;

	private static final Logger log = LogUtils.getLogger();

	public static class LRStackDecoderOptions implements Options {

		@Option(name = "decoder.nBest", usage = "Number of highest ranking hypotheses to return from the decoding process")
		public int nBest;
	}
	private final LRStackDecoderOptions opts;

	public LRStackDecoder(LRStackDecoderOptions opts, TreegraftConfig<R, T> config) {

		this.opts = opts;
		
		this.scorer = config.scorer;
		this.merger = config.merger;
		this.decoderTimer = config.profiler.decoderTimer;
		this.combineTimer = ProfilerTimer.newTimer("combine-hypotheses", decoderTimer, true, false);
	}

	public List<DecoderHypothesis<T>> getKBest(Chart<R, T> chart, ForestUnpacker<R, T> unpacker) {

		decoderTimer.go();

		// keep k-best translations for each non-terminal type and each span
		// by walking up the hypergraph, bottom-up

		final int N = chart.getInputLength();

		Beam<DecoderHypothesis<T>>[][] transferBeams = unpackTransferLattices(chart, unpacker, opts.nBest);

		// create one beam (stack) for each number of source words covered
		Beam<DecoderHypothesis<T>>[] decoderBeams = createEmptyDecoderBeams(opts.nBest, N);

		// i corresponds to # of src words covered - 1
		for (int i = 0; i < N; i++) {

			Beam<DecoderHypothesis<T>> outputBeam = decoderBeams[i];

			// copy hypotheses from the transfer beam that covers this whole
			// span
			outputBeam.addAll(transferBeams[0][i + 1]);

			// now merge transfer hypotheses ending at position i, but not
			// covering the whole input span, with decoder hypotheses starting
			// at 0
			// here, you can think of j as a "midpoint" splitting the
			// combination
			for (int j = 0; j < i; j++) {
				Beam<DecoderHypothesis<T>> decoderInputBeamLeft = decoderBeams[j];
				Beam<DecoderHypothesis<T>> transferInputBeamRight = transferBeams[j + 1][i + 1];
				if (log.isLoggable(Level.FINE)) {
					log.fine("Combining the " + decoderInputBeamLeft.currentSize()
							+ " decoder hypotheses from 0 to " + j + " with the "
							+ transferInputBeamRight.currentSize()
							+ " transfer hypotheses of from " + (j + 1) + " to " + (i + 1));
				}
				combineTimer.go();
				merger.combineCrossProductOfHypotheses(scorer, decoderInputBeamLeft,
						transferInputBeamRight, outputBeam, chart.getSourceInputTokens());
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
				log.fine("Created " + outputBeam.currentSize() + " hypotheses of length " + (i + 1));
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

	private Beam<DecoderHypothesis<T>>[] createEmptyDecoderBeams(int k, final int N) {
		Beam<DecoderHypothesis<T>>[] beams = new Beam[N];
		for (int i = 0; i < beams.length; i++) {
			beams[i] = new Beam<DecoderHypothesis<T>>(k);
		}
		return beams;
	}

	private Beam<DecoderHypothesis<T>>[][] createEmptyTransferBeams(int k, final int N) {
		Beam<DecoderHypothesis<T>>[][] beams = new Beam[N + 1][N + 1];
		for (int i = 0; i < beams.length; i++) {
			for (int j = 0; j < beams[i].length; j++) {
				beams[i][j] = new Beam<DecoderHypothesis<T>>(k);
			}
		}
		return beams;
	}

	public Beam<DecoderHypothesis<T>>[][] unpackTransferLattices(Chart<R, T> chart,
			ForestUnpacker<R, T> unpacker, int beamSize) {

		Beam<DecoderHypothesis<T>>[][] beams =
				createEmptyTransferBeams(beamSize, chart.getInputLength());

		// for each source key, keep a beam of the best hypotheses that it can
		// create each backpointer from higher-level keys will have access to
		// only the partial parses in that beam
		for (Key<R, T> key : chart.getKeys()) {
			List<Parse<T>> partialParses = unpacker.getPartialParses(key);
			for (Parse<T> parse : partialParses) {

				ArrayList<Parse<T>> singleParseList = new ArrayList<Parse<T>>(1);
				singleParseList.add(parse);

				// create a hypothesis for each partial parse coming out of the
				// transfer stage
				DecoderHypothesis<T> hyp =
						new DecoderHypothesis<T>(key.getStartIndex(), key.getEndIndex(),
								singleParseList, parse.getTargetTokens(), parse.getScores());
				beams[key.getStartIndex()][key.getEndIndex()].add(hyp);
			}
		}

		return beams;
	}
}
