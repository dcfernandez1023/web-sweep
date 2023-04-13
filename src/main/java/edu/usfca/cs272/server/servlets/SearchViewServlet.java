package edu.usfca.cs272.server.servlets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringEscapeUtils;

/**
 * Servlet to handle serving static files
 * @author domin
 *
 */
@SuppressWarnings("serial")
public class SearchViewServlet extends HttpServlet {
	/** View base constant. Where the static files are from */
	public static final String VIEW_BASE = "src/main/resources/static/";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		String query = req.getParameter("q");
		query = StringEscapeUtils.escapeHtml4(query);
		String pathInfo = req.getPathInfo();
		String content;
		// Serve default html at "/" route
		if (pathInfo.equals("/")) {
			content = Files.readString(Path.of(VIEW_BASE + "index.html"));
		} else { // Attempt to resolve the file at any other route
			content = Files.readString(Path.of(VIEW_BASE + pathInfo));
		}
		res.setContentType("text/html");
		res.getWriter().write(content);
	}
}
