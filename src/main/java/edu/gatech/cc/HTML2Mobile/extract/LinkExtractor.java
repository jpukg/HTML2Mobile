package edu.gatech.cc.HTML2Mobile.extract;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import edu.gatech.cc.HTML2Mobile.Parser.LinkGroup;

/**
 * Extractor for link (<code>&lt;a&gt;</code>) elements.
 */
public class LinkExtractor implements IExtractor {
	/** Default value for <code>maxSteps</code>. */
	private static final int DEFAULT_MAX_STEPS = 3;
	/** Default value for <code>minGroupSize</code>. */
	private static final int DEFAULT_MIN_GROUP_SIZE = 0; // FIXME: restore this after orphan handling

	/** FIXME Max recursion depth per group? */
	protected int maxSteps = DEFAULT_MAX_STEPS;
	/** FIXME Minimum links needed to be considered a group? */
	protected int minGroupSize = DEFAULT_MIN_GROUP_SIZE;

	/** Creates a link extractor with default step and minimum group sizes. */
	public LinkExtractor() { }

	/**
	 * Creates a link extractor with the given max steps and minimum group sizes.
	 * 
	 * @param maxSteps     FIXME max recursion depth?
	 * @param minGroupSize FIXME minimum number of links to consider as a single group?
	 * @throws IllegalArgumentException if either argument is not positive
	 */
	public LinkExtractor(int maxSteps, int minGroupSize) {
		if( maxSteps < 1 ) {
			throw new IllegalArgumentException("maxSteps is not positive");
		}
		if( minGroupSize < 1 ) {
			throw new IllegalArgumentException("minGroupSize is not positive");
		}
		this.maxSteps = maxSteps;
		this.minGroupSize = minGroupSize;
	}

	@Override
	public void extract(Document doc, Writer out) throws ExtractorException {
		Map<Element, LinkGroup> parents = new HashMap<Element, LinkGroup>();
		try {
			Elements eles = doc.select("a");

			// Look at the appropriate parent of each "a" tag
			for (Element a : eles) {
				if (a.html().length() == 0) {
					continue;
				}
				Element parent = a.parent();

				if (parent.tagName().equals("li")) {
					// Lists (ul or ol)
					parent = parent.parent();
					if (!parents.containsKey(parent)) {
						parents.put(parent, new LinkGroup(parent, LinkGroup.LinkGroupFormats.list, false));
					}
					parents.get(parent).add(a);
				} else if (parent.tagName().equals("td")) {
					// Tables (does not handle "th". Should it?
					parent = parent.parent().parent();
					if (!parents.containsKey(parent)) {
						parents.put(parent, new LinkGroup(parent, LinkGroup.LinkGroupFormats.table, false));
					}
					parents.get(parent).add(a);
				} else {
					for (int i=0;i<maxSteps;i++) {
						/*
						 * No guarantee that these "a"s are  close or direct descendants.
						 * Need to find a better way of doing this.
						 */
						if (parent != null && parent.select("a").size() >= minGroupSize) {
							if (!parents.containsKey(parent)) {
								parents.put(parent, new LinkGroup(parent, LinkGroup.LinkGroupFormats.other, false));
							}
							parents.get(parent).add(a);
							break;
						} else {
							parent = parent.parent();
						}
					}
				}
			}
		} catch( IllegalStateException e ) {
			// can be thrown by JSoup
			throw new ExtractorException(e);
		}

		// write out the results
		try {
			out.append("<links><count>");
			out.append(String.valueOf(parents.size()));
			out.append("</count>\n");
			for( LinkGroup linkGroup : parents.values() ) {
				out.append(linkGroup.toXmlString());
			}
			out.append(LinkGroup.orphansToXmlString());
			out.append("</links>\n");
		} catch( IOException e ) {
			throw new ExtractorException(e);
		}
	}

}
