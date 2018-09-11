/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package my.reflowcontroller.rs232.api;

import gnu.io.SerialPort;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import my.reflowcontroller.rs232.manager.RS232;
import my.reflowcontroller.rs232.events.IRS232EventObserver;
import my.reflowcontroller.rs232.events.IRS232Events;
import my.reflowcontroller.rs232.exceptions.ComPortException;
import my.reflowcontroller.rs232.exceptions.ComPortInUseException;
import my.reflowcontroller.rs232.exceptions.ComPortNotFoundException;
import org.openide.util.NbPreferences;

/**
 *
 * @author hendriksit
 */
public class RS232ConnectionAPI {

    private static RS232ConnectionAPI instance = null;
    private String selectedComPort = "";
    private int currentDataRate = 9600;
    private ArrayList<IRS232EventObserver> observers = new ArrayList<IRS232EventObserver>();

    private RS232ConnectionAPI() {
    }

    /**
     * Get the RS232 Observers list. All observers are notifyed when a 
     * RS232 event occurs. 
     * @return The list of observers.
     */
    public ArrayList<IRS232EventObserver> getObservers() {
        return observers;
    }
    
    /**
     * Is this observer currently observing the RS232 events?
     * <tt>true</tt> is the observer is listening / <tt>false</tt> it is not.
     * @param obs The observer object that needs to be checked.
     * @return <tt>true</tt> is the observer is listening.
     */
    public boolean isObserver(IRS232EventObserver obs){
        return observers.contains(obs);
    }

    /**
     * Register an observer so that it can listen to RS232 events.
     * @param obs The observer object that wants to listen to the RS232 events.
     */
    public void registerObserver(IRS232EventObserver obs) {
        observers.add(obs);
    }

    /**
     * Unregister an observer so that it stops receiving the RS232 events.
     * @param obs The observer that is to be removed from the observer list.
     */
    public void unregisterObserver(IRS232EventObserver obs) {
        observers.remove(obs);
    }

    /**
     * Notify all the observers with a RS232Event.
     * @param event The RS232 event
     */
    public void notifyObservers(IRS232Events event) {
        for (Iterator<IRS232EventObserver> it = observers.iterator(); it.hasNext();) {
            IRS232EventObserver iEventObserver = it.next();
            iEventObserver.eventListener(event);
        }
    }

    /**
     * Get the singleton instance of the <tt>RS232ConnectionAPI</tt>
     * @return 
     */
    public static RS232ConnectionAPI getInstance() {
        if (instance == null) {
            instance = new RS232ConnectionAPI();
        }
        return instance;
    }

    /**
     * Get the currently selected COM port.
     * @return 
     */
    public String getComPort() {
        return selectedComPort;
    }

    /**
     * Set the selected COM port.
     * 
     * Note: Changing the selected COM port will take effect the next 
     * time a RS232 session is started. 
     * 
     * @param ComPort 
     */
    public void setComPort(String ComPort) {
        this.selectedComPort = ComPort;
    }

    /**
     * Get the data rate.
     * @return 
     */
    public int getDataRate() {
        return currentDataRate;
    }

    /**
     * Set the data rate.
     * 
     * Note: Changing the selected COM port will take effect 
     * the next time a RS232 session is started. 
     * 
     * @param dataRate The datarate
     */
    public void setDataRate(int dataRate) {

        this.currentDataRate = dataRate;
    }
    
    /**
     * Start a new RS232 session. 
     * 
     * Note: All changes to the session settings 
     * should be set before calling this method. 
     * 
     * @throws ComPortNotFoundException
     * @throws ComPortInUseException
     * @throws ComPortException 
     */
    public void startSession() throws ComPortNotFoundException, ComPortInUseException, ComPortException {
        RS232.getInstance().initialize();
    }

    /**
     * Stop the current RS232 session.
     */
    public void stopSession() {
        RS232.getInstance().close();
    }

    /**
     * Is there a session connected? 
     * <tt>true</tt> if it is / <tt>false</tt> otherwise.
     * @return 
     */
    public boolean isSessionConnected() {
        return RS232.getInstance().isConnected();
    }

    /**
     * Send data over the RS232 connection.
     * @param data
     * @throws IOException 
     */
    public void send(byte[] data) throws IOException {
        RS232.getInstance().send(data);
    }

    /**
     * Send data over the RS232 connection.
     * @param b
     * @throws IOException 
     */
    public void send(int b) throws IOException {
        RS232.getInstance().send(b);
    }

    /**
     * Send data over the RS232 connection.
     * @param data
     * @param off
     * @param len
     * @throws IOException 
     */
    public void send(byte[] data, int off, int len) throws IOException {
        RS232.getInstance().send(data, off, len);
    }
    
    /**
     * Get the data bits setting.
     * @return The data bits value.
     */
    public int getDataBits() {
        return NbPreferences.forModule(RS232ConnectionAPI.class).getInt("dataBits", SerialPort.DATABITS_8);
    }

    /**
     * Set the data bits. The change is Persistent 
     * so its value is still available if the 
     * application restarts.
     * 
     * Note: Changing the databits will take effect 
     * the next time a RS232 session is started. 
     * 
     * 
     * @param dataBits The data bits value.
     */
    public void setDataBits(int dataBits) {
        NbPreferences.forModule(RS232ConnectionAPI.class).putInt("dataBits", dataBits);
    }

    /**
     * Get the Stop bits setting.
     * @return The Stop bits value.
     */
    public int getStopBits() {
        return NbPreferences.forModule(RS232ConnectionAPI.class).getInt("stopBits", SerialPort.STOPBITS_1);
    }

    /**
     * Set the stop bits. The change is Persistent 
     * so its value is still available if the 
     * application restarts.
     * 
     * Note: Changing the stop bits will take effect 
     * the next time a RS232 session is started. 
     * 
     * @param stopBits The stopbits value
     */
    public void setStopBits(int stopBits) {
        NbPreferences.forModule(RS232ConnectionAPI.class).putInt("stopBits", stopBits);
    }

    /**
     * Get the Parity setting.
     * 
     * @return The parity value
     */
    public int getParity() {
        return NbPreferences.forModule(RS232ConnectionAPI.class).getInt("parity", SerialPort.PARITY_NONE);
    }

    /**
     * Set the parity. The change is Persistent 
     * so its value is still available if the 
     * application restarts.
     * 
     * Note: Changing the parity will take effect 
     * the next time a RS232 session is started. 
     * 
     * @see SerialPort
     * @param parity The parity setting (see <tt>SerialPort</tt>)
     */
    public void setParity(int parity) {
        NbPreferences.forModule(RS232ConnectionAPI.class).putInt("parity", parity);
    }
    
    /**
     * Get the response timeout setting.
     * @return The timeout value
     */
    public int getTimeOut() {
        return NbPreferences.forModule(RS232ConnectionAPI.class).getInt("timeOut", 2000);
    }

    /**
     * 
     * Set the reponse timeout. The change is Persistent 
     * so its value is still available if the 
     * application restarts.
     * 
     * Note: Changing the timeout will take effect 
     * the next time a RS232 session is started.
     * 
     *  
     * 
     * @param timeOut The timeout value
     */
    public void setTimeOut(int timeOut) {
        NbPreferences.forModule(RS232ConnectionAPI.class).putInt("timeOut", timeOut);
    }

    
}
