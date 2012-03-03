package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
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

import edu.gatech.cc.HTML2Mobile.helper.CookieDelegate;
import edu.gatech.cc.HTML2Mobile.helper.CookieProxy;
import edu.gatech.cc.HTML2Mobile.helper.DebugUtil;
import edu.gatech.cc.HTML2Mobile.helper.Pair;
import edu.gatech.cc.HTML2Mobile.proxy.HeaderRepairFilter;

/** Proof of concept proxying servlet. */
public class DumbProxyServlet extends JSoupServlet {
	//
	// Static fields and methods
	private static final long serialVersionUID = 1L;

	/** Compile-time constant for logging debug info. */
	private static final boolean DEBUG = true;

	/** Constant for logging URL rewrites, separate from DEBUG. */
	private static final boolean DEBUG_REWRITES = false;

	/** Attribute name where the remote URL will be stored. */
	protected static final String ATTR_REMOTE_URL = "proxyURL";

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

	protected CookieProxy cookyProxy = new CookieProxy();

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

		Pair<ClientResponse, URL> result = this.sendRequest(url, req, resp);
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
	protected WebResource.Builder prepareClientRequest(URL target, HttpServletRequest req,
			HttpServletResponse response, boolean isPost) {

		// mimick chrome for now
		final String AGENT = "Mozilla/5.0 (X11) " +
				"AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11";

		System.out.println("DumbProxyServlet.prepareClientRequest()");
		WebResource res = webClient.resource(target.toExternalForm());
		WebResource.Builder builder = res.getRequestBuilder()
				.accept(MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.TEXT_XML)
				.header(HttpHeaders.USER_AGENT, AGENT);

		//		// transfer cookies to remote site
		//		Cookie[] cookies = req.getCookies();
		//		if( cookies != null ) {
		//			for( Cookie cookie : req.getCookies() ) {
		//				if( DEBUG ) {
		//					DebugUtil.dumpServletCookie(cookie);
		//				}
		//				// FIXME translate the path
		//				javax.ws.rs.core.Cookie newCookie = new javax.ws.rs.core.Cookie(
		//					cookie.getName(), cookie.getValue(), cookie.getPath(), target.getHost());
		//				builder = builder.cookie(newCookie);
		//			}
		//		}

		// transfer cookies to remote site
		Cookie[] cookies = req.getCookies();
		if( cookies != null ) {
			List<NewCookie> remoteCookies = CookieProxy.decodeCookies(
				target.getHost(), Arrays.asList(cookies));
			for( NewCookie remoteCookie : remoteCookies ) {
				// FIXME
				//				System.out.println("ADD REMOTE COOKIE: " + remoteCookie);
				//				builder = builder.cookie(remoteCookie);
				System.out.println("ADD REMOTE COOKIE: " + remoteCookie.toCookie());
				builder = builder.cookie(remoteCookie.toCookie());
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
	 * (up to <code>maxRedirects</code>) and transferring cookies.
	 * 
	 * @param url the URL to request
	 * @param req the proxy's request
	 * @return the client response, will not be <code>null</code>
	 * 
	 * @throws ServletException if anything goes wrong, such as a bad status from the
	 *                          remote server, too many redirects, etc
	 * @throws NullPointerException if either param is <code>null</code>
	 */
	protected Pair<ClientResponse, URL> sendRequest(URL url, HttpServletRequest req,
		HttpServletResponse resp)
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
			builder = prepareClientRequest(url, req, resp, isPost);

			// add any new cookies from redirect responses
			for( NewCookie addCookie : addCookies ) {
				try {
					// don't send malformed cookies, this will throw IllegalArgumentException in that case
					new Cookie(addCookie.getName(), addCookie.getValue());

					if( addCookie.getMaxAge() == 0 ) {
						System.out.println("EXPIRED: " + addCookie);
					} else {
						builder = builder.cookie(addCookie.toCookie());
						//						builder = builder.cookie(addCookie);
						if( DEBUG ) {
							System.out.println("REMOTE-COOKIE: " + addCookie.toCookie());
							//							System.out.println("Extra cookie: " + DebugUtil.newCookieToString(addCookie));
						}
					}
					String domain = null; // req.getServerName()
					Cookie encoded = CookieProxy.encodeCookie(domain, addCookie);
					System.out.println("PROXY-COOKIE: " + DebugUtil.servletCookieToString(encoded));
					resp.addCookie(encoded);

				} catch( IllegalArgumentException e ) {
					System.err.println("SKIPPED: " + addCookie);
					System.err.println("MESSAGE: " + e.getMessage());
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
					if( urlStr.contains("t-square.gatech.edu") ) {
						// FIXME temporary workaround for t-square empty Location: header
						location = new URI("https://t-square.gatech.edu/portal");
					}
				} catch( URISyntaxException e ) {
					throw new ServletException("Redirect [" + status + "] but no Location given.", e);
				}
				throw new ServletException("Redirect [" + status + "] but no Location given.");
				// FIXME
				//				throw new ServletException("Redirect [" + status + "] but no Location given.");
			}
			if( DEBUG ) {
				System.out.println("LOCATION: " + location.toASCIIString());
			}

			isPost = false; // don't POST again when following redirects



			addCookies = response.getCookies(); // pass along any new cookies
			for( NewCookie newCookie : addCookies ) {
				if( newCookie.getDomain() == null ) {
					System.out.println("Setting domain to '" + url.getHost() + "' in: " + newCookie);
					//					System.out.println("null domain for " + DebugUtil.newCookieToString(newCookie));

					try {
						Field dfield = javax.ws.rs.core.Cookie.class.getDeclaredField("domain");
						dfield.setAccessible(true);
						dfield.set(newCookie, url.getHost());
					} catch( IllegalAccessException e ) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch( SecurityException e ) {
						e.printStackTrace();
					} catch( NoSuchFieldException e ) {
						e.printStackTrace();
					} catch( IllegalArgumentException e ) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			if( !location.isAbsolute() ) {
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
			String domain = localRequest.getServerName();
			int port = localRequest.getServerPort();
			if( port != 80 && port != 443 ) {
				domain += ":" + port;
			}
			List<Cookie> encodedCookies = CookieProxy.encodeCookies(
				domain, respCookies);
			for( Cookie proxyCookie : encodedCookies ) {
				localResponse.addCookie(proxyCookie);
				if( DEBUG ) {
					System.out.println("Added cookie: " + DebugUtil.servletCookieToString(proxyCookie));
				}
			}
			//			for( NewCookie newCookie : respCookies ) {
			//				try {
			//					Cookie clientCookie = new Cookie(newCookie.getName(), newCookie.getValue());
			//
			//					// FIXME double check this, what about the port?
			//					String domain = localRequest.getServerName();
			//					clientCookie.setDomain(domain);
			//
			//					// FIXME check the path
			//					clientCookie.setPath("/");
			//
			//					clientCookie.setMaxAge(newCookie.getMaxAge());
			//					clientCookie.setSecure(newCookie.isSecure());
			//					clientCookie.setComment(newCookie.getComment());
			//					clientCookie.setVersion(newCookie.getVersion());
			//
			//					localResponse.addCookie(clientCookie);
			//
			//					if( DEBUG ) {
			//						System.out.println("Added cookie: " + DebugUtil.servletCookieToString(clientCookie));
			//					}
			//				} catch( IllegalArgumentException e ) {
			//					// constructor rejects some names, such as date-formatted strings
			//					System.err.println("Couldn't transfer cookie: " + e.getMessage());
			//				}
			//			}
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

			if( DEBUG_REWRITES ) {
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

			if( DEBUG_REWRITES ) {
				System.out.println("Rewrote: '" + oldVal + "' -> '" + newVal + "'");
			}
		}

		return doc.toString();
	}
}
