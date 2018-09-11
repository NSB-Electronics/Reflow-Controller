/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package my.reflowcontroller.rs232.manager;

import my.reflowcontroller.rs232.exceptions.ComPortInUseException;
import my.reflowcontroller.rs232.exceptions.ComPortException;
import my.reflowcontroller.rs232.exceptions.ComPortNotFoundException;
import my.reflowcontroller.rs232.events.DisconnectedRS232Event;
import my.reflowcontroller.rs232.events.DataReceivedRS232Event;
import my.reflowcontroller.rs232.events.ConnectedRS232Event;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.TooManyListenersException;
//import my.datalogger.rs232.UISettings;
import my.reflowcontroller.rs232.api.RS232ConnectionAPI;
import org.openide.util.Exceptions;

/**
 *
 * @author hendriksit
 */
public class RS232 implements SerialPortEventListener {

    private static RS232 instance = null;
    private boolean connected = false;
    SerialPort serialPort;
    private InputStream input;
    private OutputStream output;
    private FileOutputStream log;
    private String filepath;
    private boolean logging;

    private RS232() {
//        setLogging(UISettings.getInstance().isLogging());
    }

    public static RS232 getInstance() {
        if (instance == null) {
            instance = new RS232();
        }
        return instance;
    }

    public boolean isConnected() {
        return connected;
    }

    public void initialize() throws ComPortNotFoundException, ComPortInUseException, ComPortException {
        if (isConnected()) {
            close();
        }

//        initializeLogFile();
        CommPortIdentifier portId = initializeCommPortFinder();

        if (portId == null) {
            throw new ComPortNotFoundException("Could not find COM port.");
        }
        try {
            initializeCommPort(portId);
        } catch (PortInUseException ex) {
            throw new ComPortInUseException(ex.getCause());
        } catch (Exception ex) {
            throw new ComPortException(ex.getCause());
        }

    }

//    private void initializeLogFile() {
//        if (logging) {
//            filepath = UISettings.getInstance().getLogPath();
//
//            if (filepath.length() != 0) {
//                try {
//                    log = new FileOutputStream(new File(filepath));
//                } catch (FileNotFoundException ex) {
//                    Exceptions.printStackTrace(ex);
//                }
//
//            }
//        }
//    }

    private CommPortIdentifier initializeCommPortFinder() {
        CommPortIdentifier portId = null;
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();

        // iterate through, looking for the port
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();

            if (currPortId.getName().equals(RS232ConnectionAPI.getInstance().getComPort())) {
                portId = currPortId;
                break;
            }
        }
        return portId;
    }

    private void initializeCommPort(CommPortIdentifier portId) throws PortInUseException, TooManyListenersException, UnsupportedCommOperationException, IOException {
        // open serial port, and use class name for the appName.
        
        serialPort = (SerialPort) portId.open(this.getClass().getName(),
                2000);
        // set port parameters
        serialPort.setSerialPortParams(9600,8,1,0);
//        serialPort = (SerialPort) portId.open(this.getClass().getName(),
//                RS232ConnectionAPI.getInstance().getTimeOut());
//        System.out.println("initializeCommPort");
//        // set port parameters
//        serialPort.setSerialPortParams(RS232ConnectionAPI.getInstance().getDataRate(),
//                RS232ConnectionAPI.getInstance().getDataBits(),
//                RS232ConnectionAPI.getInstance().getStopBits(),
//                RS232ConnectionAPI.getInstance().getParity());

        // open the streams
        input = serialPort.getInputStream();
        output = serialPort.getOutputStream();

        // add event listeners
        serialPort.addEventListener(this);
        serialPort.notifyOnDataAvailable(true);
        connected = true;


        ConnectedRS232Event event = new ConnectedRS232Event();
        RS232ConnectionAPI.getInstance().notifyObservers(event);


    }

    @Override
    public void serialEvent(SerialPortEvent spe) {
        if (spe.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {

                int available = input.available();
                byte chunk[] = new byte[available];
                input.read(chunk, 0, available);
                if (log != null) {
                    log.write(chunk);
                }

                DataReceivedRS232Event event = new DataReceivedRS232Event(chunk);
                RS232ConnectionAPI.getInstance().notifyObservers(event);

            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * This should be called when you stop using the port.
     * This will prevent port locking on platforms like Linux.
     */
    public synchronized void close() {
        if (serialPort != null) {
            try {
                output.close();
                input.close();
                serialPort.removeEventListener();
                serialPort.close();
                connected = false;
                if (log != null) {
                    log.close();
                    log = null;
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }

            DisconnectedRS232Event event = new DisconnectedRS232Event();
            RS232ConnectionAPI.getInstance().notifyObservers(event);

        }
    }

    public void send(byte[] data) throws IOException {
        output.write(data);
    }

    public void send(int b) throws IOException {
        output.write(b);
    }

    public void send(byte[] data, int off, int len) throws IOException {
        output.write(data, off, len);
    }

//    public void setLogging(boolean logging) {
//        this.logging = logging;
//    }
}
