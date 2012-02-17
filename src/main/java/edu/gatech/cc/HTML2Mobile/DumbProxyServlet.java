package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;

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

		// fetch the URL
		Client client = Client.create();
		WebResource res = client.resource(urlParam);
		String contents = res.get(String.class);

		// parse the document
		Document doc = Jsoup.parse(contents);

		String requestURI = req.getRequestURI() + "?url=";

		// rewrite links to proxy through us
		StringBuilder rewrites = new StringBuilder("-LINKS:\n");
		for( Element link : doc.select("a[href]") ) {
			String href = link.attr("href"),
					newHref = href;
			if( !hasHost.matcher(href).matches() ) {
				newHref =
						new URL(url.getProtocol(), url.getHost(), url.getPort(), href).toExternalForm();
			}
			newHref = requestURI + URLEncoder.encode(newHref, "UTF-8");
			rewrites.append('\'').append(href).append("' => '").append(newHref).append("'\n");
			link.attr("href", newHref);
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
	}
}
