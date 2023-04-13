package edu.usfca.cs272.web;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A specialized version of {@link HttpsFetcher} that follows redirects and
 * returns HTML content if possible.
 *
 * @see HttpsFetcher
 *
 * @author CS 272 Software Development (University of San Francisco)
 * @version Fall 2022
 */ 
public class HtmlFetcher {
	/** Constant for content-type header */
	public static final String CONTENT_TYPE = "Content-Type";
	/** Constant for location header */
	public static final String LOCATION = "Location";
	/** Constant for content header */
	public static final String CONTENT = "Content";
	/** Constant for 200 OK status code */
	public static final int OK = 200;
	/**
	 * Returns {@code true} if and only if there is a "Content-Type" header and
	 * the first value of that header starts with the value "text/html"
	 * (case-insensitive).
	 *
	 * @param headers the HTTP/1.1 headers to parse
	 * @return {@code true} if the headers indicate the content type is HTML
	 */
	public static boolean isHtml(Map<String, List<String>> headers) {
		List<String> contentTypeHeader = getHeader(headers, CONTENT_TYPE);
		for (String val : contentTypeHeader) {
			if (val.contains("text/html")) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Parses the HTTP status code from the provided HTTP headers, assuming the
	 * status line is stored under the {@code null} key.
	 *
	 * @param headers the HTTP/1.1 headers to parse
	 * @return the HTTP status code or -1 if unable to parse for any reasons
	 */
	public static int getStatusCode(Map<String, List<String>> headers) {
		List<String> firstHeader = headers.get(null);
		try {
			String line = firstHeader.get(0);
			int statusCodeStart = line.indexOf(' ') + 1;
			int statusCodeEnd = line.indexOf(' ', statusCodeStart);
			return Integer.parseInt(line.substring(statusCodeStart, statusCodeEnd));
		} catch (Exception e) {
			return -1;
		}
	}

	/**
	 * Returns {@code true} if and only if the HTTP status code is between 300 and
	 * 399 (inclusive) and there is a "Location" header with at least one value.
	 *
	 * @param headers the HTTP/1.1 headers to parse
	 * @return {@code true} if the headers indicate the content type is HTML
	 */
	public static boolean isRedirect(Map<String, List<String>> headers) {
		int statusCode = getStatusCode(headers);
		return 300 <= statusCode && statusCode < 400 && getHeader(headers, LOCATION).size() > 0;
	}

	/**
	 * Fetches the resource at the URL using HTTP/1.1 and sockets. If the status
	 * code is 200 and the content type is HTML, returns the HTML as a single
	 * string. If the status code is a valid redirect, will follow that redirect
	 * if the number of redirects is greater than 0. Otherwise, returns
	 * {@code null}.
	 *
	 * @param url the url to fetch
	 * @param redirects the number of times to follow redirects
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 *
	 * @see HttpsFetcher#openConnection(URL)
	 * @see HttpsFetcher#printGetRequest(PrintWriter, URL)
	 * @see HttpsFetcher#getHeaderFields(BufferedReader)
	 *
	 * @see String#join(CharSequence, CharSequence...)
	 *
	 * @see #isHtml(Map)
	 * @see #isRedirect(Map)
	 */
	public static String fetch(URL url, int redirects) {
		String html = null;
		try {
			Map<String, List<String>> headers = HttpsFetcher.fetchUrl(url);
			boolean isHtml = isHtml(headers);
			boolean follow = isRedirect(headers);
			if (!isHtml && !follow) {
				return null;
			}
			int redirectCount = 0;
			while (follow && redirectCount < redirects) {
				redirectCount++;
				url = new URL(getHeader(headers, LOCATION).get(0));
				headers = HttpsFetcher.fetchUrl(url);
				follow = isRedirect(headers);
			}
			if (!isHtml(headers) || getStatusCode(headers) != OK) {
				return null;
			}
			String content = String.join(System.getProperty("line.separator"), getHeader(headers, CONTENT)) + System.getProperty("line.separator");
			return content;
		} catch (Exception e) {
			html = null;
		}

		return html;
	}

	/**
	 * Converts the {@link String} url into a {@link URL} object and then calls
	 * {@link #fetch(URL, int)}.
	 *
	 * @param url the url to fetch
	 * @param redirects the number of times to follow redirects
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 *
	 * @see #fetch(URL, int)
	 */
	public static String fetch(String url, int redirects) {
		try {
			return fetch(new URL(url), redirects);
		}
		catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Converts the {@link String} url into a {@link URL} object and then calls
	 * {@link #fetch(URL, int)} with 0 redirects.
	 *
	 * @param url the url to fetch
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 *
	 * @see #fetch(URL, int)
	 */
	public static String fetch(String url) {
		return fetch(url, 0);
	}

	/**
	 * Calls {@link #fetch(URL, int)} with 0 redirects.
	 *
	 * @param url the url to fetch
	 * @return the html or {@code null} if unable to fetch the resource or the
	 *         resource is not html
	 */
	public static String fetch(URL url) {
		return fetch(url, 0);
	}
	
	/**
	 * Helper function to get header by name
	 * @param headers the headers
	 * @param headerName the name
	 * @return list of values for the specified header
	 */
	private static List<String> getHeader(Map<String, List<String>> headers, String headerName) {
		List<String> header = headers.get(headerName);
		if (header == null) {
			header = headers.getOrDefault(headerName.toLowerCase(), new ArrayList<String>());
		}
		return header;
	}
}
