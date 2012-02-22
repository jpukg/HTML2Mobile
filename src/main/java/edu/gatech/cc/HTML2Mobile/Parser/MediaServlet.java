package edu.gatech.cc.HTML2Mobile.Parser;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import edu.gatech.cc.HTML2Mobile.JSoupServlet;

@SuppressWarnings("serial")
public class MediaServlet extends JSoupServlet {

	@Override
	protected String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException {
		StringBuilder toRet = new StringBuilder("<media>\n");

		// Grab Pictures
		for(Element media : doc.select("img[src]")) {
			toRet.append("<picture>\n");
			toRet.append(printAttributes(media.attributes()));
			toRet.append("</picture>\n");
		}

		// Grab Videos(HTML5)
		for(Element media : doc.select("video")) {
			toRet.append("<video>\n");
			toRet.append(printAttributes(media.attributes()));
			for(Element source : media.children()) {
				toRet.append("<" + source.tagName() + ">\n");
				toRet.append(printAttributes(source.attributes()));
				toRet.append("</" + source.tagName() + ">\n");
			}
			toRet.append("</video>\n");
		}

		// Grab Audio(HTML5)
		for(Element media : doc.select("audio")) {
			toRet.append("<audio>\n");
			toRet.append(printAttributes(media.attributes()));
			for(Element source : media.children()) {
				toRet.append("<" + source.tagName() + ">\n");
				toRet.append(printAttributes(source.attributes()));
				toRet.append("</" + source.tagName() + ">\n");
			}
			toRet.append("</audio>\n");
		}

		toRet.append("</media>\n");
		return toRet.toString();
	}

	private String printAttributes(Attributes attribs) {
		StringBuilder toRet = new StringBuilder();
		for(Attribute attrib : attribs) {
			if(!attrib.getValue().isEmpty()) {
				toRet.append("<" + attrib.getKey() + ">" + attrib.getValue() + "</" + attrib.getKey() + ">\n");
			}
		}
		return toRet.toString();
	}
}
