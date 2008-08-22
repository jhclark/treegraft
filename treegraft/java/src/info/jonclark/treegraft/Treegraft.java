package info.jonclark.treegraft;

import info.jonclark.log.LogUtils;
import info.jonclark.properties.SmartProperties;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.treegraft.core.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.core.lm.ARPALanguageModelLoader;
import info.jonclark.treegraft.core.lm.LanguageModel;
import info.jonclark.treegraft.core.lm.LanguageModelFeature;
import info.jonclark.treegraft.core.lm.NGramLanguageModel;
import info.jonclark.treegraft.core.mergingX.BeamSearchParsePruner;
import info.jonclark.treegraft.core.parses.BasicTreeFormatter;
import info.jonclark.treegraft.core.parses.Parse;
import info.jonclark.treegraft.core.parses.TreeFormatter;
import info.jonclark.treegraft.core.recombination.YieldRecombiner;
import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.scoring.LogLinearScorer;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.string.StringTokenFactory;
import info.jonclark.treegraft.decoder.BeamSearchDecoder;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.distributed.multithreading.MultithreadedTreegraft;
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.chartparser.ChartParser;
import info.jonclark.treegraft.parsing.grammar.Grammar;
import info.jonclark.treegraft.parsing.grammar.GrammarLoader;
import info.jonclark.treegraft.parsing.oov.OutOfVocabularyHandler;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.rules.RuleFactory;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGGrammarLoader;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRuleFactory;
import info.jonclark.util.FormatUtils;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

// TODO: Allow reflection of custom classes from properties files
// ...this means all classes that implement interfaces have to take in properties files at some point?
public class Treegraft<R extends GrammarRule<T>, T extends Token> {

	protected static final Logger log = LogUtils.getLogger();

	protected final int nBest;
	protected final Feature[] features;
	protected final RuleFactory<R, T> ruleFactory;
	protected final TokenFactory<T> tokenFactory;
	protected final OutOfVocabularyHandler<R, T> oovHandler;
	protected final Grammar<R, T> grammar;

	protected final String[] sentences;
	protected final Result<T>[] results;

	public static class ReflectionException extends Exception {
		private static final long serialVersionUID = 391200726464598307L;

		public ReflectionException(String str, Throwable t) {
			super(str, t);
		}
	}

	public class Result<K extends Token> {

		private Throwable t;
		private List<DecoderHypothesis<T>> nBestList;

		public Result(Throwable t) {
			// sentence failed to complete
			this.t = t;
		}

		public Result(List<DecoderHypothesis<T>> nBestList) {
			this.nBestList = nBestList;
		}

		public String toString() {
			if (t == null) {

				StringBuilder builder = new StringBuilder();
				for (int i = 0; i < nBestList.size(); i++) {

					// TODO: Show where phrase boundaries are (and output trees?
					// --
					// including recombined ambiguities)

					TreeFormatter<T> formatter =
							new BasicTreeFormatter<T>(tokenFactory, true, true);

					DecoderHypothesis<T> hyp = nBestList.get(i);
					String[] tokens = tokenFactory.getTokensAsStrings(hyp.getTokens());
					builder.append((i + 1) + ": " + FormatUtils.formatDouble4(hyp.getLogProb())
							+ " " + StringUtils.untokenize(tokens) + " :: ");
					for (Parse<T> parse : hyp.getParses()) {
						builder.append(parse.getTargetTree().toString(formatter) + " // ");
					}
					builder.append(" :: ");
					for (Parse<T> parse : hyp.getParses()) {
						builder.append(parse.getSourceTree().toString(formatter) + " // ");
					}
					builder.append("\n");
				}
				return builder.toString();
			} else {
				return StringUtils.getStackTrace(t);
			}
		}
	}

	private <X extends Token> Object reflect(String classname, SmartProperties props)
			throws ReflectionException {
		try {
			Class<?> clazz = Class.forName(classname);

			// everything should be parameterized by <R extends GrammarRule<T>,
			// T extends Token>
			// Class<?> iface;
			// for(Class<?> iface : clazz.getInterfaces()) {
			// if(iface.getCanonicalName(""))
			// }
			//			
			// TypeVariable<?>[] typeParameters = getTypeParameters();
			// T dummyToken = tokenFactory.makeToken("X", true);
			//typeParameters[0].getClass().isInstance(ruleFactory.makeDummyRule(
			// dummyToken));
			// typeParameters[1].getClass().isInstance(dummyToken);

			Constructor<?> constructor =
					clazz.getConstructor(SmartProperties.class, TokenFactory.class);
			Object newInstance = constructor.newInstance(props, tokenFactory);
			return newInstance;
		} catch (ClassNotFoundException e) {
			// bad class name
			throw new ReflectionException("Could not find class: " + classname, e);
		} catch (SecurityException e) {
			// other problem
			throw new ReflectionException("Error loading class: " + classname, e);
		} catch (NoSuchMethodException e) {
			// no constructor
			throw new ReflectionException("Class " + classname
					+ " must define constructor with parameters (SmartProperties, TokenFactory)", e);
		} catch (IllegalArgumentException e) {
			// bad argument to constructor
			throw new ReflectionException("Error loading class: " + classname, e);
		} catch (InstantiationException e) {
			// couldn't instantiate
			throw new ReflectionException("Error loading class: " + classname, e);
		} catch (IllegalAccessException e) {
			// bad access
			throw new ReflectionException("Error loading class: " + classname, e);
		} catch (InvocationTargetException e) {
			// something bad...
			throw new ReflectionException("Error loading class: " + classname, e);
		}
	}

	public Treegraft(SmartProperties props, TokenFactory<T> tokenFactory,
			RuleFactory<R, T> ruleFactory, GrammarLoader<R, T> grammarLoader) throws IOException,
			ParseException, ReflectionException {

		this.tokenFactory = tokenFactory;
		this.ruleFactory = ruleFactory;

		this.oovHandler =
				(OutOfVocabularyHandler<R, T>) reflect(
						props.getPropertyString("grammar.oovHandler"), props);

		// TODO: input.encoding
		this.sentences = new String[] { props.getPropertyString("input") };
		this.results = new Result[sentences.length];
		File grammarFile = props.getPropertyFile("paths.grammarFile");
		File lmFile = props.getPropertyFile("lm.modelFile");
		String lmEncoding = props.getPropertyString("lm.encoding");
		double lmOOVLogProb = props.getPropertyFloat("lm.oovLogProb");
		this.nBest = props.getPropertyInt("decoder.nBest");

		String grammarEncoding = props.getPropertyString("grammar.encoding");
		String[] startSymbols = props.getPropertyStringArray("grammar.startSymbols");
		String[] filterLHS = props.getPropertyStringArray("grammar.filterRulesWithLHS");
		String[] filterRHSNonterms =
				props.getPropertyStringArray("grammar.filterRulesWithNonterminalRHS");
		String[] filterRHSTerms =
				props.getPropertyStringArray("grammar.filterRulesWithTerminalRHS");

		ProfilerTimer timer = ProfilerTimer.newTimer("Taru-Treegraft", null, true, true);

		// init factories

		HashSet<T> vocabulary = new HashSet<T>();
		for (String sentence : sentences) {
			T[] inputTokens = tokenFactory.makeTokens(StringUtils.tokenize(sentence), true);
			for (T token : inputTokens) {
				vocabulary.add(token);
			}
		}

		// set up arbitrary rule filters
		HashSet<T> filterLHSToks =
				new HashSet<T>(Arrays.asList(tokenFactory.makeTokens(filterLHS, false)));
		HashSet<T> filterRHSToks =
				new HashSet<T>(Arrays.asList(tokenFactory.makeTokens(filterRHSNonterms, false)));
		filterRHSToks.addAll(Arrays.asList(tokenFactory.makeTokens(filterRHSTerms, true)));

		log.info("Loading grammar...");
		ProfilerTimer loadTimer = ProfilerTimer.newTimer("Loading", timer, true, true);
		ProfilerTimer loadGrammarTimer =
				ProfilerTimer.newTimer("loadGrammar", loadTimer, true, true);

		this.grammar =
				new Grammar<R, T>(tokenFactory, startSymbols, vocabulary, filterLHSToks,
						filterRHSToks);

		grammarLoader.loadGrammar(grammar, new FileInputStream(grammarFile),
				grammarFile.getAbsolutePath(), grammarEncoding);

		HashSet<T> targetVocab = grammar.getTargetVocabulary(ruleFactory.getTransducer());
		loadGrammarTimer.pause();
		loadTimer.pause();

		log.info("Loading LM...");
		ARPALanguageModelLoader<T> lmLoader = new ARPALanguageModelLoader<T>();
		LanguageModel<T> lm = new NGramLanguageModel<T>();
		lmLoader.loadLM(lm, tokenFactory, new FileInputStream(lmFile), lmEncoding, targetVocab,
				lmOOVLogProb);

		// init features
		LanguageModelFeature<R, T> lmFeature = new LanguageModelFeature<R, T>(lm, 1.0);
		this.features = new Feature[] { lmFeature };

	}

	public void translateAll() {
		for (int i = 0; i < sentences.length; i++) {
			try {
				results[i] = translate(sentences[i]);
			} catch (Throwable t) {
				// don't fail just because one sentences dies!
				results[i] = new Result<T>(t);
			}
		}
	}

	public void showResults() {
		for (Result<?> result : results) {
			System.out.println(result);
		}
	}

	public Result<T> translate(String sentence) throws RuleException {

		// TODO: Allow multithreaded calls to this method?

		ProfilerTimer processingTimer = ProfilerTimer.newTimer("Processing", null, true, true);

		T[] inputTokens = tokenFactory.makeTokens(StringUtils.tokenize(sentence), true);

		log.info("Parsing...");
		ChartParser<R, T> parser =
				new ChartParser<R, T>(ruleFactory, grammar, oovHandler, processingTimer);
		Chart<R, T> chart = parser.parse(inputTokens);

		log.info("Decoding...");
		Scorer<R, T> scorer = new LogLinearScorer<R, T>(features, tokenFactory);

		// TODO: Separate beam sizes in the transfer stage and decoding stage
		BeamSearchParsePruner<R, T> pruner = new BeamSearchParsePruner<R, T>(tokenFactory, nBest);
		ForestUnpacker<R, T> unpacker =
				new ForestUnpacker<R, T>(scorer, pruner, new YieldRecombiner<R, T>(tokenFactory),
						ruleFactory.getTransducer());

		BeamSearchDecoder<R, T> decoder = new BeamSearchDecoder<R, T>(tokenFactory, scorer, pruner);
		List<DecoderHypothesis<T>> nBestList = decoder.getKBest(chart, unpacker, nBest);
		processingTimer.pause();

		return new Result<T>(nBestList);
	}

	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.err.println("Usage: program <properties_file>");
			System.exit(1);
		}

		SmartProperties props = new SmartProperties(args[0]);

		TokenFactory<?> tokenFactory = new StringTokenFactory();
		RuleFactory<?, ?> ruleFactory = new SyncCFGRuleFactory(tokenFactory);
		GrammarLoader<?, ?> grammarLoader = new SyncCFGGrammarLoader(tokenFactory);

		int nThreads = props.getPropertyInt("global.numThreads");
		Treegraft<?, ?> treegraft;
		if (nThreads > 1) {
			treegraft =
					new MultithreadedTreegraft(props, tokenFactory, ruleFactory, grammarLoader,
							nThreads);
		} else {
			treegraft = new Treegraft(props, tokenFactory, ruleFactory, grammarLoader);
		}
		treegraft.translateAll();
		treegraft.showResults();
	}
}
