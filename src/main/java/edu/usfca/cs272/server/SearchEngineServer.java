package edu.usfca.cs272.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import edu.usfca.cs272.index.ThreadedIndexSearcher;
import edu.usfca.cs272.server.servlets.SearchServlet;
import edu.usfca.cs272.server.servlets.SearchViewServlet;
import edu.usfca.cs272.server.servlets.SessionServlet;

/**
 * The web server for the search engine
 * @author domin
 *
 */
public class SearchEngineServer {
	/** Default port 8080 */
	public static int DEFAULT_PORT = 8080;

	/** The port being used **/
	private final int port;
	/** The server instance */
	private final Server server;
	/** The index searcher for the web server */
	private final ThreadedIndexSearcher searcher;
	
	/**
	 * Constructor initializes members
	 * @param port the port to use for server
	 * @param searcher the searcher for the server
	 */
	public SearchEngineServer(int port, ThreadedIndexSearcher searcher) {
		this.port = port;
		this.server = new Server(this.port);
		this.searcher = searcher;
	}
	
	/**
	 * Starts the server
	 * @throws Exception if server failed to start
	 */
	public void start() throws Exception {		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		
		SearchServlet searchServlet = new SearchServlet(this.searcher);
		ServletHolder searchHolder = new ServletHolder(searchServlet);
		
		// API servlets
		context.addServlet(searchHolder, "/api/search");
		context.addServlet(SessionServlet.class, "/api/session");
		// View servlets
		context.addServlet(SearchViewServlet.class, "/*");
		
		HandlerList handlers = new HandlerList();
		handlers.setHandlers(new Handler[] {context, new DefaultHandler()});
		this.server.setHandler(handlers);
		
		this.server.start();
		System.out.println(String.format("Search Engine Server started on port %d", this.port));
		this.server.join();
	}
	
	/**
	 * Shuts down the server
	 * @throws Exception if server failed to shutdown
	 */
	public void shutdown() throws Exception {
		this.server.stop();
	}
}
