package info.jonclark.treegraft.core.tokens;

/**
 * Abstracts Terminal tokens away from Strings so that we can use more efficient
 * representations such as integers for storing them in memory and doing
 * comparisons while still retaining the ability to use <code>Strings</code> for
 * ease of debugging. All <code>Token</code> implementations should have
 * protected constructors and should have a corresponding
 * <code>TokenFactory</code> that can be used to create them. The implementor
 * should also take care to implement the <code>equals(Object)</code> and
 * <code>hashCode()</code> methods and to do so efficiently as they will be
 * called millions of times.
 * 
 * @author Jonathan Clark
 */
public interface Token {
	
	/**
	 * Gets the unique identifier of this token, which could be anything from a
	 * String to an integer. This identifier is typically ONLY USED IN DEBUGGING
	 * and comparisons should be perfomed with the == operator or using hashing
	 * operations for sake of speed.
	 * 
	 * @return a unique identifier as a string
	 */
	public String getId();

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object token);

	/**
	 * {@inheritDoc}
	 */
	public int hashCode();

	/**
	 * Distinguishes non-terminals from terminals in CFG rules.
	 * 
	 * @return True if this is a terminal; false otherwise
	 */
	public boolean isTerminal();
}
