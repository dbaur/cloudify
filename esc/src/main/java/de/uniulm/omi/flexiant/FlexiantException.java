package de.uniulm.omi.flexiant;

/**
 * An exception class wrapping exceptions which occur during the communication with the
 * flexiant extility api.
 */
public class FlexiantException extends Exception {

    /**
     * @see java.lang.Exception#Exception(String)
     */
    public FlexiantException(String message) {
        super(message);
    }

    /**
     * @see java.lang.Exception#Exception(String, java.lang.Throwable)
     */
    public FlexiantException(String message, Throwable cause) {
        super(message, cause);
    }
}
