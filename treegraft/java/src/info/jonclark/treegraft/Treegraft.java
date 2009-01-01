package info.jonclark.treegraft;

import info.jonclark.lang.ClassFinder;
import info.jonclark.lang.Option;
import info.jonclark.lang.OptionParser;
import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.lang.Pair;
import info.jonclark.log.LogUtils;
import info.jonclark.properties.PropertiesException;
import info.jonclark.properties.PropertyUtils;
import info.jonclark.stat.ProfilerTimer;
import info.jonclark.stat.TextProgressBar;
import info.jonclark.treegraft.core.Plugin;
import info.jonclark.treegraft.core.Result;
import info.jonclark.treegraft.core.adapters.XferAdapter;
import info.jonclark.treegraft.core.output.HypothesisFormatter;
import info.jonclark.treegraft.core.plugin.PluginLoader;
import info.jonclark.treegraft.core.plugin.ReflectionException;
import info.jonclark.treegraft.core.scoring.Feature;
import info.jonclark.treegraft.core.scoring.LogLinearScorer;
import info.jonclark.treegraft.core.scoring.Scorer;
import info.jonclark.treegraft.core.scoring.LogLinearScorer.LogLinearScorerOptions;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.decoder.Decoder;
import info.jonclark.treegraft.decoder.Lattice;
import info.jonclark.treegraft.decoder.LatticeFormatter;
import info.jonclark.treegraft.distributed.multithreading.MultithreadedTreegraft;
import info.jonclark.treegraft.parsing.Parser;
import info.jonclark.treegraft.parsing.chartparser.Chart;
import info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker;
import info.jonclark.treegraft.parsing.grammar.Grammar;
import info.jonclark.treegraft.parsing.grammar.GrammarLoader;
import info.jonclark.treegraft.parsing.merging.Merger;
import info.jonclark.treegraft.parsing.morphology.MorphologyAnalyzer;
import info.jonclark.treegraft.parsing.morphology.MorphologyGenerator;
import info.jonclark.treegraft.parsing.morphology.NullMorphologyAnalyzer;
import info.jonclark.treegraft.parsing.morphology.NullMorphologyGenerator;
import info.jonclark.treegraft.parsing.oov.OutOfVocabularyHandler;
import info.jonclark.treegraft.parsing.parses.ParseFactory;
import info.jonclark.treegraft.parsing.parses.PartialParse;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.rules.RuleFactory;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRule;
import info.jonclark.util.ArrayUtils;
import info.jonclark.util.FileUtils;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
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

	public static class TreegraftCoreOptions<R extends GrammarRule<T>, T extends Token> implements
			Options {

		@Option(name = "features", usage = "Fully qualified Java class names of features to be used in parsing and decoding", arrayDelim = " ")
		public Class<? extends Feature<R, T, ?>>[] featureClasses;

		@Option(name = "merger", usage = "Fully qualified Java class name of the Merger to be used for combining hypotheses in forest unpacking and decoding", required = false, defaultValue = "info.jonclark.treegraft.parsing.merging.BeamSearchMerger")
		public Class<? extends Merger<R, T>> mergerClass;

		@Option(name = "parser", usage = "Fully qualified Java class name of the Parser to be used", required = false, defaultValue = "info.jonclark.treegraft.parsing.chartparser.ChartParser")
		public Class<? extends Parser<R, T>> parserClass;

		@Option(name = "forestProcessor", usage = "Fully qualified Java class name of the forest processor to be used for either unpacking or decoding the source forest into a target-side forest or n-best list", required = false, defaultValue = "info.jonclark.treegraft.parsing.forestunpacking.ForestUnpacker")
		public Class<? extends ForestUnpacker<R, T>> forestProcessorClass;

		@Option(name = "decoder", usage = "Fully qualified Java class name of the decoder to be used for decoding lattices", required = false, defaultValue = "info.jonclark.treegraft.decoder.LRStackDecoder")
		public Class<? extends Decoder<R, T>> decoderClass;

		@Option(name = "grammar.oovHandler", usage = "Fully qualified Java class name of a class that will produce new rules (or halt execution) when an unknown input word is encountered")
		public Class<? extends OutOfVocabularyHandler<R, T>> oovHandlerClass;

		@Option(name = "tokenFactory", usage = "Fully qualified Java class name of the TokenFactory class that will be used to create Token objects for each word and TokenSequence objects for frequently looked-up sequences", required = false, defaultValue = "info.jonclark.treegraft.core.tokens.integer.IntegerTokenFactory")
		public Class<? extends TokenFactory<T>> tokenFactory;

		@Option(name = "ruleFactory", usage = "Fully qualified Java class name of the RuleFactory that will be used for dummy rule creation and providing transduction info for each rule", required = false, defaultValue = "info.jonclark.treegraft.parsing.synccfg.SyncCFGRuleFactory")
		public Class<? extends RuleFactory<R, T>> ruleFactory;

		@Option(name = "global.numThreads", usage = "The number of worker threads that will be started to perform parsing/translation")
		public int nThreads;

		@Option(name = "global.progressBar.width", usage = "The width in characters of text progress bars")
		public int barWidth;

		@Option(name = "global.progressBar.isAnimated", usage = "Should we animate text progress bars by using the backspace character repeatedly? (Not recommended for logging)")
		public boolean animatedBar;

		@Option(name = "output.file", usage = "The files to which parse trees, n-best lists, and/or transductions should be written, each followed by a semicolon and the HypothesisFormatter class with which the file should be formatted")
		public Pair<File, Class<? extends HypothesisFormatter<R, T>>>[] outputInfo;

		@Option(name = "transfer.latticeFile", usage = "The file to which lattices from the transfer stage should be written", errorIfFileExists = false)
		public Pair<File, Class<? extends LatticeFormatter<R, T>>> latticeInfo;

		@Option(name = "output.encoding", usage = "The character encoding for output files", required = false, defaultValue = "UTF-8")
		public String outputEncoding;

		@Option(name = "input.encoding", usage = "The character encoding for input files", required = false, defaultValue = "UTF-8")
		public String inputEncoding;

		@Option(name = "input.file", usage = "The file from which input sentences will be read", errorIfFileNotExists = true)
		public File inputFile;

		@Option(name = "grammar.grammarFile", usage = "The grammar file(s) (space delimited) to be used by the parser/transfer stage", errorIfFileNotExists = true)
		public Pair<File, Class<? extends GrammarLoader<R, T>>>[] grammarFile;

		@Option(name = "grammar.lexiconFile", usage = "The lexicon file(s) (space delimited) to be used by the parser/transfer stage", errorIfFileNotExists = true)
		public Pair<File, Class<? extends GrammarLoader<R, T>>>[] lexiconFile;

		@Option(name = "grammar.encoding", usage = "The encoding for all grammar files", required = false, defaultValue = "UTF-8")
		public String grammarEncoding;

		@Option(name = "grammar.startSymbols", usage = "The symbols (space delimited) that indicate to the parser/transfer that a constituent is a full sentence", required = false, defaultValue = "S")
		public String[] startSymbols;

		@Option(name = "grammar.filterRulesWithLHS", usage = "The left-hand symbols a.k.a. mother nodes (space delimited) whose rules should not be included in the grammar", required = false, defaultValue = "")
		public String[] filterLHS;

		@Option(name = "grammar.filterRulesWithNonterminalRHS", usage = "Any rule containing any of these right-hand side non-terminal symbols will not be included in the grammar", required = false, defaultValue = "")
		public String[] filterRHSNonterms;

		@Option(name = "grammar.filterRulesWithTerminalRHS", usage = "Any rule containing any of these right-hand side terminal symbols will not be included in the grammar", required = false, defaultValue = "")
		public String[] filterRHSTerms;

		@Option(name = "grammar.keepKBestRules", usage = "The number of best-scoring rules to keep for each source LHS-RHS pair", required = false, defaultValue = "1000")
		public int keepKBestRules;
	}

	public static class TreegraftConfig<R extends GrammarRule<T>, T extends Token> {
		public TreegraftProfiler profiler = new TreegraftProfiler();
		public TreegraftCoreOptions<R, T> opts;
		public OptionParser configurator;

		public List<Feature<R, T, ?>> features;
		public RuleFactory<R, T> ruleFactory;
		public TokenFactory<T> tokenFactory;
		public OutOfVocabularyHandler<R, T> oovHandler;
		public Grammar<R, T> grammar;

		public Parser<R, T> parser;
		public Scorer<R, T> scorer;
		public Merger<R, T> merger;
		public ForestUnpacker<R, T> unpacker;
		public Decoder<R, T> decoder;

		public HashSet<T> sourceVocab;
		public HashSet<T> targetVocab;

		public T bos;
		public T eos;
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

	protected TreegraftConfig<R, T> config = new TreegraftConfig<R, T>();

	protected final String[] sentences;
	protected final Result<R, T>[] results;

	public Treegraft(TreegraftCoreOptions<R, T> opts, OptionParser configurator)
			throws IOException, ParseException, ReflectionException, RuleException,
			InvocationTargetException, IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException, NoSuchMethodException {

		this.config.opts = opts;
		this.config.configurator = configurator;

		TokenFactory<T> tokenFactory = (TokenFactory<T>) opts.tokenFactory.newInstance();
		RuleFactory<R, T> ruleFactory =
				(RuleFactory<R, T>) opts.ruleFactory.getConstructor(TokenFactory.class).newInstance(
						tokenFactory);

		config.tokenFactory = tokenFactory;
		config.ruleFactory = ruleFactory;

		// make sure we have all necessary classes before spending any time
		// loading things from disk
		PluginLoader.validatePlugins(opts.featureClasses, configurator);
		PluginLoader.validatePlugins(new Class[] { opts.oovHandlerClass, opts.mergerClass,
				opts.forestProcessorClass, opts.parserClass, opts.decoderClass }, configurator);

		// validate hypothesis and lattice formatters before doing any loading
		// or translation
		for (Pair<File, Class<? extends HypothesisFormatter<R, T>>> outInfo : config.opts.outputInfo) {
			PluginLoader.validatePlugins(new Class[] { outInfo.second }, configurator);
		}
		PluginLoader.validatePlugins(new Class[] { config.opts.latticeInfo.second }, configurator);

		// TODO: Simulate loading hypothesis formatter...

		assert opts.inputFile != null;
		this.sentences =
				StringUtils.tokenize(FileUtils.getFileAsString(opts.inputFile,
						Charset.forName(opts.inputEncoding)), "\n");
		this.results = new Result[sentences.length];

		// init factories

		config.bos = tokenFactory.makeToken("<s>", true);
		config.eos = tokenFactory.makeToken("</s>", true);

		HashSet<T> sourceVocab = new HashSet<T>();
		// TODO: Make this more elegant somehow?
		sourceVocab.add(config.bos);
		sourceVocab.add(config.eos);
		config.sourceVocab = sourceVocab;
		for (String sentence : sentences) {
			T[] inputTokens = tokenFactory.makeTokens(StringUtils.tokenize(sentence), true);
			for (T token : inputTokens) {
				sourceVocab.add(token);
			}
		}

		// TODO: Refactor loading of lexical probs into a
		// "lexical probability loader"
		// so that we can later move to a binary format

		config.profiler.loadTimer.go();

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
		ProfilerTimer loadGrammarTimer =
				ProfilerTimer.newTimer("loadGrammar", config.profiler.loadTimer, true, true);

		Grammar<R, T> grammar =
				new Grammar<R, T>(tokenFactory, opts.startSymbols, sourceVocab, filterLHSToks,
						filterRHSToks);
		config.grammar = grammar;

		TextProgressBar grammarBar =
				new TextProgressBar(System.err, "grammar rule", 100, opts.barWidth,
						opts.animatedBar);
		for (Pair<File, Class<? extends GrammarLoader<R, T>>> grammarInfo : opts.grammarFile) {

			File grammarFile = grammarInfo.first;
			Class<? extends GrammarLoader<R, T>> grammarLoaderClass = grammarInfo.second;
			GrammarLoader<R, T> grammarLoader = loadPlugin(grammarLoaderClass);

			grammarLoader.loadGrammar(grammar, new FileInputStream(grammarFile),
					grammarFile.getAbsolutePath(), opts.grammarEncoding, grammarBar);
		}

		TextProgressBar lexiconBar =
				new TextProgressBar(System.err, "lexical rule", 100, opts.barWidth,
						opts.animatedBar);
		for (Pair<File, Class<? extends GrammarLoader<R, T>>> lexiconInfo : opts.lexiconFile) {

			File lexiconFile = lexiconInfo.first;
			Class<? extends GrammarLoader<R, T>> lexiconLoaderClass = lexiconInfo.second;
			GrammarLoader<R, T> lexiconLoader = loadPlugin(lexiconLoaderClass);

			lexiconLoader.loadGrammar(grammar, new FileInputStream(lexiconFile),
					lexiconFile.getAbsolutePath(), opts.grammarEncoding, lexiconBar);
		}

		grammar = grammar.keepKBestRules(opts.keepKBestRules);

		config.oovHandler = loadPlugin(opts.oovHandlerClass);

		config.targetVocab = grammar.getTargetVocabulary(ruleFactory.getTransducer());
		config.targetVocab.addAll(config.oovHandler.getAdditionalTargetVocabulary(sourceVocab));

		// get additional vocabulary from morphology generator
		MorphologyGenerator<T> morphGen = new NullMorphologyGenerator<T>();
		HashSet<T> addTargetVocab = morphGen.getAdditionalTargetVocabulary(sourceVocab);
		config.targetVocab.addAll(addTargetVocab);

		log.info("Source vocab size = " + config.sourceVocab.size() + "\nTarget vocab size = "
				+ config.targetVocab.size());

		loadGrammarTimer.pause();

		// load features after we already know the target-side vocabulary, etc.
		List<Feature<R, T, ?>> features =
				new ArrayList<Feature<R, T, ?>>(opts.featureClasses.length);
		for (Class<? extends Feature<R, T, ?>> featureClass : opts.featureClasses) {
			Feature<R, T, ?> f = loadPlugin(featureClass);
			features.add(f);
		}
		config.features = features;
		LogLinearScorerOptions scorerOpts = configurator.getOptions(LogLinearScorerOptions.class);
		config.scorer = new LogLinearScorer<R, T>(scorerOpts, config);

		config.profiler.loadTimer.pause();

		config.merger = loadPlugin(opts.mergerClass);
		config.unpacker = loadPlugin(opts.forestProcessorClass);
		config.parser = loadPlugin(opts.parserClass);
		config.decoder = loadPlugin(opts.decoderClass);
	}

	private <X extends Plugin<R, T>> X loadPlugin(Class<X> classToLoad) {

		// TypeVariable<Class<X>>[] genericParams =
		// classToLoad.getTypeParameters();
		// assert genericParams.length == 2;
		// String strRuleType =
		// genericParams[0].getGenericDeclaration().getName(); // getBounds();
		// String strTokenType =
		// genericParams[1].getGenericDeclaration().getName(); // getBounds();
		// assert strRuleType.equals(ruleClass.getName());
		// assert strTokenType.equals(tokenClass.getName());

		try {
			return PluginLoader.loadPlugin(classToLoad, config.configurator, this.config, false);
		} catch (InvocationTargetException e) {
			// if we've run validatePlugins() correctly, this should never
			// happen
			throw new RuntimeException(e);
		}
	}

	public void translateAll() throws RuleException {

		config.profiler.processingTimer.go();
		TextProgressBar progressBar =
				new TextProgressBar(System.err, "sent", 100, config.opts.barWidth,
						config.opts.animatedBar);
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

		config.profiler.processingTimer.pause();
		progressBar.endTask();
	}

	public int getSentenceCount() {
		return this.sentences.length;
	}

	public List<PartialParse<T>>[] getResults() {
		List<PartialParse<T>>[] list = new List[results.length];

		for (int i = 0; i < results.length; i++) {
			list[i] = results[i].nBestList;
		}

		return list;
	}

	public void writeResults() throws FileNotFoundException, UnsupportedEncodingException {

		for (Pair<File, Class<? extends HypothesisFormatter<R, T>>> outInfo : config.opts.outputInfo) {
			File outFile = outInfo.first;
			Class<? extends HypothesisFormatter<R, T>> formatterClass = outInfo.second;
			HypothesisFormatter<R, T> hypothesisFormatter = loadPlugin(formatterClass);

			PrintWriter out = new PrintWriter(outFile, config.opts.outputEncoding);
			for (Result<R, T> result : results) {
				hypothesisFormatter.formatHypothesis(result, out);
			}
			out.close();

		}

		if (config.opts.latticeInfo != null) {
			File latticeFile = config.opts.latticeInfo.first;
			Class<? extends LatticeFormatter<R, T>> formatterClass = config.opts.latticeInfo.second;
			LatticeFormatter<R, T> latticeFormatter = loadPlugin(formatterClass);

			PrintWriter out = new PrintWriter(latticeFile, config.opts.outputEncoding);
			for (Result<R, T> result : results) {
				if (result.getLattice() != null) {
					out.println(latticeFormatter.format(result.getLattice()));
				}
			}
			out.close();
		}
	}

	public Result<R, T> translate(int nSentence, String sentence) throws RuleException {

		// TODO: Allow multithreaded calls to this method?

		T[] inputTokens = config.tokenFactory.makeTokens(StringUtils.tokenize(sentence), true);

		log.fine("Parsing...");
		Chart<R, T> chart = config.parser.parse(inputTokens);

		ParseFactory<R, T> parseFactory =
				new ParseFactory<R, T>(config.tokenFactory, config.ruleFactory.getTransducer(),
						inputTokens.length);

		// create lattices by unpacking forest
		log.fine("Unpacking forest...");
		Lattice<R, T> lattice = new Lattice<R, T>(chart, config.unpacker, parseFactory);

		log.fine("Decoding...");
		List<PartialParse<T>> nBestList = config.decoder.getKBest(lattice, parseFactory);

		// let the lattice get garbage collected
		if (config.opts.latticeInfo == null)
			lattice = null;

		return new Result<R, T>(nSentence, sentence, nBestList, lattice);
	}

	public static OptionParser readOptions(String[] args) throws PropertiesException, IOException {

		if (args.length == 0) {
			System.err.println("Usage: treegraft [<properties_file> | -if <xfer_ini_file>]");
			System.exit(1);
		}

		log.info("Searching for plug-ins...");
		ClassFinder<Options> finder = new ClassFinder<Options>();
		Vector<Class<? extends Options>> configurables = finder.findSubclasses(Options.class);
		StringBuilder builder = new StringBuilder();
		for (Class<? extends Options> c : configurables) {
			builder.append(c.getSimpleName() + " ");
		}
		log.info("Found " + configurables.size() + " plug-ins: " + builder.toString() + "\n");

		Properties props = new Properties();
		if (args.length > 0) {
			if (args[0].equals("-h") || args[0].equals("-help") || args[0].equals("--help")) {
				args = ArrayUtils.cutFirst(args, 1);
				OptionParser configurator =
						new OptionParser(configurables, args, new Properties(), false);
				System.out.println(configurator.getGlobalUsage());
				System.exit(0);
			}

			if (args[0].equals("-if")) {
				props = XferAdapter.parseConfigFile(args[1]);
				args = ArrayUtils.cutFirst(args, 2);

			} else {
				props = PropertyUtils.getProperties(args[0]);
				args = ArrayUtils.cutFirst(args, 1);

			}
		}

		boolean failOnUnrecognized = false;
		OptionParser configurator =
				new OptionParser(configurables, args, props, failOnUnrecognized);
		configurator.validateConfiguration();

		return configurator;
	}

	public static void main(String[] args) throws Exception {

		System.out.println("Treegraft V0.4.0 -- by Jonathan Clark (December 2008)");
		System.out.println();

		// TODO: Allow only parsing, decoding, etc.
		// TODO: How do we run treegraft as a monolingual parser?
		// TODO: Just as a synchronous parser?
		// TODO: Future cost estimator for building chart entries
		// TODO: Deal with datatype of forest processor vs forest unpacker
		// TODO: Refactor so that parsers can return a more general data
		// structure, such as a hypergraph
		// TODO: Implement a CubePruningMerger

		OptionParser configurator = readOptions(args);
		TreegraftCoreOptions<?, ?> opts = configurator.getOptions(TreegraftCoreOptions.class);

		log.info("Initializing...");

		Treegraft<?, ?> treegraft;
		if (opts.nThreads > 1) {
			log.info("Starting " + opts.nThreads + " threads...");
			treegraft = new MultithreadedTreegraft(opts, configurator, opts.nThreads);
		} else {
			treegraft = new Treegraft(opts, configurator);
		}

		treegraft.translateAll();
		treegraft.writeResults();

		log.info(treegraft.config.profiler.treegraftTimer.getTimingReport(true));
		System.exit(0);
	}
}
