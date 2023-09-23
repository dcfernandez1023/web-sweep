package com.io;

import com.Sweep.Sweep;
import com.crawler.WebCrawler.CrawlResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.scraper.ConfigField;
import com.scraper.ScrapeResult;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JsonIO {
	public static List<ConfigField> readConfig(String path) throws Exception {
        Gson gson = new Gson();
        Type type = new TypeToken<HashMap<String, List<HashMap<String, String>>>>(){}.getType();
        FileReader fileReader = new FileReader(path);
        HashMap<String, List<HashMap<String, String>>> map = gson.fromJson(fileReader, type);
        fileReader.close();
        if (!map.containsKey("fields")) throw new Exception("Invalid JSON config for scraper.");
        
        List<ConfigField> config = new ArrayList<>();
        List<HashMap<String, String>> configFields = map.get("fields");
        for (HashMap<String, String> field : configFields) {
        	String selectorType = field.getOrDefault("selectorType", null);
        	String selectorValue = field.getOrDefault("selectorValue", null);
        	String name = field.getOrDefault("name", null);
        	String fallbackValue = field.getOrDefault("fallbackValue", null);
        	String dataType = field.getOrDefault("dataType", "text");
        	
        	if (selectorType == null || selectorValue == null || name == null || fallbackValue == null)
        		throw new Exception("Invalid JSON config for scraper.");
        	
        	config.add(new ConfigField(selectorType, selectorValue, name, fallbackValue, dataType));
        }
        return config;
	}
	
	public static Sweep readSweepConfig(String path) throws Exception {
        Gson gson = new Gson();
        Type type = new TypeToken<Sweep>(){}.getType();
        FileReader fileReader = new FileReader(path);
        Sweep sweep = gson.fromJson(fileReader, type);
        fileReader.close();
        return sweep;
	}
	
	public static void crawlResultsToJson(List<CrawlResult> crawlResults, String path) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(crawlResults);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(json);
        }
	}

	public static void scrapeResultsToJson(List<ScrapeResult> scrapeResults, String path) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(scrapeResults);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(json);
        }
	}
	
	public static List<CrawlResult> readScraperInput(String path) throws IOException {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<CrawlResult>>(){}.getType();
        FileReader fileReader = new FileReader(path);
        List<CrawlResult> myList = gson.fromJson(fileReader, listType);
        fileReader.close();
        return myList;
	}
}
