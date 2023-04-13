package edu.usfca.cs272.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Class responsible for reading and writing the files for the search engine
 * @author Dominic Fernandez
 *
 */
public class TextFileTraverser {
	/**
	 * Constant for .txt file extensions
	 */
	public static final String TXT_EXT = ".txt";
	/**
	 * Constant for .text file extensions
	 */
	public static final String TEXT_EXT = ".text";
	
	/**
	 * Reads all .txt and .text files starting from the directory specified by @param startPath
	 * @param startPath The directory to start reading from
	 * @return a {@link Stream} of all the text file paths
	 * @throws Exception If the files are failed to be read
	 */
	public static Stream<Path> readAllTextFiles(Path startPath) throws Exception {
		return Files.walk(startPath).filter((Path path) -> isTextFile(path));
	}
	
	/**
	 * Helper function to determine if a file is a .txt or .text file
	 * @param path The path to the file
	 * @return true if is a text file, false otherwise
	 */
	public static boolean isTextFile(Path path) {
		String lower = path.toString().toLowerCase();
		return (lower.endsWith(TXT_EXT) || lower.endsWith(TEXT_EXT)) && Files.isRegularFile(path);
	}
}
