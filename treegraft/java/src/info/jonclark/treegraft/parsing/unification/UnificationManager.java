package info.jonclark.treegraft.parsing.unification;

import java.util.ArrayList;

public class UnificationManager {
	private final ArrayList<UnificationFailure> failures = new ArrayList<UnificationFailure>();
	
	public void addFailure(UnificationFailure failure) {
		failures.add(failure);
	}
}
