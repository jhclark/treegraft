package info.jonclark.treegraft.parsing.unification;

public class UnificationFailure {
	private String message;
	private Constraint c;
	
	public UnificationFailure(String message, Constraint c) {
		this.message = message;
		this.c = c;
	}

	public String getMessage() {
		return message;
	}

	public Constraint getC() {
		return c;
	}
}
