package edu.gatech.cc.HTML2Mobile.datastore;

import java.util.List;

import javax.jdo.PersistenceManager;
import javax.jdo.Query;


public class XslDatastoreController implements H2MDatastoreController{

	PersistenceManager pm;

	public XslDatastoreController() {
		pm = PMF.get().getPersistenceManager();
	}

	@Override
	public boolean deleteTransform(long id) {
		Xsl myXsl = pm.getObjectById(Xsl.class, id);
		pm.deletePersistent(myXsl);
		return true;
	}

	@Override
	public String getTransform(long id) {
		return pm.getObjectById(Xsl.class, id).getXsl();
	}

	@Override
	public List<Xsl> getAllTransforms() {
		Query myQuery = pm.newQuery(Xsl.class);
		List<Xsl> queryList = (List<Xsl>) myQuery.execute();
		return queryList;
	}

	@Override
	public long newTransform(String name, String xsl) {
		Xsl newXsl = new Xsl(name, xsl);
		pm.makePersistent(newXsl);
		return newXsl.getId();
	}
}
