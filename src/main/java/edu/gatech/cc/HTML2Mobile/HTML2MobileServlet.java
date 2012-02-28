package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import edu.gatech.cc.HTML2Mobile.Parser.FormParserServlet;
import edu.gatech.cc.HTML2Mobile.Parser.LinkParserServlet;
import edu.gatech.cc.HTML2Mobile.Parser.MediaServlet;

@SuppressWarnings("serial")
public class HTML2MobileServlet extends JSoupServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
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


		Document doc = Jsoup.connect(url.toString()).get();

		String result = this.process(doc, req);

		// TODO(David) Replace this code with XSL transform
		PrintWriter writer = resp.getWriter();
		writer.write("<html><body><pre>");
		result = "<html2mobile><document>\n" + result + "\n</document></html2mobile>\n";
		writer.write(result.replace("<", "&lt;").replace(">", "&gt;"));
		writer.write("</pre></body></html>");

		//		// Set up Dummy XML
		//		File xmlFile = new File("html2mobile_1.xml");
		//		Source xmlSource = new StreamSource(xmlFile);
		//
		//		// Set up Dummy XSL
		//		File xsltFile = new File("html2mobile_1.xsl");
		//		Source xsltSource = new StreamSource(xsltFile);
		//
		//		// Apply XSL Transformation to XML
		//		TransformerFactory transFact = TransformerFactory.newInstance();
		//		try {
		//			Transformer trans = transFact.newTransformer(xsltSource);
		//			trans.transform(xmlSource, new StreamResult(resp.getWriter()));
		//		} catch (TransformerConfigurationException e) {
		//			writer.write("Error: Unable to read XSLT Source");
		//		} catch (TransformerException e) {
		//			writer.write("Error: Unable to read XML Source");
		//		}

		writer.flush();
	}

	@Override
	public String process(Document doc, HttpServletRequest req)
			throws ServletException, IOException {
		StringBuilder toRet = new StringBuilder();

		LinkParserServlet linkParser = new LinkParserServlet();
		MediaServlet mediaParser = new MediaServlet();
		FormParserServlet formParser = new FormParserServlet();

		toRet.append(linkParser.process(doc, req));
		toRet.append(mediaParser.process(doc, req));
		toRet.append(formParser.process(doc, req));

		return toRet.toString();
	}

}
