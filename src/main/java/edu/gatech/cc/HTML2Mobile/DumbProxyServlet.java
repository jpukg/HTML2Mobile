package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import edu.gatech.cc.HTML2Mobile.helper.DebugUtil;
import edu.gatech.cc.HTML2Mobile.helper.Pair;

/** Proof of concept proxying servlet. */
public class DumbProxyServlet extends JSoupServlet {
	//
	// Static fields and methods
	private static final long serialVersionUID = 1L;

	/** Compile-time constant for loging debug info. */
	private static final boolean DEBUG = true;

	/** Attribute name where the remote URL will be stored. */
	protected static final String ATTR_REMOTE_URL = "proxyURL";

	/** Jersey web client for sending proxied requests. */
	protected static final Client webClient;
	static {
		webClient = Client.create();

		// we'll handle these manually
		webClient.setFollowRedirects(false);

		if( DEBUG ) {
			webClient.addFilter(new LoggingFilter(System.out));
		}
	}

	/** Host matching pattern. */
	private static final Pattern hasHost =
			Pattern.compile("^(https?:)?//.*", Pattern.CASE_INSENSITIVE);

	/**
	 * Checks if the given url includes a host part.
	 * 
	 * @param url the url to check
	 * @return <code>true</code> if the URL has a host part
	 */
	public static boolean hasHost(String url) {
		if( url == null ) {
			throw new NullPointerException("url is null");
		}
		return hasHost.matcher(url).matches();
	}

	/**
	 * Rewrites a URL to be absolute and with a host.
	 * 
	 * @param requestURL the base URL of the request
	 * @param targetURL  the resource URL to rewrite
	 * @return the rewritten URL or <code>targetURL</code> if it already includes the host
	 * @throws MalformedURLException if the rewritten URL is not well-formed
	 */
	public static String rewriteDirectResource(URL requestURL, String targetURL)
			throws MalformedURLException {
		// no rewrite when the host is included
		if( hasHost.matcher(targetURL).matches() ) {
			return targetURL;
		}

		// make relative URLs absolute
		if( !targetURL.startsWith("/") ) {
			String path = requestURL.getPath();

			// strip off non-dir part
			int lastSlash = path.lastIndexOf('/');
			if( -1 != lastSlash ) {
				path = path.substring(0, lastSlash);
			}

			targetURL = path + "/" + targetURL;
		}

		// now point to the original server
		URL newTarget = new URL(requestURL.getProtocol(), requestURL.getHost(), requestURL.getPort(), targetURL);
		return newTarget.toExternalForm();
	}

	//
	// Instance fields / methods

	/** The maximum number of redirects per request. */
	protected int maxRedirects = 20;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		// TODO: move into JSoupServlet
		if( DEBUG ) {
			DebugUtil.dumpRequestInfo(req);
		}

		URL url = this.getProxiedURL(req);

		Pair<ClientResponse, URL> result = this.sendRequest(url, req);
		ClientResponse response = result.getOne();
		url = result.getTwo();

		// for process method to find
		req.setAttribute(ATTR_REMOTE_URL, url);

		this.copyProxiedCookies(response, req, resp);

		// parse the document
		String contents = response.getEntity(String.class);
		Document doc = Jsoup.parse(contents);

		// this implementation rewrites various element urls
		String output = this.process(doc, req);

		// write out the new contents
		PrintWriter writer = resp.getWriter();
		writer.write(output);
		writer.flush();
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
	 * @throws URISyntaxException if <code>target</code> cannot be parsed as a {@link URI}
	 */
	protected WebResource.Builder prepareClientRequest(URL target, HttpServletRequest req, boolean isPost) {
		WebResource res = webClient.resource(target.toExternalForm());
		WebResource.Builder builder = res.getRequestBuilder();

		// transfer cookies to remote site
		Cookie[] cookies = req.getCookies();
		if( cookies != null ) {
			for( Cookie cookie : req.getCookies() ) {
				if( DEBUG ) {
					DebugUtil.dumpServletCookie(cookie);
				}
				// FIXME translate the path
				javax.ws.rs.core.Cookie newCookie = new javax.ws.rs.core.Cookie(
					cookie.getName(), cookie.getValue(), cookie.getPath(), target.getHost());
				builder = builder.cookie(newCookie);
			}
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
	 * (up to <code>maxRedirects</code>) and transerring cookies.
	 * 
	 * @param url the URL to request
	 * @param req the proxy's request
	 * @return the client response, will not be <code>null</code>
	 * 
	 * @throws ServletException if anything goes wrong, such as a bad status from the
	 *                          remote server, too many redirects, etc
	 * @throws NullPointerException if either param is <code>null</code>
	 */
	protected Pair<ClientResponse, URL> sendRequest(URL url, HttpServletRequest req)
			throws ServletException {
		if( url == null ) {
			throw new NullPointerException("url is null");
		}
		if( req == null ) {
			throw new NullPointerException("req is null");
		}

		boolean isPost = "POST".equals(req.getMethod());

		WebResource.Builder builder = null;
		ClientResponse response = null;
		int redirects = 0;
		List<NewCookie> addCookies = Collections.emptyList();
		// will return or throw exception to terminate the loop
		while( true ) {
			// build the request
			builder = prepareClientRequest(url, req, isPost);

			// add any new cookies from redirect responses
			for( NewCookie addCookie : addCookies ) {
				javax.ws.rs.core.Cookie newCookie = new javax.ws.rs.core.Cookie(
					addCookie.getName(), addCookie.getValue(), addCookie.getPath(),
					addCookie.getDomain(), addCookie.getVersion()
						);
				builder = builder.cookie(newCookie);
				if( DEBUG ) {
					System.out.println("Extra cookie: " + DebugUtil.newCookieToString(addCookie));
				}
			}

			// send the request
			response = isPost ? builder.post(ClientResponse.class) : builder.get(ClientResponse.class);

			// request succeeded
			if( ClientResponse.Status.OK.equals(response.getClientResponseStatus()) ) {
				return Pair.makePair(response, url);
			}

			// request failed
			int status = response.getStatus();
			if( status < 300 || status >= 400 ) {
				throw new ServletException("Server responded with: " + status);
			}

			// else must be a redirect
			URI location = response.getLocation();
			if( location == null ) {
				throw new ServletException("Redirect [" + status + "] but no Location given.");
			}

			// try again with the new location
			URL newUrl;
			try {
				newUrl = location.toURL();
			} catch( MalformedURLException e ) {
				throw new ServletException("Couldn't parse redirect URI as URL: " + location);
			}
			if( newUrl.equals(url) ) {
				throw new ServletException("Infinite redirect to: " + newUrl.toExternalForm());
			}
			url = newUrl;

			isPost = false; // don't POST again when following redirects
			addCookies = response.getCookies(); // pass along any new cookies

			if( ++redirects > this.maxRedirects ) {
				throw new ServletException("Redirect limit exceeded: " + maxRedirects);
			}
		}
	}

	/**
	 * Reads and parses the <code>url</code> parameter.  Also assigns the
	 * result to the {@link #ATTR_REMOTE_URL} attribute of <code>req</code>.
	 * 
	 * @param req the proxy servlet request
	 * @return the parsed URL
	 * @throws ServletException if the <code>url</code> parameter is missing or unparseable
	 */
	protected URL getProxiedURL(HttpServletRequest req) throws ServletException {
		// read requested URL
		String urlParam = req.getParameter("url");
		if( urlParam == null ) {
			throw new ServletException("No URL");
		}

		// add protocol if not present
		if( !hasHost(urlParam) ) {
			try {
				urlParam = new URI(req.getRequestURI()).getScheme() + "://" + urlParam;
			} catch( URISyntaxException e ) {
				throw new ServletException(e);
			}
		}

		// now parse
		try {
			URL url = new URL(urlParam);
			if( DEBUG ) {
				System.out.println("URL=" + url.toExternalForm());
			}
			req.setAttribute(ATTR_REMOTE_URL, url);
			return url;
		} catch( MalformedURLException e ) {
			throw new ServletException("Cannot parse: " + urlParam, e);
		}
	}

	/**
	 * Copy cookies from a proxied response into the current servlet response.
	 * 
	 * @param response the remote response
	 * @param localRequest the local proxy servlet request
	 * @param localResponse the local proxy servlet response
	 */
	protected void copyProxiedCookies(ClientResponse response, HttpServletRequest localRequest,
			HttpServletResponse localResponse) {
		// translate cookies back to our client
		List<NewCookie> respCookies = response.getCookies();
		if( respCookies != null ) {
			for( NewCookie newCookie : respCookies ) {
				try {
					Cookie clientCookie = new Cookie(newCookie.getName(), newCookie.getValue());

					// FIXME double check this, what about the port?
					clientCookie.setDomain(localRequest.getLocalName());

					// FIXME check the path
					clientCookie.setPath("/");

					clientCookie.setMaxAge(newCookie.getMaxAge());
					clientCookie.setSecure(newCookie.isSecure());
					clientCookie.setComment(newCookie.getComment());
					clientCookie.setVersion(newCookie.getVersion());

					localResponse.addCookie(clientCookie);

					if( DEBUG ) {
						System.out.println("Added cookie: " + DebugUtil.servletCookieToString(clientCookie));
					}
				} catch( IllegalArgumentException e ) {
					// constructor rejects some names, such as date-formatted strings
					System.err.println("Couldn't transfer cookie: " + e.getMessage());
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * This implementation rewrites various URLs to either proxy through us or to
	 * hit the remote server directly.
	 */
	@Override
	public String process(Document doc, HttpServletRequest req) throws ServletException, IOException {
		URL url = (URL)req.getAttribute(ATTR_REMOTE_URL);
		String requestURI = req.getRequestURI() + "?url=";

		// rewrite elements that need to proxy through us
		for( Element el : doc.select("a[href], form[action], iframe[src]") ) {
			String node = el.nodeName();
			String attr;
			if( "a".equalsIgnoreCase(node) ) {
				attr = "href";
			} else if( "form".equalsIgnoreCase(node) ) {
				attr = "action";
			} else if( "iframe".equalsIgnoreCase(node) ) {
				attr = "src";
			} else {
				throw new ServletException("Unexpected node type: " + node);
			}

			String oldVal = el.attr(attr);
			String newVal = requestURI + URLEncoder.encode(rewriteDirectResource(url, oldVal), "UTF-8");
			el.attr(attr, newVal);

			if( DEBUG ) {
				System.out.println("Rewrote: '" + oldVal + "' -> '" + newVal + "'");
			}
		}

		// rewrite elements that should not proxy through us
		for( Element el : doc.select("img[src], script[src], link[href]") ) {
			String node = el.nodeName().toLowerCase();
			String attr;
			if( "link".equals(node) ) {
				attr = "href";
			} else if ( "img".equals(node) || "script".equals(node) ) {
				attr = "src";
			} else {
				throw new ServletException("Unexpected node type: " + node);
			}

			String oldVal = el.attr(attr);
			String newVal = rewriteDirectResource(url, oldVal);
			el.attr(attr, newVal);

			if( DEBUG ) {
				System.out.println("Rewrote: '" + oldVal + "' -> '" + newVal + "'");
			}
		}

		return doc.toString();
	}
}
