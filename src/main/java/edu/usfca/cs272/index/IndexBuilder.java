package edu.usfca.cs272.index;

import static opennlp.tools.stemmer.snowball.SnowballStemmer.ALGORITHM.ENGLISH;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.usfca.cs272.io.TextFileTraverser;
import edu.usfca.cs272.io.WordCleaner;
import edu.usfca.cs272.threading.WorkQueue;
import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.stemmer.snowball.SnowballStemmer;

/**
 * Class responsible for building a {@link InvertedIndex} data structure
 * @author Dominic Fernandez
 *
 */
public class IndexBuilder {
	/** Logger used for debugging */
	private static final Logger LOGGER = LogManager.getRootLogger();
		/**
		 * Task to be run by the TaskManager
		 * @author domin
		 *
		 */
		private static class Task implements Runnable {
			/** The path to process */
			private final Path path;
			/** The index to use */
			private final ThreadSafeInvertedIndex index;
			 /**
			  * Constructor increments number of pending tasks and assigns the path member
			  * @param path The path to process
			  * @param index The inverted index to build
			  */
			public Task(Path path, ThreadSafeInvertedIndex index) {
				this.path = path;
				this.index = index;
				LOGGER.debug("Created IndexBuilder task for {}", path);
			}
			
			@Override
			public void run() {
				try {
					InvertedIndex local = new InvertedIndex();
					addStems(path, local);
					index.addAll(local);
				} catch (IOException e) {
					LOGGER.error("Exception occurred in IndexBuilder task", e);
				} finally {
					LOGGER.debug("Completed IndexBuilder task for {}", path);
				}
			}
		}
	/**
	 * Builds the index 
	 * @param textPath The {@link Path} to the file or directory to build the index from
	 * @param index The {@link InvertedIndex} to build
	 * @throws Exception If the index failed to build
	 */
	public static void build(Path textPath, InvertedIndex index) throws Exception {
		buildHelper(textPath, index);
	}
	
	/**
	 * Builds the index with multiple threads
	 * @param textPath The {@link Path} to the file or directory to build the index from
	 * @param index The {@link InvertedIndex} to build
	 * @param queue The WorkQueue used to multi-thread the build
	 * @throws Exception If the index failed to build
	 */
	public static void build(Path textPath, ThreadSafeInvertedIndex index, WorkQueue queue) throws Exception {
		buildHelper(textPath, index, queue);
	}
	
	/**
	 * Private helper method to build the index
	 * @param textPath Path to build the index from
	 * @param index The index to build
	 * @throws Exception If failed to build the index
	 */
	private static void buildHelper(Path textPath, InvertedIndex index) throws Exception {
		boolean isPathDir = Files.isDirectory(textPath);
		// If isPathDir, need to traverse directory and sub-directories
		if (isPathDir) {
			Stream<Path> textFileStream = TextFileTraverser.readAllTextFiles(textPath);
			textFileStream.forEach(filePath -> {
				try {
					addStems(filePath, index);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} else {
			addStems(textPath, index);
		}
	}
	
	/**
	 * Private helper method to build the index
	 * @param textPath Path to build the index from
	 * @param index The index to build
	 * @param queue the WorkQueue used to multi-thread the build
	 * @throws Exception If failed to build the index
	 */
	private static void buildHelper(Path textPath, ThreadSafeInvertedIndex index, WorkQueue queue) throws Exception {
		boolean isPathDir = Files.isDirectory(textPath);
		// If isPathDir, need to traverse directory and sub-directories
		if (isPathDir) {
			Stream<Path> textFileStream = TextFileTraverser.readAllTextFiles(textPath);
			textFileStream.forEach(filePath -> {
				// Creates a new Runnable task for each textPath
				queue.execute(new Task(filePath, index));
			});
		} else {
			addStems(textPath, index);
		}
	}
	
	/**
	 * Helper function to get and add the word stems to the index 
	 * @param path The location of the data to read 
	 * @param index the {@link InvertedIndex} being built
	 * @throws IOException If @param path could not be read
	 */
	public static void addStems(Path path, InvertedIndex index) throws IOException {
		Stemmer stemmer = new SnowballStemmer(ENGLISH);
		String location = path.toString();
		try (BufferedReader br = Files.newBufferedReader(path)) {
			String line;
			int pos = 0;
			while((line = br.readLine()) != null) {
				String[] words = WordCleaner.parse(line);
				for(int i = 0; i < words.length; i++) {
					String stem = stemmer.stem(words[i]).toString();
					if (stem.isBlank()) {
						continue;
					}
					index.addWord(stem, location, ++pos);
				}
			}
		}
	}
	
	/**
	 * Helper function to get and add the word stems to the index 
	 * @param text the text to parse
	 * @param location The location of the data to read 
	 * @param index the {@link InvertedIndex} being built
	 * @throws IOException If @param path could not be read
	 */
	public static void addStems(String text, String location, InvertedIndex index) throws IOException {
		Stemmer stemmer = new SnowballStemmer(ENGLISH);
		try (BufferedReader br = new BufferedReader(new StringReader(text));) {
			String line;
			int pos = 0;
			while((line = br.readLine()) != null) {
				String[] words = WordCleaner.parse(line);
				for(int i = 0; i < words.length; i++) {
					String stem = stemmer.stem(words[i]).toString();
					if (stem.isBlank()) {
						continue;
					}
					index.addWord(stem, location, ++pos);
				}
			}
		}
	}
}
