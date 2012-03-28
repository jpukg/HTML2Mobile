package edu.gatech.cc.HTML2Mobile.transform;

/**
 * Transforms markup contents for the mobile proxy.
 */
public interface ITransformer {
	/**
	 * Transforms the contents of <code>contents</code>.
	 * 
	 * @param contents the contents to transform
	 * @throws NullPointerException if <code>contents</code> is <code>null</code>
	 * @throws TransformException if anything goes wrong
	 */
	public void transform(StringBuffer contents) throws TransformException;
}
