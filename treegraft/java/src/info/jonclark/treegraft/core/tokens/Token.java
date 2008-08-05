package info.jonclark.treegraft.core.tokens;

/**
 * Abstracts Terminal tokens away from Strings so that we can use more efficient
 * representations such as integers for storing them in memory and doing
 * comparisons while still retaining the ability to use String for ease of
 * debugging. All Token implementations should have protected constructors and
 * should have a corresponding TokenFactory that can be used to create them. The
 * implementor should also take care to implement the equals(Object) and hashCode()
 * methods and to do so efficiently as they will be called millions of times.
 * 
 * @author jon
 */
public interface Token {
	/**
	 * Gets the unique identifier of this token, which could be anything from a
	 * String to an integer.
	 * 
	 * @return
	 */
	public String getId();

	public boolean equals(Object token);

	public int hashCode();
	
	/**
	 * Distinguishes non-terminals from terminals in CFG rules.
	 * 
	 * @return
	 */
	public boolean isTerminal();
}
