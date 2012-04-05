package edu.gatech.cc.HTML2Mobile.datastore;

import java.util.List;

public interface H2MDatastoreController {

	/*
	 * Stores a new transform in the datastore
	 */
	public long newTransform(String name, String xsl);

	/*
	 * Retrieves a transform from the datastore
	 */
	public String getTransform(long id);

	/*
	 * Gets all the Transforms!
	 */
	public List<?> getAllTransforms();

	/*
	 * Deletes a transform from the datastore
	 */
	public boolean deleteTransform(long id);
}
