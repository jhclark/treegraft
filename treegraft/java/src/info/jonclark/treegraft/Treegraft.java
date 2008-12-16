package info.jonclark.treegraft;

import info.jonclark.lang.ClassFinder;
import info.jonclark.lang.Option;
import info.jonclark.lang.OptionParser;
import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.log.LogUtils;
import info.jonclark.properties.PropertyUtils;
import info.jonclark.properties.SmartProperties;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.stat.TextProgressBar;
import info.jonclark.treegraft.core.Result;
import info.jonclark.treegraft.core.adapters.XferAdapter;
import info.jonclark.treegraft.core.output.BasicHypothesisFormatter;
import info.jonclark.treegraft.core.output.HypothesisFormatter;
import info.jonclark.treegraft.core.plugin.PluginLoader;
import info.jonclark.treegraft.core.plugin.ReflectionException;
import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.scoring.LogLinearScorer;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.scoring.LogLinearScorer.LogLinearScorerOptions;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.integer.IntegerTokenFactory;
import info.jonclark.treegraft.decoder.Decoder;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.decoder.LRStackDecoder;
import info.jonclark.treegraft.decoder.Lattice;
import info.jonclark.treegraft.decoder.LRStackDecoder.LRStackDecoderOptions;
import info.jonclark.treegraft.distributed.multithreading.MultithreadedTreegraft;
import info.jonclark.treegraft.parsing.Parser;
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.chartparser.ChartParser;
import info.jonclark.treegraft.parsing.chartparser.ChartParser.ChartParserOptions;
import info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker.ForestUnpackerOptions;
import info.jonclark.treegraft.parsing.grammar.Grammar;
import info.jonclark.treegraft.parsing.grammar.GrammarLoader;
import info.jonclark.treegraft.parsing.merging.BeamSearchMerger;
import info.jonclark.treegraft.parsing.merging.Merger;
import info.jonclark.treegraft.parsing.merging.BeamSearchMerger.BeamSearchMergerOptions;
import info.jonclark.treegraft.parsing.morphology.MorphologyAnalyzer;
import info.jonclark.treegraft.parsing.morphology.MorphologyGenerator;
import info.jonclark.treegraft.parsing.morphology.NullMorphologyAnalyzer;
import info.jonclark.treegraft.parsing.morphology.NullMorphologyGenerator;
import info.jonclark.treegraft.parsing.oov.OutOfVocabularyHandler;
import info.jonclark.treegraft.parsing.parses.BasicTreeFormatter;
import info.jonclark.treegraft.parsing.parses.ParseFactory;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.rules.RuleFactory;
import info.jonclark.treegraft.parsing.synccfg.OneLineLexiconGrammarLoader;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGGrammarLoader;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRule;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRuleFactory;
import info.jonclark.util.ArrayUtils;
import info.jonclark.util.FileUtils;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Logger;

// TODO: Allow reflection of custom classes from properties files
// ...this means all classes that implement interfaces have to take in properties files at some point?
@OptionsTarget(Treegraft.TreegraftCoreOptions.class)
public class Treegraft<R extends SyncCFGRule<T>, T extends Token> {

	protected static final Logger log = LogUtils.getLogger();

	public static class TreegraftCoreOptions implements Options {

		@Option(name = "global.numThreads", usage = "The number of worker threads that will be started to perform parsing/translation")
		public int nThreads;

		@Option(name = "features", usage = "Fully qualified Java class names of features to be used in parsing and decoding", delim = " ")
		public String[] featureClasses;

		@Option(name = "global.progressBar.width", usage = "The width in characters of text progress bars")
		public int barWidth;

		@Option(name = "global.progressBar.isAnimated", usage = "Should we animate text progress bars by using the backspace character repeatedly? (Not recommended for logging)")
		public boolean animatedBar;

		@Option(name = "output.file", usage = "The file to which parse trees, n-best lists, and/or transductions should be written", errorIfFileExists = true)
		public File outFile;

		@Option(name = "transfer.latticeFile", usage = "The file to which lattices from the transfer stage should be written", errorIfFileExists = true)
		public File latticeFile;

		@Option(name = "output.encoding", usage = "The character encoding for output files", required = false, defaultValue = "UTF-8")
		public String outputEncoding;

		@Option(name = "input.encoding", usage = "The character encoding for input files", required = false, defaultValue = "UTF-8")
		public String inputEncoding;

		@Option(name = "input.file", usage = "The file from which input sentences will be read", errorIfFileNotExists = true)
		public File inputFile;

		@Option(name = "grammar.grammarFile", usage = "The grammar file(s) (space delimited) to be used by the parser/transfer stage", errorIfFileNotExists = true, delim = " ")
		public File[] grammarFile;

		@Option(name = "grammar.lexiconFile", usage = "The lexicon file(s) (space delimited) to be used by the parser/transfer stage", errorIfFileNotExists = true, delim = " ")
		public File[] lexiconFile;

		@Option(name = "grammar.encoding", usage = "The encoding for all grammar files", required = false, defaultValue = "UTF-8")
		public String grammarEncoding;

		@Option(name = "grammar.startSymbols", usage = "The symbols (space delimited) that indicate to the parser/transfer that a constituent is a full sentence", required = false, defaultValue = "S", delim = " ")
		public String[] startSymbols;

		@Option(name = "grammar.filterRulesWithLHS", usage = "The left-hand symbols a.k.a. mother nodes (space delimited) whose rules should not be included in the grammar", required = false, defaultValue = "", delim = " ")
		public String[] filterLHS;

		@Option(name = "grammar.filterRulesWithNonterminalRHS", usage = "Any rule containing any of these right-hand side non-terminal symbols will not be included in the grammar", required = false, defaultValue = "", delim = " ")
		public String[] filterRHSNonterms;

		@Option(name = "grammar.filterRulesWithTerminalRHS", usage = "Any rule containing any of these right-hand side terminal symbols will not be included in the grammar", required = false, defaultValue = "", delim = " ")
		public String[] filterRHSTerms;
	}

	public static class TreegraftConfig<R extends GrammarRule<T>, T extends Token> {
		public TreegraftProfiler profiler;
		public TreegraftCoreOptions opts;

		public Feature<R, T, ?>[] features;
		public RuleFactory<R, T> ruleFactory;
		public TokenFactory<T> tokenFactory;
		public OutOfVocabularyHandler<R, T> oovHandler;
		public Grammar<R, T> grammar;
		public Scorer<R, T> scorer;
		public Merger<R, T> merger;
		public OptionParser configurator;

		public HashSet<T> sourceVocab;
		public HashSet<T> targetVocab;
	}

	public static class TreegraftProfiler {
		public final ProfilerTimer treegraftTimer;
		public final ProfilerTimer loadTimer;
		public final ProfilerTimer processingTimer;
		public final ProfilerTimer decoderTimer;
		public final ProfilerTimer featureTimer;
		public final ProfilerTimer parseTimer;

		public TreegraftProfiler() {

			this.treegraftTimer = ProfilerTimer.newTimer("treegraft", null, true, true);
			this.loadTimer = ProfilerTimer.newTimer("loading", treegraftTimer, true, false);
			this.processingTimer =
					ProfilerTimer.newTimer("Processing", treegraftTimer, true, false);
			this.parseTimer = ProfilerTimer.newTimer("parsing", processingTimer, true, false);
			this.decoderTimer = ProfilerTimer.newTimer("decoder", processingTimer, true, false);
			this.featureTimer = ProfilerTimer.newTimer("features", processingTimer, true, false);
		}
	}

	protected TreegraftCoreOptions opts;
	protected TreegraftConfig<R, T> config = new TreegraftConfig<R, T>();
	protected TreegraftProfiler profiler = new TreegraftProfiler();

	protected final String[] sentences;
	protected final Result<R, T>[] results;

	public Treegraft(TreegraftCoreOptions opts, SmartProperties props, OptionParser configurator,
			TokenFactory<T> tokenFactory, RuleFactory<R, T> ruleFactory,
			GrammarLoader<R, T> grammarLoader, GrammarLoader<R, T> lexiconLoader)
			throws IOException, ParseException, ReflectionException, RuleException,
			InvocationTargetException {

		// for convenience...
		this.config.profiler = profiler;
		this.config.opts = opts;

		this.opts = opts;
		this.config.configurator = configurator;

		config.tokenFactory = tokenFactory;
		config.ruleFactory = ruleFactory;

		// make sure we have all necessary classes before spending any time
		// loading things from disk
		validateFeatures(opts.featureClasses, configurator);

		BeamSearchMergerOptions mergerOpts =
				config.configurator.getOptions(BeamSearchMergerOptions.class);
		config.merger = new BeamSearchMerger<R, T>(mergerOpts, config);

		config.oovHandler =
				(OutOfVocabularyHandler<R, T>) PluginLoader.reflect(
						props.getPropertyString("grammar.oovHandler"), tokenFactory, props);

		this.sentences =
				StringUtils.tokenize(FileUtils.getFileAsString(opts.inputFile,
						Charset.forName(opts.inputEncoding)), "\n");
		this.results = new Result[sentences.length];

		// init factories

		HashSet<T> sourceVocab = new HashSet<T>();
		config.sourceVocab = sourceVocab;
		for (String sentence : sentences) {
			T[] inputTokens = tokenFactory.makeTokens(StringUtils.tokenize(sentence), true);
			for (T token : inputTokens) {
				sourceVocab.add(token);
			}
		}

		// init features BEFORE taking time to load grammar and LM
		// TODO: Refactor loading of lexical probs into a
		// "lexical probability loader"
		// so that we can later move to a binary format

		profiler.loadTimer.go();

		// set up arbitrary rule filters
		HashSet<T> filterLHSToks =
				new HashSet<T>(Arrays.asList(tokenFactory.makeTokens(opts.filterLHS, false)));
		HashSet<T> filterRHSToks =
				new HashSet<T>(
						Arrays.asList(tokenFactory.makeTokens(opts.filterRHSNonterms, false)));
		filterRHSToks.addAll(Arrays.asList(tokenFactory.makeTokens(opts.filterRHSTerms, true)));

		// get additional vocab from morph analyzer
		MorphologyAnalyzer<T> morphAnalyzer = new NullMorphologyAnalyzer<T>();
		HashSet<T> addSourceVocab = morphAnalyzer.getAdditionalSourceVocabulary(sourceVocab);
		sourceVocab.addAll(addSourceVocab);

		log.info("Loading grammar...");
		ProfilerTimer loadTimer =
				ProfilerTimer.newTimer("Loading", profiler.treegraftTimer, true, true);
		ProfilerTimer loadGrammarTimer =
				ProfilerTimer.newTimer("loadGrammar", loadTimer, true, true);

		Grammar<R, T> grammar =
				new Grammar<R, T>(tokenFactory, opts.startSymbols, sourceVocab, filterLHSToks,
						filterRHSToks);
		config.grammar = grammar;

		TextProgressBar grammarBar =
				new TextProgressBar(System.err, "rule", 100, opts.barWidth, opts.animatedBar);
		for (File grammarFile : opts.grammarFile)
			if (!grammarFile.getName().equals("null"))
				grammarLoader.loadGrammar(grammar, new FileInputStream(grammarFile),
						grammarFile.getAbsolutePath(), opts.grammarEncoding, grammarBar);

		TextProgressBar lexiconBar =
				new TextProgressBar(System.err, "rule", 100, opts.barWidth, opts.animatedBar);
		for (File lexiconFile : opts.lexiconFile)
			if (!lexiconFile.getName().equals("null"))
				lexiconLoader.loadGrammar(grammar, new FileInputStream(lexiconFile),
						lexiconFile.getAbsolutePath(), opts.grammarEncoding, lexiconBar);

		grammar = grammar.keepKBestRules(40);

		config.targetVocab = grammar.getTargetVocabulary(ruleFactory.getTransducer());
		config.targetVocab.addAll(config.oovHandler.getAdditionalTargetVocabulary(sourceVocab));

		// get additional vocabulary from morphology generator
		MorphologyGenerator<T> morphGen = new NullMorphologyGenerator<T>();
		HashSet<T> addTargetVocab = morphGen.getAdditionalTargetVocabulary(sourceVocab);
		config.targetVocab.addAll(addTargetVocab);

		loadGrammarTimer.pause();
		loadTimer.pause();

		// load features after we already know the target-side vocabulary, etc.
		List<Feature<R, T, ?>> features =
				new ArrayList<Feature<R, T, ?>>(opts.featureClasses.length);
		for (String featureClassName : opts.featureClasses) {
			Feature<R, T, ?> f = loadFeature(featureClassName, configurator, false);
			features.add(f);
		}
		config.features = features.toArray(new Feature[features.size()]);
		LogLinearScorerOptions scorerOpts = configurator.getOptions(LogLinearScorerOptions.class);
		config.scorer = new LogLinearScorer<R, T>(scorerOpts, config);

		loadTimer.pause();
	}

	private void validateFeatures(String[] featureClasses, OptionParser configurator)
			throws InvocationTargetException {
		for (String strFeatureClass : featureClasses) {
			loadFeature(strFeatureClass, configurator, true);
		}
	}

	// TODO: Validate that all features exist before loading anything
	private Feature<R, T, ?> loadFeature(String featureClassName, OptionParser configurator,
			boolean simulate) throws InvocationTargetException {

		String strFeatureOptionsName = "?";
		try {
			Class<Feature<R, T, ?>> featureClass =
					(Class<Feature<R, T, ?>>) Class.forName(featureClassName);

			// we have to load the configuration options before we can load the
			// class itself

			// figure out what class this feature expects as its Options
			// argument
			OptionsTarget optionsTarget = featureClass.getAnnotation(OptionsTarget.class);
			if (optionsTarget == null) {
				throw new RuntimeException("Feature " + featureClassName
						+ " must define the @OptionsTarget annotation");
			}
			Class<? extends Options> featureOptionsClass = optionsTarget.value();
			strFeatureOptionsName = featureOptionsClass.getSimpleName();

			// load options from the user's config file and the command line
			// to populate an instance of the specified Options class
			Options opts = configurator.getOptions(featureOptionsClass);

			// find the constructor that we require (Options, TreegraftConfig)
			// and instantiate the feature
			Constructor<Feature<R, T, ?>> constructor =
					featureClass.getConstructor(featureOptionsClass, TreegraftConfig.class);

			if (simulate) {
				return null;
			} else {
				Feature<R, T, ?> feature = constructor.newInstance(opts, config);
				return feature;
			}

		} catch (ClassNotFoundException e) {
			// TODO: Make this a configuration exception
			throw new Error(e);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			throw new Error(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(featureClassName
					+ " must define a constructor that takes arguments (" + strFeatureOptionsName
					+ ", TreegraftConfig)");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			throw new Error(e);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			throw new Error(e);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			throw new Error(e);
		} catch (InvocationTargetException e) {
			throw e;
		}
	}

	public void translateAll() throws RuleException {

		profiler.processingTimer.go();
		TextProgressBar progressBar =
				new TextProgressBar(System.err, "sent", 100, opts.barWidth, opts.animatedBar);
		progressBar.beginTask(sentences.length);

		for (int i = 0; i < sentences.length; i++) {
			// try {
			results[i] = translate(i, sentences[i]);
			// } catch (Throwable t) {
			// // don't fail just because one sentences dies!
			// results[i] = new Result<T>(i, t);
			// log.severe("Ignoring sentence failure: " +
			// StringUtils.getStackTrace(t));
			// }
			progressBar.recordEventCompletion();
		}

		profiler.processingTimer.pause();
		progressBar.endTask();
	}

	public int getSentenceCount() {
		return this.sentences.length;
	}

	public List<DecoderHypothesis<T>>[] getResults() {
		List<DecoderHypothesis<T>>[] list = new List[results.length];

		for (int i = 0; i < results.length; i++) {
			list[i] = results[i].nBestList;
		}

		return list;
	}

	public void showResults(HypothesisFormatter<R, T> hypothesisFormatter)
			throws FileNotFoundException, UnsupportedEncodingException {

		PrintWriter out = new PrintWriter(opts.outFile, opts.outputEncoding);
		for (Result<R, T> result : results) {
			hypothesisFormatter.formatHypothesis(result, out);
		}
		out.close();

		out = new PrintWriter(opts.latticeFile, opts.outputEncoding);
		for (Result<R, T> result : results) {
			if (result.getLattice() != null) {
				out.println(result.getLattice().toString(
						new BasicTreeFormatter<T>(config.tokenFactory, true, true, config.scorer,
								true)));
			}
		}
		out.close();
	}

	public Result<R, T> translate(int nSentence, String sentence) throws RuleException {

		// TODO: Allow multithreaded calls to this method?

		T[] inputTokens = config.tokenFactory.makeTokens(StringUtils.tokenize(sentence), true);

		log.fine("Parsing...");
		ChartParserOptions parserOpts = config.configurator.getOptions(ChartParserOptions.class);
		Parser<R, T> parser = new ChartParser<R, T>(parserOpts, config);

		// TODO: Refactor so that parsers can return a more general data
		// structure, such as a hypergraph

		Chart<R, T> chart = parser.parse(inputTokens);

		log.fine("Decoding...");

		// TODO: Implement a CubePruningMerger

		// TODO: Load BeamSearchMerger, Parser, ForstUnpacker, and Decoder as
		// plug-ins

		ParseFactory<R, T> parseFactory =
				new ParseFactory<R, T>(config.tokenFactory, config.ruleFactory.getTransducer(),
						inputTokens.length);
		ForestUnpackerOptions unpackerOpts =
				config.configurator.getOptions(ForestUnpackerOptions.class);
		ForestUnpacker<R, T> unpacker =
				new ForestUnpacker<R, T>(unpackerOpts, config, parseFactory,
						Arrays.asList(inputTokens));

		// create lattices
		Lattice<R, T> lattice = null; // new Lattice<R, T>(chart, unpacker);

		LRStackDecoderOptions decoderOpts =
				config.configurator.getOptions(LRStackDecoderOptions.class);
		Decoder<R, T> decoder = new LRStackDecoder<R, T>(decoderOpts, config);
		List<DecoderHypothesis<T>> nBestList = decoder.getKBest(chart, unpacker);

		return new Result<R, T>(nSentence, sentence, nBestList, lattice);
	}

	public static void main(String[] args) throws Exception {

		System.out.println("Treegraft V0.4.0 -- by Jonathan Clark (December 2008)");
		System.out.println();

		if (args.length != 1 && args.length != 2) {
			System.err.println("Usage: treegraft [<properties_file> | -if <xfer_ini_file>]");
			System.exit(1);
		}

		// TODO: How do we run treegraft as a monolingual parser?
		// TODO: Just as a synchronous parser?
		// TODO: Future cost estimator for building chart entries

		// TODO: Scan for jython plugins such as formatters

		log.info("Searching for plug-ins...");
		ClassFinder<Options> finder = new ClassFinder<Options>();
		Vector<Class<? extends Options>> configurables = finder.findSubclasses(Options.class);
		StringBuilder builder = new StringBuilder();
		for (Class<? extends Options> c : configurables) {
			builder.append(c.getSimpleName() + " ");
		}
		log.info("Found " + configurables.size() + " plug-ins: " + builder.toString() + "\n");

		if (args[0].equals("-h") || args[0].equals("-help") || args[0].equals("--help")) {
			args = ArrayUtils.cutFirst(args, 1);
			OptionParser configurator =
					new OptionParser(configurables, args, new Properties(), false);
			System.out.println(configurator.getGlobalUsage());
			System.exit(0);
		}

		Properties props;
		if (args[0].equals("-if")) {
			props = XferAdapter.parseConfigFile(args[1]);
			args = ArrayUtils.cutFirst(args, 2);

		} else {
			props = PropertyUtils.getProperties(args[0]);
			args = ArrayUtils.cutFirst(args, 1);

		}

		boolean failOnUnrecognized = false;
		OptionParser configurator =
				new OptionParser(configurables, args, props, failOnUnrecognized);
		configurator.validateConfiguration();

		TreegraftCoreOptions opts = configurator.getOptions(TreegraftCoreOptions.class);

		boolean convertScoresToLog = true;

		TokenFactory<?> tokenFactory = new IntegerTokenFactory();
		RuleFactory<?, ?> ruleFactory = new SyncCFGRuleFactory(tokenFactory);
		GrammarLoader<?, ?> grammarLoader =
				new SyncCFGGrammarLoader(tokenFactory, convertScoresToLog);
		GrammarLoader<?, ?> lexiconLoader = new OneLineLexiconGrammarLoader(tokenFactory);

		log.info("Initializing...");

		Treegraft<?, ?> treegraft;
		if (opts.nThreads > 1) {
			log.info("Starting " + opts.nThreads + " threads...");
			treegraft =
					new MultithreadedTreegraft(opts, new SmartProperties(props), configurator,
							tokenFactory, ruleFactory, grammarLoader, lexiconLoader, opts.nThreads);
		} else {
			treegraft =
					new Treegraft(opts, new SmartProperties(props), configurator, tokenFactory,
							ruleFactory, grammarLoader, lexiconLoader);
		}
		treegraft.translateAll();

		// When reflecting, allow either the name of a java class, or a file
		// name containing a jython class. just check for the .py/.jy extension

		// List<File> plugins =
		// JythonFactory.scanDirectoryForPlugins(new File("plugins"),
		// HypothesisFormatter.class);
		// assert plugins.size() == 1;
		// HashMap<String, Object> instanceVariables = new HashMap<String,
		// Object>();
		// instanceVariables.put("tokenFactory", tokenFactory);
		// instanceVariables.put("scorer", treegraft.getScorer());
		// HypothesisFormatter formatter =
		// JythonFactory.get(plugins.get(0), HypothesisFormatter.class,
		// instanceVariables);
		//
		// treegraft.showResults(formatter);
		opts.outFile = new File(opts.outFile.getParentFile(), opts.outFile.getName());
		treegraft.showResults(new BasicHypothesisFormatter(tokenFactory, treegraft.config.scorer));
		// treegraft.showResults(new
		// OptimizeNBestHypothesisFormatter(tokenFactory, treegraft.scorer));

		log.info(treegraft.profiler.treegraftTimer.getTimingReport(true));
		System.exit(0);
	}
}
