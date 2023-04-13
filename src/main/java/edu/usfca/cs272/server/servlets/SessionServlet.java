package edu.usfca.cs272.server.servlets;

import java.io.IOException;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.core.util.IOUtils;

import edu.usfca.cs272.server.SearchEngineSessionHelper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet to handle http session logic for specific clients
 * @author domin
 *
 */
@SuppressWarnings("serial")
public class SessionServlet extends HttpServlet {
	/** History constant */
	public static final String HISTORY = "history";
	/** Visited constant */
	public static final String VISITED = "visited";
	/** Favorites constant */
	public static final String FAVORITES = "favorites";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String view = req.getParameter("view");
		view = StringEscapeUtils.escapeHtml4(view);
		
		// Initialize session
		HttpSession session = req.getSession();
		SearchEngineSessionHelper.initSession(session);
		String sessionData = "[]";
		// Get data from session
		if (view.equals(HISTORY)) {
			sessionData = SearchEngineSessionHelper.historyToJson(session);
		} else if (view.equals(VISITED)) {
			sessionData = SearchEngineSessionHelper.visitedToJson(session);
		} else if (view.equals(FAVORITES)) {
			sessionData = SearchEngineSessionHelper.favoriteToJson(session);
		}

		res.setContentType("application/json");
		res.setCharacterEncoding("UTF-8");	
		res.getWriter().print(sessionData);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String view = req.getParameter("view");
		view = StringEscapeUtils.escapeHtml4(view);
		
		String body = IOUtils.toString(req.getReader());
		if (body == null) {
			return;
		}
		
		String[] data = body.split("\n");
		if (data.length != 2) {
			return;
		}
		String url = data[0];
		String title = data[1];
		
		HttpSession session = req.getSession();
		SearchEngineSessionHelper.initSession(session);
		if (view.equals(HISTORY)) {
			SearchEngineSessionHelper.addHistory(session, url, title);
		} else if (view.equals(VISITED)) {
			SearchEngineSessionHelper.addVisited(session, url, title);
		} else if (view.equals(FAVORITES)) {
			SearchEngineSessionHelper.addFavorite(session, url, title);
		}
	}
	
	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String view = req.getParameter("view");
		view = StringEscapeUtils.escapeHtml4(view);
		// The only supported DELETE operation is for the favorites data within the session
		if (!view.equals(FAVORITES)) {
			return;
		}
		
		String url = req.getParameter("url");
		url = StringEscapeUtils.escapeHtml4(url);
		
		HttpSession session = req.getSession();
		SearchEngineSessionHelper.initSession(session);
		SearchEngineSessionHelper.removeFavorite(session, url);
	}
}
