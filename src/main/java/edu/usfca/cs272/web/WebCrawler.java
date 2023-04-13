package edu.usfca.cs272.web;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.usfca.cs272.index.IndexBuilder;
import edu.usfca.cs272.index.InvertedIndex;
import edu.usfca.cs272.index.ThreadSafeInvertedIndex;
import edu.usfca.cs272.io.PrettyJsonWriter;
import edu.usfca.cs272.threading.WorkQueue;

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
	/** Logger used for debugging */
	private static final Logger LOGGER = LogManager.getRootLogger();
	/** HashMap used to store titles of web pages */
	public final HashMap<String, WebPageMetadata> metadata;
	
	/**
	 * Class to contain metadata for each web page crawled
	 * @author domin
	 *
	 */
	public static class WebPageMetadata {
		/**
		 * Title
		 */
		private final String title;
		/**
		 * Timestamp when crawled
		 */
		private final String timestamp;
		/**
		 * Description (if any)
		 */
		private final String description;
		
		/**
		 * Constructor initializes to empty strings
		 */
		public WebPageMetadata() {
			this.title = "";
			this.timestamp = "";
			this.description = "";
		}
		
		/**
		 * Constructor initializes to provided params
		 * @param title the title
		 * @param timestamp the timestamp
		 * @param description the description
		 */
		public WebPageMetadata(String title, String timestamp, String description) {
			this.title = title;
			this.timestamp = timestamp;
			this.description = description;
		}
		
		/**
		 * Getter
		 * @return title
		 */
		public String getTitle() {
			return this.title;
		}
		/**
		 * Getter
		 * @return timestamp
		 */
		public String getTimestamp() {
			return this.timestamp;
		}
		/**
		 * Getter
		 * @return description
		 */
		public String getDescription() {
			return this.description;
		}
		
		/**
		 * Prints to json
		 * @return String of metadata in json format
		 * @throws IOException if failed to write to json
		 */
		public String toJson() throws IOException {
			Writer writer = new StringWriter();
			
			writer.write(PrettyJsonWriter.OPEN_CURLY_BRACE);
			
			PrettyJsonWriter.writeQuote("title", writer, 0);
			writer.write(PrettyJsonWriter.COLON);
			PrettyJsonWriter.writeQuote(this.title, writer, 0);
			writer.write(PrettyJsonWriter.COMMA);
			
			PrettyJsonWriter.writeQuote("timestamp", writer, 0);
			writer.write(PrettyJsonWriter.COLON);
			PrettyJsonWriter.writeQuote(this.timestamp, writer, 0);
			writer.write(PrettyJsonWriter.COMMA);
			
			PrettyJsonWriter.writeQuote("description", writer, 0);
			writer.write(PrettyJsonWriter.COLON);
			PrettyJsonWriter.writeQuote(this.description, writer, 0);
			
			writer.write(PrettyJsonWriter.CLOSE_CURLY_BRACE);
			return writer.toString();
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
		/** Number of max URLs to visit */
		private final int maxUrls;
		/** Set to keep track of URLs already visited */
		private final Set<String> visited;
		/** The inverted index to add to */
		private final ThreadSafeInvertedIndex index;
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
		private Task(String seed, int maxUrls, Set<String> visited, ThreadSafeInvertedIndex index, WorkQueue queue) {
			this.seed = seed;
			this.maxUrls = maxUrls;
			this.visited = visited;
			this.index = index;
			this.queue = queue;
		}
		
		@Override
		public void run() {
			try {
				synchronized (this.visited) {
					if (this.visited.contains(this.seed) || this.visited.size() >= this.maxUrls) {
						return;
					}
					this.visited.add(this.seed);
				}
				InvertedIndex local = new InvertedIndex();
				HashMap<String, WebPageMetadata> localMetadata = new HashMap<>();
				String html = processHtml(this.seed, local, localMetadata);
				List<String> links = extractLinks(html, this.seed);
				synchronized (this.index) {
					this.index.addAll(local);
				}
				synchronized (metadata) {
					metadata.putAll(localMetadata);
				}
				for (String link : links) {
					this.queue.execute(new Task(link, this.maxUrls, this.visited, this.index, this.queue));	
				}
			} catch (IOException e) {
				LOGGER.error("Exception occurred in WebCrawler task", e);
			}
		}
	}
	
	/**
	 * Constructor initializes metadata
	 */
	public WebCrawler() {
		this.metadata = new HashMap<>();
	}
	
	/**
	 * Begins the multi-threaded web crawl
	 * @param seed initial url to start with
	 * @param maxUrls number of max urls to visit
	 * @param index the index to add the words in each web page to
	 * @param queue the WorkQueue for multi-threading
	 */
	public void crawl(String seed, int maxUrls, ThreadSafeInvertedIndex index, WorkQueue queue) {
		Set<String> visited = new HashSet<>();
		queue.execute(new Task(seed, maxUrls, visited, index, queue));
	}
	
	/**
	 * Gets web page metadata for a speicifc url crawled
	 * @param url the url crawled
	 * @return the WebPageMetadata of the url
	 */
	public WebPageMetadata getWebPageMetadata(String url) {
		return this.metadata.getOrDefault(url, new WebPageMetadata());
	}
	
	/**
	 * Writes all web page metadata to json
	 * @return json string
	 * @throws IOException if failed to write to json
	 */
	public String webPageMetadataToJson() throws IOException {
		Writer writer = new StringWriter();
		var iterator = this.metadata.entrySet().iterator();
		writer.write(PrettyJsonWriter.OPEN_CURLY_BRACE);
		if (iterator.hasNext()) {
			var element = iterator.next();
			String key = element.getKey();
			WebPageMetadata value = element.getValue();
			PrettyJsonWriter.writeQuote(key, writer, 0);
			writer.write(PrettyJsonWriter.COLON);
			writer.write(value.toJson());
			while(iterator.hasNext()) {
				writer.write(PrettyJsonWriter.COMMA);
				element = iterator.next();
				key = element.getKey();
				value = element.getValue();
				PrettyJsonWriter.writeQuote(key, writer, 0);
				writer.write(PrettyJsonWriter.COLON);
				writer.write(value.toJson());
			}
		}
		writer.write(PrettyJsonWriter.CLOSE_CURLY_BRACE);
		String jsonString = writer.toString();
		writer.close();
		return jsonString;
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
			String match = m.group(1);
			URL url = new URL(new URL(base), match);
		    links.add(url.toString());
		}
		return links;
	}
	
	/**
	 * Helper method to fetch and process html and add it to the inverted index
	 * @param base the base url
	 * @param index the index to add to
	 * @param metadata the metadata to add for the web page visited
	 * @return String of the html downloaded from the base url
	 * @throws IOException if failed to fetch and process html
	 */
	private String processHtml(String base, InvertedIndex index, HashMap<String, WebPageMetadata> metadata) throws IOException {
		String html = HtmlFetcher.fetch(base, REDIRECTS_LIMIT);
		if (html == null) {
			return "";
		}
		WebPageMetadata pageMetadata = this.getMetadata(html);
		metadata.put(base, pageMetadata);
		String text = HtmlCleaner.stripHtml(html).strip();
		IndexBuilder.addStems(text, base, index);
		return html;
	}
	
	/**
	 * Extracts the title of an html web page
	 * @param html the html 
	 * @return the title, or empty string if not found
	 */
	private WebPageMetadata getMetadata(String html) {
		Pattern p = Pattern.compile(".*?<title>(.*?)<\\/title>.*?", Pattern.DOTALL);
		Matcher m = p.matcher(html);
		String title = m.find() ? m.group(1) : "";
		String timestamp = Long.toString(System.currentTimeMillis());
		return new WebPageMetadata(title, timestamp, "");
	}
}
