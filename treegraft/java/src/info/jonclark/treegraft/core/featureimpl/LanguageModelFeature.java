package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.lang.Option;
import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.log.LogUtils;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.stat.TextProgressBar;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.lm.ARPALanguageModelLoader;
import info.jonclark.treegraft.core.lm.EfficientNGramLanguageModel;
import info.jonclark.treegraft.core.lm.LanguageModel;
import info.jonclark.treegraft.core.lm.LanguageModelLoader;
import info.jonclark.treegraft.core.lm.LanguageModelMultiScore;
import info.jonclark.treegraft.core.lm.LanguageModelScore;
import info.jonclark.treegraft.core.lm.EfficientNGramLanguageModel.EfficientNGramLanguageModelOptions;
import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.scoring.ProbUtils;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.core.tokens.integer.IntegerToken;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

// This class is NOT responsible for adding <s> and </s>. This is the responsibility of the ParseFactory
@OptionsTarget(LanguageModelFeature.LanguageModelOptions.class)
public class LanguageModelFeature<R extends GrammarRule<T>, T extends Token> implements
		Feature<R, T, LanguageModelMultiScore> {

	public static class LanguageModelOptions implements Options {

		@Option(name = "features.lm.weight", required = true, usage = "Log-linear lambda weight for the language model feature")
		public double lmWeight;

		@Option(name = "lm.modelFile", usage = "The language model file(s) (space delimited) to be used by the language model feature(s)", errorIfFileNotExists = true, arrayDelim = " ")
		public File[] lmFiles;

		@Option(name = "lm.encoding", usage = "The encoding for all language model files", required = false, defaultValue = "UTF-8")
		public String lmEncoding;
	}

	private final LanguageModel<T>[] lmArr;
	private double[] weights;
	private final ProfilerTimer lmParserTimer;
	private final ProfilerTimer lmHypTimer;
	private final ProfilerTimer lmTerminalTimer;

	private static final Logger log = LogUtils.getLogger();

	// TODO: How do we get the LM loader in here w/o layered reflection?
	public LanguageModelFeature(LanguageModelOptions opts, TreegraftConfig<R, T> config)
			throws IOException {

		log.info("Loading LMs...");

		// load each LM the user specified
		this.lmArr = new LanguageModel[opts.lmFiles.length];
		for (int i = 0; i < opts.lmFiles.length; i++) {

			File lmFile = opts.lmFiles[i];

			// TODO: Allow the user to specify which LM loader to use on a
			// per-LM basis
			// TODO: Allow the user to specify which kind of LM implementation
			// to use
			LanguageModelLoader<T> lmLoader = new ARPALanguageModelLoader<T>();

			// lmArr[i] = new
			// SimpleNGramLanguageModel<T>(config.profiler.featureTimer);
			EfficientNGramLanguageModelOptions childOpts =
					config.configurator.getOptions(EfficientNGramLanguageModelOptions.class);
			lmArr[i] =
					(LanguageModel<T>) new EfficientNGramLanguageModel(childOpts,
							(TreegraftConfig<?, IntegerToken>) config);
			InputStream stream = new FileInputStream(lmFile);
			if (lmFile.getName().endsWith(".gz")) {
				stream = new GZIPInputStream(stream);
			}

			TextProgressBar progressBar =
					new TextProgressBar(System.err, "n-gram", 100, config.opts.barWidth,
							config.opts.animatedBar);
			lmLoader.loadLM(lmArr[i], config.tokenFactory, stream, opts.lmEncoding,
					config.targetVocab, progressBar);

			this.weights = new double[] { opts.lmWeight };

			// ScoredToken dummy = new ScoredToken(-100, 1, 0);
			// ArrayList<ScoredToken> list = new ArrayList<ScoredToken>(1);
			// list.add(dummy);
		}

		this.lmParserTimer =
				ProfilerTimer.newTimer("lmParser", config.profiler.featureTimer, true, false);
		this.lmHypTimer =
				ProfilerTimer.newTimer("lmHyp", config.profiler.featureTimer, true, false);
		this.lmTerminalTimer =
				ProfilerTimer.newTimer("lmTerminal", config.profiler.featureTimer, true, false);
	}

	public LanguageModelMultiScore combineHypotheses(DecoderHypothesis<T> hyp1,
			TokenSequence<T> tokensFromHyp1, LanguageModelMultiScore scoreFromHyp1,
			DecoderHypothesis<T> hyp2, TokenSequence<T> tokensFromHyp2,
			LanguageModelMultiScore scoreFromHyp2, TokenSequence<T> combinedTokenSequence,
			List<T> inputSentence) {

		LanguageModelScore[] vec = new LanguageModelScore[lmArr.length];
		lmHypTimer.go();
		for (int i = 0; i < lmArr.length; i++) {
			vec[i] =
					lmArr[i].scoreBoundaryAndCombine(tokensFromHyp1, scoreFromHyp1.scores[i],
							tokensFromHyp2, scoreFromHyp2.scores[i], combinedTokenSequence);
			assert vec[i].getSequenceScore() <= 0.0 : "Log probs must be <= 0.0";
		}
		lmHypTimer.pause();
		return new LanguageModelMultiScore(vec);
	}

	public LanguageModelMultiScore combineChildParseScores(PartialParse<T> accumulatedParse,
			TokenSequence<T> accumulatedSeq, LanguageModelMultiScore accumulatedScore,
			PartialParse<T> addedChild, TokenSequence<T> addedSeq,
			LanguageModelMultiScore addedScore, TokenSequence<T> combinedSequence,
			List<T> inputSentence) {

		LanguageModelScore[] vec = new LanguageModelScore[lmArr.length];

		lmParserTimer.go();
		for (int i = 0; i < lmArr.length; i++) {
			vec[i] =
					lmArr[i].scoreBoundaryAndCombine(accumulatedSeq, accumulatedScore.scores[i],
							addedSeq, addedScore.scores[i], combinedSequence);
			assert vec[i].getSequenceScore() <= 0.0 : "Log probs must be <= 0.0";
		}
		lmParserTimer.pause();

		return new LanguageModelMultiScore(vec);
	}

	public LanguageModelMultiScore scoreTerminalParse(PartialParse<T> terminalParse,
			TokenSequence<T> seq) {

		LanguageModelScore[] vec = new LanguageModelScore[lmArr.length];

		lmTerminalTimer.go();
		for (int i = 0; i < lmArr.length; i++) {
			vec[i] = lmArr[i].scoreSequence(seq);
			assert vec[i].getSequenceScore() <= 0.0 : "Log probs must be <= 0.0";
		}
		lmTerminalTimer.pause();
		return new LanguageModelMultiScore(vec);
	}

	public LanguageModelMultiScore getInitialScore() {
		LanguageModelScore[] vec = new LanguageModelScore[lmArr.length];
		for (int i = 0; i < lmArr.length; i++) {
			vec[i] = new LanguageModelScore(Collections.EMPTY_LIST, 0.0);
		}
		return new LanguageModelMultiScore(vec);
	}

	public LanguageModelMultiScore combineRuleScoreWithChildren(PartialParse<T> parse,
			LanguageModelMultiScore parseScore, R ruleToAppend, List<T> inputSentence) {

		// This won't affect terminals, therefore the LM doesn't care
		return parseScore;
	}

	public LanguageModelMultiScore recombine(LanguageModelMultiScore a, LanguageModelMultiScore b) {
		LanguageModelScore[] vec = new LanguageModelScore[lmArr.length];
		for (int i = 0; i < lmArr.length; i++) {
			double sum =
					ProbUtils.sumInNonLogSpace(a.scores[i].getSequenceScore(),
							b.scores[i].getSequenceScore());
			vec[i] = new LanguageModelScore(a.scores[i].getScoredTokens(), sum);
		}
		return new LanguageModelMultiScore(vec);
	}

	public String getFeatureName() {
		return "lm";
	}

	public String[] getFeatureProbVectorLabels() {
		return new String[] { "score" };
	}

	public double[] getFeatureWeightVector() {
		return weights;
	}

	public void setFeatureWeightVector(double[] lambdas) {
		this.weights = lambdas;
	}
}
