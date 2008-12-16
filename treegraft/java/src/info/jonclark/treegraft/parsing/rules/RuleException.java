package info.jonclark.treegraft.parsing.rules;

public class RuleException extends Exception {

	private static final long serialVersionUID = -5492166788629557072L;

	public RuleException(String str) {
		super(str);
	}

	public RuleException(String str, Throwable cause) {
		super(str, cause);
	}
}
