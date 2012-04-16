package edu.gatech.cc.HTML2Mobile.datastore;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import com.google.appengine.api.datastore.Text;

@PersistenceCapable
public class Xsl {
	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	@Persistent
	private String name;

	@Persistent
	private Text xsl;

	public Xsl(String name, String xsl) {
		this.name = name;
		this.xsl = new Text(xsl);
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getXsl() {
		return xsl.getValue();
	}

	public void setXsl(String xsl) {
		this.xsl = new Text(xsl);
	}
}
