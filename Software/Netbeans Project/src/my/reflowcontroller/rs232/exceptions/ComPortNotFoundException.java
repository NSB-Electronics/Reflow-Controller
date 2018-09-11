/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package my.reflowcontroller.rs232.exceptions;

/**
 *
 * @author hendriksit
 */
public class ComPortNotFoundException extends ComPortException {

    
    
    public ComPortNotFoundException(String message) {
        super(message);
    }

    public ComPortNotFoundException(Throwable cause) {
        super(cause);
    }

    public ComPortNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    
    
}
