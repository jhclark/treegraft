package info.jonclark.treegraft.distributed.multithreading;

import info.jonclark.lang.Pair;
import info.jonclark.properties.SmartProperties;
import info.jonclark.treegraft.Treegraft;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.grammar.GrammarLoader;
import info.jonclark.treegraft.parsing.rules.GrammarRule;
import info.jonclark.treegraft.parsing.rules.RuleFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.ArrayBlockingQueue;

public class MultithreadedTreegraft<R extends GrammarRule<T>, T extends Token> extends
		Treegraft<R, T> {

	private final ArrayBlockingQueue<Pair<Integer, String>> sentences;
	private final int nThreads;

	public MultithreadedTreegraft(SmartProperties props, TokenFactory<T> tokenFactory,
			RuleFactory<R, T> ruleFactory, GrammarLoader<R, T> grammarLoader, int nThreads)
			throws IOException, ParseException, ReflectionException {

		// use one of everything... except parsers and decoders
		super(props, tokenFactory, ruleFactory, grammarLoader);

		this.nThreads = nThreads;
		this.sentences =
				new ArrayBlockingQueue<Pair<Integer, String>>(super.sentences.length, true);
		for (int i = 0; i < super.sentences.length; i++) {
			this.sentences.add(new Pair<Integer, String>(i, super.sentences[i]));
		}
	}

	@Override
	public void translateAll() {

		// share the large read-only data structures such as LM and Grammar
		// just use a separate thread per sentence

		final Thread[] threads = new Thread[nThreads];

		for (int i = 0; i < nThreads; i++) {
			threads[i] = new Thread() {
				public void run() {
					Pair<Integer, String> pair;
					while ((pair = sentences.poll()) != null) {
						try {
							results[pair.first] = translate(pair.second);
						} catch (Throwable t) {
							// don't fail just because one sentences dies!
							results[pair.first] = new Result<T>(t);
						}
					}
				}
			};
			threads[i].start();
		}
	}
}
