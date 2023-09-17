package com.scraper;

import java.util.HashMap;

public class ScrapeResult {
	private final String url;
	private final HashMap<String, String> data;
	private final String timestamp;
	
	public ScrapeResult(String url) {
		this.url = url;
		this.data = new HashMap<>();
		this.timestamp = Long.toString(System.currentTimeMillis());
	}
	
	public String getUrl() {
		return this.url;
	}
	
	public HashMap<String, String> getData() {
		return this.data;
	}
	
	public String getTimestamp() {
		return this.timestamp;
	}
	
	public void addFeature(String name, String value) {
		this.data.put(name, value);
	}
}
