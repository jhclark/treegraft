package info.jonclark.treegraft.decoder.mert;

import info.jonclark.properties.SmartProperties;
import info.jonclark.treegraft.Treegraft;
import info.jonclark.treegraft.core.plugin.ReflectionException;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.decoder.DecoderHypothesis;
import info.jonclark.treegraft.distributed.multithreading.MultithreadedTreegraft;
import info.jonclark.treegraft.parsing.grammar.GrammarLoader;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.rules.RuleFactory;
import info.jonclark.treegraft.parsing.synccfg.OneLineLexiconGrammarLoader;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGGrammarLoader;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRuleFactory;
import info.jonclark.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.phramer.mert.generation.NewHypothesesGenerationManager;
import org.phramer.mert.item.Hypothesis;
import org.phramer.v1.decoder.mert.item.ESentence;

public class TreegraftPhramerMERTAdapter<T extends Token> implements NewHypothesesGenerationManager {

	private final Treegraft<?, T> treegraft;
	private final TokenFactory<T> tokenFactory;

	public TreegraftPhramerMERTAdapter(Properties props, TokenFactory<T> tokenFactory)
			throws IOException, ParseException, ReflectionException, RuleException {

		SmartProperties smartProps = new SmartProperties(props);

		this.tokenFactory = tokenFactory;
		RuleFactory<?, ?> ruleFactory = new SyncCFGRuleFactory(tokenFactory);
		// XXX: Do scores need converting to log domain?
		GrammarLoader<?, ?> grammarLoader = new SyncCFGGrammarLoader(tokenFactory, true);
		GrammarLoader<?, ?> lexiconLoader = new OneLineLexiconGrammarLoader(tokenFactory);

		int nThreads = smartProps.getPropertyInt("global.numThreads");
		if (nThreads > 1) {
			treegraft =
					new MultithreadedTreegraft(smartProps, tokenFactory, ruleFactory,
							grammarLoader, lexiconLoader, nThreads);
		} else {
			treegraft =
					new Treegraft(smartProps, tokenFactory, ruleFactory, grammarLoader,
							lexiconLoader);
		}

		// treegraft.translateAll();
		// treegraft.showResults();
	}

	public Vector<Hypothesis>[] getHypotheses(double[] lambda, int run) {

		treegraft.getScorer().setFeatureWeightVector(lambda);
		try {
			treegraft.translateAll();
		} catch (RuleException e) {
			throw new RuntimeException(e);
		}

		List<DecoderHypothesis<T>>[] treegraftHypotheses = treegraft.getResults();
		Vector<Hypothesis>[] phramerHypotheses = new Vector[treegraft.getSentenceCount()];

		PrintWriter out;
		try {
			out = new PrintWriter("hyps." + run);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		for (int i = 0; i < phramerHypotheses.length; i++) {
			phramerHypotheses[i] = new Vector<Hypothesis>();

			for (DecoderHypothesis<T> treegraftHypothesis : treegraftHypotheses[i]) {
				
				Hypothesis mertHypothesis = new Hypothesis();

				// convert tokens back to strings
				List<T> tokens = treegraftHypothesis.getTokens();
				String[] translatedTokens = new String[tokens.size()];
				for (int k = 0; k < translatedTokens.length; k++) {
					translatedTokens[k] = tokenFactory.getTokenAsString(tokens.get(k));
				}
				
				out.println(StringUtils.untokenize(translatedTokens));

				double[] probVector = treegraftHypothesis.getScores().getFeatureLogProbVector();
				mertHypothesis.h = new ESentence(translatedTokens, translatedTokens);
				mertHypothesis.p = new double[probVector.length];
				phramerHypotheses[i].add(mertHypothesis);
			}
			
			out.println("---");
		}
		
		out.close();
		
		return phramerHypotheses;
	}

	public String printDecoderParameters(double[] lambda) {
		StringBuilder builder = new StringBuilder();
		String[] labels = treegraft.getScorer().getFeatureProbVectorLabels();
		builder.append("# Feature weights:\n");
		for (int i = 0; i < labels.length; i++) {
			builder.append("features." + labels[i] + " = " + lambda[i] + "\n");
		}		
		
		return builder.toString();
	}

}
