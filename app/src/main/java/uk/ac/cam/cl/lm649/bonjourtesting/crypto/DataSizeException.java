package uk.ac.cam.cl.lm649.bonjourtesting.crypto;

public class DataSizeException extends Exception {

    public DataSizeException(String message) {
        super(message);
    }

    public DataSizeException(Throwable e) {
        super(e);
    }

    public DataSizeException(String message, Throwable e) {
        super(message, e);
    }

    public DataSizeException() {
        super();
    }

}
