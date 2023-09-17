package com.crawler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.threading.WorkQueue;

/**
 * Web Crawler to download and build an inverted index of the text in each web page it visits
 * @author domin
 *
 */
public class WebCrawler {
	/** Constant for default max URLs to visit */
	public static final int MAX_URLS_DEFAULT = 1;
	/** Constant for default number of redirects to follow for a given URL */
	public static final int REDIRECTS_LIMIT = 3;
	
	public static class CrawlResult {
		private final String url;
		private final String timestamp;
		
		public CrawlResult(String url, String timestamp) {
			this.url = url;
			this.timestamp = timestamp;
		}
		
		public String getUrl() {
			return this.url;
		}
		
		public String getTimestamp() {
			return this.timestamp;
		}
		
		@Override
		public String toString() {
			return "Url=" + this.url + " Timestamp=" + timestamp;
		}
	}
	
	/**
	 * Task to be run by WorkQueue for multi-threading
	 * @author domin
	 *
	 */
	private class Task implements Runnable {
		/** URL to visit */
		private final String seed;
		private final String base;
		/** Number of max URLs to visit */
		private final int maxUrls;
		/** Set to keep track of URLs already visited */
		private final Set<String> visited;
		private final String routeRegex;
		private final List<CrawlResult> crawlResults;
		/** The WorkQueue to add more tasks recursively */
		private final WorkQueue queue;
		
		/**
		 * Constructor initializes members
		 * @param seed the url to visit
		 * @param maxUrls max urls to visit
		 * @param visited urls already visited 
		 * @param index the index to add to 
		 * @param queue the WorkQueue to add more tasks to recursively
		 */
		private Task(
			String seed, 
			String base,
			int maxUrls, 
			Set<String> visited, 
			String routeRegex,
			List<CrawlResult> crawlResults, 
			WorkQueue queue
		) {
			this.seed = seed;
			this.base = base;
			this.maxUrls = maxUrls;
			this.visited = visited;
			this.routeRegex = routeRegex;
			this.crawlResults = crawlResults;
			this.queue = queue;
		}
		
		@Override
		public void run() {
			try {
				synchronized (this.visited) {
					if (this.visited.contains(this.seed) || this.visited.size() >= this.maxUrls || !this.seed.startsWith(this.base)) {
						// System.out.println("Skipping, " + this.seed);
						return;
					}
					this.visited.add(this.seed);
				}
				if (this.matchesRouteRegex()) {
					synchronized (this.crawlResults) {
						String timestamp = Long.toString(System.currentTimeMillis());
						CrawlResult crawlResult = new CrawlResult(this.seed, timestamp);
						this.crawlResults.add(crawlResult);
					}
				}
				// System.out.println("Processing html...");
				String html = processHtml(this.seed);
				// System.out.println("Processed html");
				// System.out.println("Extracting links...");
				List<String> links = extractLinks(html, this.seed);
				// System.out.println("Extracted links");
				for (String link : links) {
					this.queue.execute(new Task(
						link,
						this.base,
						this.maxUrls, 
						this.visited, 
						this.routeRegex, 
						this.crawlResults, 
						this.queue)
					);	
				}
			} catch (Exception e) {
				System.out.println("Exception occurred in WebCrawler task: " + e.getMessage());
			}
		}
		
		private boolean matchesRouteRegex() {
			Pattern pattern = Pattern.compile(this.routeRegex);
			Matcher matcher = pattern.matcher(this.seed);
			return matcher.find();
		}
	}
	
	/**
	 * Begins the multi-threaded web crawl
	 * @param seed initial url to start with
	 * @param maxUrls number of max urls to visit
	 * @param index the index to add the words in each web page to
	 * @param queue the WorkQueue for multi-threading
	 */
	public List<CrawlResult> crawl(String seed, String base, int maxUrls, String routeRegex, WorkQueue queue) {
		List<CrawlResult> crawlResults = Collections.synchronizedList(new ArrayList<CrawlResult>());
		Set<String> visited = new HashSet<>();
		queue.execute(new Task(seed, base, maxUrls, visited, routeRegex, crawlResults, queue));
		return crawlResults;
	}
	
	/**
	 * Helper method to extract links from raw html
	 * @param html the html to extract links from
	 * @param base the base url 
	 * @return List of the extracted links
	 * @throws MalformedURLException if url is not valid
	 */
	private List<String> extractLinks(String html, String base) throws MalformedURLException {
		html = HtmlCleaner.stripBlockElements(HtmlCleaner.stripComments(html));
		Pattern p = Pattern.compile("href=\"(.*?)(#|\")");
		Matcher m = p.matcher(html);
		List<String> links = new ArrayList<>();
		while (m.find()) {
			try {
				String match = m.group(1);
				URL url = new URL(new URL(base), match);
				String link = url.toString();
			    links.add(link);
			} catch (Exception e) {
				continue;
			}
		}
		return links;
	}
	
	/**
	 * Helper method to fetch and process html and add it to the inverted index
	 * @param base the base url
	 * @)param index the index to add to
	 * @param metadata the metadata to add for the web page visited
	 * @return String of the html downloaded from the base url
	 * @throws IOException if failed to fetch and process html
	 */
	private String processHtml(String base) throws IOException {
		String html = HtmlFetcher.fetch(base, REDIRECTS_LIMIT);
		if (html == null) {
			return "";
		}
		// String text = HtmlCleaner.stripHtml(html).strip();
		return html;
	}
}
