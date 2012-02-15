package edu.gatech.cc.HTML2Mobile;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

@SuppressWarnings("serial")
public class HTML2MobileServlet extends HttpServlet {
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws IOException {
		// Set up Dummy XML
		File xmlFile = new File("html2mobile_1.xml");
		Source xmlSource = new StreamSource(xmlFile);

		// Set up Dummy XSL
		File xsltFile = new File("html2mobile_1.xsl");
		Source xsltSource = new StreamSource(xsltFile);

		// Apply XSL Transformation to XML
		TransformerFactory transFact = TransformerFactory.newInstance();
		try {
			Transformer trans = transFact.newTransformer(xsltSource);
			trans.transform(xmlSource, new StreamResult(resp.getWriter()));
		} catch (TransformerConfigurationException e) {
			resp.getWriter().println("Error: Unable to read XSLT Source");
		} catch (TransformerException e) {
			resp.getWriter().println("Error: Unable to read XML Source");
		}
	}
}
