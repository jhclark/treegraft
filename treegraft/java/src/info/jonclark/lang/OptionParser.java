package info.jonclark.lang;

import info.jonclark.properties.PropertiesException;
import info.jonclark.properties.PropertyUtils;
import info.jonclark.util.StringUtils;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

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

			for (Entry<Object, Object> entry : props.entrySet()) {

				String key = (String) entry.getKey();
				String value = (String) entry.getValue();

				Option opt = nameToOption.get(key);
				if (opt == null) {
					// if (failOnUnrecognized) {
					// throw new RuntimeException("Did not recognize option " +
					// key);
					// } else {
					// System.err.println("WARNING: Did not recognize option " +
					// key);
					// }
					continue;
				}
				seenOpts.add(key);
				Field field = nameToField.get(key);
				Class<?> fieldType = field.getType();
				// If option is boolean type then
				// we set the associate field to true
				if (fieldType == boolean.class) {
					field.setBoolean(options, true);
					// passedInOptions.append(String.format(" %s => true",
					// opt.name()));
				}
				// Otherwise look at next arg and
				// set field to that value
				// this will automatically
				// convert String to double or
				// whatever
				else {
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

						String[] toks = StringUtils.tokenize(value, opt.delim());
						// System.out.println(key + " :: " +
						// StringUtils.untokenize(toks, "#"));

						// get a constructors for this class type
						Constructor<?> constructor =
								fieldType.getComponentType().getConstructor(String.class);

						Object arr = Array.newInstance(fieldType.getComponentType(), toks.length);
						for (int i = 0; i < toks.length; i++) {
							Object arrayElement = constructor.newInstance(toks[i]);
							Array.set(arr, i, arrayElement);
						}
						field.set(options, arr);

					} else if (fieldType == File.class) {

						// TODO: Check for existence of arrays of files
						File f = new File(value);
						if (opt.errorIfFileExists() && f.exists()) {
							throw new RuntimeException("File for " + key + " already exists: "
									+ f.getAbsolutePath());
						}
						if (opt.errorIfFileNotExists() && !f.exists()) {
							throw new RuntimeException("File for " + key + " does not exist: "
									+ f.getAbsolutePath());
						}
						field.set(options, f);

					} else {
						try {
							Constructor<?> constructor = fieldType.getConstructor(String.class);
							field.set(options, constructor.newInstance(value));
						} catch (NoSuchMethodException e) {
							throw new Error("Cannot construct object of type "
									+ fieldType.getCanonicalName() + " from just a string", e);
						} catch (InstantiationException e) {
							throw new Error(e);
						} catch (InvocationTargetException e) {
							throw new Error(e);
						}
					}
				}
			}

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
					System.err.println("WARNING: Did not recognize option " + key);
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
