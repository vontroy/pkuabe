package pku.abe.commons.switcher;

public class SwitcherException extends RuntimeException {
    private static final long serialVersionUID = -2968779530742478292L;

    public SwitcherException() {}

    /**
     * @param message
     */
    public SwitcherException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public SwitcherException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public SwitcherException(String message, Throwable cause) {
        super(message, cause);
    }

}
