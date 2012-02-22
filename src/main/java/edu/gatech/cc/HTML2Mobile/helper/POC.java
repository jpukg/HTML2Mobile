package edu.gatech.cc.HTML2Mobile.helper;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class POC {

	private Map<String, String> cookies;
	private Map<String, String> headers;

	public POC() {
		cookies = new HashMap<String, String>();
		headers = new HashMap<String, String>();
		headers.put("Cache-Control", "no-cache");
		headers.put("Pragma", "no-cache");
	}

	private void handleHeaderCookies(Connection connection) {

		if (connection != null && connection.request() != null && connection.request().cookies() != null) {
			StringBuilder sb = new StringBuilder();
			for (Entry<String, String> cookie : connection.request().cookies().entrySet()) {
				sb.append(cookie.getKey()).append("=").append(cookie.getValue()).append("; ");
			}
			if (sb.length() > 0) {
				sb.delete(sb.length()-2, sb.length());
			}
			headers.put("Cookie", sb.toString());
		}
	}

	public Document send(String url, Map<String, String> data, boolean updateHeaderCookies) throws Exception {
		return send(url, data, Method.GET, updateHeaderCookies);
	}

	public Document send(String url, Map<String, String> data, Method meth, boolean updateHeaderCookies) throws Exception {
		Connection connection = Jsoup.connect(url).followRedirects(true).method(meth);
		for (Entry<String, String> cookie : cookies.entrySet()) {
			connection.cookie(cookie.getKey(), cookie.getValue());
		}
		for (Entry<String, String> header : headers.entrySet()) {
			connection.header(header.getKey(), header.getValue());
		}
		System.out.println(url);
		System.out.println("\t"+cookies);
		System.out.println("\t"+headers);

		if (data != null) {
			for (Entry<String, String> me : data.entrySet()) {
				connection.data(me.getKey(), me.getValue());
			}
		}
		Response response = connection.execute();

		cookies.putAll(response.cookies());
		if (updateHeaderCookies) {
			handleHeaderCookies(connection);
		}
		//	    headers.putAll(response.headers());
		return response.parse();
	}

	public Document post(String url, Map<String, String> data) {
		Connection connection = Jsoup.connect(url).followRedirects(true);
		for (Entry<String, String> cookie : cookies.entrySet()) {
			connection.cookie(cookie.getKey(), cookie.getValue());
		}
		for (Entry<String, String> header : headers.entrySet()) {
			connection.header(header.getKey(), header.getValue());
		}
		if (data != null) {
			for (Entry<String, String> me : data.entrySet()) {
				connection.data(me.getKey(), me.getValue());
			}
		}
		try {
			Response response = connection.execute();
			cookies.putAll(response.cookies());
			//		    headers.putAll(response.headers());
			return response.parse();
		} catch (Exception e) {
			try {
				String[] arr = e.getMessage().split(" ");
				String newUrl = "https://t-square.gatech.edu/portal";
				connection = Jsoup.connect(newUrl).followRedirects(true);
				for (Entry<String, String> cookie : cookies.entrySet()) {
					connection.cookie(cookie.getKey(), cookie.getValue());
				}
				for (Entry<String, String> header : headers.entrySet()) {
					connection.header(header.getKey(), header.getValue());
				}
				Response response = connection.execute();
				cookies.putAll(response.cookies());
				//			    headers.putAll(response.headers());
				return response.parse();
			} catch (Exception j) { j.printStackTrace(); }
			return null;
		}
	}

	public POC deepCopy() {
		POC newPOC = new POC();
		Map<String, String> nCookies = new HashMap<String, String>();
		Map<String, String> nHeaders = new HashMap<String, String>();
		nCookies.putAll(cookies);
		nHeaders.putAll(headers);
		newPOC.cookies = nCookies;
		newPOC.headers = nHeaders;
		return newPOC;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Map<String, String> data = new HashMap<String,String>();
		// You will need to do this on the first run... or get it through command line...
		// SemiSecurePassword.storePassword("<<GTusername>>", "<<password>>");
		try {
			POC facesHandler = new POC();
			// Goto T-Square
			Document doc = facesHandler.send("http://t-square.gatech.edu/", null, true);
			Elements eles = doc.select("a:containsOwn(Login)");
			// Goto Login
			doc = facesHandler.send(eles.get(0).attr("href"), null, true);
			eles = doc.select("form[id=fm1]");
			// Submit login form
			String loginAction = doc.baseUri();
			data.put("username", "desposito6");
			data.put("password", "");
			data.put("warn", "true");
			data.put("lt","e1s1");
			data.put("_eventId","submit");

			// Submit login form
			doc = facesHandler.post(loginAction, data);

			// Get T-Square Course Links
			eles = doc.select("a[title~=[A-Z]{2} ?- ?\\d{4}.*]");

			//			new TSquareClassExplorer(eles.get(0).attr("title"), eles.get(0).attr("href"), facesHandler.deepCopy()).start();

			//			for (TSquareClassExplorer t : TSquareClassExplorer.allTSquareClasses)
			//				t.join();


		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}