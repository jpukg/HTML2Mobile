package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import edu.gatech.cc.HTML2Mobile.helper.DebugUtil;

/** Proof of concept proxying servlet. */
public class DumbProxyServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final boolean DEBUG = true;

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

	private boolean isPost = false;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		isPost = true;
		this.doGet(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if( DEBUG ) {
			DebugUtil.dumpRequestInfo(req);
		}

		// read requested URL
		String urlParam = req.getParameter("url");
		if( urlParam == null ) {
			throw new ServletException("No URL");
		}
		if( !hasHost(urlParam) ) {
			try {
				urlParam = new URI(req.getRequestURI()).getScheme() + "://" + urlParam;
			} catch( URISyntaxException e ) {
				throw new ServletException(e);
			}
		}
		URL url = null;
		try {
			url = new URL(urlParam);
		} catch( MalformedURLException e ) {
			throw new ServletException("Cannot parse: " + urlParam, e);
		}
		if( DEBUG ) {
			System.out.println("URL=" + url.toExternalForm());
		}

		String requestURI = req.getRequestURI() + "?url=";

		// fetch the URL
		Client client = Client.create();
		WebResource res = client.resource(urlParam);
		WebResource.Builder builder = res.getRequestBuilder();

		// transfer cookies to remote site
		Cookie[] cookies = req.getCookies();
		if( cookies != null ) {
			for( Cookie cookie : req.getCookies() ) {
				DebugUtil.dumpServletCookie(cookie);
				// FIXME translate the path
				javax.ws.rs.core.Cookie newCookie = new javax.ws.rs.core.Cookie(
					cookie.getName(), cookie.getValue(), cookie.getPath(), url.getHost());
				builder = builder.cookie(newCookie);
			}
		}


		ClientResponse response;
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
			response = builder.type("application/x-www-form-urlencoded")
					.post(ClientResponse.class, postParams);
		} else {
			response = builder.get(ClientResponse.class);
		}

		if( DEBUG ) {
			DebugUtil.dumpClientResponse(response);
		}

		//		ClientResponse response = builder.get(ClientResponse.class);
		if(!ClientResponse.Status.OK.equals(response.getClientResponseStatus()) ) {
			throw new ServletException("Server responded with: " + response.getStatus());
		}
		String contents = response.getEntity(String.class);

		// translate cookies back to our client
		List<NewCookie> respCookies = response.getCookies();
		if( respCookies != null ) {
			for( NewCookie newCookie : respCookies ) {
				if( DEBUG ) {
					DebugUtil.dumpNewCookie(newCookie);
				}
				try {
					Cookie clientCookie = new Cookie(newCookie.getName(), newCookie.getValue());

					// FIXME can't count on this header
					clientCookie.setDomain("localhost");
					// FIXME check the path
					clientCookie.setPath("/");
					clientCookie.setMaxAge(newCookie.getMaxAge());
					clientCookie.setSecure(newCookie.isSecure());
					clientCookie.setComment(newCookie.getComment());
					clientCookie.setVersion(newCookie.getVersion());

					resp.addCookie(clientCookie);

					if( DEBUG ) {
						System.out.println("Added cookie:");
						DebugUtil.dumpServletCookie(clientCookie);
					}
				} catch( IllegalArgumentException e ) {
					System.err.println("Couldn't transfer cookie.");
					System.err.println(e.getMessage());
				}
			}
		}

		// parse the document
		Document doc = Jsoup.parse(contents);

		//		Document doc = Jsoup.connect(url.toString()).get();

		// rewrite links to proxy through us
		StringBuilder rewrites = new StringBuilder("-LINKS:\n");
		for( Element link : doc.select("a[href]") ) {
			String href = link.attr("href");
			String newHref = requestURI +
					URLEncoder.encode(rewriteDirectResource(url, href), "UTF-8");
			link.attr("href", newHref);
			rewrites.append('\'').append(href).append("' => '").append(newHref).append("'\n");
		}

		// rewrite forms to post to us
		rewrites.append("-FORM-ACTIONS:\n");
		for( Element form : doc.select("form[action]") ) {
			String action = form.attr("action");
			String newAction = requestURI + URLEncoder.encode(
				rewriteDirectResource(url, action), "UTF-8");
			form.attr("action", newAction);
			rewrites.append('\'').append(action).append("' => '").append(newAction).append("'\n");
		}

		rewrites.append("\n-IMG/SCRIPT:\n");
		// rewrite images and javascript to request from the original server
		for( Element img : doc.select("img[src], script[src]") ) {
			String src = img.attr("src");
			String newSrc = rewriteDirectResource(url, src);
			if( !src.equals(newSrc) ) {
				rewrites.append('\'').append(src).append("' => '").append(newSrc).append("'\n");
				img.attr("src", newSrc);
			}
		}

		rewrites.append("\n-CSS:\n");
		// same thing for CSS
		for( Element css : doc.select("link[href]") ) {
			String href = css.attr("href");
			String newHref = rewriteDirectResource(url, href);
			if( !href.equals(newHref) ) {
				rewrites.append('\'').append(href).append("' => '").append(newHref).append("'\n");
				css.attr("href", newHref);
			}
		}

		// write out the new contents
		PrintWriter writer = resp.getWriter();
		writer.write(doc.toString());

		// Debugging info
		writer.write("\n\n<!--\n");
		writer.write("requestURI: " + req.getRequestURI() + "\n");
		writer.write("contextPath: " + req.getContextPath() + "\n");
		writer.write("pathInfo: " + req.getPathInfo() + "\n");
		writer.write("REWRITES:\n");
		writer.write(rewrites.toString());
		writer.write("\n-->\n");
		writer.flush();

		isPost = false;
	}
}
