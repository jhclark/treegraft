package info.jonclark.treegraft.parsing.unification;

import info.jonclark.treegraft.parsing.unification.Feature.FeatureType;

import java.util.HashMap;
import java.util.Map.Entry;

public class PseudoFeatureStructure implements FeatureStructure<PseudoFeatureStructure> {

	private final HashMap<String, PseudoFeatureStructure> paths =
			new HashMap<String, PseudoFeatureStructure>();
	private final HashMap<String, Feature> values = new HashMap<String, Feature>();
	private final UnificationManager unificationMan;

	public PseudoFeatureStructure(UnificationManager unificationMan) {
		this.unificationMan = unificationMan;
	}

	/**
	 * Returns NULL if unification fails
	 */
	public PseudoFeatureStructure unify(PseudoFeatureStructure fs2, Constraint c) {

		PseudoFeatureStructure unified = new PseudoFeatureStructure(unificationMan);

		// TODO: Evaluate the union only once instead of doing it once in each
		// direction

		if (!unifyValues(this, fs2, c, unified.values))
			return null;

		if (!unifyValues(fs2, this, c, unified.values))
			return null;

		if (!unifyPaths(this, fs2, c, unified.paths))
			return null;

		if (!unifyPaths(fs2, this, c, unified.paths))
			return null;

		return unified;
	}

	private boolean unifyValues(PseudoFeatureStructure fs1, PseudoFeatureStructure fs2,
			Constraint c, HashMap<String, Feature> unifiedValues) {

		for (final Entry<String, Feature> e1 : fs1.values.entrySet()) {

			Feature f1 = e1.getValue();
			Feature f2 = fs2.values.get(e1.getKey());
			if (f2 != null) {

				// make sure the values are the same
				if (f1.getType() == FeatureType.VALUE && f2.getType() == FeatureType.VALUE) {
					if (f1.getValue() == f2.getValue()) {
						unifiedValues.put(e1.getKey(), f1);
					} else {
						unificationMan.addFailure(new UnificationFailure("Could not unify values: "
								+ f1.getValue() + " & " + f2.getValue(), c));
						return false;
					}
				} else {
					throw new RuntimeException("TODO: Implement functions.");
				}

				// TODO: Handle *OR* functions
				// actually go inside *OR* to check combinations!
			} else {
				// just add it
				unifiedValues.put(e1.getKey(), f1);
			}
		}

		return true;
	}

	private static boolean unifyPaths(PseudoFeatureStructure fs1, PseudoFeatureStructure fs2,
			Constraint c, HashMap<String, PseudoFeatureStructure> unifiedPaths) {

		for (final Entry<String, PseudoFeatureStructure> e1 : fs1.paths.entrySet()) {

			PseudoFeatureStructure path1 = e1.getValue();
			PseudoFeatureStructure path2 = fs2.paths.get(e1.getKey());
			if (path2 != null) {

				// make sure the values are the same
				PseudoFeatureStructure unifiedPath = path1.unify(path2, c);
				if (unifiedPath != null) {
					unifiedPaths.put(e1.getKey(), unifiedPath);
				} else {
					// failure point will already be logged in the unification
					// manager from recursive call
					return false;
				}

				// TODO: Handle *OR* functions
				// actually go inside *OR* to check combinations!
			} else {
				// just add it
				unifiedPaths.put(e1.getKey(), path1);
			}
		}

		return true;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder("(");
		for (final Entry<String, Feature> entry : values.entrySet()) {
			builder.append("(" + entry.getKey() + " " + entry.getValue().getValue() + ")");
		}
		builder.append(")");
		return builder.toString();
	}

	public static void main(String[] args) throws Exception {
		UnificationManager u = new UnificationManager();

		PseudoFeatureStructure s1 = new PseudoFeatureStructure(u);
		s1.values.put("number", new Feature(FeatureType.VALUE, "sg"));
		s1.values.put("person", new Feature(FeatureType.VALUE, "3"));

		PseudoFeatureStructure s2 = new PseudoFeatureStructure(u);
		s2.values.put("number", new Feature(FeatureType.VALUE, "sg"));
		s2.values.put("pred", new Feature(FeatureType.VALUE, "dog"));

		Constraint c = new Constraint("(do this)", null, -1);

		PseudoFeatureStructure s3 = s1.unify(s2, c);
		System.out.println(s3);
	}
}
