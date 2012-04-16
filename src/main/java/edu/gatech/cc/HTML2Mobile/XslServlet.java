package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.lang3.StringEscapeUtils;

import edu.gatech.cc.HTML2Mobile.datastore.Xsl;
import edu.gatech.cc.HTML2Mobile.datastore.XslDatastoreController;


public class XslServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log =
		Logger.getLogger(FileUpload.class.getName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		PrintWriter writer = resp.getWriter();
		XslDatastoreController xslDb = new XslDatastoreController();
		writer.println("<html>");
		writer.println("<body>");


		//Upload Form
		writer.println("<h1>Upload your XSL stylesheet:</h1>");
		writer.println("<form method='post' enctype='multipart/form-data'>");
		writer.println("<input name='xslFile' type='file' size='40'/> <br/>");
		writer.println("<input name='Submit' type='submit' value='Sumbit'/>");
		writer.println("</form>");

		//Display Existing XSL Forms
		List<Xsl> xslList = xslDb.getAllTransforms();
		writer.println("<h1>Existing XSL Stylsheets:</h1>");
		writer.println("<table>");
		writer.println("<tr>");
		writer.println("<th>ID</th>");
		writer.println("<th>Name</th>");
		writer.println("<th>Contents</th>");
		writer.println("</tr>");
		for(Xsl xsl : xslList) {
			writer.println("<tr>");
			writer.println("<td>" + xsl.getId() + "</td>");
			writer.println("<td>" + xsl.getName() + "</td>");
			writer.println("<td>" + StringEscapeUtils.escapeHtml4(xsl.getXsl()) + "</td>");
			writer.println("</tr>");
		}
		writer.println("</table>");

		writer.println("</body>");
		writer.println("</html>");
	}


	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		try {
			ServletFileUpload upload = new ServletFileUpload();
			XslDatastoreController xslDb = new XslDatastoreController();

			FileItemIterator iterator = upload.getItemIterator(req);
			while (iterator.hasNext()) {
				FileItemStream item = iterator.next();
				InputStream stream = item.openStream();

				if (item.isFormField()) {
					log.warning("Got a form field: " + item.getFieldName());
				} else {
					log.warning("Got an uploaded file: " + item.getFieldName() +
							", name = " + item.getName());
					String xslContents = Streams.asString(stream);
					xslDb.newTransform(item.getName(), xslContents);
				}
			}

			res.sendRedirect("/xsl");
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
	}
}