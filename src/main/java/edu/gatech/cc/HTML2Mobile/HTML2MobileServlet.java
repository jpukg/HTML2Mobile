package edu.gatech.cc.HTML2Mobile;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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
		if (req.getParameter("debug") != null) {
			writer.write("<html><body><pre>");
			result = "<html2mobile><document>\n" + result + "\n</document></html2mobile>\n";
			writer.write(result.replace("<", "&lt;").replace(">", "&gt;"));
			writer.write("</pre></body></html>");
		} else {

			result = "<html2mobile><document>\n" + result + "\n</document></html2mobile>\n";

			// Set up Dummy XML
			StringReader xmlReader = new StringReader(result.replaceAll("&", "&amp;"));
			Source xmlSource = new StreamSource(xmlReader);

			// Set up Dummy XSL
			File xsltFile = new File("html2mobile_v3.xsl");
			//		File xsltFile = new File("html2mobile_1.xsl");
			Source xsltSource = new StreamSource(xsltFile);

			// Apply XSL Transformation to XML
			TransformerFactory transFact = TransformerFactory.newInstance();
			try {
				Transformer trans = transFact.newTransformer(xsltSource);
				StringWriter output = new StringWriter();
				//			trans.transform(xmlSource, new StreamResult(resp.getWriter()));
				trans.transform(xmlSource, new StreamResult(output));
				writer.append(output.toString());//.replaceAll("&lt;", "<").replaceAll("&gt;", ">"));//.replaceAll("&amp;", "&"));
			} catch (TransformerConfigurationException e) {
				writer.write("Error: Unable to read XSLT Source");
			} catch (TransformerException e) {
				writer.write("Error: Unable to read XML Source");
			}
		}
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
