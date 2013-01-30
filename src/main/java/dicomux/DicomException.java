package dicomux;

public class DicomException extends Exception {
	public DicomException() {
		super();
	}

	public DicomException(String message, Throwable cause) {
		super(message, cause);
	}

	public DicomException(String message) {
		super(message);
	}

	public DicomException(Throwable cause) {
		super(cause);
	}
}
