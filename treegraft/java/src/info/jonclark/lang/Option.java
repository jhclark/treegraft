package info.jonclark.lang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An idea taken from the Berkeley parser.
 * 
 * @author jon
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Option {

	String name();

	String usage();

	boolean required() default true;

	String defaultValue() default "";

	boolean errorIfFileNotExists() default false;
	
	boolean errorIfFileExists() default false;
	
	/**
	 * For options with array data types, the delimiter that separates the entries of the array
	 * @return
	 */
	String arrayDelim() default " \t\n";
	
	String pairDelim() default ":";
}
