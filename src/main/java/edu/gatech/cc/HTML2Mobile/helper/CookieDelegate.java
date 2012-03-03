package edu.gatech.cc.HTML2Mobile.helper;

import java.util.regex.Pattern;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant.VariantListBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import com.sun.jersey.core.impl.provider.header.NewCookieProvider;

public class CookieDelegate extends NewCookieProvider {
	private static final Pattern hasExpires =
			Pattern.compile("expires=", Pattern.CASE_INSENSITIVE);

	private DateTime requestTime = null;

	public void setRequestTime(DateTime time) {
		requestTime = time;
	}

	public DateTime getRequestTime() {
		return requestTime;
	}

	@Override
	public NewCookie fromString(String headerValue) throws IllegalArgumentException {
		// FIXME
		System.out.println("PARSE COOKIE: " + headerValue);

		String splitOn = "[;,]";
		int version = 1;
		if( hasExpires.matcher(headerValue).find() ) {
			splitOn = ";";
			version = 1;
		}

		String name = null, value = null, comment = null, domain = null, path = null;
		boolean secure = false;
		int maxAge = -1;

		String bites[] = headerValue.split(splitOn);
		for( String bite : bites ) {
			String crumbs[] = bite.split("=", 2);
			String fieldName = crumbs.length > 0 ? crumbs[0].trim() : "";
			String fieldValue = crumbs.length > 1 ? crumbs[1].trim() : "";
			if( fieldValue.startsWith("\"") && fieldValue.endsWith("\"") && fieldValue.length() > 1 ) {
				fieldValue = fieldValue.substring(1, fieldValue.length() - 1);

				// FIXME workaround app engine vs jetty servlet api differences
				fieldValue = fieldValue.replace("\\\"", "\"");
			}

			if( name == null ) {
				name = fieldName;
				value = fieldValue;
			} else {
				fieldName = fieldName.toLowerCase();
				if( fieldName.startsWith("comment") ) {
					comment = value;
				} else if( fieldName.startsWith("domain") ) {
					domain = fieldValue;
				} else if( fieldName.startsWith("max-age") ) {
					maxAge = Integer.parseInt(fieldValue);
				} else if( fieldName.startsWith("path") ) {
					path = fieldValue;
				} else if( fieldName.startsWith("secure") ) {
					secure = true;
				} else if( fieldName.startsWith("version") ) {
					version = Integer.parseInt(fieldValue);
				} else if( fieldName.startsWith("domain") ) {
					domain = fieldValue;
				} else if( fieldName.startsWith("expires") ) {

					try {

						DateTime expires = DateUtil.parseHttpDate(fieldValue);
						DateTime requested = requestTime != null ? requestTime : new DateTime();

						// FIXME
						System.out.println("requested: " + requested);
						System.out.println("expires: " + expires);

						int seconds = Seconds.secondsBetween(requested, expires).getSeconds();

						System.out.println("secs: " + seconds);
						System.out.println();

						if( requested.isBefore(expires) ) {
							maxAge = Math.max(0, seconds);
						} else {
							maxAge = 0;
						}
					} catch ( IllegalArgumentException e ) {
						System.err.println("Could not parse value: " + fieldValue);
						System.err.println("Message: " + e.getMessage());
					}
				} else {
					System.err.println("Unhandled cookie field: " + fieldName + "=" + fieldValue);
				}
			}
		}

		NewCookie newCookie = new NewCookie(name, value, path, domain, version, comment, maxAge, secure);
		System.out.println("PARSED: " + newCookie);
		System.out.println("ASCOOK: " + newCookie.toCookie());
		return newCookie;
	}

	public void registerDelegate() {
		new CookieRuntimeDelegate();
	}

	private class CookieRuntimeDelegate extends RuntimeDelegate {
		private final RuntimeDelegate delegate;

		public CookieRuntimeDelegate() {
			delegate = RuntimeDelegate.getInstance();
			RuntimeDelegate.setInstance(this);
		}

		@Override
		public <T> HeaderDelegate<T> createHeaderDelegate(Class<T> type) {
			if( CookieDelegate.this.supports(type) ) {
				return (HeaderDelegate<T>)CookieDelegate.this;
			}
			return delegate.createHeaderDelegate(type);
		}

		@Override
		public UriBuilder createUriBuilder() {
			return delegate.createUriBuilder();
		}

		@Override
		public ResponseBuilder createResponseBuilder() {
			return delegate.createResponseBuilder();
		}

		@Override
		public VariantListBuilder createVariantListBuilder() {
			return delegate.createVariantListBuilder();
		}

		@Override
		public <T> T createEndpoint(Application application, Class<T> endpointType) throws IllegalArgumentException, UnsupportedOperationException {
			return delegate.createEndpoint(application, endpointType);
		}

	}
}
