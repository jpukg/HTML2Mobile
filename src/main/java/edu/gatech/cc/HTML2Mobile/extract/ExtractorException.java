package edu.gatech.cc.HTML2Mobile.extract;

/**
 * Exception thrown by {@link IExtractor}.
 */
public class ExtractorException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ExtractorException() {
	}

	public ExtractorException(String message) {
		super(message);
	}

	public ExtractorException(Throwable cause) {
		super(cause);
	}

	public ExtractorException(String message, Throwable cause) {
		super(message, cause);
	}
}
