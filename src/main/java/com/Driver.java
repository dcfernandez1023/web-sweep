package com;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.Sweep.Sweep;
import com.args.ArgumentParser;
import com.crawler.WebCrawler;
import com.crawler.WebCrawler.CrawlResult;
import com.io.JsonIO;
import com.io.SweepRunIO;
import com.scraper.ConfigField;
import com.scraper.ProductScraper;
import com.scraper.ScrapeResult;
import com.threading.WorkQueue;

public class Driver {
	public static final String SEED_FLAG = "-seed";
	public static final String URL_BASE_FLAG = "-base";
	public static final String MATCH_FLAG = "-match";
	public static final String SCRAPER_CONFIG = "-scraperConfig";
	public static final String SWEEP_CONFIG = "-sweepConfig";
	public static final String MAX_URLS_FLAG = "-maxUrls";
	public static final String MAX_RESULTS_FLAG = "-maxResults";
	public static final String MAX_REDIRECT_FLAG = "-maxRedirect";
	public static final String THREADS_FLAG = "-threads";
	public static final String THREADED_SCRAPER = "-threadedScraper";
	public static final String SWEEP_RUN_DIR = "-sweepRunDir";
	public static final String SWEEP_RESULTS_DIR = "-sweepResultsDir";
	
	public static void main(String[] args) {
		String sweepRunId = UUID.randomUUID().toString() + "_run";
		String sweepResultsId = UUID.randomUUID().toString() + "_results";
		
		String sweepRunPath = null;
		String sweepResultsPath = null;
		long startTime = System.currentTimeMillis();
		try {
			// Parse arguments
			ArgumentParser argumentParser = new ArgumentParser(args);
			String sweepConfig = argumentParser.getString(SWEEP_CONFIG, null);
			String seed;
			String base;
			String routeRegex;
			String scraperConfig;
			String sweepRunDir;
			String sweepResultsDir;
			boolean threadedScraper;
			int maxUrls;
			int threads;
			
			Sweep sweep = null;
			
			// If sweep config provided, then read arguments and scraper fields from there
			if (sweepConfig != null) {
				System.out.println("Sweep config provided. Reading args from sweep file");
				sweep = JsonIO.readSweepConfig(sweepConfig);
				System.out.println("Sweep name: " + sweep.getName());
				ArgumentParser configArgumentParser = new ArgumentParser(sweep.getArgs());
				seed = configArgumentParser.getString(SEED_FLAG, null);
				base = configArgumentParser.getString(URL_BASE_FLAG, "http");
				routeRegex = configArgumentParser.getString(MATCH_FLAG, null);
				scraperConfig = configArgumentParser.getString(SCRAPER_CONFIG, null);
				sweepRunDir = configArgumentParser.getString(SWEEP_RUN_DIR, null);
				sweepResultsDir = configArgumentParser.getString(SWEEP_RESULTS_DIR, null);
				threadedScraper = configArgumentParser.hasFlag(THREADED_SCRAPER);
				maxUrls = configArgumentParser.getInteger(MAX_URLS_FLAG, WebCrawler.MAX_URLS_DEFAULT);
				threads = configArgumentParser.getInteger(THREADS_FLAG, WorkQueue.DEFAULT);	
			} else {
				seed = argumentParser.getString(SEED_FLAG, null);
				base = argumentParser.getString(URL_BASE_FLAG, "http");
				routeRegex = argumentParser.getString(MATCH_FLAG, null);
				scraperConfig = argumentParser.getString(SCRAPER_CONFIG, null);
				sweepRunDir = argumentParser.getString(SWEEP_RUN_DIR, null);
				sweepResultsDir = argumentParser.getString(SWEEP_RESULTS_DIR, null);
				threadedScraper = argumentParser.hasFlag(THREADED_SCRAPER);
				maxUrls = argumentParser.getInteger(MAX_URLS_FLAG, WebCrawler.MAX_URLS_DEFAULT);
				threads = argumentParser.getInteger(THREADS_FLAG, WorkQueue.DEFAULT);	
			}

			if (seed == null || base == null || routeRegex == null) {
				throw new Exception("Must provide a value for -seed, -base, and -match flags");
			}
			
			System.out.println("Arguments: " + argumentParser.toString());
			
			if (sweepRunDir != null) {
				sweepRunPath = sweepRunDir + "/" + sweepRunId + ".txt";
				SweepRunIO.initSweepRun(sweepRunPath);
			}

			List<CrawlResult> crawlResults = null;
			WorkQueue queue = new WorkQueue(threads);
			
			WebCrawler crawler = new WebCrawler();
			
			System.out.println("Initialized work queue and web crawler");
			System.out.println("Running web crawler...");
			
			crawlResults = crawler.crawl(seed, base, maxUrls, routeRegex, queue);
			queue.finish();
			queue.shutdown();
			
			System.out.println("Crawl finished. Total results: " + crawlResults.size());
			
			if (scraperConfig != null || sweep != null) {
				List<ConfigField> fields = sweep != null ? sweep.getFields() : JsonIO.readConfig(scraperConfig);
				ProductScraper scraper = new ProductScraper();
				List<ScrapeResult> scrapeResults = new ArrayList<>();
				System.out.println("Running scraper... Threaded=" + threadedScraper);
				if (threadedScraper) {
					WorkQueue scraperQueue = new WorkQueue(5);
					scraper.scrape(crawlResults, fields, scrapeResults, scraperQueue);
					scraperQueue.finish();
					scraperQueue.shutdown();
				} else {
					scraper.scrape(crawlResults, fields, scrapeResults);
				}
				
				System.out.println("Scrape finished. Total results: " + scrapeResults.size());
				
				if (sweepResultsDir != null) {
					sweepResultsPath = sweepResultsDir + "/" + sweepResultsId + ".json";
					System.out.println("Writing sweep results to " + sweepResultsPath);
					JsonIO.scrapeResultsToJson(scrapeResults, sweepResultsPath);
				}
			}
			
			if (sweepRunPath != null && sweepResultsId != null && sweepResultsPath != null) {
				System.out.println("Completing sweep. Writing run output to " + sweepRunPath);
				SweepRunIO.completeSweepRun(sweepRunPath, sweepRunId, startTime, "Success", sweepResultsPath);
			}
		} catch (Exception e) {
			SweepRunIO.completeSweepRun(sweepRunPath, sweepRunId, startTime, "Failure", sweepResultsPath);
			System.out.println("Failed: " + e.getMessage());
		}
	}
}
