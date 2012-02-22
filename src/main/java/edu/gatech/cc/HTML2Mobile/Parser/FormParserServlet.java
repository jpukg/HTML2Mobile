package edu.gatech.cc.HTML2Mobile.Parser;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FormParserServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		String url = req.getParameter("q");
		if (url == null) {
			resp.getWriter().print("<div style='color:red;fontweight:bold'>");
			resp.getWriter().print("<center>Please  provide a valid url");
			resp.getWriter().println("</center></div>");
		} else {
			Document doc = Jsoup.parse(new URL(url), 1000);
			Elements forms = doc.select("form");
			for (Element e : forms) {
				FormObj fobj = new FormObj(e);
				resp.getWriter().println(fobj.toHtml2Mobile_2());
			}
		}
	}
}
