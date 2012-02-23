package edu.gatech.cc.HTML2Mobile.Parser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.gatech.cc.HTML2Mobile.JSoupServlet;

public class LinkParserServlet extends JSoupServlet {

	public static final int MAX_STEPS = 3;
	public static final int MIN_GROUP_SIZE = 3;

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
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

		Document doc = Jsoup.parse(url, 1000);
		resp.getWriter().println(process(doc, req));
	}

	/*
	 * (non-Javadoc)
	 * @see edu.gatech.cc.HTML2Mobile.JSoupServlet#process(org.jsoup.nodes.Document, javax.servlet.http.HttpServletRequest)
	 * 
	 * Still need to convert links to map back to us correctly. Add
	 * get variables to define session/user info?
	 */
	@Override
	protected String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException {
		Map<Element, LinkGroup> parents = new HashMap<Element, LinkGroup>();
		Elements eles = doc.select("a");

		// Look at the appropriate parent of each "a" tag
		for (Element a : eles) {
			Element parent = a.parent();

			if (parent.tagName().equals("li")) {
				// Lists (ul or ol)
				parent = parent.parent();
				if (!parents.containsKey(parent)) {
					parents.put(parent, new LinkGroup(parent, LinkGroup.LinkGroupFormats.list, false));
				}
				parents.get(parent).add(a);
			} else if (parent.tagName().equals("td")) {
				// Tables (does not handle "th". Should it?
				parent = parent.parent().parent();
				if (!parents.containsKey(parent)) {
					parents.put(parent, new LinkGroup(parent, LinkGroup.LinkGroupFormats.table, false));
				}
				parents.get(parent).add(a);
			} else {
				for (int i=0;i<MAX_STEPS;i++) {
					/*
					 * No guarantee that these "a"s are  close or direct descendants.
					 * Need to find a better way of doing this.
					 */
					if (parent != null && parent.select("a").size() > MIN_GROUP_SIZE) {
						if (!parents.containsKey(parent)) {
							parents.put(parent, new LinkGroup(parent, LinkGroup.LinkGroupFormats.other, false));
						}
						parents.get(parent).add(a);
						break;
					} else {
						parent = parent.parent();
					}
				}
			}
		}
		StringBuilder sb = new StringBuilder("<links>\n");
		for (LinkGroup g : parents.values()) {
			sb.append(g.toXmlString()).append("\n");
		}
		return sb.append("</links>").toString();
	}

}
