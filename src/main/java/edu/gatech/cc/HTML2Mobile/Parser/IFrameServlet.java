package edu.gatech.cc.HTML2Mobile.Parser;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.nodes.Document;

import edu.gatech.cc.HTML2Mobile.JSoupServlet;


@SuppressWarnings("serial")
public class IFrameServlet extends JSoupServlet {

	@Override
	public String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException {
		StringBuilder toRet = new StringBuilder("<iframe>\n");

		toRet.append("</iframe>\n");
		return toRet.toString();
	}

}
