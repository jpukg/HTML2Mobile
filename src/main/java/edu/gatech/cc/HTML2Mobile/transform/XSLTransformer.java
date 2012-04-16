package edu.gatech.cc.HTML2Mobile.transform;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XSLTransformer implements ITransformer {

	@Override
	public void transform(StringBuffer contents) throws TransformException {

		StringBuilder writer = new StringBuilder();

		// Set up Dummy XML
		StringReader xmlReader = new StringReader(contents.toString());
		Source xmlSource = new StreamSource(xmlReader);

		// Set up Dummy XSL
		File xsltFile = new File("main.xsl");
		Source xsltSource = new StreamSource(xsltFile);

		// Apply XSL Transformation to XML
		TransformerFactory transFact = TransformerFactory.newInstance();
		try {
			Transformer trans = transFact.newTransformer(xsltSource);
			StringWriter output = new StringWriter();
			trans.transform(xmlSource, new StreamResult(output));
			writer.append(output.toString());//.replaceAll("&lt;", "<").replaceAll("&gt;", ">"));//.replaceAll("&amp;", "&"));
		} catch (TransformerConfigurationException e) {
			writer.append("Error: Unable to read XSLT Source");
		} catch (TransformerException e) {
			writer.append("Error: Unable to read XML Source");
		}
		contents.delete(0, contents.length());
		contents.append(writer.toString());
	}
}
