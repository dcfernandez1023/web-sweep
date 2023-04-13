package edu.usfca.cs272.server.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.text.StringEscapeUtils;

import edu.usfca.cs272.index.ThreadedIndexSearcher;
import edu.usfca.cs272.index.InvertedIndex.SearchResult;
import edu.usfca.cs272.server.SearchEngineSessionHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet handling logic for returning search results
 * @author domin
 *
 */
@SuppressWarnings("serial")
public class SearchServlet extends HttpServlet {	
	/** Index searcher to use*/
	private final ThreadedIndexSearcher searcher;
	
	/**
	 * Constructor initializes members
	 * @param searcher the searcher to use
	 */
	public SearchServlet(ThreadedIndexSearcher searcher) {
		this.searcher = searcher;
	}
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String query = req.getParameter("q");
		query = StringEscapeUtils.escapeHtml4(query);
		
		HttpSession session = req.getSession();
		SearchEngineSessionHelper.initSession(session);

		List<SearchResult> results;
		if (query == null || query.isBlank()) {
			results = new ArrayList<>();
		} else {
			query = query.toLowerCase().strip();
			results = this.searcher.getResults(query, false);
		}
		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");	
		res.getWriter().print(results.toString());
	}
}
