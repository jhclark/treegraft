package info.jonclark.treegraft.core.featureimpl;

import info.jonclark.lang.Option;
import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.lang.Pair;
import info.jonclark.log.LogUtils;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.stat.TextProgressBar;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.scoring.ProbUtils;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.parsing.parses.Parse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.util.FileUtils;
import info.jonclark.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

@OptionsTarget(LexicalProbsFeature.LexicalProbsFeatureOptions.class)
public class LexicalProbsFeature<R extends GrammarRule<T>, T extends Token> implements
		Feature<R, T, LexicalProbsScore> {

	private double[] weights;
	private final ProfilerTimer lexProbTimer;
	private static final LexicalProbsScore DUMMY_SCORE = new LexicalProbsScore(1.0, 1.0);

	public static final int DEFAULT_LEX_PROB_SIZE = 1000000;

	// BOTH hashmaps index by source word THEN target word
	private final HashMap<Pair<T, T>, Double> sgtLex =
			new HashMap<Pair<T, T>, Double>(DEFAULT_LEX_PROB_SIZE);
	private final HashMap<Pair<T, T>, Double> tgsLex =
			new HashMap<Pair<T, T>, Double>(DEFAULT_LEX_PROB_SIZE);

	private static final Logger log = LogUtils.getLogger();

	public static class LexicalProbsFeatureOptions implements Options {

		@Option(name = "features.lex.sgt.weight", usage = "The log-linear lambda weight for the source-given-target lexical translation probability feature")
		public double lexSgtWeight;

		@Option(name = "features.lex.tgs.weight", usage = "The log-linear lambda weight for the target-given-source lexical translation probability feature")
		public double lexTgsWeight;

		@Option(name = "features.lex.sgtgiza", usage = "The source-given-target lexical probabilities file in GIZA (??? check this format) format (sourceWord prob targetWord)", errorIfFileNotExists = true)
		public File sgtFile;

		@Option(name = "features.lex.tgsgiza", usage = "The target-given-source lexical probabilities file in GIZA (??? check this format) format (targetWord prob sourceWord)", errorIfFileNotExists = true)
		public File tgsFile;
	}

	// TODO: Take in LexProbs object via LexProbsLoader
	public LexicalProbsFeature(LexicalProbsFeatureOptions opts, TreegraftConfig<R, T> config)
			throws IOException {

		log.info("Loading lexical probabilities...");
		TextProgressBar task =
				new TextProgressBar(System.err, "prob", 100, config.opts.barWidth,
						config.opts.animatedBar);

		this.lexProbTimer =
				ProfilerTimer.newTimer("lexProbs", config.profiler.featureTimer, true, false);
		this.weights = new double[] { opts.lexSgtWeight, opts.lexTgsWeight };

		System.out.println(opts.sgtFile);
		System.out.println(opts.tgsFile);
		
		int nEntries = FileUtils.countLines(opts.sgtFile) + FileUtils.countLines(opts.tgsFile);
		task.beginTask(nEntries);

		BufferedReader in = new BufferedReader(new FileReader(opts.sgtFile));
		String line;
		int nRead = 0;
		int nKept = 0;
		int nLine = 0;
		while ((line = in.readLine()) != null) {
			nLine++;

			// TODO: Make casing policy optional
			line = line.toLowerCase();

			String[] toks = StringUtils.tokenize(line);
			T source = config.tokenFactory.makeToken(toks[0], true);
			T target = config.tokenFactory.makeToken(toks[1], true);
			double prob = Double.parseDouble(toks[2]);

			nRead++;
			if ((config.sourceVocab == null || config.sourceVocab.contains(source))
					&& (config.targetVocab == null || config.targetVocab.contains(target))) {
				Pair<T, T> key = new Pair<T, T>(source, target);
				sgtLex.put(key, prob);
				nKept++;
			}
			task.recordEventCompletion();
		}
		in.close();

		// read opposite alignments AND FLIP!
		in = new BufferedReader(new FileReader(opts.tgsFile));
		while ((line = in.readLine()) != null) {

			// TODO: Make casing policy optional
			line = line.toLowerCase();

			String[] toks = StringUtils.tokenize(line);
			T target = config.tokenFactory.makeToken(toks[0], true);
			T source = config.tokenFactory.makeToken(toks[1], true);
			double prob = Double.parseDouble(toks[2]);

			nRead++;
			if ((config.sourceVocab == null || config.sourceVocab.contains(source))
					&& (config.targetVocab == null || config.targetVocab.contains(target))) {
				Pair<T, T> key = new Pair<T, T>(source, target);
				tgsLex.put(key, prob);
				nKept++;
			}
			task.recordEventCompletion();
		}
		in.close();

		// TODO: Smooth
		// TODO: Logify?

		task.endTask();

		log.info("Read " + nRead + " and kept " + nKept + " lexical probability entries.");
	}

	private static <T extends Token> double agbProb(HashMap<Pair<T, T>, Double> agbLexicon, T a, T b) {
		Pair<T, T> key = new Pair<T, T>(a, b);
		Double prob = agbLexicon.get(key);
		if (prob != null) {
			return prob;
		} else {
			// constant inherited from Lexicon.hh in STTK
			return 1.0e-10;
		}
	}

	/**
	 * Lexical translation probability of a tokens given b tokens according to
	 * the a given b lexicon
	 * 
	 * @param <T>
	 * @param agbLexicon
	 * @param aTokens
	 * @param bTokens
	 * @return
	 */
	private static <T extends Token> double calcAgbProb(HashMap<Pair<T, T>, Double> agbLexicon,
			List<T> aTokens, List<T> bTokens) {

		// TODO: fix this ugly slow method for computing lexical probabilities
		HashSet<T> aTokensSet = new HashSet<T>(aTokens);

		double sumLogP = 0.0;
		for (T bWord : bTokens) {
			double sumP = 0.0;
			for (T aWord : aTokensSet) {
				sumP += agbProb(agbLexicon, aWord, bWord);
			}
			if (sumP > 1.0 && sumP < 1.0 + ProbUtils.EPSILON)
				sumP = 1.0;
			double logP = ProbUtils.logProb(sumP);
			assert logP <= 0.0 : "Log probs must be <= 0.0";
			sumLogP += logP;
		}
		return sumLogP;
	}

	private LexicalProbsScore calcLexicalProb(Parse<T> a, Parse<T> b) {

		List<T> sourceTokens =
				new ArrayList<T>(a.getSourceTokens().size() + b.getSourceTokens().size());
		List<T> targetTokens =
				new ArrayList<T>(a.getTargetTokens().size() + b.getTargetTokens().size());
		sourceTokens.addAll(a.getSourceTokens());
		sourceTokens.addAll(b.getSourceTokens());
		targetTokens.addAll(a.getTargetTokens());
		targetTokens.addAll(b.getTargetTokens());

		double sgtLogProb = calcAgbProb(sgtLex, sourceTokens, targetTokens);
		double tgsLogProb = calcAgbProb(tgsLex, targetTokens, sourceTokens);
		assert sgtLogProb <= 0.0 : "Log probs must be <= 0.0";
		assert tgsLogProb <= 0.0 : "Log probs must be <= 0.0";
		return new LexicalProbsScore(sgtLogProb, tgsLogProb);
	}

	public LexicalProbsScore combineChildParseScores(Parse<T> accumulatedParse,
			TokenSequence<T> accumulatedSeq, LexicalProbsScore accumulatedScore,
			Parse<T> addedChild, TokenSequence<T> addedSeq, LexicalProbsScore addedScore,
			TokenSequence<T> combinedSeq, List<T> inputSentence) {

		if (accumulatedScore == DUMMY_SCORE || addedScore == DUMMY_SCORE) {
			// we have never given a valid seed to this parse before
			// so do it now

			lexProbTimer.go();
			LexicalProbsScore lexicalProb = calcLexicalProb(accumulatedParse, addedChild);
			lexProbTimer.pause();
			return lexicalProb;
		} else {

			// to save time, just sum in log space
			double sgt = accumulatedScore.sgt + addedScore.sgt;
			double tgs = accumulatedScore.tgs + addedScore.tgs;
			assert sgt <= 0.0 : "Log probs must be <= 0.0";
			assert tgs <= 0.0 : "Log probs must be <= 0.0";
			return new LexicalProbsScore(sgt, tgs);
		}
	}

	public LexicalProbsScore combineHypotheses(DecoderHypothesis<T> hyp1,
			TokenSequence<T> tokensFromHyp1, LexicalProbsScore scoreFromHyp1,
			DecoderHypothesis<T> hyp2, TokenSequence<T> tokensFromHyp2,
			LexicalProbsScore scoreFromHyp2, TokenSequence<T> combinedTokenSequence,
			List<T> inputSentence) {

		double sgt = scoreFromHyp1.sgt + scoreFromHyp2.sgt;
		double tgs = scoreFromHyp1.tgs + scoreFromHyp2.tgs;
		assert sgt <= 0.0 : "Log probs must be <= 0.0";
		assert tgs <= 0.0 : "Log probs must be <= 0.0";
		return new LexicalProbsScore(sgt, tgs);
	}

	public LexicalProbsScore recombine(LexicalProbsScore a, LexicalProbsScore b) {
		double sgt = ProbUtils.sumInNonLogSpace(a.sgt, b.sgt);
		double tgs = ProbUtils.sumInNonLogSpace(a.tgs, b.tgs);
		assert sgt <= 0.0 : "Log probs must be <= 0.0";
		assert tgs <= 0.0 : "Log probs must be <= 0.0";
		return new LexicalProbsScore(sgt, tgs);
	}

	public LexicalProbsScore combineRuleScoreWithChildren(Parse<T> parse,
			LexicalProbsScore parseScore, R ruleToAppend, List<T> inputSentence) {

		// no terminals change, no change in score
		return parseScore;
	}

	public String getFeatureName() {
		return "lex";
	}

	public String[] getFeatureProbVectorLabels() {
		return new String[] { "sgt", "tgs" };
	}

	public double[] getFeatureWeightVector() {
		return weights;
	}

	public void setFeatureWeightVector(double[] lambdas) {
		this.weights = lambdas;
	}

	public LexicalProbsScore getInitialScore() {
		return DUMMY_SCORE;
	}

	public LexicalProbsScore scoreTerminalParse(Parse<T> terminalParse, TokenSequence<T> seq) {

		return DUMMY_SCORE;
	}
}
