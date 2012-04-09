package edu.gatech.cc.HTML2Mobile;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
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


public class XslServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log =
		Logger.getLogger(FileUpload.class.getName());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	throws ServletException, IOException {
		PrintWriter writer = resp.getWriter();
		writer.println("<html>");
		writer.println("<body>");
		writer.println("<form method='post' enctype='multipart/form-data'>");
		writer.println("<input name='xslFile' type='file' size='40'> <br/>");
		writer.println("<input name='Submit' type='submit' value='Sumbit'>");
		writer.println("</body>");
		writer.println("</html>");
	}


	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		try {
			ServletFileUpload upload = new ServletFileUpload();
			res.setContentType("text/plain");

			FileItemIterator iterator = upload.getItemIterator(req);
			while (iterator.hasNext()) {
				FileItemStream item = iterator.next();
				InputStream stream = item.openStream();

				if (item.isFormField()) {
					log.warning("Got a form field: " + item.getFieldName());
				} else {
					log.warning("Got an uploaded file: " + item.getFieldName() +
							", name = " + item.getName());

					String xsl = Streams.asString(stream);
					res.getOutputStream().println(xsl);
				}
			}
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
	}
}