package pku.abe.commons.switcher;

public class SwitcherNotFindException extends SwitcherException {

    private static final long serialVersionUID = -1279964914956915480L;

    public SwitcherNotFindException() {}

    /**
     * @param message
     */
    public SwitcherNotFindException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public SwitcherNotFindException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public SwitcherNotFindException(String message, Throwable cause) {
        super(message, cause);
    }

}
