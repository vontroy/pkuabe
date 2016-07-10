package pku.abe.commons.switcher;

public class SwitcherCloseException extends SwitcherException {

    private static final long serialVersionUID = 3131593079137433432L;

    public SwitcherCloseException() {
        super();
    }

    public SwitcherCloseException(String message, Throwable cause) {
        super(message, cause);
    }

    public SwitcherCloseException(String message) {
        super(message);
    }

    public SwitcherCloseException(Throwable cause) {
        super(cause);
    }

}
