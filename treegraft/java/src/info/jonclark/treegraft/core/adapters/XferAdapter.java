package info.jonclark.treegraft.core.adapters;

import info.jonclark.util.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

/**
 * Allows parses a file passed in with "-if configfile.ini" in the xfer 3.0
 * format.
 */
public class XferAdapter {
	public static Properties parseConfigFile(String iniFile) throws IOException {

		Properties props = new Properties();

		// set defaults
		props.setProperty("features",
				"info.jonclark.treegraft.core.featureimpl.LanguageModelFeature "
						+ "info.jonclark.treegraft.core.featureimpl.RuleFeature "
						+ "info.jonclark.treegraft.core.featureimpl.LexicalProbsFeature "
						+ "info.jonclark.treegraft.core.featureimpl.FragmentationFeature "
						+ "info.jonclark.treegraft.core.featureimpl.LengthFeature");
		props.setProperty("global.numThreads", "1");
		props.setProperty("global.progressBar.width", "180");
		props.setProperty("global.progressBar.isAnimated", "true");
		props.setProperty("input.encoding", "UTF8");
		props.setProperty("output.encoding", "UTF8");
		props.setProperty("lm.encoding", "UTF8");
		props.setProperty("grammar.encoding", "UTF8");
		props.setProperty("grammar.startSymbols", "S");
		props.setProperty("parser.doYieldRecombination", "true");
		props.setProperty("decoder.doHypothesisRecombination", "true");
		props.setProperty("transfer.latticeFile", "null");
		props.setProperty("grammar.grammarFile", "null");
		props.setProperty("grammar.lexiconFile", "null");

		props.setProperty("grammar.filterRulesWithLHS", "");
		props.setProperty("grammar.filterRulesWithNonterminalRHS", "");
		props.setProperty("grammar.filterRulesWithTerminalRHS", "");
		props.setProperty("grammar.oovHandler",
				"info.jonclark.treegraft.parsing.oov.CopyOOVHandler");
		props.setProperty("grammar.oovHandler.oovRuleSgtLogProb", "-10");
		props.setProperty("grammar.oovHandler.oovRuleTgsLogProb", "-10");
		props.setProperty("grammar.oovHandler.oovRuleLhsList", "OOV");
		props.setProperty("grammar.filterRulesWithTerminalRHS", "");

		HashMap<String, String> mapping = new HashMap<String, String>();

		// TODO: Map on/off to true/false

		// globals
		mapping.put("srclang", "global.sourceLanguage");
		mapping.put("tgtlang", "global.targetLanguage");
		mapping.put("threads", "global.numThreads");
		mapping.put("loadgra", "grammar.grammarFile");
		mapping.put("loadrules", "grammar.grammarFile");
		mapping.put("loadlex", "lexicon.lexiconFile");
		mapping.put("topnode", "grammar.startSymbols");
		mapping.put("nbest", "decoder.nBest");
		mapping.put("progressbar", "global.progressbar.isAnimated");

		// beams
		mapping.put("latticebeam", "transfer.beamSize");
		mapping.put("rangebeam", "transfer.beamSize"); // same as above
		mapping.put("beamsize", "transfer.beamSize"); // same as above
		mapping.put("decodebeam", "decoder.beamSize");
		mapping.put("decoderbeam", "decoder.beamSize"); // same as above

		// features
		mapping.put("uselm", "lm.modelFile");
		mapping.put("usesgt", "features.lex.tgsgiza");
		mapping.put("usetgs", "features.lex.sgtgiza");
		mapping.put("usesgtgiza", "features.lex.sgtgiza");
		mapping.put("usetgsgiza", "features.lex.tgsgiza");
		mapping.put("smtfile", "SPECIAL");
		mapping.put("smttrans", "SPECIAL");
		mapping.put("smttranslate", "SPECIAL");
		mapping.put("lengthratio", "features.length.ratio");

		// weights
		mapping.put("lmweight", "features.lm.weight");
		mapping.put("probweight", "features.lm.weight");
		mapping.put("ruleweight", "features.rules.tgs.weight");
		mapping.put("rulesgtweight", "features.rule.sgt.weight");
		mapping.put("ruletgsweight", "features.rule.tgs.weight");
		mapping.put("ruleweight", "features.rule.tgs.weight");
		mapping.put("fragweight", "features.frag.weight");
		mapping.put("lengthweight", "features.length.weight");
		mapping.put("lenweight", "features.length.weight");
		mapping.put("transweight", "features.lex.tgs.weight");
		mapping.put("sgtweight", "features.lex.sgt.weight");
		mapping.put("tgsweight", "features.lex.tgs.weight");

		mapping.put("scoreweight", "features.rules.weight"); // what does this
		// mean?

		// unsupported
		mapping.put("capitalize", "X");
		mapping.put("comment", "X");
		mapping.put("break", "X");
		mapping.put("reloadgra", "X");
		mapping.put("reloadrules", "X");
		mapping.put("savegra", "X");
		mapping.put("savelex", "X");
		mapping.put("loadcontext", "X");
		mapping.put("loadphrasefertility", "X");
		mapping.put("loadconstraints", "X");
		mapping.put("matchtypes", "X");
		mapping.put("matchtype", "X");
		mapping.put("arcscores", "X");
		mapping.put("docid", "X");
		mapping.put("sysid", "X");
		mapping.put("rule", "X");
		mapping.put("pg", "X");
		mapping.put("allparsegroups", "X");
		mapping.put("allrules", "X");
		mapping.put("alllexicon", "X");
		mapping.put("memtserver", "X");
		mapping.put("xferserver", "X");
		mapping.put("partialfilter", "X");
		mapping.put("allowedpartials", "X");
		mapping.put("openclass", "X");
		mapping.put("lmcasepolicy", "X");
		mapping.put("sortrules", "X");
		mapping.put("xferscoring", "X");
		mapping.put("skipsort", "X");
		mapping.put("alwaysmorph", "X");
		mapping.put("alwaysgenmorph", "X");
		mapping.put("checklexicon", "X");
		mapping.put("parsebyfs", "X");
		mapping.put("showtrace", "X");
		mapping.put("setlog", "X");
		mapping.put("uselimits", "X");
		mapping.put("findall", "X");
		mapping.put("clear", "X");
		mapping.put("clearall", "X");
		mapping.put("usesa", "X");
		mapping.put("usefs", "X");
		mapping.put("posfilter", "X");
		mapping.put("useposfilter", "X");
		mapping.put("emptyword", "X");
		mapping.put("lmrefresh", "X");
		mapping.put("transfile", "X");
		mapping.put("memtfile", "X");
		mapping.put("memtfull", "X");
		mapping.put("logdomain", "X");
		mapping.put("parallel", "X");
		mapping.put("smtfull", "X");
		mapping.put("smtlattice", "X");
		mapping.put("smtlatticetrans", "X");
		mapping.put("smttransfs", "X");
		mapping.put("smtcontext", "X"); // what does this mean?
		mapping.put("decode", "X");
		mapping.put("ptrans", "X");
		mapping.put("transn", "X");
		mapping.put("transnfile", "X");

		// interactive only
		mapping.put("help", "X");
		mapping.put("h", "X");
		mapping.put("lmscore", "X");
		mapping.put("lmprob", "X");
		mapping.put("dolengthpenalty", "X");
		mapping.put("lengthscore", "X");
		mapping.put("rmfiler", "X");
		mapping.put("listfilters", "X");
		mapping.put("addwatch", "X");
		mapping.put("rmwatch", "X");
		mapping.put("listwatches", "X");
		mapping.put("addfail", "X");
		mapping.put("rmfail", "X");
		mapping.put("listfails", "X");
		mapping.put("parse", "X");

		// debugging
		mapping.put("enginedebug", "X");
		mapping.put("lmdebug", "X");
		mapping.put("lexdebug", "X");
		mapping.put("attemptdebug", "X");
		mapping.put("decodedebug", "X");
		mapping.put("grammardebug", "X");
		mapping.put("parsedebug", "X");
		mapping.put("traversedebug", "X");
		mapping.put("transferdebug", "X");
		mapping.put("fsdebug", "X");
		mapping.put("minimaldebug", "X");
		mapping.put("filldebug", "X");
		mapping.put("stepdebug", "X");
		mapping.put("alldebug", "X");
		mapping.put("allon", "X");
		mapping.put("alloff", "X");
		mapping.put("quiet", "X");

		// To be supported in future version of treegraft
		mapping.put("unkserver", "X");
		mapping.put("morphserver", "X");
		mapping.put("anamorphserver", "X");
		mapping.put("genmorphserver", "X");
		mapping.put("usemorphtrans", "X");
		mapping.put("domorph", "X");
		mapping.put("doanamorph", "X");
		mapping.put("dogenmorph", "X");
		mapping.put("server", "X");
		mapping.put("normalizecase", "X");
		mapping.put("includesource", "X");
		mapping.put("includetrace", "X");
		mapping.put("lexlimit", "X");
		mapping.put("loadlimit", "X");
		mapping.put("lengthlimit", "X");
		mapping.put("parsepruning", "X"); // what does this do?
		mapping.put("xferpruning", "X"); // what does this do?
		mapping.put("lexpruning", "X"); // what does this do?
		mapping.put("randomscores", "X");
		mapping.put("timer", "X");
		mapping.put("lmscorefile", "X");
		mapping.put("lmprobfile", "X");
		mapping.put("sgmlout", "X");
		mapping.put("sgmlin", "X");
		mapping.put("transfilesgm", "X");
		mapping.put("lexbeam", "X");
		mapping.put("rulenormalized", "X");
		mapping.put("lexnormalized", "X");
		mapping.put("lmnormalized", "X");
		mapping.put("gzip", "X");
		mapping.put("filterrulesbyliterals", "X");
		mapping.put("latticedups", "X");
		mapping.put("latticescores", "X");
		mapping.put("lengthpenalty", "X");
		mapping.put("fragbias", "X");
		mapping.put("addfilter", "X");
		mapping.put("del", "X");

		// TODO: Special hooks for these
		mapping.put("loadonelinelex", "lexicon.inputFile");
		mapping.put("unkpolicy", "X");
		mapping.put("utf8", "on/off");
		mapping.put("latin1", "on/off");

		BufferedReader in = new BufferedReader(new FileReader(iniFile));
		int nLine = 0;
		String line;
		while ((line = in.readLine()) != null) {
			nLine++;

			if (line.startsWith(";") || line.trim().equals("") || line.startsWith("quit"))
				continue;

			String xferParam = StringUtils.substringBefore(line, " ");
			String treegraftParam = mapping.get(xferParam);

			// specialized handling
			if (xferParam.equals("smtfile") || xferParam.equals("smttrans")
					|| xferParam.equals("smttranslate")) {
				String[] args = StringUtils.tokenize(line);
				props.setProperty("input.file", args[1]);
				props.setProperty("output.file", args[2]);
			} else if (xferParam.equals("loadonelinelex")) {
				props.setProperty("grammar.lexiconFormat", "one-line");
				String filename = StringUtils.substringAfter(line, " ");
				props.setProperty("grammar.lexiconFile", filename);

			}

			// default handling
			if (treegraftParam == null) {
				throw new RuntimeException("Unrecognized parameter: " + xferParam + " at line "
						+ nLine);
			} else if (xferParam.equals("X")) {
				throw new RuntimeException("Unsupported xfer parameter: " + xferParam + " at line "
						+ nLine);
			} else {
				String xferArgs = StringUtils.substringAfter(line, " ").trim();
				if (xferArgs.equalsIgnoreCase("on") || xferArgs.equalsIgnoreCase("off")) {
					xferArgs = StringUtils.replaceFast(xferArgs, "on", "true");
					xferArgs = StringUtils.replaceFast(xferArgs, "off", "false");
				}
				props.setProperty(treegraftParam, xferArgs);
			}
		}
		in.close();

		return props;
	}
}
