package edu.gatech.cc.HTML2Mobile.proxy;

/**
 * Common exception type for proxy related functionality.
 */
public class ProxyException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public ProxyException() {}
	public ProxyException(String message) {
		super(message);
	}
	public ProxyException(Throwable cause) {
		super(cause);
	}
	public ProxyException(String message, Throwable cause) {
		super(message, cause);
	}
}
