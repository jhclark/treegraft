package info.jonclark.treegraft.core.lm;

import info.jonclark.log.LogUtils;
import info.jonclark.stat.TaskListener;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Logger;

public class ARPALanguageModelLoader<T extends Token> implements LanguageModelLoader<T> {

	private static final Logger log = LogUtils.getLogger();

	public void loadLM(LanguageModel<T> lm, TokenFactory<T> tokenFactory, InputStream stream,
			String encoding, HashSet<T> targetVocab, TaskListener task) throws IOException {

		TokenSequence<T> bos =
				tokenFactory.makeTokenSequence(Arrays.asList(tokenFactory.makeToken("<s>", true)));
		TokenSequence<T> eos = tokenFactory.makeTokenSequence(Arrays.asList(tokenFactory.makeToken("</s>", true)));
		lm.setSentenceBeginMarker(bos);
		lm.setSentenceEndMarker(eos);

		BufferedReader in = new BufferedReader(new InputStreamReader(stream, encoding));

		// 0 = data
		// 1 = 1-grams
		// 2 = 2-grams
		// / etc.
		int filePosition = -1;
		ArrayList<Integer> ngramEntries = new ArrayList<Integer>();

		int expectedEntries = 0;
		int total = 0;
		int kept = 0;

		T unk = tokenFactory.makeToken("<unk>", true);

		String line;
		while ((line = in.readLine()) != null) {

			line = line.trim();
			if (line.equals("\\data\\")) {

				line = in.readLine().trim();

				filePosition = 0;
				while (line.startsWith("ngram ")) {
					int numEntiresForN = Integer.parseInt(StringUtils.substringAfter(line, "="));
					ngramEntries.add(numEntiresForN);
					expectedEntries += numEntiresForN;
					line = in.readLine();
				}

				lm.setOrder(ngramEntries.size());
				if (task != null)
					task.beginTask(expectedEntries);

			} else if (line.startsWith("\\") && line.endsWith("-grams:")) {
				filePosition++;
				int numEntriesForN = ngramEntries.get(filePosition - 1);

				for (int i = 0; i < numEntriesForN; i++) {

					line = in.readLine();
					String[] entry = StringUtils.tokenize(line, "\t");

					String[] strTokens = StringUtils.tokenize(entry[1]);
					ArrayList<T> tokens = new ArrayList<T>(strTokens.length);
					for (String token : strTokens) {
						tokens.add(tokenFactory.makeToken(token, true));
					}

					// TODO: Make sure OOV prob gets set
					if (tokens.size() == 1 && tokens.get(0).equals(unk)) {
						double logProb = Double.parseDouble(entry[0]);
						lm.setOOVProb(logProb);
					}

					boolean vocabContainsAll = true;
					if (targetVocab != null) {
						for (T token : tokens) {
							if (targetVocab.contains(token) == false) {
								vocabContainsAll = false;
								break;
							}
						}
					}

					if (task != null)
						task.recordEventCompletion();
					total++;

					// if(total >= 50000) {
					// log.warning("HACK done loading LM: Read " + total +
					// " n-grams and kept " + kept);
					// return;
					// }

					if (vocabContainsAll) {
						kept++;

						TokenSequence<T> tokenSequence = tokenFactory.makeTokenSequence(tokens);

						double logProb = Double.parseDouble(entry[0]);
						double backoffLogProb = Double.NEGATIVE_INFINITY;
						if (entry.length == 3) {
							backoffLogProb = Double.parseDouble(entry[2]);
						}

						lm.addEntry(tokenSequence, logProb, backoffLogProb);
					}
				}
			}
		}

		if (task != null)
			task.endTask();
		in.close();

		log.info("Finished loading LM: Read " + total + " n-grams and kept " + kept);
	}
}
