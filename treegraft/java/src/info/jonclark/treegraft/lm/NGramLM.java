package info.jonclark.treegraft.lm;

import info.jonclark.treegraft.core.tokens.Token;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

public class NGramLM implements LanguageModel {

	private HashMap<Token, Double>[] probs;
	private double oovProb;

	public NGramLM() {
	}

	public void load(InputStream inputStream) throws IOException {

		// TODO: Read in these values from the serialized LM
		int order = 3;
		double oovProb = 0.5;

		probs = new HashMap[order];
		for (int i = 0; i < order; i++)
			probs[i] = new HashMap<Token, Double>();

		this.oovProb = oovProb;

		BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		while ((line = in.readLine()) != null) {

		}
		in.close();
	}

	public double score(Token[] tokens) {
		if (probs == null)
			throw new RuntimeException("No language model loaded.");

		int order = tokens.length;
		Double score = probs[order - 1].get(tokens);
		while (score == null) {
			if (order == -1)
				return oovProb;
			score = probs[order - 1].get(tokens);
			order--;
		}
		return score;
	}
}
