package edu.usfca.cs272.index;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.usfca.cs272.io.PrettyJsonWriter;
import edu.usfca.cs272.web.WebCrawler;
import edu.usfca.cs272.web.WebCrawler.WebPageMetadata;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Class responsible for representing an inverted index
 * @author Dominic Fernandez
 *
 */
public class InvertedIndex {
	/**
	 * The {@link TreeMap} underlying the inverted index
	 */
	private final TreeMap<String, TreeMap<String, Set<Integer>>> index;
	
	/**
	 * The {@link TreeMap} underlying the word counts
	 */
	private final TreeMap<String, Integer> wordCount;
	
	/**
	 * WebCrawler used to build the index
	 */
	private final WebCrawler crawler;
	
	/**
	 * Inner class to contain search result data
	 * @author Dominic Fernandez
	 */
	public class SearchResult implements Comparable<SearchResult> {
		/**
		 * Count of result
		 */
		private int count;
		/**
		 * Score of result
		 */
		private double score;
		/**
		 * Location of where the count and score were taken from
		 */
		private final String where;
		/**
		 * The title of the search result web page
		 */
		private String title;
		/**
		 * The timestamp of the search result web page
		 */
		private String timestamp;
		/**
		 * The description of the search result web page
		 */
		private String description;
		
		/**
		 * Constructor 
		 * @param where the location
		 */
		public SearchResult(String where) {
			this.where = where;
		}
		
		/**
		 * Constructor 
		 * @param where the location
		 * @param match the matching query
		 */
		public SearchResult(String where, String match) {
			this.where = where;
			this.count = 0;
			this.score = 0;
			this.update(match);
		}
		
		/**
		 * Constructor 
		 * @param where the location
		 * @param match the matching query
		 * @param title the title of the web page
		 * @param timestamp the timestamp the web page was crawled
		 * @param description the description of the web page
		 */
		public SearchResult(String where, String match, String title, String timestamp, String description) {
			this.where = where;
			this.title = title;
			this.timestamp = timestamp;
			this.description = description;
			this.count = 0;
			this.score = 0;
			this.update(match);
		}
		
		/**
		 * Updates the SearchResult's score and count based on the match
		 * @param match The matching word from the index
		 */
		private void update(String match) {
			this.count += index.get(match).get(where).size();
			this.score = this.count / (double) wordCount.get(where);
		}
		
		@Override
		public int compareTo(SearchResult sr) {
			int scoreCompare = Double.compare(sr.score, this.score);
			if (scoreCompare != 0) return scoreCompare;
			
			int countCompare = Integer.compare(sr.count, this.count);
			if (countCompare != 0) return countCompare;
			
			int locationCompare = this.where.compareToIgnoreCase(sr.where);
			return locationCompare;
		}
		
		/**
		 * Getter for count
		 * @return int
		 */
		public int getCount() {
			return this.count;
		}
		/**
		 * Getter for score
		 * @return double
		 */
		public double getScore() {
			return this.score;
		}
		/**
		 * Getter for where
		 * @return String
		 */
		public String getWhere() {
			return this.where;
		}
		
		/**
		 * Writes itself to json 
		 * @param writer the writer used to output results
		 * @param indent initial indent
		 * @param withMetadata whether or not to get the WebPageMetadata for each web page crawled
		 * @throws IOException if failed to write
		 */
		public void toJson(Writer writer, int indent, boolean withMetadata) throws IOException {
			PrettyJsonWriter.writeIndent(writer, indent+1);
			writer.write(PrettyJsonWriter.OPEN_CURLY_BRACE);
			writer.write(PrettyJsonWriter.NEW_LINE);
			
			PrettyJsonWriter.writeQuote("count", writer, indent+2);
			writer.write(PrettyJsonWriter.COLON);
			writer.write(Integer.valueOf(this.count).toString());
			writer.write(PrettyJsonWriter.COMMA);
			writer.write(PrettyJsonWriter.NEW_LINE);
			
			PrettyJsonWriter.writeQuote("score", writer, indent+2);
			writer.write(PrettyJsonWriter.COLON);
			writer.write(String.format("%.8f", this.score));
			writer.write(PrettyJsonWriter.COMMA);
			writer.write(PrettyJsonWriter.NEW_LINE);
			
			PrettyJsonWriter.writeQuote("where", writer, indent+2);
			writer.write(PrettyJsonWriter.COLON);
			PrettyJsonWriter.writeQuote(this.where, writer, 0);
			if (withMetadata) {
				writer.write(PrettyJsonWriter.COMMA);
			}
			writer.write(PrettyJsonWriter.NEW_LINE);
			
			if (withMetadata) {
				PrettyJsonWriter.writeQuote("title", writer, indent+2);
				writer.write(PrettyJsonWriter.COLON);
				PrettyJsonWriter.writeQuote(this.title, writer, 0);
				writer.write(PrettyJsonWriter.COMMA);
				writer.write(PrettyJsonWriter.NEW_LINE);
				
				PrettyJsonWriter.writeQuote("timestamp", writer, indent+2);
				writer.write(PrettyJsonWriter.COLON);
				PrettyJsonWriter.writeQuote(this.timestamp, writer, 0);
				writer.write(PrettyJsonWriter.COMMA);
				writer.write(PrettyJsonWriter.NEW_LINE);
				
				PrettyJsonWriter.writeQuote("description", writer, indent+2);
				writer.write(PrettyJsonWriter.COLON);
				PrettyJsonWriter.writeQuote(this.description, writer, 0);
				writer.write(PrettyJsonWriter.NEW_LINE);
			}
			
			PrettyJsonWriter.writeIndent(writer, indent+1);
			writer.write(PrettyJsonWriter.CLOSE_CURLY_BRACE);
		}
		
		@Override
		public String toString() {
			String s;
			try {
				StringWriter writer = new StringWriter();
				this.toJson(writer, 0, (crawler != null));
				s = writer.toString();
				writer.close();
			} catch (IOException e) {
				return "";
			}
			return s;
		}
	}
	
	/**
	 * Constructor instantiates the index as a TreeMap
	 */
	public InvertedIndex() {
		this.index = new TreeMap<>();
		this.wordCount = new TreeMap<>();
		this.crawler = null;
	}
	
	/**
	 * Constructor instantiates the index as a TreeMap
	 * @param crawler the crawler that was used to build the index
	 */
	public InvertedIndex(WebCrawler crawler) {
		this.index = new TreeMap<>();
		this.wordCount = new TreeMap<>();
		this.crawler = crawler;
	}
	
	/**
	 * Searches the index
	 * @param queries Set of queries to perform the search with
	 * @param exact Whether or not to perform an exact search
	 * @return a List of {@link SearchResult}
	 */
	public List<SearchResult> search(Set<String> queries, boolean exact) { 
		return exact ? exactSearch(queries) : partialSearch(queries);
	}
	
	/**
	 * Performs an exact search on the index
	 * @param queries the words to search
	 * @return a List of {@link SearchResult}
	 */
	public List<SearchResult> exactSearch(Set<String> queries) {
		List<SearchResult> results = new LinkedList<>();
		HashMap<String, SearchResult> matches = new HashMap<>();
		for (String query : queries) {
			if (this.index.containsKey(query)) {
				this.searchHelper(matches, query, results);
			}
		}
		Collections.sort(results);
		return results;
	}
	
	/**
	 * Performs a partial search on the index
	 * @param queries the words to search
	 * @return a List of {@link SearchResult}
	 */
	public List<SearchResult> partialSearch(Set<String> queries) {
		List<SearchResult> results = new LinkedList<>();
		HashMap<String, SearchResult> matches = new HashMap<>();
		for (String query : queries) {
			for (String word : this.index.tailMap(query).keySet()) {
				if (word.startsWith(query)) {
					this.searchHelper(matches, word, results);
				} else {
					break;
				}
			}
		}
		Collections.sort(results);
		return results;
	}
	
	/**
	 * Gets the count of words at the given location 
	 * @param location the location to get the count 
	 * @return int
	 */
	public int getCount(String location) {
		return this.wordCount.getOrDefault(location, 0);
	}
	
	/**
	 * Adds a new word to the index
	 * @param word The word to add to the index
	 * @param location The location representing where the word came from
	 * @param position The position of the word when being processed by the WordCleaner object
	 */
	public void addWord(String word, String location, int position) {
		this.index.putIfAbsent(word, new TreeMap<>());
		Map<String, Set<Integer>> wordMapping = this.index.get(word);
		wordMapping.putIfAbsent(location, new TreeSet<Integer>());
		boolean modified = wordMapping.get(location).add(position);
		if (modified) {
			this.wordCount.merge(location, 1, Integer::sum);
		}
	}
	
	/**
	 * Adds a list of words to the index at a given location
	 * @param words the list of words to add
	 * @param location the locations of the words 
	 */
	public void addWords(List<String> words, String location) {
		int counter = 1;
		for (String word: words) {
			this.addWord(word, location, counter++);
		}
	}
	
	/**
	 * Adds another inverted index to this index
	 * @param otherIndex the other inverted index to add
	 */
	public void addAll(InvertedIndex otherIndex) {
		for (var entry : otherIndex.index.entrySet()) {
			String word = entry.getKey();
			if (this.index.containsKey(word)) {
				// test here if we can do a put for the set or must addAll sets together
				TreeMap<String, Set<Integer>> otherLocations = otherIndex.index.get(word);
				for (var locationEntry : otherLocations.entrySet()) {
					String otherLocation = locationEntry.getKey();
					Set<Integer> otherPositions = locationEntry.getValue();
					if (this.index.get(word).containsKey(otherLocation)) {
						this.index.get(word).get(otherLocation).addAll(otherPositions);
					} else {
						this.index.get(word).put(otherLocation, otherPositions);
					}
				}
			}
			else {
				this.index.put(entry.getKey(), entry.getValue());
			}
		}
		
		for (var entry : otherIndex.wordCount.entrySet()) {
			String key = entry.getKey();
			Integer value = entry.getValue();
			if (this.wordCount.containsKey(key)) {
				this.wordCount.put(key, this.wordCount.get(key) + value);
			} else {
				this.wordCount.put(key, value);
			}
		}
	}
	
	/**
	 * Returns true if the word is in the index, false otherwise
	 * @param word the word to look for
	 * @return boolean
	 */
	public boolean hasWord(String word) {
		return this.index.containsKey(word);
	}
	
	/**
	 * Returns true if the location is in the index, false otherwise
	 * @param word the word to look for
	 * @param location the location to look for
	 * @return boolean
	 */
	public boolean hasLocation(String word, String location) {
		return this.index.containsKey(word) && this.index.get(word).containsKey(location);
	}
	
	/**
	 * Returns true if the position for the word and location is in the index, false otherwise
	 * @param word the word to look for
	 * @param location the location to look for
	 * @param position the position to look for 
	 * @return boolean
	 */
	public boolean hasPosition(String word, String location, int position) {
		return this.hasLocation(word, location) && this.index.get(word).get(location).contains(position);
	}
	
	/**
	 * Returns the number of words in the index
	 * @return int
	 */
	public int numWords() {
		return this.index.size();
	}
	
	/**
	 * Returns the number of locations for a given word in the index
	 * @param word the word to return the num of locations for
	 * @return int
	 */
	public int numLocations(String word) {
		return this.index.containsKey(word) 
			? this.index.get(word).size()
			: 0;
	}
	
	/**
	 * Returns the number of positions for a given word and location 
	 * @param word the word to look for
	 * @param location the location to look for
	 * @return int
	 */
	public int numPositions(String word, String location) {
		return this.hasLocation(word, location)
			? this.index.get(word).get(location).size()
			: 0;
	}
	
	/**
	 * Gets the words in the index
	 * @return Set containing words in index
	 */
	public Set<String> getWords() {
		return Collections.unmodifiableSet(this.index.keySet());
	}

	/**
	 * Gets the locations in the index under a given word
	 * @param word the word to look for
	 * @return Set containing the locations in the index
	 */
	public Set<String> getLocations(String word) {
		return this.index.containsKey(word)
			? Collections.unmodifiableSet(this.index.get(word).keySet())
			: Collections.emptySet();
	}
	
	/**
	 * Returns a Set of all the locations in the index
	 * @return Set containing all of the unique locations
	 */
	public Set<String> getAllLocations() {
		return Collections.unmodifiableSet(this.wordCount.keySet());
	}
	
	/**
	 * Gets the positions of a given word and location 
	 * @param word the word to look for
	 * @param location the location to look for
	 * @return Set containing the positions
	 */
	public Set<Integer> getPositions(String word, String location) {
		return this.hasLocation(word, location)
			? Collections.unmodifiableSet(this.index.get(word).get(location))
			: Collections.emptySet();
	}
	
	@Override
	public String toString() {
		return this.index.toString();
	}
	
	/**
	 * Writes index to console
	 * @throws IOException if failed to write
	 */
	public void toJson() throws IOException {
		try (Writer writer = new PrintWriter(System.out, true)) {
			PrettyJsonWriter.indexToJson(
				this.index, 
				writer, 
				0
			);
		}
	}
	
	/**
	 * Writes index to json file
	 * @param path The path of the file to write
	 * @throws IOException If failed to write index to json file
	 */
	public void toJson(Path path) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			PrettyJsonWriter.indexToJson(
				this.index, 
				writer, 
				0
			);
		}
	}
	
	/**
	 * Writes word count to json file
	 * @param path The path of the file to write
	 * @throws IOException If failed to write to json file
	 */
	public void wordCountToJson(Path path) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			PrettyJsonWriter.wordCountToJson(
				this.wordCount, 
				writer, 
				0
			);
		}
	}
	
	/**
	 * Helper function to perform partial or exact search
	 * @param matches HashMap containing search results at their respective locations
	 * @param word The word currently being used to search 
	 * @param results The list of SearchResults being produced
	 */
	private void searchHelper(HashMap<String, SearchResult> matches, String word, List<SearchResult> results) {
		for (var entry : index.get(word).entrySet()) {
			String location = entry.getKey();
			if (matches.containsKey(location)) {
				if (index.containsKey(word) && index.get(word).containsKey(location)) {
					matches.get(location).update(word);
				}
			} else {
				// If a WebCrawler was provided in the constructor, then use it to populate search result additional fields
				SearchResult searchResult;
				if (this.crawler != null) {
					WebPageMetadata metadata = this.crawler.getWebPageMetadata(location);
					searchResult = new SearchResult(location, word, metadata.getTitle(), metadata.getTimestamp(), metadata.getDescription());
				} else {
					searchResult = new SearchResult(location, word);
				}
				matches.put(location, searchResult);
				results.add(searchResult);
			}
		}
	}
}
