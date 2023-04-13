package edu.usfca.cs272.io;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.usfca.cs272.index.InvertedIndex.SearchResult;

/**
 * Outputs several simple data structures in "pretty" JSON format where newlines
 * are used to separate elements and nested elements are indented using spaces.
 *
 * Warning: This class is not thread-safe. If multiple threads access this class
 * concurrently, access must be synchronized externally.
 *
 * @author CS 272 Software Development (University of San Francisco)
 * @version Fall 2022
 */
public class PrettyJsonWriter {
	/**
	 * Open brace constant
	 */
	public static final String OPEN_BRACE = "[";
	/**
	 * Close brace constant
	 */
	public static final String CLOSE_BRACE = "]";
	/**
	 * Open curly brace constant
	 */
	public static final String OPEN_CURLY_BRACE = "{";
	/**
	 * Close burly brace constant
	 */
	public static final String CLOSE_CURLY_BRACE = "}";
	/**
	 * New line constant
	 */
	public static final String NEW_LINE = "\n";
	/**
	 * Comma constant
	 */
	public static final String COMMA = ",";
	/**
	 * Colon constant
	 */
	public static final String COLON = ": ";
	
	/**
	 * Indents the writer by the specified number of times. Does nothing if the
	 * indentation level is 0 or less.
	 *
	 * @param writer the writer to use
	 * @param indent the number of times to indent
	 * @throws IOException if an IO error occurs
	 */
	public static void writeIndent(Writer writer, int indent) throws IOException {
		while (indent-- > 0) {
			writer.write("  ");
		}
	}

	/**
	 * Indents and then writes the String element.
	 *
	 * @param element the element to write
	 * @param writer the writer to use
	 * @param indent the number of times to indent
	 * @throws IOException if an IO error occurs
	 */
	public static void writeIndent(String element, Writer writer, int indent) throws IOException {
		writeIndent(writer, indent);
		writer.write(element);
	}

	/**
	 * Indents and then writes the text element surrounded by {@code " "}
	 * quotation marks.
	 *
	 * @param element the element to write
	 * @param writer the writer to use
	 * @param indent the number of times to indent
	 * @throws IOException if an IO error occurs
	 */
	public static void writeQuote(String element, Writer writer, int indent) throws IOException {
		writeIndent(writer, indent);
		writer.write('"');
		writer.write(element);
		writer.write('"');
	}

	/**
	 * Writes the elements as a pretty JSON array.
	 *
	 * @param elements the elements to write
	 * @param writer the writer to use
	 * @param indent the initial indent level; the first bracket is not indented,
	 *   inner elements are indented by one, and the last bracket is indented at
	 *   the initial indentation level
	 * @throws IOException if an IO error occurs
	 *
	 * @see Writer#write(String)
	 * @see #writeIndent(Writer, int)
	 * @see #writeIndent(String, Writer, int)
	 */
	public static void writeArray(Collection<? extends Number> elements,
			Writer writer, int indent) throws IOException {
		Iterator<? extends Number> iterator = elements.iterator();
		
		if (elements.isEmpty()) return;
		String element = iterator.next().toString();
		
		writer.write(OPEN_BRACE);
		writer.write(NEW_LINE);
		writeIndent(writer, indent+1);
		writer.write(element);
		
		while (iterator.hasNext()) {
			element = iterator.next().toString();
			writer.write(COMMA);
			writer.write(NEW_LINE);
			writeIndent(writer, indent+1);
			writer.write(element);
		}
		
		writer.write(NEW_LINE);
		writeIndent(writer, indent);
		writer.write(CLOSE_BRACE);
	}

	/**
	 * Writes the elements as a pretty JSON array to file.
	 *
	 * @param elements the elements to write
	 * @param path the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see Files#newBufferedReader(Path, java.nio.charset.Charset)
	 * @see StandardCharsets#UTF_8
	 * @see #writeArray(Collection, Writer, int)
	 */
	public static void writeArray(Collection<? extends Number> elements,
			Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
			writeArray(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON array.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see StringWriter
	 * @see #writeArray(Collection, Writer, int)
	 */
	public static String writeArray(Collection<? extends Number> elements) {
		try {
			StringWriter writer = new StringWriter();
			writeArray(elements, writer, 0);
			return writer.toString();
		}
		catch (IOException e) {
			return null;
		}
	}

	/**
	 * Writes the elements as a pretty JSON object.
	 *
	 * @param elements the elements to write
	 * @param writer the writer to use
	 * @param indent the initial indent level; the first bracket is not indented,
	 *   inner elements are indented by one, and the last bracket is indented at
	 *   the initial indentation level
	 * @throws IOException if an IO error occurs
	 *
	 * @see Writer#write(String)
	 * @see #writeIndent(Writer, int)
	 * @see #writeIndent(String, Writer, int)
	 */
	public static void writeObject(Map<String, ? extends Number> elements,
			Writer writer, int indent) throws IOException {
		writer.write(OPEN_CURLY_BRACE);
		var iterator = elements.entrySet().iterator();
		if (!elements.isEmpty()) {
			writer.write(NEW_LINE);
			var element = iterator.next();
			String key = element.getKey();
			var value = element.getValue();
			
			writeQuote(key, writer, indent+1);
			writer.write(COLON);
			writeValue(value, writer);
			
			while (iterator.hasNext()) {
				writer.write(COMMA);
				writer.write(NEW_LINE);
				element = iterator.next();
				key = element.getKey();
				value = element.getValue();
				writeQuote(key, writer, indent+1);
				writer.write(COLON);
				writeValue(value, writer);
			}
		}
		writeIndent(writer, indent);
		writer.write(NEW_LINE);
		writer.write(CLOSE_CURLY_BRACE);
	}

	/**
	 * Writes the elements as a pretty JSON object to file.
	 *
	 * @param elements the elements to write
	 * @param path the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see Files#newBufferedReader(Path, java.nio.charset.Charset)
	 * @see StandardCharsets#UTF_8
	 * @see #writeObject(Map, Writer, int)
	 */
	public static void writeObject(Map<String, ? extends Number> elements,
			Path path) throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
			writeObject(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON object.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see StringWriter
	 * @see #writeObject(Map, Writer, int)
	 */
	public static String writeObject(Map<String, ? extends Number> elements) {
		try {
			StringWriter writer = new StringWriter();
			writeObject(elements, writer, 0);
			return writer.toString();
		}
		catch (IOException e) {
			return null;
		}
	}

	/**
	 * Writes the elements as a pretty JSON object with nested arrays. The generic
	 * notation used allows this method to be used for any type of map with any
	 * type of nested collection of number objects.
	 *
	 * @param elements the elements to write
	 * @param writer the writer to use
	 * @param indent the initial indent level; the first bracket is not indented,
	 *   inner elements are indented by one, and the last bracket is indented at
	 *   the initial indentation level
	 * @throws IOException if an IO error occurs
	 *
	 * @see Writer#write(String)
	 * @see #writeIndent(Writer, int)
	 * @see #writeIndent(String, Writer, int)
	 * @see #writeArray(Collection)
	 */
	public static void writeNestedArrays(
			Map<String, ? extends Collection<? extends Number>> elements,
			Writer writer, int indent) throws IOException {
		var iterator = elements.entrySet().iterator();
		
		if (elements.isEmpty()) return;
		
		var element = iterator.next();
		String key = element.getKey();
		var value = element.getValue();
		
		writer.write(OPEN_CURLY_BRACE);
		writer.write(NEW_LINE);
		
		writeQuote(key, writer, indent+1);
		writer.write(COLON);
		writeArray(value, writer, indent+1);
	
		while (iterator.hasNext()) {
			element = iterator.next();
			key = element.getKey();
			value = element.getValue();
			writer.write(COMMA);
			writer.write(NEW_LINE);
			writeQuote(key, writer, indent+1);
			writer.write(COLON);
			writeArray(value, writer, indent+1);
		}
		writer.write(NEW_LINE);
		writeIndent(writer, indent);
		writer.write(CLOSE_CURLY_BRACE);
	}

	/**
	 * Writes the elements as a pretty JSON object with nested arrays to file.
	 *
	 * @param elements the elements to write
	 * @param path the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see Files#newBufferedReader(Path, java.nio.charset.Charset)
	 * @see StandardCharsets#UTF_8
	 * @see #writeNestedArrays(Map, Writer, int)
	 */
	public static void writeNestedArrays(
			Map<String, ? extends Collection<? extends Number>> elements, Path path)
			throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
			writeNestedArrays(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON object with nested arrays.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see StringWriter
	 * @see #writeNestedArrays(Map, Writer, int)
	 */
	public static String writeNestedArrays(
			Map<String, ? extends Collection<? extends Number>> elements) {
		try {
			StringWriter writer = new StringWriter();
			writeNestedArrays(elements, writer, 0);
			return writer.toString();
		}
		catch (IOException e) {
			return null;
		}
	}

	/**
	 * Writes the elements as a pretty JSON array with nested objects. The generic
	 * notation used allows this method to be used for any type of collection with
	 * any type of nested map of String keys to number objects.
	 *
	 * @param elements the elements to write
	 * @param writer the writer to use
	 * @param indent the initial indent level; the first bracket is not indented,
	 *   inner elements are indented by one, and the last bracket is indented at
	 *   the initial indentation level
	 * @throws IOException if an IO error occurs
	 *
	 * @see Writer#write(String)
	 * @see #writeIndent(Writer, int)
	 * @see #writeIndent(String, Writer, int)
	 * @see #writeObject(Map)
	 */
	public static void writeNestedObjects(
			Collection<? extends Map<String, ? extends Number>> elements,
			Writer writer, int indent) throws IOException {
		var iterator = elements.iterator();
		writer.write(OPEN_BRACE);
		writer.write(NEW_LINE);
		while (iterator.hasNext() && !elements.isEmpty()) {
			var map = iterator.next();
			writeIndent(writer, indent+1);
			writeObject(map, writer, indent+1);
			if (iterator.hasNext())
				writer.write(COMMA);
			writer.write(NEW_LINE);
		}
		writeIndent(writer, indent);
		writer.write(CLOSE_BRACE);
	}

	/**
	 * Writes the elements as a pretty JSON array with nested objects to file.
	 *
	 * @param elements the elements to write
	 * @param path the file path to use
	 * @throws IOException if an IO error occurs
	 *
	 * @see Files#newBufferedReader(Path, java.nio.charset.Charset)
	 * @see StandardCharsets#UTF_8
	 * @see #writeNestedObjects(Collection)
	 */
	public static void writeNestedObjects(
			Collection<? extends Map<String, ? extends Number>> elements, Path path)
			throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(path, UTF_8)) {
			writeNestedObjects(elements, writer, 0);
		}
	}

	/**
	 * Returns the elements as a pretty JSON array with nested objects.
	 *
	 * @param elements the elements to use
	 * @return a {@link String} containing the elements in pretty JSON format
	 *
	 * @see StringWriter
	 * @see #writeNestedObjects(Collection)
	 */
	public static String writeNestedObjects(
			Collection<? extends Map<String, ? extends Number>> elements) {
		try {
			StringWriter writer = new StringWriter();
			writeNestedObjects(elements, writer, 0);
			return writer.toString();
		}
		catch (IOException e) {
			return null;
		}
	}
	
	/**
	 * Writes an index to a file
	 * @param index The TreeMap representing the index to convert to pretty JSON
	 * @param writer The writer to use to write the index
	 * @param indent The initial indent amount
	 * @throws IOException If the underlying {@link StringWriter} object fails
	 */
	public static void indexToJson(Map<String, ? extends Map<String, ? extends Collection<? extends Number>>> index, Writer writer, int indent) throws IOException {
		writer.write(OPEN_CURLY_BRACE);
		
		var iterator = index.entrySet().iterator();
		if (iterator.hasNext()) {
			writer.write(NEW_LINE);
			var element = iterator.next();
			
			String key = element.getKey();
			var value = element.getValue();
			
			writeQuote(key, writer, indent+1);
			writer.write(COLON);
			writeNestedArrays(value, writer, indent+1);
			
			while (iterator.hasNext()) {
				element = iterator.next();
				key = element.getKey();
				value = element.getValue();
				writer.write(COMMA);
				writer.write(NEW_LINE);
				writeQuote(key, writer, indent+1);
				writer.write(COLON);
				writeNestedArrays(value, writer, indent+1);
			}
		}
		writer.write(NEW_LINE);
		writer.write(CLOSE_CURLY_BRACE);
	}
	
	/**
	 * Writes the word count to json file
	 * @param wordCount the wordCount data TreeMap data structure
	 * @param writer the writer to use to write the json
	 * @param indent initial indent
	 * @throws IOException if failed to write
	 */
	public static void wordCountToJson(Map<String, ? extends Number> wordCount, Writer writer, int indent) throws IOException {
		writeObject(wordCount, writer, indent);
	}
	
	/**
	 * Writes a list of SearchResult objects to JSON
	 * @param key The key of the SearchResult list 
	 * @param results The list of SearchResult objects
	 * @param writer The writer to write to
	 * @param indent How much to indent
	 * @throws IOException if failed to write
	 */
	public static void searchResultListToJson(String key, List<SearchResult> results, Writer writer, int indent) throws IOException {
		writeQuote(key, writer, indent+1);
		writer.write(COLON);
		writer.write(OPEN_BRACE);
		
		var iterator = results.iterator();
		if (results.size() > 0) {
			writer.write(NEW_LINE);
			SearchResult result = iterator.next();
			result.toJson(writer, indent+1, false);
		}
		while (iterator.hasNext()) {
			writer.write(COMMA);
			writer.write(NEW_LINE);
			SearchResult result = iterator.next();
			result.toJson(writer, indent+1, false);
		}
		writer.write(NEW_LINE);
		writeIndent(writer, indent+1);
		writer.write(CLOSE_BRACE);
	}
	
	/**
	 * Writes search results to json file
	 * @param searchResults the Map data structure containing the search results to write
	 * @param writer the writer to output the results to
	 * @param indent the initial indent
	 * @throws IOException if failed to write
	 */
	public static void searchResultsToJson(Map<String, List<SearchResult>> searchResults, Writer writer, int indent) throws IOException {
		writer.write(OPEN_CURLY_BRACE);
		writer.write(NEW_LINE);
		var iterator = searchResults.entrySet().iterator();
		if (!searchResults.isEmpty()) {
			var element = iterator.next();
			String key = element.getKey();
			var value = element.getValue();
			
			searchResultListToJson(key, value, writer, indent);
			
			while (iterator.hasNext()) {
				element = iterator.next();
				key = element.getKey();
				value = element.getValue();
				writer.write(COMMA);
				writer.write(NEW_LINE);
				
				searchResultListToJson(key, value, writer, indent);
			}
		}
		writer.write(NEW_LINE);
		writer.write(CLOSE_CURLY_BRACE);
	}
	
	/**
	 * Helper function to write a key's value
	 * @param value The value to write
	 * @param writer The writer to write the value to
	 * @throws IOException If failed to write
	 */
	private static void writeValue(Object value, Writer writer) throws IOException {
		String writeVal = value.toString();
		if (value instanceof Number)
			writer.write(writeVal);
		else
			writeQuote(writeVal, writer, 0);
	}
}
