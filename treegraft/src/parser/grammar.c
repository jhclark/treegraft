private boolean useTopDownPredictions;
private final GrammarRule[] ruleList;
private final HashMap<String, GrammarRule[]> rules = new HashMap<String, GrammarRule[]>();
private final HashMap<String, String[]> words = new HashMap<String, String[]>();

grammar_t* grammar_init(char* filename) {
	BufferedReader in = new BufferedReader(new FileReader(file));
	ArrayList<GrammarRule> ruleList = new ArrayList<GrammarRule>();

	String line;
	while ((line = in.readLine()) != null) {

		// parse full rules
		if (line.contains("=>")) {
			String lhs = StringUtils.substringBefore(line, "=>").trim();
			String after = StringUtils.substringAfter(line, "=>").trim();
			String[] rhs = StringUtils.tokenize(after);
			GrammarRule rule = new GrammarRule(lhs, rhs);
			ruleList.add(rule);

			// since we aren't likely to expand frequently and
			// this code isn't time-critical, use a poor man's vector
			if (rules.containsKey(rhs[0])) {
				GrammarRule[] existingRules = rules.get(rhs[0]);
				GrammarRule[] newRules = new GrammarRule[existingRules.length + 1];
				System.arraycopy(existingRules, 0, newRules, 0,
						existingRules.length);
				newRules[existingRules.length] = rule;
				rules.put(rhs[0], newRules);
			} else {
				rules.put(rhs[0], new GrammarRule[] {rule}
						);
					}

			// parse lexical rules
		} else if (line.contains("->")) {
			String pos = StringUtils.substringBefore(line, "->").trim();
			String word = StringUtils.substringAfter(line, "->").trim();

			// since we aren't likely to expand frequently and
			// this code isn't time-critical, use a poor man's vector
			if (words.containsKey(word)) {
				String[] existingPos = words.get(word);
				String[] newPos = new String[existingPos.length + 1];
				System.arraycopy(existingPos, 0, newPos, 0, existingPos.length);
				newPos[existingPos.length] = pos;
				words.put(word, newPos);
			} else {
				words.put(word, new String[] {pos}
						);
					}
		}
	}

	this.ruleList = ruleList.toArray(new GrammarRule[ruleList.size()]);
	in.close();
}

/**
 * Gets all valid parts of speech for a word
 * 
 * @param word
 * @return
 */
public String[] grammar_get_pos(String word) {
	String[] pos = words.get(word);
	if (pos == null) {
		return new String[0];
	} else {
		return pos;
	}
}

public GrammarRule[] getRulesStartingWith(Key key) {
	if (useTopDownPredictions) {

	}
	GrammarRule[] result = rules.get(key.getLhs());
	if (result == null) {
		return new GrammarRule[0];
	} else {
		return result;
	}
}

public GrammarRule[] getRules() {
	return ruleList;
}

