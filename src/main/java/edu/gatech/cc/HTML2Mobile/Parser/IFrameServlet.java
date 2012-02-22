package edu.gatech.cc.HTML2Mobile.Parser;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.nodes.Document;

import edu.gatech.cc.HTML2Mobile.JSoupServlet;


public class IFrameServlet extends JSoupServlet {

	@Override
	protected String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException {
		StringBuilder toRet = new StringBuilder("<media>\n");

		toRet.append("</media>\n");
		return toRet.toString();
	}

}
