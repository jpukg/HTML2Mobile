package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
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

public class DumbProxyServlet extends HttpServlet {
	private static final Pattern hasHost =
			Pattern.compile("^(https?:)?//.*", Pattern.CASE_INSENSITIVE);
	public static String rewriteDirectResource(URL requestURL, String targetURL)
			throws MalformedURLException {
		// no rewrite when the host is included
		if( hasHost.matcher(targetURL).matches() ) {
			return targetURL;
		}

		// make relative URLs absolute
		if( !targetURL.startsWith("/") ) {
			targetURL = requestURL.getPath() + "/" + targetURL;
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
		// read requested URL
		String urlParam = req.getParameter("url");
		if( urlParam == null ) {
			throw new ServletException("No URL");
		}
		URL url = null;
		try {
			url = new URL(urlParam);
		} catch( MalformedURLException e ) {
			throw new ServletException("Cannot parse: " + urlParam, e);
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
				dumpServletCookie(cookie);
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

		//		ClientResponse response = builder.get(ClientResponse.class);
		if(!ClientResponse.Status.OK.equals(response.getClientResponseStatus()) ) {
			throw new ServletException("Server responded with: " + response.getStatus());
		}
		String contents = response.getEntity(String.class);

		// translate cookies back to our client
		List<NewCookie> respCookies = response.getCookies();
		if( respCookies != null ) {
			for( NewCookie newCookie : respCookies ) {
				dumpNewCookie(newCookie);
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

					System.out.println("Added cookie:");
					dumpServletCookie(clientCookie);
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
			String href = link.attr("href"),
					newHref = href;
			if( !hasHost.matcher(href).matches() ) {
				newHref =
						new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath() + "/" + href).toExternalForm();
			}
			newHref = requestURI + URLEncoder.encode(newHref, "UTF-8");
			rewrites.append('\'').append(href).append("' => '").append(newHref).append("'\n");
			link.attr("href", newHref);
		}

		// rewrite forms to post to us
		rewrites.append("-FORM-ACTIONS:\n");
		for( Element form : doc.select("form[action]") ) {
			String action = form.attr("action");
			String newAction;
			if( !hasHost.matcher(action).matches() ) {
				newAction = new URL(
					url.getProtocol(), url.getHost(), url.getPort(), url.getPath() + "/" + action).toExternalForm();
			} else {
				// preserve prefix for relative paths
				newAction = urlParam + "/" + action;
				System.out.println("urlParam: " + urlParam);
			}
			newAction = requestURI + URLEncoder.encode(newAction, "UTF-8");
			rewrites.append('\'').append(action).append("' => '").append(newAction).append("'\n");
			form.attr("action", newAction);
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

	private static void dumpServletCookie(Cookie cookie) {
		System.out.println("Cookie: name=" + cookie.getName() + ", value=" + cookie.getValue() +
			", domain=" + cookie.getDomain() + ", path=" + cookie.getPath() + ", comment=" + cookie.getComment() +
			", maxage=" + cookie.getMaxAge() + ", version=" + cookie.getVersion() + ", secure=" + cookie.getSecure());
	}

	private static void dumpNewCookie(NewCookie newCookie) {
		System.out.println("NewCookie: name=" + newCookie.getName() + ", value=" + newCookie.getValue() +
			", domain=" + newCookie.getDomain() + ", path=" + newCookie.getPath() + ", comment=" + newCookie.getComment() +
			", maxage=" + newCookie.getMaxAge() + ", version=" + newCookie.getVersion() + ", secure=" + newCookie.isSecure());
	}
}
