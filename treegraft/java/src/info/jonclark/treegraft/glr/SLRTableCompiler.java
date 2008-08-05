package info.jonclark.treegraft.glr;

import info.jonclark.treegraft.core.Grammar;
import info.jonclark.treegraft.core.rules.DottedGrammarRule;
import info.jonclark.treegraft.core.rules.GrammarRule;
import info.jonclark.treegraft.core.rules.MonoCFGRule;
import info.jonclark.util.HashUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class SLRTableCompiler {
	public static SLRTable compile(Grammar grammar) {
		SLRTable table = new SLRTable();
//
//		GrammarRule[] grammarRules = grammar.getRules();
//
//		int nStateLabel = 0;
//		ArrayList<DottedGrammarRule> dottedRules =
//				new ArrayList<DottedGrammarRule>(grammarRules.length + 1);
//
//		// add initial state
//		
//		// TODO: Come up with a more intelligent file/line for this rule
//		dottedRules.add(new DottedGrammarRule(0, new MonoCFGRule("S'",
//				new String[] { "S", "$" }, null, new File("null"), 0)));
//		for (int i = 0; i < grammarRules.length; i++) {
//			dottedRules.add(new DottedGrammarRule(0, grammarRules[i]));
//		}
//		State state = new State();
//		state.label = nStateLabel;
//		nStateLabel++;
//		state.rules = dottedRules.toArray(new DottedGrammarRule[dottedRules.size()]);
//		table.addState(state);
//
//		HashMap<String, ArrayList<DottedGrammarRule>> rulesByNeeds =
//				new HashMap<String, ArrayList<DottedGrammarRule>>();
//
//		// now put the rules into buckets based on what the dot expects next
//		for (final DottedGrammarRule r : dottedRules) {
//			HashUtils.append(rulesByNeeds, r.getNeededSymbol(), r);
//		}
//
//		// TODO: closure of the initial rules and follow rules
//		// TODO: adding all additional rules
		return table;
	}
}
