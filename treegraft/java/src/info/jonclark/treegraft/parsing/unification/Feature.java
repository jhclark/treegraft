package info.jonclark.treegraft.parsing.unification;

import java.util.ArrayList;

public class Feature {
	public enum FeatureType {
		VALUE, FUNCTION
	};
	
	private FeatureType type;

	private String value;

	private String functionName;
	private ArrayList<String> valueList;
	
	public Feature(FeatureType type, String value) {
		this.type = type;
		this.value = value;
	}

	void setType(FeatureType type) {
		this.type = type;
	}

	FeatureType getType() {
		return type;
	}

	void setValue(String value) {
		this.value = value;
	}

	String getValue() {
		return value;
	}

	void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	String getFunctionName() {
		return functionName;
	}

	ArrayList<String> getValueList() {
		return valueList;
	}
}
