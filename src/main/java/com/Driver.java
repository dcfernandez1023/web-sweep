package com;

import java.util.ArrayList;
import java.util.List;

import com.args.ArgumentParser;
import com.crawler.WebCrawler;
import com.crawler.WebCrawler.CrawlResult;
import com.json.JsonIO;
import com.scraper.ConfigField;
import com.scraper.ProductScraper;
import com.scraper.ScrapeResult;
import com.threading.WorkQueue;

public class Driver {
	public static final String SEED_FLAG = "-seed";
	public static final String URL_BASE_FLAG = "-base";
	public static final String MATCH_FLAG = "-match";
	public static final String MAX_URLS_FLAG = "-maxUrls";
	public static final String MAX_RESULTS_FLAG = "-maxResults";
	public static final String MAX_REDIRECT_FLAG = "-maxRedirect";
	public static final String THREADS_FLAG = "-threads";
	public static final String OUTPUT_FILE = "-crawlOutput";
	public static final String SCRAPER_CONFIG = "-scraperConfig";
	public static final String SCRAPER_INPUT_FILE = "-scraperInput";
	public static final String SCRAPE_OUTPUT_FILE = "-scraperOutput";
	public static final String THREADED_SCRAPER = "-threadedScraper";
	
	public static void main(String[] args) {
		try {
			// Parse arguments 
			ArgumentParser argumentParser = new ArgumentParser(args);
			String seed = argumentParser.getString(SEED_FLAG, null);
			String base = argumentParser.getString(URL_BASE_FLAG, "http");
			String routeRegex = argumentParser.getString(MATCH_FLAG, null);
			String outputPath = argumentParser.getString(OUTPUT_FILE, null);
			String scraperConfig = argumentParser.getString(SCRAPER_CONFIG, null);
			String scrapeInputPath = argumentParser.getString(SCRAPER_INPUT_FILE, null);
			String scrapeOutputPath = argumentParser.getString(SCRAPE_OUTPUT_FILE, null);
			boolean scraperOnly = argumentParser.hasFlag(SCRAPER_INPUT_FILE);
			boolean threadedScraper = argumentParser.hasFlag(THREADED_SCRAPER);
			int maxUrls = argumentParser.getInteger(MAX_URLS_FLAG, WebCrawler.MAX_URLS_DEFAULT);
			int threads = argumentParser.getInteger(THREADS_FLAG, WorkQueue.DEFAULT);
			
			if (seed == null || routeRegex == null) {
				throw new Exception("Must provide a value for -seed and -match flags");
			}
			// TODO: add more argument validation 
			System.out.println("Arguments: " + argumentParser.toString());
			
			List<CrawlResult> crawlResults = null;
			WorkQueue queue = new WorkQueue(threads);
			
			if (!scraperOnly) {
				WebCrawler crawler = new WebCrawler();
				
				System.out.println("Initialized work queue and web crawler");
				System.out.println("Running web crawler...");
				
				crawlResults = crawler.crawl(seed, base, maxUrls, routeRegex, queue);
				queue.finish();
				queue.shutdown();
				
				System.out.println("Crawl finished. Total results: " + crawlResults.size());
				
				if (outputPath != null) {
					JsonIO.crawlResultsToJson(crawlResults, outputPath);
					System.out.println("Wrote JSON output to " + outputPath);
				}
			} else {
				System.out.println("Not runing web crawler. Reading scraper input file...");
				crawlResults = JsonIO.readScraperInput(scrapeInputPath);
				System.out.println("Read scraper input file");
			}
			
			if (scraperConfig != null) {
				List<ConfigField> config = JsonIO.readConfig(scraperConfig);
				System.out.println("Read scraper JSON config");
				ProductScraper scraper = new ProductScraper();
				List<ScrapeResult> scrapeResults = new ArrayList<>();
				System.out.println("Running scraper... Threaded=" + threadedScraper);
				if (threadedScraper) {
					WorkQueue scraperQueue = new WorkQueue(5);
					scraper.scrape(crawlResults, config, scrapeResults, scraperQueue);
					scraperQueue.finish();
					scraperQueue.shutdown();
				} else {
					scraper.scrape(crawlResults, config, scrapeResults);
				}
				
				System.out.println("Scrape finished. Total results: " + scrapeResults.size());
				if (scrapeOutputPath != null) {
					JsonIO.scrapeResultsToJson(scrapeResults, scrapeOutputPath);
					System.out.println("Wrote JSON output to " + scrapeOutputPath);
				}
			}
			
		} catch (Exception e) {
			System.out.println("Failed: " + e.getMessage());
		}
	}
}
