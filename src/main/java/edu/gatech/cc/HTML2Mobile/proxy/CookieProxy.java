package edu.gatech.cc.HTML2Mobile.proxy;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.NewCookie;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.gatech.cc.HTML2Mobile.helper.DebugUtil;
import edu.gatech.cc.HTML2Mobile.helper.Tuple;

public class CookieProxy {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final String PREFIX = "__remote__";
	private static final Pattern isRemoteCookieName =
			Pattern.compile(Pattern.quote(PREFIX) + "[0-9A-Fa-f]{32}");

	/**
	 * Map tuple(name, domain, path) -> cookie
	 */
	protected Map<Tuple, HttpCookie> cookies =
			new LinkedHashMap<Tuple, HttpCookie>();

	public CookieProxy() {

	}

	public void loadRemoteCookies(HttpServletRequest req) {
		if( req == null ) {
			throw new NullPointerException("req is null");
		}

		// transfer cookies to remote site
		Cookie[] cookies = req.getCookies();
		if( cookies != null ) {
			List<NewCookie> remoteCookies = this.decodeCookies(null, Arrays.asList(cookies));
			for( NewCookie cookie : remoteCookies ) {
				HttpCookie httpCookie = newToHttpCookie(cookie);

				if( httpCookie.getDomain() == null ) {
					System.err.println("NULL-DOMAIN-REMOTE-COOKIE: " + httpCookie);
				}

				System.out.println("LOAD-REMOTE-COOKIE: " + httpCookie);
				this.addCookie(httpCookie);
			}
		}
	}

	public static NewCookie httpToNewCookie(HttpCookie c) {
		if( c == null ) {
			throw new NullPointerException("cookie is null");
		}

		return new NewCookie(c.getName(), c.getValue(), c.getPath(),
			c.getDomain(), c.getVersion(), c.getComment(),
			(int)c.getMaxAge(), c.getSecure());
	}

	public static HttpCookie newToHttpCookie(NewCookie cookie) {
		if( cookie == null ) {
			throw new NullPointerException("c is null");
		}

		HttpCookie httpCookie = new HttpCookie(cookie.getName(), cookie.getValue());
		httpCookie.setDomain(cookie.getDomain());
		httpCookie.setMaxAge(cookie.getMaxAge());
		httpCookie.setPath(cookie.getPath());
		httpCookie.setComment(cookie.getComment());
		httpCookie.setVersion(cookie.getVersion());
		httpCookie.setSecure(cookie.isSecure());

		return httpCookie;
	}

	public void addCookies(String domain, NewCookie... cookies) {
		if( domain == null ) {
			throw new NullPointerException("domain is null");
		}

		HttpCookie[] httpCookies = new HttpCookie[cookies.length];
		for( int i = 0, ilen = cookies.length; i < ilen; ++i ) {
			httpCookies[i] = newToHttpCookie(cookies[i]);
		}

		addCookies(domain, httpCookies);
	}

	public void addCookies(String domain, HttpCookie... cookies) {
		if( domain == null ) {
			throw new NullPointerException("domain is null");
		}

		for( HttpCookie cookie : cookies ) {
			if( cookie.getDomain() == null ) {
				cookie.setDomain(domain);
				System.out.println("SET-DOMAIN-COOKIE: " + cookie);
			}
			addCookie(cookie);
		}
	}

	public void addCookie(HttpCookie cookie) {
		if( cookie == null ) {
			throw new NullPointerException("cookie is null");
		}
		Tuple key = new Tuple(cookie.getName(), cookie.getDomain(), cookie.getPath());
		this.cookies.put(key, cookie);
	}

	public void addCookie(NewCookie cookie) {
		Tuple key = new Tuple(cookie.getName(), cookie.getDomain(), cookie.getPath());
		this.cookies.put(key, newToHttpCookie(cookie));
	}

	public List<HttpCookie> getCookiesForLocation(URL requestURL) {
		String host = requestURL.getHost();
		String path = requestURL.getPath();

		ArrayList<HttpCookie> hostCookies = new ArrayList<HttpCookie>();

		for( Entry<Tuple, HttpCookie> entry : cookies.entrySet() ) {
			HttpCookie cookie = entry.getValue();
			if( cookie.hasExpired() ) {
				System.out.println("SKIP-EXPIRED: " + cookie);
			} else if( HttpCookie.domainMatches(cookie.getDomain(), host)) {
				// FIXME path check and filter to only most-specific
				hostCookies.add(cookie);
				System.out.println("HOST-COOKIE: " + cookie);
			} else {
				System.out.println("NOT-HOST-COOKIE: " + cookie);
			}
		}

		return hostCookies;
	}

	public void storeRemoteCookies(HttpServletRequest req, HttpServletResponse resp) {

		ArrayList<NewCookie> newCookies = new ArrayList<NewCookie>(cookies.size());
		for( HttpCookie httpCookie : cookies.values() ) {
			newCookies.add(httpToNewCookie(httpCookie));
		}

		List<Cookie> proxyCookies = this.encodeCookies(null, newCookies);
		for( Cookie proxyCookie : proxyCookies ) {
			resp.addCookie(proxyCookie);
			System.out.println("STORE-REMOTE-COOKIE: " + DebugUtil.servletCookieToString(proxyCookie));
		}
	}

	public static String hashCookie(NewCookie cookie) {
		if( cookie == null ) {
			throw new NullPointerException("cookie is null");
		}

		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch( NoSuchAlgorithmException e ) {
			throw new ProxyException("MD5 unsupported?", e);
		}

		String input = "name:" + cookie.getName() + ":path:" + cookie.getPath()
				+ ":domain:" + cookie.getDomain();
		digest.update(input.getBytes(UTF8));

		// 32-digit hexadecimal, zero padded
		return String.format("%1$032X", new BigInteger(1, digest.digest()));
	}

	public static boolean isRemoteCookie(String name) {
		if( name == null ) {
			throw new NullPointerException("name is null");
		}
		return isRemoteCookieName.matcher(name).matches();
	}

	public static List<Cookie> encodeCookies(String domain, Collection<NewCookie> newCookies) {
		return encodeCookies(domain, "/", newCookies);
	}

	public static List<Cookie> encodeCookies(String domain, String path, Collection<NewCookie> newCookies) {
		if( newCookies == null ) {
			throw new NullPointerException("newCookies is null");
		}
		if( path == null ) {
			throw new NullPointerException("path is null");
		}

		ArrayList<Cookie> cookies = new ArrayList<Cookie>(newCookies.size());
		for( NewCookie newCookie : newCookies ) {
			String name = PREFIX + hashCookie(newCookie);
			String value = serializeCookie(newCookie);

			try {
				Cookie cookie = new Cookie(name, value);
				if( domain != null ) {
					cookie.setDomain(domain);
				}
				cookie.setPath(path);
				cookie.setVersion(newCookie.getVersion());
				cookie.setMaxAge(newCookie.getMaxAge());

				// FIXME
				//				cookie.setSecure(newCookie.isSecure());
				cookie.setSecure(false);

				cookies.add(cookie);
			} catch( IllegalArgumentException e ) {
				// FIXME better error handling
				System.err.println("Couldn't transfer cookie: " + e.getMessage());
			}
		}
		return cookies;
	}

	public static boolean isTargetDomain(String hostDomain, String cookieDomain) {
		// FIXME
		System.out.println("CookieHelper.isTargetDomain()");
		System.out.println("host=" + hostDomain);
		System.out.println("cook=" + cookieDomain);
		if( hostDomain == null ) {
			throw new NullPointerException("hostDomain is null");
		}

		// FIXME: check the right way
		boolean isTarget = cookieDomain == null || hostDomain.endsWith(cookieDomain);
		System.out.println(hostDomain + " isTarget(" + cookieDomain + ")? " + isTarget);
		return isTarget;
	}

	public static List<NewCookie> decodeCookies(String domain, Collection<Cookie> cookies) {
		if( cookies == null || cookies.size() == 0 ) {
			return Collections.emptyList();
		}

		// treat blank strings as no domain (null)
		if( domain != null && (domain = domain.trim()).length() == 0 ) {
			domain = null;
		}

		ArrayList<NewCookie> newCookies = new ArrayList<NewCookie>(cookies.size());
		for( Cookie cookie : cookies ) {
			String name = cookie.getName();
			String value = cookie.getValue();

			// only handle proxied cookies
			if( isRemoteCookie(name) ) {
				try {
					NewCookie newCookie = deserializeCookie(value);//NewCookie.valueOf(value);
					new Cookie(name, value); // throws IllegalArgumentException if we won't be able to send this back

					//  maybe filter domains
					if( domain == null || isTargetDomain(domain, newCookie.getDomain())) {
						newCookies.add(newCookie);
					} else {
						System.out.println("Skipped cookie: " + name);
					}
				} catch( IllegalArgumentException e ) {
					System.err.println("WARN: Could not parse cookie value: " + value);
					System.err.println(e.getMessage());
					if( e.getCause() != null ) {
						e.getCause().printStackTrace();
					} else {
						e.printStackTrace();
					}
				}
			} else {
				System.out.println("Not remote: " + name);
			}
		}
		return newCookies;
	}



	public static String serializeCookie(NewCookie cookie) {
		Gson gson = new Gson();
		String json = gson.toJson(cookie.toCookie());
		try {
			return URLEncoder.encode(json, "UTF-8");
		} catch( UnsupportedEncodingException e ) {
			throw new ProxyException("UTF-8 not supported?");
		}
	}

	public static NewCookie deserializeCookie(String cookie) {
		try {
			String decoded = null;
			try {
				decoded = URLDecoder.decode(cookie, "UTF-8");
			} catch( UnsupportedEncodingException e ) {
				throw new ProxyException("UTF-8 not supported?");
			}

			Gson gson = new Gson();
			JsonElement el = gson.fromJson(decoded, JsonElement.class);
			JsonObject obj = el.getAsJsonObject();

			// FIXME
			//			System.out.println("CookieHelper.deserializeCookie()");
			//			System.out.println("cookie=" + cookie);
			//			for( Entry<String, JsonElement> o : obj.entrySet() ) {
			//				System.out.println("  " + o.getKey() + "=" + o.getValue());
			//			}

			String name = obj.get("name").getAsString(),
					value = obj.get("value").getAsString();
			String domain =  obj.has("domain") ? obj.get("domain").getAsString() : null;
			String path = obj.has("path") ? obj.get("path").getAsString() : "/";
			String comment = obj.has("comment") ? obj.get("comment").getAsString() : null;
			boolean secure = obj.has("secure") ? obj.get("secure").getAsBoolean() : false;
			int maxAge = obj.has("maxAge") ? obj.get("maxAge").getAsInt() : -1;

			return new NewCookie(name, value, path, domain, comment, maxAge, secure);
		} catch( RuntimeException e ) {
			System.out.println("BAD COOKIE VAL: " + cookie);
			throw new IllegalArgumentException(e);
		}
	}
}
