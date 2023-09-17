package com.scraper;

public class ConfigField {
	private final String selectorType;
	private final String selectorValue;
	private final String name;
	private final String fallbackValue;
	private final String dataType;
	
	public ConfigField(String selectorType, String selectorValue, String name, String fallbackValue, String dataType) {
		this.selectorType = selectorType;
		this.selectorValue = selectorValue;
		this.name = name;
		this.fallbackValue = fallbackValue;
		this.dataType = dataType;
	}
	
	public String getSelectorType() {
		return this.selectorType;
	}
	
	public String getSelectorValue() {
		return this.selectorValue;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getFallbackValue() {
		return this.fallbackValue;
	}
	
	public String getDataType() {
		return this.dataType;
	}
}
