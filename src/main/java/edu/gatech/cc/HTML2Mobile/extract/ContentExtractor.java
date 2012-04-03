package edu.gatech.cc.HTML2Mobile.extract;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * An extractor that rewrites links for proxy purposes.
 * <p>
 * Note this extractor does not produce any output, it simply modifies URLs in
 * the document.
 * </p>
 */
public class ContentExtractor implements IExtractor {

	// final variables
	public final static int COUNT = 100;
	public final static double PERCENTAGE = 0.20;
	public final static int SUMMARY_LENGTH = 50;
	// general extraction variables
	public static Set<String> skip;
	public static Set<String> base;

	static {
		skip = new HashSet<String>();
		skip.add("script");
		skip.add("head");
		skip.add("form");

		base = new HashSet<String>();
		base.add("table");
	}

	// control variables
	private boolean use_count = true;
	private boolean use_parent_text = true;
	// extraction threshold
	private double content_percentage_threshold;
	private int content_count_threshold;

	public ContentExtractor(int count) {
		this(count, PERCENTAGE, true);
	}

	public ContentExtractor(double percentage) {
		this(COUNT, percentage, false);
	}

	public ContentExtractor(int count, double percentage, boolean use_count) {
		this.content_count_threshold = count;
		this.content_percentage_threshold = percentage;
		this.use_count = use_count;
	}

	/**
	 * {@inheritDoc} Rewrites URLs in <code>doc</code>.
	 */
	@Override
	public void extract(Document doc, Writer out) throws ExtractorException {
		if (out == null || doc == null) {
			throw new ExtractorException(
					"Invalid parameters: parameters cannot be null");
		}
		if (base == null || skip == null) {
			throw new ExtractorException(
					"Invalid parameters: parameters cannot be null");
		}
		try {
			out.append("\t<content>");
			Boolean temp = extract(doc.body(), out, doc.body().text().length());
			out.append("\n\t</content>");
			/*
			 * temp is only true if content was written to output. Extraction
			 * controller can take ignore this exception.
			 */
			if (temp != null && !temp) {
				throw new ExtractorException(
						"Error extracting content. No content found.");
			} // else success
		} catch (IOException e) {
			throw new ExtractorException(
					"Unknown error writing extracted content to output.");
		}
	}

	private Boolean extract(Element ele, Writer out, int reference_count) {
		if (!skip.contains(ele.tagName())) {
			if (!base.contains(ele.tagName())) {
				// if still enought text to parse
				if ((use_count && ele.text().length() >= content_count_threshold)
						|| (!use_count && (double) ele.text().length()
								/ (double) reference_count > content_percentage_threshold)) {
					boolean result = false;
					/*
					 * Test each child to see if they have enough content. If
					 * any child has enough content then it is assumed that the
					 * other child should as well (this could be very wrong). If
					 * any child appends its content then all non-appending
					 * children are ignored.
					 * 
					 * Assumptions: ------------ - "base" children always
					 * contain content - "skip" children never conatains content
					 * - If any child appends then all children containing
					 * content should append, excluding "base" children.
					 */
					List<Element> bases = new ArrayList<Element>();
					for (Element child : ele.children()) {
						if (bases.contains(child.tag())) {
							bases.add(child);
						} else {
							Boolean temp = extract(child, out,
									use_parent_text ? ele.text().length()
											: reference_count);
							if (temp == null) {
								// element is the anscestor of a base
								bases.add(ele);
							} else {
								result = result || temp;
							}
						}
					}
					if (result) {
						// append all the bases as seperate contents
						for (Element base : bases) {
							append(base, out);
						}
						return true;
					} else {
						// has enough content, but none of its children did, so
						// APPEND IT!!!
						append(ele, out);
						return true;
					}
				} else {
					// not enough text to parse. Let parent element handle
					// current text.
					return false;
				}
			} else {
				// base element: { table }. Let parent handle.
				return null;
			}
		} else {
			// element should be skipped: { script, head }
			return false;
		}
	}

	private void append(Element ele, Writer out) {
		try {
			out.append("\n\t\t<section>\n\t\t\t<summary>");
			String text = ele.text();
			out.append(text.substring(
					0,
					SUMMARY_LENGTH >= text.length() ? text.length()-1
							: SUMMARY_LENGTH));
			out.append("...\n\t\t\t</summary>\n\t\t\t<text>");
			out.append(text);
			out.append("\n\t\t\t</text>\n\t\t</section>");
		} catch (IOException e) {
			throw new ExtractorException(
					"Unknown error writing extracted content to output.");
		}
	}
}
