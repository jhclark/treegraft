package info.jonclark.treegraft.unification;

public interface FeatureStructure<T extends FeatureStructure<T>> {
	public T unify(T b, Constraint c);
}
