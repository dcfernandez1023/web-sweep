package edu.usfca.cs272.server;

import java.util.Date;
import java.util.HashMap;

import jakarta.servlet.http.HttpSession;

/**
 * Helper for Search Engine http sessions
 * @author domin
 *
 */
public class SearchEngineSessionHelper {
	/** Constant for created */
	public static final String CREATED = "created";
	/** Constant for user data */
	public static final String USER_DATA = "userData";
	
	/**
	 * Inner class to hold user's data in session
	 * @author domin
	 *
	 */
	private static class UserData {
		/** User's history */
		private HashMap<String, UrlStore> history;
		/** User's visited URLs */
		private HashMap<String, UrlStore> visited;
		/** User's favorites */
		private HashMap<String, UrlStore> favorites;
		
		/** 
		 * Inner class to hold data under each user's category
		 * @author domin
		 *
		 */
		private static class UrlStore {
			/** The url */
			private String url;
			/** The title */
			private String title;
			/** The timestamp */
			private long timestamp;
			/** Whether or not this is favorited by the user */
			private boolean isFavorite;
			
			/**
			 * Constructor initializes members
			 * @param url the url
			 * @param title the title
			 */
			private UrlStore(String url, String title) {
				this.url = url;
				this.title = title;
				this.timestamp = System.currentTimeMillis();
				this.isFavorite = false;
			}
			
			/**
			 * To json method to return to client
			 * @return json string
			 */
			private String toJson() {
				StringBuilder sb = new StringBuilder();
				sb.append(String.format("{\"url\": \"%s\",", this.url));
				sb.append(String.format("\"title\": \"%s\",", this.title));
				sb.append(String.format("\"isFavorite\": %b,", this.isFavorite));
				sb.append(String.format("\"timestamp\": %d}", this.timestamp));
				return sb.toString();
			}
			
			@Override
			public String toString() {
				return this.toJson();
			}
		}
		
		/**
		 * Constructor initializes members
		 */
		private UserData() {
			this.history = new HashMap<>();
			this.visited = new HashMap<>();
			this.favorites = new HashMap<>();
		}
		
		/**
		 * Adds history
		 * @param query query
 		 * @param title title
		 */
		private void addHistory(String query, String title) {
			this.history.put(query, new UrlStore(query, title));
		}
		
		/**
		 * Adds visited
		 * @param url url
		 * @param title title
		 */
		private void addVisited(String url, String title) {
			this.visited.put(url, new UrlStore(url, title));
		}
		
		/**
		 * Adds a favorite
		 * @param url url
		 * @param title title
		 */
		private void addFavorite(String url, String title) {
			this.favorites.put(url, new UrlStore(url, title));
			if (this.history.containsKey(url)) {
				this.history.get(url).isFavorite = true;
			} 
			if (this.visited.containsKey(url)) {
				this.visited.get(url).isFavorite = true;
			}
		}
		
		/**
		 * Removes a favorite
		 * @param url url
		 */
		private void removeFavorite(String url) {
			this.favorites.remove(url);
			if (this.history.containsKey(url)) {
				this.history.get(url).isFavorite = false;
			}
			if (this.visited.containsKey(url)) {
				this.visited.get(url).isFavorite = false;
			}
		}
		
		/**
		 * To json method 
		 * @return String
		 */
		private String historyToJson() {
			return this.dataToJson(this.history);
		}
		/**
		 * To json method 
		 * @return String
		 */
		private String visitedToJson() {
			return this.dataToJson(this.visited);
		}
		/**
		 * To json method 
		 * @return String
		 */
		private String favoritesToJson() {
			return this.dataToJson(this.favorites);
		}
		/**
		 * To json helper method 
		 * @param data the data to turn to json
		 * @return String
		 */
		private String dataToJson(HashMap<String, UrlStore> data) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			var iterator = data.entrySet().iterator();
			if (iterator.hasNext()) {
				sb.append(iterator.next().getValue().toJson());
			}
			while (iterator.hasNext()) {
				sb.append(",");
				sb.append(iterator.next().getValue().toJson());
			}
			sb.append("]");
			return sb.toString();
		}
	}
	
	/**
	 * Initializes http session
	 * @param session the session to initialize
	 */
	public static void initSession(HttpSession session) {
		if (session.getAttribute(CREATED) == null) {
			session.setAttribute(CREATED, new Date());
		}
		if (session.getAttribute(USER_DATA) == null) {
			session.setAttribute(USER_DATA, new UserData());
		}
	}
	
	/**
	 * Add history
	 * @param session the session
	 * @param query the query
	 * @param title the title
	 */
	public static void addHistory(HttpSession session, String query, String title) {
		UserData userData = getUserDataFromSession(session);
		userData.addHistory(query, title);
	}
	/**
	 * Add visited
	 * @param session the session
	 * @param url the query
	 * @param title the title
	 */
	public static void addVisited(HttpSession session, String url, String title) {
		UserData userData = getUserDataFromSession(session);
		userData.addVisited(url, title);
	}
	/**
	 * Add favorite
	 * @param session the session
	 * @param url the query
	 * @param title the title
	 */
	public static void addFavorite(HttpSession session, String url, String title) {
		UserData userData = getUserDataFromSession(session);
		userData.addFavorite(url, title);
	}
	/**
	 * Removes favorite
	 * @param session the session
	 * @param url the url
	 */
	public static void removeFavorite(HttpSession session, String url) {
		UserData userData = getUserDataFromSession(session);
		userData.removeFavorite(url);
	}
	/**
	 * to json
	 * @param session the session
	 * @return json string
	 */
	public static String historyToJson(HttpSession session) {
		UserData userData = getUserDataFromSession(session);
		return userData.historyToJson();
	}
	/**
	 * to json
	 * @param session the session
	 * @return json string
	 */
	public static String visitedToJson(HttpSession session) {
		UserData userData = getUserDataFromSession(session);
		return userData.visitedToJson();
	}
	/**
	 * to json
	 * @param session the session
	 * @return json string
	 */
	public static String favoriteToJson(HttpSession session) {
		UserData userData = getUserDataFromSession(session);
		return userData.favoritesToJson();
	}
	/**
	 * Helper method to get user data from session
	 * @param session the session
	 * @return UserData
	 */
	private static UserData getUserDataFromSession(HttpSession session) {
		Object userData = session.getAttribute(USER_DATA);
		if (userData == null) {
			userData = new UserData();
			session.setAttribute(USER_DATA, userData);
		}
		return (UserData) userData;
	}
	
}
