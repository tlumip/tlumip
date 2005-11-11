package com.pb.tlumip.ts.assign;

/**
 * This is thrown by the XmlRpcClient if the remote server reported an error.
 * If something went wrong at a lower level (e.g. no network connection) an
 * IOException will be thrown instead.
 *
 */
public class NoPathFoundException extends Exception
{
    /**
     * The fault code of the exception. For servers based on this library, this
     * will always be 0.
     */
    public int code = 0;

    /**
     * The underlying cause of this exception.
     */
    private Throwable cause;

    /**
     */
    public NoPathFoundException(Throwable cause)
    {
        this.cause = cause;
    }

    /**
     */
    public NoPathFoundException(String message, Throwable cause)
    {
        super(message);
        this.cause = cause;
    }

    /**
     */
    public NoPathFoundException(int code, String message)
    {
        this(code, message, null);
    }

    /**
     * Creates an instance with the specified message and root cause
     * exception.
     *
     * @param code The fault code for this problem.
     * @param message The message describing this exception.
     * @param cause The root cause of this exception.
     */
    public NoPathFoundException(int code, String message, Throwable cause)
    {
        super(message);
        this.code = code;
        this.cause = cause;
    }

    /**
     * Returns the cause of this throwable or null if the cause is nonexistent
     * or unknown. (The cause is the throwable that caused this throwable to
     * get thrown.)
     *
     * This implementation returns the cause that was supplied via the constructor,
     * according to the rules specified for a "legacy chained throwable" that
     * predates the addition of chained exceptions to Throwable.
     *
     * See the <a
     * href="http://java.sun.com/j2se/1.4.1/docs/api/java/lang/Throwable.html">JDK
     * 1.4 Throwable documentation</a> for more information.
     */
    public Throwable getCause()
    {
        return cause;
    }
}
