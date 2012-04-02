package edu.gatech.cc.HTML2Mobile.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Runs a set of transformers.
 */
public class TransformController {
	/** The transformers. */
	protected List<ITransformer> transformers;

	/**
	 * Creates an extraction controller.
	 * @param transformers the initial transformer list
	 * @throws NullPointerException if <code>transformers</code> is <code>null</code>
	 *                              or contains a <code>null</code> element
	 */
	public TransformController(ITransformer... transformers) {
		if( transformers == null ) {
			throw new NullPointerException("transformers is null");
		}
		this.transformers = createTransformerList(transformers.length);
		addTransformers(transformers);
	}

	/**
	 * Instantiates the list implementation.
	 * 
	 * @param size the initial size, a negative value
	 *             should be interpreted as using the implementation default
	 * @return the list implementation
	 */
	protected List<ITransformer> createTransformerList(int size) {
		if( size < 0 ) {
			return new ArrayList<ITransformer>();
		} else {
			return new ArrayList<ITransformer>(size);
		}
	}

	/**
	 * Runs the transformers on <code>contents</code>.
	 * 
	 * @param contents the contents to transform
	 * @throws TransformException if anything goes wrong
	 * @throws NullPointerException if <code>contents</code> is <code>null</code>
	 */
	public void transform(StringBuffer contents) throws TransformException {
		if( contents == null ) {
			throw new NullPointerException("contents is null");
		}
		for( ITransformer transformer : transformers ) {
			transformer.transform(contents);
		}
	}

	/**
	 * Adds one or more transformers.
	 * @param transformers the transformers to add
	 * @throws NullPointerException if any transformer is <code>null</code>
	 */
	public void addTransformers(ITransformer... transformers) {
		if( transformers != null ) {
			for( ITransformer transformer : transformers ) {
				if( transformer == null ) {
					throw new NullPointerException("an transformer is null");
				}
				this.transformers.add(transformer);
			}
		}
	}

	/**
	 * Removes one or more transformers.
	 * @param transformers the transformers to remove
	 */
	public void removeTransformers(ITransformer... transformers) {
		if( transformers != null ) {
			this.transformers.removeAll(Arrays.asList(transformers));
		}
	}

	/**
	 * Returns an unmodifiable view of the transformers.
	 * @return the transformers
	 */
	public List<ITransformer> getTransformers() {
		return Collections.unmodifiableList(transformers);
	}
}
