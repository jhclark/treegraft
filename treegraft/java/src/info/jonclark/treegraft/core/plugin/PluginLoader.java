package info.jonclark.treegraft.core.plugin;

import info.jonclark.properties.SmartProperties;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class PluginLoader {

	public static <X extends Token> Object reflect(String classname, TokenFactory<X> tokenFactory,
			SmartProperties props) throws ReflectionException {
		
		try {
			Class<?> clazz = Class.forName(classname);

			// everything should be parameterized by <R extends GrammarRule<T>,
			// T extends Token>
			// Class<?> iface;
			// for(Class<?> iface : clazz.getInterfaces()) {
			// if(iface.getCanonicalName(""))
			// }
			//			
			// TypeVariable<?>[] typeParameters = getTypeParameters();
			// T dummyToken = tokenFactory.makeToken("X", true);
			//typeParameters[0].getClass().isInstance(ruleFactory.makeDummyRule(
			// dummyToken));
			// typeParameters[1].getClass().isInstance(dummyToken);

			Constructor<?> constructor =
					clazz.getConstructor(SmartProperties.class, TokenFactory.class);
			Object newInstance = constructor.newInstance(props, tokenFactory);
			return newInstance;
		} catch (ClassNotFoundException e) {
			// bad class name
			throw new ReflectionException("Could not find class: " + classname, e);
		} catch (SecurityException e) {
			// other problem
			throw new ReflectionException("Error loading class: " + classname, e);
		} catch (NoSuchMethodException e) {
			// no constructor
			throw new ReflectionException("Class " + classname
					+ " must define constructor with parameters (SmartProperties, TokenFactory)", e);
		} catch (IllegalArgumentException e) {
			// bad argument to constructor
			throw new ReflectionException("Error loading class: " + classname, e);
		} catch (InstantiationException e) {
			// couldn't instantiate
			throw new ReflectionException("Error loading class: " + classname, e);
		} catch (IllegalAccessException e) {
			// bad access
			throw new ReflectionException("Error loading class: " + classname, e);
		} catch (InvocationTargetException e) {
			// something bad...
			throw new ReflectionException("Error loading class: " + classname, e);
		}
	}
}
