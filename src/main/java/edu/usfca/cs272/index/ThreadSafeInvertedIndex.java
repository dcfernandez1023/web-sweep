package edu.usfca.cs272.index;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.usfca.cs272.threading.ReadWriteLock;
import edu.usfca.cs272.web.WebCrawler;

/**
 * Thread safe index
 * @author domin
 *
 */
public class ThreadSafeInvertedIndex extends InvertedIndex {
	/** Logger for debugging */
	private static final Logger LOGGER = LogManager.getRootLogger();
	
	/** The lock for the data structure underlying the inverted index */
	private final ReadWriteLock lock;
	
	/**
	 * Constructor initializes locks
	 */
	public ThreadSafeInvertedIndex() {
		super();
		this.lock = new ReadWriteLock();
	}
	
	/**
	 * Constructor initializes locks
	 * @param crawler the crawler used to build the index
	 */
	public ThreadSafeInvertedIndex(WebCrawler crawler) {
		super(crawler);
		this.lock = new ReadWriteLock();
	}

	@Override
	public void addAll(InvertedIndex otherIndex) {
		lock.write().lock();
		try {
			super.addAll(otherIndex);
		} finally {
			lock.write().unlock();
		}
	}

	@Override
	public List<SearchResult> exactSearch(Set<String> queries) {
		lock.read().lock();
		try {
			return super.exactSearch(queries);	
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public List<SearchResult> partialSearch(Set<String> queries) {
		lock.read().lock();
		try {
			return super.partialSearch(queries);
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public int getCount(String location) {
		lock.read().lock();
		try {
			return super.getCount(location);
		} finally {
			lock.read().unlock();
		}
	}
	
	@Override
	public void addWord(String word, String location, int position) {
		lock.write().lock();
		try {
			super.addWord(word, location, position);
		} finally {
			lock.write().unlock();
		}
	}
	
	@Override
	public void addWords(List<String> words, String location) {
		lock.write().lock();
		try {
			super.addWords(words, location);
		} finally {
			lock.write().unlock();
		}
	}

	@Override
	public boolean hasWord(String word) {
		lock.read().lock();
		try {
			return super.hasWord(word);
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public boolean hasLocation(String word, String location) {
		lock.read().lock();
		try {
			return super.hasLocation(word, location);
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public boolean hasPosition(String word, String location, int position) {
		lock.read().lock();
		try {
			return super.hasPosition(word, location, position);
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public int numWords() {
		lock.read().lock();
		try {
			return super.numWords();
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public int numLocations(String word) {
		lock.read().lock();
		try {
			return super.numLocations(word);
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public int numPositions(String word, String location) {
		lock.read().lock();
		try {
			return super.numPositions(word, location);
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public Set<String> getWords() {
		lock.read().lock();
		try {
			return super.getWords();
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public Set<String> getLocations(String word) {
		lock.read().lock();
		try {
			return super.getLocations(word);
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public Set<String> getAllLocations() {
		lock.read().lock();
		try {
			return super.getAllLocations();
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public Set<Integer> getPositions(String word, String location) {
		lock.read().lock();
		try {
			return super.getPositions(word, location);
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public String toString() {
		lock.read().lock();
		try {
			return super.toString();
		} finally {
			lock.read().unlock();
		}
	}
	
	@Override
	public void toJson() throws IOException {
		lock.read().lock();
		try {
			LOGGER.debug("Writing index to json...");
			super.toJson();
			LOGGER.debug("Successfully wrote index to json");
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public void toJson(Path path) throws IOException {
		lock.read().lock();
		try {
			LOGGER.debug("Writing index to json...");
			super.toJson(path);
			LOGGER.debug("Successfully wrote index to json");
		} finally {
			lock.read().unlock();
		}
	}

	@Override
	public void wordCountToJson(Path path) throws IOException {
		lock.read().lock();
		try {
			super.wordCountToJson(path);
		} finally {
			lock.read().unlock();
		}
	}
}
