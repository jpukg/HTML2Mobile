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

		/*
		 * Do we want to support embedded youtube videos as well? (Flash)
		 * I think they are doing it in an iframe now so we may be able
		 * to force it to "fallback" to html5. Check the source of:
		 * 
		 * http://www.youtube.com/embed/kPdYpST_yoE?version=3&amp;rel=1&amp;fs=1&amp;showsearch=0&amp;showinfo=1&amp;iv_load_policy=1&amp;wmode=transparent
		 */
		// Grab Videos(HTML5)
		for(Element media : doc.select("video")) {
			toRet.append("<video type='html5'>\n");
			toRet.append(printAttributes(media.attributes()));
			for(Element source : media.children()) {
				toRet.append("<").append(source.tagName()).append(">\n");
				toRet.append(printAttributes(source.attributes()));
				toRet.append("</").append(source.tagName()).append(">\n");
			}
			toRet.append("</video>\n");
		}

		// Grab Videos(Youtube)
		for(Element media : doc.select("embed[src*=http://www.youtube.com], iframe[src*=http://www.youtube.com]")) {
			toRet.append("<video type='youtube'>\n");
			toRet.append(printAttributes(media.attributes()));
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
