package edu.gatech.cc.HTML2Mobile.Parser;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.gatech.cc.HTML2Mobile.JSoupServlet;

public class FormParserServlet extends JSoupServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	protected String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException {
		StringBuilder sb = new StringBuilder();
		Elements forms = doc.select("form");
		for (Element e : forms) {
			FormObj fobj = new FormObj(e);
			sb.append(fobj.toHtml2Mobile_2()).append("\n");
		}
		return sb.toString();
	}
}
