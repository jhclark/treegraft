package edu.cmu.cs.lti.avenue;

import info.jonclark.stat.SecondTimer;
import info.jonclark.treegraft.core.tokens.integer.IntegerToken;
import info.jonclark.treegraft.core.tokens.integer.IntegerTokenFactory;
import info.jonclark.treegraft.core.tokens.integer.IntegerTokenSequence;
import info.jonclark.treegraft.parsing.rules.RuleException;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGGrammarLoader;
import info.jonclark.treegraft.parsing.synccfg.SyncCFGRule;
import info.jonclark.util.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class FastLexiconFilterer {

	private int nGramLengthLimit = 7;
	private IntegerTokenFactory tokenFactory = new IntegerTokenFactory();
	private final HashSet<IntegerTokenSequence> testNgrams =
			new HashSet<IntegerTokenSequence>(10000);
	private final String fSentsIn;
	private final String fLexiconIn;
	private final String fLexiconOut;
	private final int threshold;

	private final List<SyncCFGRule<IntegerToken>> POISON;
	private final SyncCFGRule<IntegerToken> POISON2;

	private ArrayBlockingQueue<List<SyncCFGRule<IntegerToken>>> rulesIn =
			new ArrayBlockingQueue<List<SyncCFGRule<IntegerToken>>>(1000000);
	private ArrayBlockingQueue<SyncCFGRule<IntegerToken>> rulesOut =
			new ArrayBlockingQueue<SyncCFGRule<IntegerToken>>(1000000);
	private Thread[] workers;

	public FastLexiconFilterer(String string, String string2, String string3, int threshold)
			throws RuleException {
		this.fSentsIn = string;
		this.fLexiconIn = string2;
		this.fLexiconOut = string3;
		this.threshold = threshold;

		POISON = new ArrayList<SyncCFGRule<IntegerToken>>(0);
		POISON2 =
				new SyncCFGRule<IntegerToken>(null, null, null, null, null, null, null, null, null,
						0, null, null);
	}

	public void loadSentences() throws IOException {

		System.out.println("Loading test sentences...");
		BufferedReader sentsIn = new BufferedReader(new FileReader(fSentsIn));
		String line;
		int n = 0;
		while ((line = sentsIn.readLine()) != null) {

			List<IntegerToken> words =
					Arrays.asList(tokenFactory.makeTokens(StringUtils.tokenize(line), true));
			for (int i = 0; i < words.size(); i++) {
				for (int j = i; j <= Math.min(i + nGramLengthLimit, words.size()); j++) {
					List<IntegerToken> sublist = words.subList(i, j);
					// System.out.println(StringUtils.untokenize(tokenFactory.getTokensAsStrings(sublist)));
					IntegerTokenSequence subseq = new IntegerTokenSequence(sublist);
					testNgrams.add(subseq);
				}
			}
			n++;
		}
		sentsIn.close();
		System.out.println("Loaded " + n + " test sentences.");
	}

	private class ReaderThread implements Runnable {
		public void run() {
			try {
				BufferedReader lexiconIn = new BufferedReader(new FileReader(fLexiconIn),
				// 1000 *
						1000 * 1000);

				SyncCFGGrammarLoader<IntegerToken> loader =
						new SyncCFGGrammarLoader<IntegerToken>(tokenFactory);

				IntegerToken[] prevRhs = null;
				List<SyncCFGRule<IntegerToken>> ruleGroup =
						new ArrayList<SyncCFGRule<IntegerToken>>();
				SyncCFGRule<IntegerToken> rule = null;
				int i = 0;
				SecondTimer timer = new SecondTimer(false, true);
				do {
					try {

						rule = loader.nextRule(lexiconIn, fLexiconIn);
						if (rule == null)
							break;

						// add previous rule group when we get to a new group
						// (determined by source rhs)
						IntegerToken[] curRhs = rule.getRhs();
						if (prevRhs != null && !Arrays.equals(prevRhs, curRhs)) {
							rulesIn.put(ruleGroup);
							ruleGroup = new ArrayList<SyncCFGRule<IntegerToken>>();
						}
						
						// prepare for next iteration
						ruleGroup.add(rule);
						prevRhs = curRhs;

						if (i % 1000000 == 0) {
							System.out.println("Read " + i + " rules so far... ("
									+ timer.getEventsPerSecond(i) + " rules/sec)");
						}
						i++;
					} catch (RuleException e) {
						System.err.println("Invalid rule: " + e.getMessage());
					}
				} while (rule != null);
				
				// put the last rule group
				if (prevRhs != null) {
					rulesIn.put(ruleGroup);
				}

				for (i = 0; i < 100; i++) {
					rulesIn.put(POISON);
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class WriterThread implements Runnable {
		public void run() {
			try {
				PrintWriter lexiconOut = new PrintWriter(fLexiconOut);

				int nPoison = 0;
				SyncCFGRule<IntegerToken> rule;
				while (nPoison < workers.length) {
					rule = rulesOut.take();
					if (rule == POISON2) {
						nPoison++;
					} else {
						lexiconOut.print(rule.originalString);
					}
				}

				lexiconOut.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private class WorkerThread implements Runnable {
		public void run() {

			Comparator<SyncCFGRule<IntegerToken>> c = new Comparator<SyncCFGRule<IntegerToken>>() {
				public int compare(SyncCFGRule<IntegerToken> o1, SyncCFGRule<IntegerToken> o2) {
					if (o1.getRuleScores().sgt * o1.getRuleScores().tgs > o2.getRuleScores().sgt
							* o2.getRuleScores().tgs) {
						return -1;
					} else {
						return 1;
					}
				}
			};

			List<SyncCFGRule<IntegerToken>> ruleGroup;
			try {
				while ((ruleGroup = rulesIn.take()) != POISON) {

					List<IntegerToken> rhs = Arrays.asList(ruleGroup.get(0).getRhs());
					IntegerTokenSequence seq = new IntegerTokenSequence(rhs);
					if (testNgrams.contains(seq)) {

						// keep n-best rules
						Collections.sort(ruleGroup, c);
						for (int i = 0; i < ruleGroup.size() && i < threshold; i++) {
							rulesOut.put(ruleGroup.get(i));
//							System.out.println("YES: "
//									+ StringUtils.untokenize(tokenFactory.getTokensAsStrings(rhs)));
						}
					} else {
//						System.out.println("NO: "
//								+ StringUtils.untokenize(tokenFactory.getTokensAsStrings(rhs)));
					}
				}

				rulesOut.put(POISON2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.err.println("Usage: program <test_sents> <lexicon_in> <lexicon_out> <n-best-rule-threshold>");
			System.exit(1);
		}

		FastLexiconFilterer filt =
				new FastLexiconFilterer(args[0], args[1], args[2], Integer.parseInt(args[3]));
		filt.loadSentences();
		filt.start();
	}

	private void start() throws InterruptedException {

		workers = new Thread[4];

		Thread reader = new Thread(new ReaderThread());
		reader.start();
		Thread writer = new Thread(new WriterThread());
		writer.start();

		for (int i = 0; i < workers.length; i++) {
			workers[i] = new Thread(new WorkerThread());
			workers[i].start();
		}
		reader.join();
		for (int i = 0; i < workers.length; i++) {
			workers[i].join();
		}
		writer.join();
	}
}
