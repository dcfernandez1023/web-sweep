package com.crawler;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * An alternative to using {@link Socket} connections instead of a
 * {@link URLConnection} to fetch the headers and content from a URL on the web.
 */
public class HttpsFetcher {
	/**
	 * Fetches the headers and content for the specified URL. The content is
	 * placed as a list of all the lines fetched under the "Content" key.
	 *
	 * @param url the url to fetch
	 * @return a map with the headers and content
	 * @throws IOException if unable to fetch headers and content
	 * @throws InterruptedException 
	 */
	public static HttpResponse<String> fetchUrl(URL url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .build();
        HttpResponse<String> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
        return httpResponse;
	}
}