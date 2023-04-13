package edu.usfca.cs272;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.usfca.cs272.args.ArgumentParser;
import edu.usfca.cs272.index.IndexBuilder;
import edu.usfca.cs272.index.IndexSearcher;
import edu.usfca.cs272.index.InvertedIndex;
import edu.usfca.cs272.index.Searcher;
import edu.usfca.cs272.index.ThreadedIndexSearcher;
import edu.usfca.cs272.server.SearchEngineServer;
import edu.usfca.cs272.threading.WorkQueue;
import edu.usfca.cs272.web.WebCrawler;
import edu.usfca.cs272.index.ThreadSafeInvertedIndex;

/**
 * Class responsible for running this project based on the provided command-line
 * arguments. See the README for details.
 *
 * @author Dominic Fernandez
 * @author CS 272 Software Development (University of San Francisco)
 * @version Fall 2022
 */
public class Driver {
	/** Logger for debugging */
	private static final Logger LOGGER = LogManager.getRootLogger();
	
	/**
	 * Constant for -text flag
	 */
	public static final String TEXT_FLAG = "-text";
	/**
	 * Constant for -index flag
	 */
	public static final String INDEX_FLAG = "-index";
	/**
	 * Constant for -counts flag
	 */
	public static final String COUNTS_FLAG = "-counts";
	/**
	 * Constant for query flag
	 */
	public static final String QUERY_FLAG = "-query";
	/**
	 * Constant for exact flag
	 */
	public static final String EXACT_FLAG = "-exact";
	/**
	 * Constant for results flag
	 */
	public static final String RESULTS_FLAG = "-results";
	/**
	 * Constant for threads flag
	 */
	public static final String THREADS_FLAG = "-threads";
	/**
	 * Constant for html flag
	 */
	public static final String HTML_FLAG = "-html";
	/**
	 * Constant for max flag 
	 */
	public static final String MAX_FLAG = "-max";
	/**
	 * Constant for server flag
	 */
	public static final String SERVER_FLAG = "-server";
	/**
	 * Constant for default output file path
	 */
	public static final String DEFAULT_INDEX_OUTPUT_PATH = "./index.json";
	/**
	 * Constant for default counts output file path
	 */
	public static final String DEFAULT_COUNTS_OUTPUT_PATH = "./counts.json";
	/**
	 * Constant for default results output file path
	 */
	public static final String DEFAULT_RESULTS_OUTPUT_PATH = "./results.json";
	
	/**
	 * Initializes the classes necessary based on the provided command-line
	 * arguments. This includes (but is not limited to) how to build or search an
	 * inverted index.
	 *
	 * @param args flag/value pairs used to start this program
	 */
	public static void main(String[] args) {
		// store initial start time
		Instant start = Instant.now();
		System.out.println("Received arguments: " + Arrays.toString(args));
		
		// Parse and validate arguments
		ArgumentParser argumentParser = new ArgumentParser(args);
		
		// Initialize index and index searcher accordingly
		InvertedIndex index = null;
		Searcher indexSearcher = null;
		WorkQueue queue = null;
		WebCrawler crawler = null;
		
		if (argumentParser.hasFlag(HTML_FLAG)) {
			try {
				if (!argumentParser.hasValue(HTML_FLAG)) {
					throw new Exception("Must provide a value for " + HTML_FLAG);
				}
				if (argumentParser.hasFlag(TEXT_FLAG)) {
					throw new Exception(HTML_FLAG + " and " + TEXT_FLAG + " cannot both be present");
				}
				String seed = argumentParser.getString(HTML_FLAG);
				int maxUrls = argumentParser.getInteger(MAX_FLAG, WebCrawler.MAX_URLS_DEFAULT);
				queue = new WorkQueue(argumentParser.getInteger(THREADS_FLAG, WorkQueue.DEFAULT));
				crawler = new WebCrawler();
				/* 
				 * If -server flag provided, then we pass the crawler to InvertedIndex 
				 * to get the web page metadata and include it in the search results.
				 */
				ThreadSafeInvertedIndex threadSafeIndex = argumentParser.hasFlag(SERVER_FLAG) 
						? new ThreadSafeInvertedIndex(crawler)
						: new ThreadSafeInvertedIndex();
				crawler.crawl(seed, maxUrls, threadSafeIndex, queue);
				index = threadSafeIndex;
				indexSearcher = new ThreadedIndexSearcher(threadSafeIndex, queue);
				queue.finish();
			} catch (Exception e) {
				System.out.println("Failed to build index from web crawl: " + e.getMessage());
			}
		} else if (argumentParser.hasFlag(THREADS_FLAG)) {
			try {
				queue = new WorkQueue(argumentParser.getInteger(THREADS_FLAG, WorkQueue.DEFAULT));
				ThreadSafeInvertedIndex threadSafeIndex = new ThreadSafeInvertedIndex();
				index = threadSafeIndex;
				indexSearcher = new ThreadedIndexSearcher(threadSafeIndex, queue);
				queue.finish();
			} catch (Exception e) {
				System.out.println("Failed to set up multi-threaded code: " + e.getMessage());
			}
		} else {
			index = new InvertedIndex();
			indexSearcher = new IndexSearcher(index);
			queue = null;
		}
		
		if (argumentParser.hasFlag(TEXT_FLAG) && argumentParser.hasValue(TEXT_FLAG)) {
			try {
				String textFlagVal = argumentParser.getString(TEXT_FLAG);
				Path textPath = Path.of(textFlagVal);
				if (argumentParser.hasFlag(THREADS_FLAG) && queue != null) {
					LOGGER.debug("Building index...");
					IndexBuilder.build(textPath, (ThreadSafeInvertedIndex) index, queue);
					queue.finish();
					LOGGER.debug("Finished building index");
				} else {
					IndexBuilder.build(textPath, index);
				}
				
				// Ensure -text flag value is a valid path
				if (!Files.exists(textPath))
					throw new Exception("Invalid path for -text flag");
			} catch (Exception e) {
				System.out.println("Failed to build inverted index: " + e.getMessage());
			}
		}
		
		if (argumentParser.hasFlag(QUERY_FLAG) && argumentParser.hasValue(QUERY_FLAG)) {
			try {
				Path queryPath = Path.of(argumentParser.getString(QUERY_FLAG));
				indexSearcher.search(queryPath, argumentParser.hasFlag(EXACT_FLAG));
				if (queue != null) {
					queue.finish();
				}
			} catch (Exception e) {
				System.out.println("Failed to perform search: " + e.getMessage());
			}
		}
		
		if (argumentParser.hasFlag(INDEX_FLAG)) {
			try {
				String indexFlagVal = argumentParser.getString(INDEX_FLAG, DEFAULT_INDEX_OUTPUT_PATH);
				Path indexPath = Path.of(indexFlagVal);
				index.toJson(indexPath);
			} catch (Exception e) {
				System.out.println("Failed to write index to file: " + e.getMessage());
			}
		}
		
		if (argumentParser.hasFlag(COUNTS_FLAG)) {
			try {
				String countsFlagVal = argumentParser.getString(COUNTS_FLAG, DEFAULT_COUNTS_OUTPUT_PATH);
				Path countsPath = Path.of(countsFlagVal);
				index.wordCountToJson(countsPath);
			} catch (Exception e) {
				System.out.println("Failed to produce word counts: " + e.getMessage());
			}
		}
		
		if (argumentParser.hasFlag(RESULTS_FLAG)) {
			try {
				Path resultPath = Path.of(argumentParser.getString(RESULTS_FLAG, DEFAULT_RESULTS_OUTPUT_PATH));
				indexSearcher.searchResultstoJson(resultPath);
			} catch (Exception e) {
				System.out.println("Failed to output results: " + e.getMessage());
			}
		}

		// calculate time elapsed and output
		long elapsed = Duration.between(start, Instant.now()).toMillis();
		double seconds = (double) elapsed / Duration.ofSeconds(1).toMillis();
		System.out.printf("Elapsed: %f seconds%n", seconds);
		
		if (argumentParser.hasFlag(SERVER_FLAG)) {
			try {
				if (index == null || indexSearcher == null || queue == null) {
					throw new Exception("The index, index searcher, and work queue must be initialized with the appropriate flags");
				}
				SearchEngineServer server = new SearchEngineServer(
					argumentParser.getInteger(SERVER_FLAG, SearchEngineServer.DEFAULT_PORT),
					(ThreadedIndexSearcher) indexSearcher
				);
				server.start();
			} catch (Exception e) {
				System.out.println("Failed to start server: " + e.getMessage());
			}
		}
		
		// Shutdown WorkQueue if it was used
		if (queue != null) {
			queue.shutdown();
		}
	}
}
