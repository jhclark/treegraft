package info.jonclark.treegraft;

import info.jonclark.lang.hash.WideHashMapTest;
import info.jonclark.treegraft.chartparser.ChartParserTest;
import info.jonclark.treegraft.core.IntegerTokenTest;
import info.jonclark.treegraft.core.lm.LanguageModelTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { IntegerTokenTest.class, WideHashMapTest.class, LanguageModelTest.class,
		ChartParserTest.class })
public class FullTest {
}
