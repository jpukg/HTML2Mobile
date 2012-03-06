package edu.gatech.cc.HTML2Mobile.Parser;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.gatech.cc.HTML2Mobile.JSoupServlet;


@SuppressWarnings("serial")
public class ContentParserServlet extends JSoupServlet {

	@Override
	public String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException {
		BaseGroup group = new BaseGroup("iframe","iframegroup");

		Elements eles = doc.select("[class*=content]");
		for (Element a : eles) {
			group.addElement(a);
		}
		eles = doc.select("[id*=content]");
		for (Element a : eles) {
			group.addElement(a);
		}
		return group.toString();
	}

}
