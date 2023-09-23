package com.Sweep;

import java.util.List;

import com.scraper.ConfigField;

public class Sweep {
	private String name;
	private String args;
	private List<ConfigField> fields;
	
	public Sweep(String name, String args, List<ConfigField> fields) {
		this.name = name;
		this.args = args;
		this.fields = fields;
	}
	
	public String getName() {
		return this.name;
	}
	public String[] getArgs() {
		return this.args.split(" ");
	}
	public List<ConfigField> getFields() {
		return this.fields;
	}
}
