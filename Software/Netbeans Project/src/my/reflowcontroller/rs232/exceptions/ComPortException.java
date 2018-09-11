/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package my.reflowcontroller.rs232.exceptions;

/**
 *
 * @author hendriksit
 */
public class ComPortException extends Exception {

    public ComPortException(String message) {
        super(message);
    }

    public ComPortException(Throwable cause) {
        super(cause);
    }

    public ComPortException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
