/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package my.reflowcontroller.rs232.exceptions;

/**
 *
 * @author hendriksit
 */
public class ComPortInUseException extends ComPortException {

    public ComPortInUseException(String message) {
        super(message);
    }

    public ComPortInUseException(Throwable cause) {
        super(cause);
    }

    public ComPortInUseException(String message, Throwable cause) {
        super(message, cause);
    }

    
    
}
