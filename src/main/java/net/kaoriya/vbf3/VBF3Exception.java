package net.kaoriya.vbf3;

public class VBF3Exception extends Exception {

    String message;
    Throwable cause;

    VBF3Exception(String message) {
        this(message, null);
    }

    VBF3Exception(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.cause = cause;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    public final static class ParameterMismatch extends VBF3Exception {
        ParameterMismatch(RedisVBF3.Props want, RedisVBF3.Props got) {
            // TODO: better message
            super("mismatch parameter");
        }
    }

    public final static class InvalidLife extends VBF3Exception {
        InvalidLife(short life, short min, short max) {
            // TODO: better message
            super("invalid life range");
        }
    }

    public final static class NoGenerationInfo extends VBF3Exception {
        NoGenerationInfo() {
            // TODO: better message
            super("no generation information");
        }
    }

    public final static class TooBigLife extends VBF3Exception {
        TooBigLife(short max) {
            super(String.format("life should be less than (<=) %d", max));
        }
    }
}
