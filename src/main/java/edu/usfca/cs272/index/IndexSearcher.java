package edu.usfca.cs272.index;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.usfca.cs272.index.InvertedIndex.SearchResult;
import edu.usfca.cs272.io.PrettyJsonWriter;
import edu.usfca.cs272.io.WordCleaner;


/**
 * Class responsible for searching an inverted index
 * @author Dominic Fernandez
 *
 */
public class IndexSearcher implements Searcher {
	/**
	 * The inverted index to search
	 */
	private final InvertedIndex index;
	
	/**
	 * Data structure representing the search results 
	 */
	private final TreeMap<String, List<SearchResult>> searchResults;
	
	/**
	 * Constructor initializes search results TreeMap data structure
	 * @param index the index to search
	 */
	public IndexSearcher(InvertedIndex index) {
		this.index = index;
		this.searchResults = new TreeMap<>();
	}
	
	@Override
	public void search(String line, boolean isExact) throws IOException {
		TreeSet<String> uniqueStems = WordCleaner.uniqueStems(line);
		String cleanedQueryLine = String.join(" ", uniqueStems);
		if (!cleanedQueryLine.isBlank() && !this.searchResults.containsKey(cleanedQueryLine)) {
			searchResults.put(cleanedQueryLine, this.index.search(uniqueStems, isExact));
		}
	}
	
	@Override
	public List<SearchResult> getResults(String line, boolean isExact) {
		TreeSet<String> uniqueStems = WordCleaner.uniqueStems(line);
		String joined = String.join(" ", uniqueStems);
		if (this.searchResults.containsKey(joined)) {
			return Collections.unmodifiableList(searchResults.get(joined));
		}
		return this.index.search(uniqueStems, isExact);
	}
	
	@Override
	public void searchResultstoJson(Path path) throws IOException {
		try(Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			PrettyJsonWriter.searchResultsToJson(
				this.searchResults, 
				writer,
				0
			);
		}
	}
}
