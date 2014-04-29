package de.uniulm.omi.flexiant;

/**
 * Created by daniel on 28.04.14.
 */
public class FlexiantException extends Exception {
    public FlexiantException(String message) {
        super(message);
    }

    public FlexiantException(String message, Throwable cause) {
        super(message, cause);
    }
}
