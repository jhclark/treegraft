package info.jonclark.treegraft.core.lm;

import info.jonclark.log.LogUtils;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.core.tokens.TokenSequence;
import info.jonclark.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

public class ARPALanguageModelLoader<T extends Token> implements LanguageModelLoader<T> {

	private static final Logger log = LogUtils.getLogger();

	// TODO: Update targetVocab to deal with possiblity of OOV's
	public void loadLM(LanguageModel<T> lm, TokenFactory<T> tokenFactory, InputStream stream,
			String encoding, HashSet<T> targetVocab, double oovLogProb) throws IOException {

		BufferedReader in = new BufferedReader(new InputStreamReader(stream, encoding));

		// 0 = data
		// 1 = 1-grams
		// 2 = 2-grams
		// / etc.
		int filePosition = -1;
		ArrayList<Integer> ngramEntries = new ArrayList<Integer>();

		lm.setOOVProb(oovLogProb);

		int total = 0;
		int kept = 0;

		String line;
		while ((line = in.readLine()) != null) {

			line = line.trim();
			if (line.equals("\\data\\")) {
				
				line = in.readLine().trim();

				filePosition = 0;
				while (line.startsWith("ngram ")) {
					int numEntiresForN = Integer.parseInt(StringUtils.substringAfter(line, "="));
					ngramEntries.add(numEntiresForN);
					line = in.readLine();
				}

				lm.setOrder(ngramEntries.size());

			} else if (line.startsWith("\\") && line.endsWith("-grams:")) {
				filePosition++;
				int numEntriesForN = ngramEntries.get(filePosition-1);

				for (int i = 0; i < numEntriesForN; i++) {

					line = in.readLine();
					String[] entry = StringUtils.tokenize(line, "\t");

					String[] strTokens = StringUtils.tokenize(entry[1]);
					ArrayList<T> tokens = new ArrayList<T>(strTokens.length);
					for (String token : strTokens) {
						tokens.add(tokenFactory.makeToken(token, true));
					}

					boolean vocabContainsAll = true;
					for (T token : tokens) {
						if (targetVocab.contains(token) == false) {
							vocabContainsAll = false;
							break;
						}
					}

					total++;
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

		in.close();

		log.info("Finished loading LM: Read " + total + " n-grams and kept " + kept);
	}
}
