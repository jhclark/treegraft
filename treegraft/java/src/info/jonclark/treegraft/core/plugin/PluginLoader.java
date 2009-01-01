package info.jonclark.treegraft.core.plugin;

import info.jonclark.lang.OptionParser;
import info.jonclark.lang.Options;
import info.jonclark.lang.OptionsTarget;
import info.jonclark.properties.SmartProperties;
import info.jonclark.treegraft.Treegraft.TreegraftConfig;
import info.jonclark.treegraft.core.tokens.Token;
import info.jonclark.treegraft.core.tokens.TokenFactory;
import info.jonclark.treegraft.parsing.rules.GrammarRule;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class PluginLoader {

	public static <R extends GrammarRule<T>, T extends Token> void validatePlugins(
			Class[] pluginClasses, OptionParser configurator) throws InvocationTargetException {

		for (Class clazz : pluginClasses) {
			loadPlugin(clazz, configurator, null, true);
		}
	}

	public static <X, R extends GrammarRule<T>, T extends Token> X loadPlugin(Class<X> classToLoad,
			OptionParser configurator, TreegraftConfig<R, T> config, boolean simulate)
			throws InvocationTargetException {

		String strFeatureOptionsName = "?";
		try {

			// we have to load the configuration options before we can load the
			// class itself

			// figure out what class this feature expects as its Options
			// argument
			OptionsTarget optionsTarget = classToLoad.getAnnotation(OptionsTarget.class);
			if (optionsTarget == null) {
				throw new RuntimeException("Feature " + classToLoad.getName()
						+ " must define the @OptionsTarget annotation");
			}
			Class<? extends Options> pluginOptionsClass = optionsTarget.value();
			strFeatureOptionsName = pluginOptionsClass.getSimpleName();

			// load options from the user's config file and the command line
			// to populate an instance of the specified Options class
			Options opts = configurator.getOptions(pluginOptionsClass);

			// find the constructor that we require (Options, TreegraftConfig)
			// and instantiate the feature
			Constructor<X> constructor =
					classToLoad.getConstructor(pluginOptionsClass, TreegraftConfig.class);

			if (simulate) {
				return null;
			} else {
				X plugin = constructor.newInstance(opts, config);
				return plugin;
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			throw new Error(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(classToLoad.getName()
					+ " must define a constructor that takes arguments (" + strFeatureOptionsName
					+ ", TreegraftConfig)");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			throw new Error(e);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			throw new Error(e);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			throw new Error(e);
		} catch (InvocationTargetException e) {
			throw e;
		}
	}

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
			// typeParameters[0].getClass().isInstance(ruleFactory.makeDummyRule(
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
