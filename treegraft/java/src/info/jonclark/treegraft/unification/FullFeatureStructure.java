package info.jonclark.treegraft.unification;

import info.jonclark.treegraft.unification.Feature.FeatureType;

import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

public class FullFeatureStructure implements FeatureStructure<FullFeatureStructure> {

	private final HashMap<String, FullFeatureStructure> paths =
			new HashMap<String, FullFeatureStructure>();
	private final HashMap<String, Feature> values = new HashMap<String, Feature>();
	private final UnificationManager unificationMan;

	public FullFeatureStructure(UnificationManager unificationMan) {
		this.unificationMan = unificationMan;
	}

	/**
	 * Returns NULL if unification fails
	 */
	public FullFeatureStructure unify(FullFeatureStructure fs2, Constraint c) {

		FullFeatureStructure unified = new FullFeatureStructure(unificationMan);

		// TODO: Evaluate the union only once instead of doing it once in each
		// direction

		// first, see if unification is possible via simulation
		if (!unifyValues(this, fs2, c, unified.values, true))
			return null;
		if (!unifyValues(fs2, this, c, unified.values, true))
			return null;
		if (!unifyPaths(this, fs2, c, unified.paths, true))
			return null;
		if (!unifyPaths(fs2, this, c, unified.paths, true))
			return null;

		// unification must work, so now write changes
		if (!unifyValues(this, fs2, c, unified.values, false))
			assert false : "Unification failed unexpectedly.";
		if (!unifyValues(fs2, this, c, unified.values, false))
			assert false : "Unification failed unexpectedly.";
		if (!unifyPaths(this, fs2, c, unified.paths, false))
			assert false : "Unification failed unexpectedly.";
		if (!unifyPaths(fs2, this, c, unified.paths, false))
			assert false : "Unification failed unexpectedly.";

		return unified;
	}

	private boolean unifyValues(FullFeatureStructure fs1, FullFeatureStructure fs2, Constraint c,
			HashMap<String, Feature> unifiedValues, boolean simulate) {

		for (final Entry<String, Feature> e1 : fs1.values.entrySet()) {

			Feature f1 = e1.getValue();
			Feature f2 = fs2.values.get(e1.getKey());
			if (f2 != null) {

				// make sure the values are the same
				if (f1.getType() == FeatureType.VALUE && f2.getType() == FeatureType.VALUE) {
					if (f1.getValue() == f2.getValue()) {
						if (!simulate)
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
				if (!simulate)
					unifiedValues.put(e1.getKey(), f1);
			}
		}

		return true;
	}

	private static boolean unifyPaths(FullFeatureStructure fs1, FullFeatureStructure fs2,
			Constraint c, HashMap<String, FullFeatureStructure> unifiedPaths, boolean simulate) {

		for (final Entry<String, FullFeatureStructure> e1 : fs1.paths.entrySet()) {

			FullFeatureStructure path1 = e1.getValue();
			FullFeatureStructure path2 = fs2.paths.get(e1.getKey());
			if (path2 != null) {

				// make sure the values are the same
				FullFeatureStructure unifiedPath = path1.unify(path2, c);
				if (unifiedPath != null) {
					if (!simulate)
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
				if (!simulate)
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

		FullFeatureStructure s1 = new FullFeatureStructure(u);
		s1.values.put("number", new Feature(FeatureType.VALUE, "sg"));
		s1.values.put("person", new Feature(FeatureType.VALUE, "3"));

		FullFeatureStructure s2 = new FullFeatureStructure(u);
		s2.values.put("number", new Feature(FeatureType.VALUE, "sg"));
		s2.values.put("pred", new Feature(FeatureType.VALUE, "dog"));

		Constraint c = new Constraint("(do this)", new File("null"), -1);

		FullFeatureStructure s3 = s1.unify(s2, c);
		System.out.println(s3);
	}
}
