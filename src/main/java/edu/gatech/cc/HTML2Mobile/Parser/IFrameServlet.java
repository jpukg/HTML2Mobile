package edu.gatech.cc.HTML2Mobile.Parser;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.gatech.cc.HTML2Mobile.JSoupServlet;


@SuppressWarnings("serial")
public class IFrameServlet extends JSoupServlet {

	@Override
	public String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException {

		Elements eles = doc.select("iframe");
		BaseGroup group = new BaseGroup("iframe","iframegroup");
		for (Element a : eles) {
			group.addElement(a);
		}
		return group.toString();
	}

}
