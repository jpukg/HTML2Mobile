package edu.gatech.cc.HTML2Mobile.helper;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.NewCookie;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CookieProxy {
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final String PREFIX = "__remote__";
	private static final Pattern isRemoteCookieName =
			Pattern.compile(Pattern.quote(PREFIX) + "[0-9A-Fa-f]{32}");



	// match the pieces and discard the rest, be tolerant of different seperators
	private static final String SEP = "[\\-\\s_:]";
	private static final String EXPIRE_REGEX = "^.*" +
			"([a-zA-Z]{3}),\\s*" +
			"(\\d{2})" + SEP +
			"([a-zA-Z]{3})" + SEP +
			"(\\d{4})\\s+" +
			"(\\d{2})" + SEP +
			"(\\d{2})" + SEP +
			"(\\d{2})" +
			".*$";
	// replace with standardized format for datetime parsing
	private static final String EXPIRE_REPLACE = "$2-$3-$4 $5:$6:$7";
	private static final String DATE_FORMAT = "dd-MMM-yyyy HH:mm:ss";

	private static final Pattern normalizeDate = Pattern.compile(EXPIRE_REGEX);

	/** Attempt to parse an expires= value into a datetime in UTC
	 * .
	 * @param date the date
	 * @return the datetime represented by <code>date</code>
	 * @throws IllegalArgumentException if the datestring cannot be parsed
	 * @throws NullPointerException if <code>date</code> is <code>null</code>
	 */
	public static DateTime parseExpirationDate(String date) {
		if( date == null ) {
			throw new NullPointerException("date is null");
		}

		// try to cleanup variations in the date string
		String normalized = normalizeDate.matcher(date).replaceFirst(EXPIRE_REPLACE);

		// attempt to parse
		DateTimeFormatter format = DateTimeFormat.forPattern(DATE_FORMAT).withZoneUTC();
		DateTime dateTime = format.parseDateTime(normalized);

		return dateTime;
	}

	public static String hashCookie(NewCookie cookie) {
		if( cookie == null ) {
			throw new NullPointerException("cookie is null");
		}

		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch( NoSuchAlgorithmException e ) {
			// FIXME: better exception type
			throw new RuntimeException("MD5 unsupported?", e);
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
		boolean isRemote = isRemoteCookieName.matcher(name).matches();
		System.out.println(name + " isRemote? " + isRemote);
		return isRemote;
	}

	public static List<Cookie> encodeCookies(String domain, Collection<NewCookie> newCookies) {
		return encodeCookies(domain, "/", newCookies);
	}

	public static Cookie encodeCookie(String domain, NewCookie newCookie) {
		return encodeCookies(domain, "/",
			Collections.singletonList(newCookie)).get(0);
	}
	public static Cookie encodeCookie(String domain, String path, NewCookie newCookie) {
		return encodeCookies(domain, path,
			Collections.singletonList(newCookie)).get(0);
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
			String value = serializeCookie(newCookie);//newCookie.toString();

			System.out.println("DEBUG_VAL: " + DebugUtil.newCookieToString(newCookie));
			System.out.println("TOSTR_VAL: " + value);

			try {
				Cookie cookie = new Cookie(name, value);
				if( domain != null ) {
					cookie.setDomain(domain);
				}
				cookie.setPath(path);
				cookie.setVersion(newCookie.getVersion());
				cookie.setMaxAge(newCookie.getMaxAge());

				// FIXME
				cookie.setSecure(newCookie.isSecure());
				//				cookie.setSecure(false);

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
			throw new RuntimeException("UTF-8 not supported?");
		}
	}

	public static NewCookie deserializeCookie(String cookie) {
		try {
			String decoded = null;
			try {
				decoded = URLDecoder.decode(cookie, "UTF-8");
			} catch( UnsupportedEncodingException e ) {
				throw new RuntimeException("UTF-8 not supported?");
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
