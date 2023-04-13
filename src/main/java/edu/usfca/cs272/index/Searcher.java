package edu.usfca.cs272.index;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import edu.usfca.cs272.index.InvertedIndex.SearchResult;

/**
 * Interface for index searcher
 * @author domin
 *
 */
public interface Searcher {
	/**
	 * Searches the given inverted index
	 * @param path the path to read the queries from
	 * @param isExact whether or not to perform an exact search
	 * @throws IOException if could not read query file
	 */
	public default void search(Path path, boolean isExact) throws IOException {
		try (Stream<String> stream = Files.lines(path)) {
			stream.forEach((String line) -> {
				try {
					this.search(line, isExact);	
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}
	}
	
	/**
	 * Performs a search given a single query line
	 * @param line The query line to search
	 * @param isExact whether or not to perform an exact search
	 * @throws IOException if failed to clean words from line
	 */
	public void search(String line, boolean isExact) throws IOException;
	/**
	 * Gets the SearchResults for a single query line
	 * @param line the query line
	 * @param isExact whether to perform an exact search or not
	 * @return List of SearchResult objects
	 */
	public List<SearchResult> getResults(String line, boolean isExact);
	/**
	 * Writes search results to json file
	 * @param path The path of the file to write
	 * @throws IOException If failed to write to json file
	 */
	public void searchResultstoJson(Path path) throws IOException;
}
