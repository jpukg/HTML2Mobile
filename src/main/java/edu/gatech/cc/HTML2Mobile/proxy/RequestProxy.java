package edu.gatech.cc.HTML2Mobile.proxy;

import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import edu.gatech.cc.HTML2Mobile.helper.CookieDelegate;
import edu.gatech.cc.HTML2Mobile.helper.CookieProxy;
import edu.gatech.cc.HTML2Mobile.helper.Pair;

/**
 * Proxies remote requests on behalf of some servlet.
 * 
 * <p>
 * A <code>RequestProxy</code> deals with sending requests, following redirects,
 * and receiving the response.  It also uses a {@link CookieProxy} to handle
 * remote cookies.
 * </p>
 */
public class RequestProxy {
	/** Compile-time constant for logging debug info. */
	private static final boolean DEBUG = false;

	/** Attribute name where the remote URL will be stored. */
	public static final String ATTR_REMOTE_URL = "RequestProxy.proxyURL";

	/** Attribute name where the cookie proxy will be stored. */
	protected static final String ATTR_COOKIE_PROXY = "RequestProxy.cookieProxy";

	// FIXME simultaneous requests
	protected static CookieDelegate cookieDelegate;

	/** Jersey web client for sending proxied requests. */
	protected static final Client webClient;
	static {
		try {
			// FIXME overriding cookie handling for now
			cookieDelegate = new CookieDelegate();
			cookieDelegate.registerDelegate();

			webClient = Client.create();
			webClient.setConnectTimeout(30000);
			webClient.setReadTimeout(30000);

			// we'll handle these manually
			webClient.setFollowRedirects(false);

			webClient.addFilter(new HeaderRepairFilter());

			if( DEBUG ) {
				webClient.addFilter(new LoggingFilter(System.out));
			}
		} catch( RuntimeException e ) {
			e.printStackTrace();
			throw e;
		}
	}


	/** The maximum number of redirects per request. */
	protected int maxRedirects = 20;

	/** Creates a new request proxy. */
	public RequestProxy() {

	}

	/**
	 * Proxies a request on behalf of a servlet.
	 * 
	 * <p>
	 * The <code>ClientResponse</code> returned will be the final result after
	 * following any redirects.  Any remote cookies will have been written to
	 * <code>resp</code>.  You can retrieve the final URL (after any redirects)
	 * as follows: <br />
	 * <code>URL url = (URL)req.getAttribute(RequestProxy.ATTR_REMOTE_URL);</code>
	 * </p><p>
	 * The body of the response is retrieved in the usual way, for example as a <code>String</code>:<br />
	 * <code>String contents = response.getEntity(String.class);</code>
	 * </p>
	 * 
	 * @param url the remote url
	 * @param req the servlet request
	 * @param resp the servlet response
	 * @return the client response from the remote site
	 * 
	 * @throws NullPointerException if any argument is <code>null</code>
	 * @throws ProxyException if request proxying fails
	 */
	public ClientResponse proxyRequest(URL url, HttpServletRequest req, HttpServletResponse resp)
			throws ProxyException {
		if( url == null ) {
			throw new NullPointerException("url is null");
		}
		if( req == null ) {
			throw new NullPointerException("req is null");
		}
		if( resp == null ) {
			throw new NullPointerException("resp is null");
		}

		try {
			// prepare the cookie proxy and load any remote cookies from this request
			CookieProxy cookieProxy = new CookieProxy();
			req.setAttribute(ATTR_COOKIE_PROXY, cookieProxy);
			cookieProxy.loadRemoteCookies(req);

			// send the request
			Pair<ClientResponse, URL> result = this.sendRequest(url, req, resp);
			ClientResponse response = result.getOne();
			url = result.getTwo();

			// store remaining cookies and discard the cookie proxy
			cookieProxy.storeRemoteCookies(req, resp);
			req.removeAttribute(ATTR_COOKIE_PROXY);

			// note the final url for the caller
			req.setAttribute(ATTR_REMOTE_URL, url);

			return response;
		} catch( UniformInterfaceException e ) {
			throw new ProxyException(e);
		}
	}


	/**
	 * Prepares a Jersey client request.
	 * 
	 * Cookies will be copied from <code>req</code> and sent along with the request.  If
	 * <code>isPost</code> is true, request parameters will be added and the type will be
	 * set to {@link MediaType#APPLICATION_FORM_URLENCODED_TYPE}.
	 * 
	 * @param target the target URL for the request
	 * @param req    the proxy servlet's request
	 * @param isPost <true> if this is a <code>POST</code>, otherwise <code>GET</code> will be used
	 * @return the request builder, ready to send
	 * 
	 * @throws UniformInterfaceException if thrown by Jersey client library
	 * @throws URISyntaxException if <code>target</code> cannot be parsed as a {@link URI}
	 */
	protected WebResource.Builder prepareClientRequest(URL target, HttpServletRequest req,
			HttpServletResponse response, boolean isPost) {

		// mimick chrome for now
		final String AGENT = "Mozilla/5.0 (X11) " +
				"AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11";

		WebResource res = webClient.resource(target.toExternalForm());
		WebResource.Builder builder = res.getRequestBuilder()
				.accept(MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.TEXT_XML)
				.header(HttpHeaders.USER_AGENT, AGENT);

		// set cookies for remote location
		CookieProxy cookieProxy = (CookieProxy)req.getAttribute(ATTR_COOKIE_PROXY);
		List<HttpCookie> cookies = cookieProxy.getCookiesForLocation(target);
		for( HttpCookie cookie : cookies ) {
			builder.cookie(CookieProxy.httpToNewCookie(cookie).toCookie());
		}

		// form encode parameters if POST'ing
		if( isPost ) {
			MultivaluedMap<String, String> postParams = new MultivaluedMapImpl();
			@SuppressWarnings("unchecked")
			Map<String,String[]> reqParams = req.getParameterMap();
			for( Map.Entry<String, String[]> paramEntry : reqParams.entrySet() ) {
				String key = paramEntry.getKey();
				String[] vals = paramEntry.getValue();
				String val = vals[0];
				if( vals.length > 1 ) {
					System.err.println("WARN: More than one value for key: " + key);
				}
				postParams.add(key, val);
			}
			builder = builder.entity(postParams, MediaType.APPLICATION_FORM_URLENCODED_TYPE);
		}

		return builder;
	}

	/**
	 * Attempts to send a request to <code>url</code>.  This handles redirects
	 * (up to <code>maxRedirects</code>) and transferring cookies.
	 * 
	 * @param url the URL to request
	 * @param req the proxy's request
	 * @return the client response, will not be <code>null</code>
	 * 
	 * @throws NullPointerException if either param is <code>null</code>
	 * @throws ProxyException if anything goes wrong, such as a bad status from the
	 *                          remote server, too many redirects, etc
	 * @throws UniformInterfaceException if thrown by the Jersey client library
	 */
	protected Pair<ClientResponse, URL> sendRequest(URL url, HttpServletRequest req,
		HttpServletResponse resp) throws ProxyException {
		if( url == null ) {
			throw new NullPointerException("url is null");
		}
		if( req == null ) {
			throw new NullPointerException("req is null");
		}

		CookieProxy cookieProxy = (CookieProxy)req.getAttribute(ATTR_COOKIE_PROXY);

		boolean isPost = "POST".equals(req.getMethod());
		int redirects = 0;
		// will return or throw exception to terminate the loop
		while( true ) {
			// build and send the request
			WebResource.Builder builder = prepareClientRequest(url, req, resp, isPost);
			ClientResponse response = (isPost ?
				builder.post(ClientResponse.class) : builder.get(ClientResponse.class));

			// request succeeded
			if( ClientResponse.Status.OK.equals(response.getClientResponseStatus()) ) {
				cookieProxy.addCookies(url.getHost(),
					response.getCookies().toArray(new NewCookie[0]));
				return Pair.makePair(response, url);
			}

			// request failed
			int status = response.getStatus();
			if( status < 300 || status >= 400 ) {
				throw new ProxyException("Server responded with: " + status);
			}

			// else must be a redirect
			URI location = response.getLocation();
			if( location == null || "".equals(location.toASCIIString()) ) {
				System.err.println("WARN: Redirect [" + status + "] but no Location given.");

				// FIXME try to workaround
				try {
					String urlStr = url.toExternalForm();
					int slash = urlStr.lastIndexOf('/');
					if( -1 != slash ) {
						urlStr = urlStr.substring(0, slash);
					}
					location = new URI(urlStr);
				} catch( URISyntaxException e ) {
					throw new ProxyException("Redirect [" + status + "] but no Location given.", e);
				}
				throw new ProxyException("Redirect [" + status + "] but no Location given.");
			}
			if( DEBUG ) {
				System.out.println("LOCATION: " + location.toASCIIString());
			}

			isPost = false; // don't POST again when following redirects

			// pass along any new cookies
			cookieProxy.addCookies(url.getHost(),
				response.getCookies().toArray(new NewCookie[0]));

			// try again with the new location
			URL newUrl;
			try {
				newUrl = location.toURL();
			} catch( MalformedURLException e ) {
				throw new ProxyException("Couldn't parse redirect URI as URL: " + location);
			}
			if( newUrl.equals(url) ) {
				throw new ProxyException("Infinite redirect to: " + newUrl.toExternalForm());
			}
			url = newUrl;

			if( ++redirects > this.maxRedirects ) {
				throw new ProxyException("Redirect limit exceeded: " + maxRedirects);
			}
		}
	}

	// Accessors
	//

	public int getMaxRedirects() {
		return maxRedirects;
	}

	public void setMaxRedirects(int maxRedirects) {
		if( maxRedirects < 0 ) {
			throw new IllegalArgumentException("maxRedirects may not be negative: " + maxRedirects);
		}
		this.maxRedirects = maxRedirects;
	}
}
