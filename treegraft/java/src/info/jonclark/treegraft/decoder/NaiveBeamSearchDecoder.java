package info.jonclark.treegraft.decoder;

import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.search.Beam;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.chartparser.Key;
import info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.parsing.merging.Merger;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.util.ArrayList;
import java.util.List;

public class NaiveBeamSearchDecoder<R extends GrammarRule<T>, T extends Token> {

	private final TokenFactory<T> tokenFactory;
	private final Scorer<R, T> scorer;
	private final Merger<R, T> merger;
	private final ProfilerTimer decoderTimer;
	private final ProfilerTimer combineTimer;

	public NaiveBeamSearchDecoder(TokenFactory<T> tokenFactory, Scorer<R, T> scorer,
			Merger<R, T> merger, ProfilerTimer decoderTimer) {

		this.tokenFactory = tokenFactory;
		this.scorer = scorer;
		this.merger = merger;
		this.decoderTimer = decoderTimer;
		this.combineTimer = ProfilerTimer.newTimer("combine-hypotheses", decoderTimer, true, false);
	}

	public List<DecoderHypothesis<T>> getKBest(Chart<R, T> chart, ForestUnpacker<R, T> unpacker,
			int k) {

		// keep k-best translations for each non-terminal type and each span
		// by walking up the hypergraph, bottom-up

		final int N = chart.getInputLength();
		Beam<DecoderHypothesis<T>>[][] beams = createEmptyBeams(k, N);
		seedBeamsWithPartialParses(chart, unpacker, beams);

		// now that we've seeded our possible translations, start propagating
		// possibilities up through the spans
		decode(beams, chart);

		// return the hypotheses that cover the whole input
		return beams[0][N];
	}

	private Beam<DecoderHypothesis<T>>[][] createEmptyBeams(int k, final int N) {
		Beam<DecoderHypothesis<T>>[][] beams = new Beam[N + 1][N + 1];
		for (int i = 0; i < beams.length; i++) {
			for (int j = 0; j < beams[i].length; j++) {
				beams[i][j] = new Beam<DecoderHypothesis<T>>(k);
			}
		}
		return beams;
	}

	private void seedBeamsWithPartialParses(Chart<R, T> chart, ForestUnpacker<R, T> unpacker,
			Beam<DecoderHypothesis<T>>[][] beams) {

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
	}

	private void decode(Beam<DecoderHypothesis<T>>[][] beams, Chart<R, T> chart) {

		final int N = beams.length - 1;

		// iterate over possible beams to output to and pairs of beams to read
		// from
		for (int hypothesisLength = 2; hypothesisLength < N + 1; hypothesisLength++) {
			System.out.println("Creating hypotheses of length " + hypothesisLength);
			for (int outputStart = 0; outputStart + hypothesisLength < N + 1; outputStart++) {
				int outputEnd = outputStart + hypothesisLength;
				for (int midpoint = outputStart + 1; midpoint < outputEnd; midpoint++) {

					int input1Start = outputStart, input1End = midpoint, input2Start = midpoint, input2End =
							outputEnd;

					// create a new output hypothesis by joining the
					// 2 input hypotheses
					Beam<DecoderHypothesis<T>> outputBeam = beams[outputStart][outputEnd];
					Beam<DecoderHypothesis<T>> beam1 = beams[input1Start][input1End];
					Beam<DecoderHypothesis<T>> beam2 = beams[input2Start][input2End];

					combineTimer.go();
					merger.combineCrossProductOfHypotheses(scorer, beam1, beam2,
							outputBeam, chart.getSourceInputTokens());
					combineTimer.pause();

					// System.out.println("Combined (" + input1Start + ", " +
					// input1End + ")["
					// + beam1.currentSize() + " hypotheses] with (" +
					// input2Start + ", "
					// + input2End + ")[" + beam2.currentSize() +
					// " hypotheses] into ("
					// + outputStart + ", " + outputEnd + ")[" +
					// outputBeam.currentSize()
					// + " hypotheses]");
				}
			}
		}
	}

	// TODO: Fix this completely screwed up method of combining LM scores
	// TODO: Hypothesis recombination
	// TODO: Support translation of lattices

	// TODO: Where do we store this LM score along with the hypothesis?
	// TODO: How do we fit the LM cleanly into this feature architecture?
	// Can each feature in the FeatureScore object have its own meta-data
	// component?
}
