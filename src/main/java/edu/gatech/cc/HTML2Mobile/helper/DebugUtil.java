package edu.gatech.cc.HTML2Mobile.helper;

import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResourceLinkHeaders;

/** Misc methods to dump debug info. */
public class DebugUtil {


	private DebugUtil() {}

	/**
	 * Dump client response info to standard out.
	 * 
	 * @param r the response
	 */
	public static void dumpClientResponse(ClientResponse r) {
		System.out.println("\nCLIENT RESPONSE:");

		Date lastMod = null;
		try {
			lastMod = r.getLastModified();
		} catch( IllegalArgumentException e ) {

		}
		System.out.println(String.format("location=%s%n" +
				"type=%s%n" +
				"status=%s%n" +
				"allow=%s",
				r.getLocation(),
				r.getType(),
				r.getClientResponseStatus(),
				r.getAllow()
				));
		if( lastMod != null ) {
			System.out.println("modified=" + lastMod);
		}

		System.out.println("properties=");
		for( Map.Entry<String, Object> entry : r.getProperties().entrySet() ) {
			System.out.println("  " + entry.getKey() + "=" + entry.getValue());
		}

		WebResourceLinkHeaders links = r.getLinks();
		System.out.println("links=" + links);

		System.out.println("headers=");
		MultivaluedMap<String, String> h = r.getHeaders();
		for( Map.Entry<String, List<String>> entry : h.entrySet() ) {
			System.out.println("  " + entry.getKey() + "=" + entry.getValue());
		}

		System.out.println("cookies=");
		for( NewCookie cookie : r.getCookies() ) {
			System.out.println("  " + newCookieToString(cookie));
		}
	}

	/**
	 * Dump servlet request info to standard out.
	 * @param req the request
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void dumpRequestInfo(HttpServletRequest req) {
		System.out.println("\nHTTP-SERVLET-REQUEST:");
		System.out.println(String.format("method=%s%n" +
				"context-path=%s%n" +
				"path-info=%s%n" +
				"path-translated=%s%n" +
				"protocol=%s%n" +
				"req-uri=%s%n" +
				"query=%s",
				req.getMethod(),
				req.getContextPath(),
				req.getPathInfo(),
				req.getPathTranslated(),
				req.getProtocol(),
				req.getRequestURI(),
				req.getQueryString()
				));
		System.out.println("headers=");
		for( Enumeration<String> headerNames = req.getHeaderNames(); headerNames.hasMoreElements(); ) {
			String header = headerNames.nextElement();
			System.out.println("  " + header + ": " + req.getHeader(header));
		}

		System.out.println("params=");
		for( Map.Entry<String, String[]> entry : (Set<Map.Entry>)req.getParameterMap().entrySet() ) {
			System.out.println("  " + entry.getKey() + "=" + Arrays.toString(entry.getValue()));
		}
	}

	public static String newCookieToString(NewCookie newCookie) {
		return "name=" + newCookie.getName() + ", value=" + newCookie.getValue() +
				", domain=" + newCookie.getDomain() + ", path=" + newCookie.getPath() + ", comment=" + newCookie.getComment() +
				", maxage=" + newCookie.getMaxAge() + ", version=" + newCookie.getVersion() + ", secure=" + newCookie.isSecure();
	}

	public static String servletCookieToString(Cookie cookie) {
		return "name=" + cookie.getName() + ", value=" + cookie.getValue() +
				", domain=" + cookie.getDomain() + ", path=" + cookie.getPath() + ", comment=" + cookie.getComment() +
				", maxage=" + cookie.getMaxAge() + ", version=" + cookie.getVersion() + ", secure=" + cookie.getSecure();
	}

	public static void dumpServletCookie(Cookie cookie) {
		System.out.println("ServletCookie: " + servletCookieToString(cookie));
	}

	public static void dumpNewCookie(NewCookie newCookie) {
		System.out.println("NewCookie: " + newCookieToString(newCookie));
	}
}
