package info.jonclark.treegraft.decoder.mert;

import info.jonclark.properties.PropertyUtils;
import info.jonclark.treegraft.core.tokens.string.StringTokenFactory;
import info.olteanu.utils.PropertiesTools;
import info.olteanu.utils.StringTools;
import info.olteanu.utils.debug.DebugInitializer;

import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import org.phramer.PhramerException;
import org.phramer.mert.MinimumErrorRateTraining;
import org.phramer.mert.evaluation.Evaluator;
import org.phramer.mert.evaluation.xer.PEREvaluator;
import org.phramer.mert.evaluation.xer.WEREvaluator;
import org.phramer.mert.generation.NewHypothesesGenerationManager;
import org.phramer.mert.intersection.OptimumLambdaCalculatorTool;
import org.phramer.mert.item.ReferenceLoader;
import org.phramer.mert.strategy.LambdaNormalizer;
import org.phramer.mert.strategy.SearchLambdaStep;
import org.phramer.mert.strategy.SearchLambdaStrategy;
import org.phramer.mert.strategy.implementations.LambdaNormalizerMax1;
import org.phramer.mert.strategy.implementations.LambdaNormalizerMedian1;
import org.phramer.mert.strategy.implementations.LambdaNormalizerSumX;
import org.phramer.mert.strategy.implementations.MultiThreadedSearchStrategy;
import org.phramer.mert.strategy.implementations.SearchLambdaStrategyOrthogonalDiagonal;
import org.phramer.v1.decoder.mert.evaluation.bleu.BleuR1N4Evaluator;
import org.phramer.v1.decoder.mert.evaluation.bleu.BleuRnN4Evaluator;
import org.phramer.v1.decoder.mert.item.ReferenceLoaderMT;

public class MERT {
	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.err.println("Usage: program <properties_file>");
			System.exit(1);
		}

		DebugInitializer.initialize();

		// Properties p = PropertiesTools.getProperties(args[0]);
		Properties p = PropertyUtils.getProperties(args[0]);
		System.out.println("Config file: " + args[0]);

		// overwrite with the command line
		if (args.length > 1)
			PropertiesTools.addPropertiesFromCommandLine(p, StringTools.cutFirst(args, 1), "--");

		double[] initialLambda = parseInitialLambda(p);
		int nStepsOrthogonal = parseNStepsOrthogonal(p);
		int nStepsDiagonal = parseNStepsDiagonal(p);
		double minDeltaScore = parseMinDeltaScore(p);

		int nSkipIterations = parseNSkipIterationsGeneration(p);
		double forcedLambda[][] = parseForcedLambda(p);
		if (forcedLambda != null || nSkipIterations >= 2)
			if (forcedLambda.length != nSkipIterations
					&& forcedLambda.length + 1 != nSkipIterations)
				throw new IOException("forcedLambda.length and nSkipIterations don't match");

		ReferenceLoader loader = new ReferenceLoaderMT();

		// Orthogonal
		SearchLambdaStrategy strategy =
				new SearchLambdaStrategyOrthogonalDiagonal(nStepsOrthogonal, nStepsDiagonal,
						parseRandomLambdaRanges(p), null, minDeltaScore);
		// Multithreaded?
		if (p.getProperty("search.threads") != null
				&& Integer.parseInt(p.getProperty("search.threads")) != 0)
			strategy =
					new MultiThreadedSearchStrategy(
							Integer.parseInt(p.getProperty("search.threads")), nStepsOrthogonal,
							(SearchLambdaStep) strategy, parseRandomLambdaRanges(p));

		setParams(p);

		String norm = p.getProperty("normalizer.type", "max");
		String normListStr = p.getProperty("normalizer.index.list");
		int[] normList = null;
		if (normListStr != null) {
			String[] k = StringTools.tokenize(normListStr, " ,;");
			normList = new int[k.length];
			for (int i = 0; i < k.length; i++)
				normList[i] = Integer.parseInt(k[i]);
		}
		LambdaNormalizer normalizer =
				norm.equals("median") ? new LambdaNormalizerMedian1(normList) : norm.equals("sum")
						? new LambdaNormalizerSumX(normList, 1)
						: new LambdaNormalizerMax1(normList);
		String fileRef = parseFileRef(p);
		String filesRef[] = null;
		// multi-ref
		if (fileRef.indexOf(";") != -1) {
			filesRef = StringTools.tokenize(fileRef, ";");
			for (int i = 0; i < filesRef.length; i++)
				filesRef[i] = filesRef[i].trim();
			fileRef = null;
		}

		String evaluatorType = p.getProperty("evaluator.type", "bleu").toLowerCase();
		// Evaluator
		Evaluator eval;
		if (evaluatorType.equals("bleu"))
			eval = fileRef != null ? new BleuR1N4Evaluator() : new BleuRnN4Evaluator(filesRef[0]);
		else if (evaluatorType.equals("wer"))
			eval = new WEREvaluator();
		else if (evaluatorType.equals("per"))
			eval = new PEREvaluator();
		else
			throw new PhramerException("Unknown evaluator type: " + evaluatorType);

		// first value: mode
		if (filesRef != null)
			filesRef = StringTools.cutFirst(filesRef, 1);

		NewHypothesesGenerationManager mgr =
			new TreegraftPhramerMERTAdapter(p, new StringTokenFactory());
		double[] newLambda =
				MinimumErrorRateTraining.searchLambda(loader, mgr, eval, strategy, normalizer,
						fileRef, filesRef, initialLambda, null, forcedLambda);
		
		System.out.println("New lambdas: ");
		for (int i = 0; i < newLambda.length; i++)
			System.out.print(newLambda[i] + " ");
		System.out.println();

		mgr.printDecoderParameters(newLambda);
	}

	// sets special parameters in search
	private static void setParams(Properties p) {
		if (p.getProperty("search.params.extreme.delta") != null)
			OptimumLambdaCalculatorTool.DELTA =
					Double.parseDouble(p.getProperty("search.params.extreme.delta"));
		if (p.getProperty("search.params.extreme.coef") != null)
			OptimumLambdaCalculatorTool.COEF =
					Double.parseDouble(p.getProperty("search.params.extreme.coef"));
	}

	private static double[][] parseForcedLambda(Properties p) {
		if (p.getProperty("debug.resume.lambda.count") == null)
			return null;
		int n = Integer.parseInt(p.getProperty("debug.resume.lambda.count"));
		double[][] forcedLambda = new double[n][];
		for (int i = 1; i <= n; i++)
			forcedLambda[i - 1] = parseLambdas(p.getProperty("debug.resume.lambda." + i));
		return forcedLambda;
	}

	private static int parseNSkipIterationsGeneration(Properties p) {
		if (p.getProperty("debug.resume.generation.n-skip") == null)
			return 0;
		return Integer.parseInt(p.getProperty("debug.resume.generation.n-skip"));
	}

	private static double parseMinDeltaScore(Properties p) {
		return Double.parseDouble(p.getProperty("search.score.min-delta"));
	}

	private static int parseNStepsOrthogonal(Properties p) {
		return Integer.parseInt(p.getProperty("search.step-count"));
	}

	private static int parseNStepsDiagonal(Properties p) {
		if (p.getProperty("search.step-count.diagonal") == null)
			return 0;
		return Integer.parseInt(p.getProperty("search.step-count.diagonal"));
	}

	private static String parseFileRef(Properties p) {
		return p.getProperty("file.dev.e");
	}

	private static double[] parseInitialLambda(Properties p) {
		return parseLambdas(p.getProperty("lambda.initial-values"));
	}

	private static double[][] parseRandomLambdaRanges(Properties p) {
		String str = p.getProperty("lambda.random.ranges");
		StringTokenizer st = new StringTokenizer(str);
		double[][] lambda = new double[st.countTokens()][2];
		for (int i = 0; i < lambda.length; i++) {
			String token = st.nextToken();
			lambda[i][0] = Double.parseDouble(token.substring(0, token.indexOf('-', 1)));
			lambda[i][1] = Double.parseDouble(token.substring(token.indexOf('-', 1) + 1));
		}
		return lambda;
	}

	public static double[] parseLambdas(String str) throws NumberFormatException {
		// ignore non-numerical tokens also: March 06, 2006

		// StringTokenizer st = new StringTokenizer(str, ", \t\n\r\f");
		// double[] lambda = new double[st.countTokens()];
		// for (int i = 0; i < lambda.length; i++)
		// lambda[i] = Double.parseDouble(st.nextToken());
		// return lambda;

		StringTokenizer st = new StringTokenizer(str, ", \t\n\r\f");
		Vector<String> tokens = new Vector<String>();
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			try {
				Double.parseDouble(tok);

				tokens.add(tok);
			} catch (NumberFormatException e) {
			}
		}
		double[] lambda = new double[tokens.size()];
		for (int i = 0; i < lambda.length; i++)
			lambda[i] = Double.parseDouble(tokens.get(i));
		return lambda;
	}
}
