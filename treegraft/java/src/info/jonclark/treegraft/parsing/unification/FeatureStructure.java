package info.jonclark.treegraft.parsing.unification;

public interface FeatureStructure<T extends FeatureStructure<T>> {
	public T unify(T b, Constraint c);
}
