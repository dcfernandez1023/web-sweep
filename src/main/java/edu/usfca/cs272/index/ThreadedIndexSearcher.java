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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.usfca.cs272.index.InvertedIndex.SearchResult;
import edu.usfca.cs272.io.PrettyJsonWriter;
import edu.usfca.cs272.io.WordCleaner;
import edu.usfca.cs272.threading.ReadWriteLock;
import edu.usfca.cs272.threading.WorkQueue;

/**
 * Class responsible for searching an inverted index
 * @author Dominic Fernandez
 *
 */
public class ThreadedIndexSearcher implements Searcher {
	/** Logger for debuggin */
	private static final Logger LOGGER = LogManager.getRootLogger();
	
	/** Data structure representing the search results */
	private final TreeMap<String, List<SearchResult>> searchResults;
	/** The index to search, thread safe*/
	private final ThreadSafeInvertedIndex index;
	/** Lock for controlling thread access to searchResults */
	private final ReadWriteLock lock;
	/** The Workthis.queue for multi-threading the search */
	private final WorkQueue queue;
	
	/**
	 * Task to be executed by the WorkQueue for the searcher
	 * @author domin
	 *
	 */
	private class Task implements Runnable {
		/** The line to process */
		private final String line;
		/** Weather to perform an exact search or not*/
		private final boolean isExact;
		
		/**
		 * Constructor initializes members
		 * @param line The line to process
		 * @param isExact The type of search to perform
		 */
		public Task(String line, boolean isExact) {
			this.line = line;
			this.isExact = isExact;
			LOGGER.debug("Created IndexSearcher task for {}", line);
		}
		
		@Override
		public void run() {
			try {
				TreeSet<String> uniqueStems = WordCleaner.uniqueStems(line);
				String cleanedQueryLine = String.join(" ", uniqueStems);
				boolean performSearch;
				synchronized (searchResults) {
					performSearch = !cleanedQueryLine.isBlank() && !searchResults.containsKey(cleanedQueryLine);
				}
				if (performSearch) {
					List<SearchResult> local = index.search(uniqueStems, isExact);
					synchronized (searchResults) {
						searchResults.put(cleanedQueryLine, local);
					}
				}
			} finally {
				LOGGER.debug("Completed IndexSearcher task for {}", line);
			}
		}
	}

	/**
	 * Constructor initializes members
	 * @param index The index to search
	 * @param queue The WorkQueue used to multi-thread the search
	 */
	public ThreadedIndexSearcher(ThreadSafeInvertedIndex index, WorkQueue queue) {
		this.index = index;
		this.searchResults = new TreeMap<>();
		this.lock = new ReadWriteLock();
		this.queue = queue;
	}

	@Override
	public void search(String line, boolean isExact) throws IOException {
		this.queue.execute(new Task(line, isExact));
	}

	@Override
	public List<SearchResult> getResults(String line, boolean isExact) {
		lock.read().lock();
		try {
			TreeSet<String> uniqueStems = WordCleaner.uniqueStems(line);
			String joined = String.join(" ", uniqueStems);
			if (this.searchResults.containsKey(joined)) {
				return Collections.unmodifiableList(searchResults.get(joined));
			}
			return this.index.search(uniqueStems, isExact);
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public void searchResultstoJson(Path path) throws IOException {
		try {
			this.lock.read().lock();
			try(Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				PrettyJsonWriter.searchResultsToJson(
					this.searchResults, 
					writer,
					0
				);
			}
		}
		finally {
			this.lock.read().unlock();
		}
	}
}
