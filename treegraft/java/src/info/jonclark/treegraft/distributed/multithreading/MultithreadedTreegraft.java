package info.jonclark.treegraft.distributed.multithreading;

import info.jonclark.lang.OptionParser;
import info.jonclark.lang.Pair;
import info.jonclark.stat.TextProgressBar;
import info.jonclark.treegraft.Treegraft;
import info.jonclark.treegraft.core.plugin.ReflectionException;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRule;
import info.jonclark.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.concurrent.ArrayBlockingQueue;

public class MultithreadedTreegraft<R extends SyncCFGRule<T>, T extends Token> extends
		Treegraft<R, T> {

	private ArrayBlockingQueue<Pair<Integer, String>> sentences;
	private final int nThreads;

	public MultithreadedTreegraft(TreegraftCoreOptions<R, T> config, OptionParser configurator,
			int nThreads) throws IOException, ParseException, ReflectionException, RuleException,
			InvocationTargetException, IllegalArgumentException, SecurityException,
			InstantiationException, IllegalAccessException, NoSuchMethodException {

		// use one of everything... except parsers and decoders
		super(config, configurator);

		this.sentences =
				new ArrayBlockingQueue<Pair<Integer, String>>(super.sentences.length, true);

		this.nThreads = nThreads;
	}

	@Override
	public void translateAll() {

		// share the large read-only data structures such as LM and Grammar
		// just use a separate thread per sentence

		this.sentences.clear();
		for (int i = 0; i < super.sentences.length; i++) {
			this.sentences.add(new Pair<Integer, String>(i, super.sentences[i]));
		}

		final TextProgressBar progressBar =
				new TextProgressBar(System.err, "sent", 100, super.config.opts.barWidth,
						super.config.opts.animatedBar);
		progressBar.beginTask(sentences.size());

		final Thread[] threads = new Thread[nThreads];

		for (int i = 0; i < nThreads; i++) {
			threads[i] = new Thread() {
				public void run() {
					Pair<Integer, String> pair;
					while ((pair = sentences.poll()) != null) {
						try {
							results[pair.first] = translate(pair.first, pair.second);
						} catch (Throwable t) {
							t.printStackTrace();
							System.exit(1);
							// results[pair.first] = new Result<T>(pair.first,
							// t);
							// log.severe("Ignoring sentence failure: " +
							// StringUtils.getStackTrace(t));
						}
						synchronized (progressBar) {
							progressBar.recordEventCompletion();
						}
					}
				}
			};
			threads[i].start();
		}

		for (int i = 0; i < threads.length; i++) {
			try {
				threads[i].join();
			} catch (InterruptedException e) {
				log.severe(StringUtils.getStackTrace(e));
			}
		}
		progressBar.endTask();
	}
}
