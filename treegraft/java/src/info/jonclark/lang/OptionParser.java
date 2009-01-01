package info.jonclark.lang;

import info.jonclark.log.LogUtils;
import info.jonclark.properties.PropertiesException;
import info.jonclark.properties.PropertyUtils;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 * A nice little Option parsing class lifted from the Berkeley parser and
 * modified to support automatic discovery of configuration for plug-ins, which
 * are also discovered at runtime.
 * 
 * @author jon
 */
public class OptionParser {

	private Map<String, Option> nameToOption = new HashMap<String, Option>();
	private Map<String, Field> nameToField = new HashMap<String, Field>();
	private Set<String> requiredOptions = new HashSet<String>();
	private StringBuilder passedInOptions;

	private final Vector<Class<? extends Options>> optionsClasses;
	private final boolean failOnUnrecognized;
	private final Properties props;

	private static final Logger log = LogUtils.getLogger();

	public OptionParser(Vector<Class<? extends Options>> optionsClasses, String[] args,
			Properties props, boolean failOnUnrecognized) throws PropertiesException {

		this.failOnUnrecognized = failOnUnrecognized;
		this.props = props;
		PropertyUtils.parseCommandLineArgs(args, props);
		this.optionsClasses = optionsClasses;

		for (Class<?> optionsClass : optionsClasses) {
			getOptionAnnotations(optionsClass);
		}
	}

	private void clear() {
		nameToOption = new HashMap<String, Option>();
		nameToField = new HashMap<String, Field>();
		requiredOptions = new HashSet<String>();
	}

	private void getOptionAnnotations(Class<?> optionsClass) {

		for (Field field : optionsClass.getDeclaredFields()) {
			Option option = field.getAnnotation(Option.class);
			if (option == null) {
				continue;
			}
			nameToOption.put(option.name(), option);
			nameToField.put(option.name(), field);
			if (option.required()) {
				requiredOptions.add(option.name());
			}
		}
	}

	public String getGlobalUsage() {

		StringBuilder usage = new StringBuilder();

		for (Class<?> optionsClass : optionsClasses) {

			usage.append("=== " + optionsClass.getSimpleName() + " ===\n\n");
			clear();
			getOptionAnnotations(optionsClass);
			for (Option opt : nameToOption.values()) {
				usage.append(String.format("%-30s%s", opt.name(), opt.usage()));
				if (!opt.required()) {
					usage.append(" [optional]");
				}
				usage.append("\n");
			}
			usage.append("\n");
		}
		usage.append(String.format("%-30shelp message\n", "--help"));

		return usage.toString();
	}

	/**
	 * Ensures that all required options are present and that they contain
	 * reasonable values (it is useful to check this before, say spending 20
	 * minutes loading a large language model). If an option is a file and
	 * specifies that we should check if the file exists, we can do that too.
	 * 
	 * @param optionsClasses
	 */
	public void validateConfiguration() {
		for (Class<? extends Options> optionsClass : optionsClasses) {
			getOptionAnnotations(optionsClass);
		}
		validate();
	}

	public <X extends Options> X getOptions(Class<X> optionsClass) {
		clear();
		getOptionAnnotations(optionsClass);
		return getOptions(optionsClass, true);
	}

	private <X extends Options> X getOptions(Class<X> optionsClass, boolean dummy) {

		try {
			Set<String> seenOpts = new HashSet<String>();
			passedInOptions = new StringBuilder("{");
			X options = optionsClass.newInstance();

			// first, read all default values
			for (String key : nameToOption.keySet()) {
				Option opt = nameToOption.get(key);
				String value = opt.defaultValue();
				if (value != null && !value.equals("")) {
					try {
						setValue(options, key, value, opt);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException("Class not found while parsing option: " + key
								+ " = " + value, e);
					}
				}
			}

			for (Entry<Object, Object> entry : props.entrySet()) {

				String key = (String) entry.getKey();
				String value = (String) entry.getValue();

				Option opt = nameToOption.get(key);
				if (opt == null) {
					// if (failOnUnrecognized) {
					// throw new
					// RuntimeException("Did not recognize option " +
					// key);
					// } else {
					// System.err.println("WARNING: Did not recognize option "
					// +
					// key);
					// }
					continue;
				}
				seenOpts.add(key);

				// // If option is boolean type then
				// // we set the associate field to true
				// if (fieldType == boolean.class) {
				// field.setBoolean(options, true);
				// // passedInOptions.append(String.format(" %s => true",
				// // opt.name()));
				// }
				// Otherwise look at next arg and
				// set field to that value
				// this will automatically
				// convert String to double or
				// whatever
				// else {
				try {
					setValue(options, key, value, opt);
				} catch (ClassNotFoundException e) {
					throw new RuntimeException("Class not found while parsing option: " + key
							+ " = " + value, e);
				}
			}
			// }

			// passedInOptions.append(" }");

			Set<String> optionsLeft = new HashSet<String>(requiredOptions);
			optionsLeft.removeAll(seenOpts);

			if (!optionsLeft.isEmpty()) {
				System.err.println(getGlobalUsage());
				System.err.println("Failed to specify required options: " + optionsLeft);
				System.exit(1);
			}

			return options;
		} catch (IllegalArgumentException e) {
			throw new Error(e);
		} catch (IllegalAccessException e) {
			throw new Error(e);
		} catch (InstantiationException e) {
			throw new Error(e);
		} catch (SecurityException e) {
			throw new Error(e);
		} catch (NoSuchMethodException e) {
			throw new Error(e);
		} catch (InvocationTargetException e) {
			throw new Error(e);
		}
	}

	private Class[] getGenericParams(ParameterizedType generic) {

		Type[] typeArguments = generic.getActualTypeArguments();
		Class[] params = new Class[typeArguments.length];
		for (int i = 0; i < params.length; i++) {
			Type t = typeArguments[i];
			if (t instanceof Class) {
				params[i] = (Class) t;
			} else if (t instanceof ParameterizedType) {
				params[i] = (Class) ((ParameterizedType) t).getRawType();
			} else {
				throw new RuntimeException("Unexpected generic parameter type: "
						+ t.getClass().getName());
			}
		}
		return params;
	}

	private Object create(Class<?> fieldType, Type genType, String key, String value, Option opt)
			throws IllegalArgumentException, SecurityException, InstantiationException,
			IllegalAccessException, InvocationTargetException, NoSuchMethodException,
			ClassNotFoundException {

		Object obj = null;
		if (fieldType == File.class) {
			
			File f = new File(value);
			if (opt.errorIfFileExists() && f.exists()) {
				throw new RuntimeException("File for " + key + " already exists: "
						+ f.getAbsolutePath());
			}
			if (opt.errorIfFileNotExists() && !f.exists()) {
				throw new RuntimeException("File for " + key + " does not exist: "
						+ f.getAbsolutePath());
			}
			obj = f;
			
		} else if (fieldType == Class.class) {

			obj = Class.forName(value);

		} else if (fieldType == Pair.class) {

			ParameterizedType genericType = (ParameterizedType) genType;
			Class[] genericParams = getGenericParams(genericType);
			assert genericParams.length == 2;
			Class<?> param1 = genericParams[0];
			Class<?> param2 = genericParams[1];

			String[] values = StringUtils.tokenize(value, opt.pairDelim());
			if (values.length != 2) {
				throw new RuntimeException(key + " did not contain 2 values delimited by \""
						+ opt.pairDelim() + "\" for " + key + ": " + value);
			}
			String value1 = values[0];
			String value2 = values[1];

			// create a pair for the required class types
			Pair pair = new Pair();
			pair.first = create(param1, param1, key, value1, opt);
			pair.second = create(param2, param2, key, value2, opt);
			obj = pair;

		} else {
			try {
				Constructor<?> constructor = fieldType.getConstructor(String.class);
				obj = constructor.newInstance(value);
			} catch (NoSuchMethodException e) {
				throw new Error("Cannot construct object of type " + fieldType.getCanonicalName()
						+ " from just a string", e);
			} catch (InstantiationException e) {
				throw new Error(e);
			} catch (InvocationTargetException e) {
				throw new Error(e);
			}
		}

		return obj;
	}

	private <X> void setValue(X options, String key, String value, Option opt)
			throws IllegalAccessException, NoSuchMethodException, InstantiationException,
			InvocationTargetException, Error, IllegalArgumentException, SecurityException,
			ClassNotFoundException {

		Field field = nameToField.get(key);
		Class<?> fieldType = field.getType();

		if (value != null)
			value.trim();
		// passedInOptions.append(String.format(" %s => %s",
		// opt.name(), value));
		if (fieldType == int.class) {
			field.setInt(options, Integer.parseInt(value));
		} else if (fieldType == double.class) {
			field.setDouble(options, Double.parseDouble(value));
		} else if (fieldType == float.class) {
			field.setFloat(options, Float.parseFloat(value));
		} else if (fieldType == short.class) {
			field.setFloat(options, Short.parseShort(value));
		} else if (fieldType == boolean.class) {
			field.setBoolean(options, Boolean.parseBoolean(value));
		} else if (fieldType.isEnum()) {
			Object[] possibleValues = fieldType.getEnumConstants();
			boolean found = false;
			for (Object possibleValue : possibleValues) {

				String enumName = ((Enum<?>) possibleValue).name();
				if (value.equals(enumName)) {
					field.set(options, possibleValue);
					found = true;
					break;
				}

			}
			if (!found) {
				// if (failOnUnrecognized) {
				// throw new
				// RuntimeException("Unrecognized enumeration option "
				// + value);
				// } else {
				// System.err.println("WARNING: Did not recognize option Enumeration option "
				// + value);
				// }
			}
		} else if (fieldType == String.class) {
			field.set(options, value);
		} else if (fieldType.isArray()) {

			String[] toks = StringUtils.tokenize(value, opt.arrayDelim());

			Type genericComponentType;
			if (field.getGenericType() instanceof GenericArrayType) {
				genericComponentType =
						((GenericArrayType) field.getGenericType()).getGenericComponentType();
			} else {
				genericComponentType = fieldType.getComponentType();
			}

			Object arr = Array.newInstance(fieldType.getComponentType(), toks.length);
			for (int i = 0; i < toks.length; i++) {
				Object arrayElement =
						create(fieldType.getComponentType(), genericComponentType, key, toks[i],
								opt);
				Array.set(arr, i, arrayElement);
			}
			field.set(options, arr);
		} else {
			Object obj = create(fieldType, field.getGenericType(), key, value, opt);
			field.set(options, obj);
		}

	}

	private void validate() {

		Set<String> seenOpts = new HashSet<String>();
		passedInOptions = new StringBuilder("{");

		for (Entry<Object, Object> entry : props.entrySet()) {

			String key = (String) entry.getKey();
			String value = (String) entry.getValue();

			Option opt = nameToOption.get(key);
			if (opt == null) {
				if (failOnUnrecognized) {
					throw new RuntimeException("Did not recognize option " + key);
				} else {
					log.warning("Did not recognize option " + key);
				}
				continue;
			}
			seenOpts.add(key);
			// passedInOptions.append(" }");
		}

		Set<String> optionsLeft = new HashSet<String>(requiredOptions);
		optionsLeft.removeAll(seenOpts);
		if (!optionsLeft.isEmpty()) {
			System.err.println(getGlobalUsage());
			System.err.println("Failed to specify required options: " + optionsLeft);
			System.exit(1);
		}
	}

	public String getPassedInOptions() {
		return passedInOptions.toString();
	}

}
