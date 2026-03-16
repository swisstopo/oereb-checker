package ch.swisstopo.oerebchecker.models;

public class ResponseStatusCode {
    public static final int OK = 200;
    public static final int NO_CONTENT = 204;
    public static final int SEE_OTHER = 303;
    public static final int INTERNAL_SERVER_ERROR = 500;

    // use only for followOneRedirect
    public static final int MOVED_PERMANENTLY = 301;
    public static final int FOUND = 302;
    // public static final int SEE_OTHER = 303;
    public static final int TEMPORARY_REDIRECT = 307;
    public static final int PERMANENT_REDIRECT = 308;
}
