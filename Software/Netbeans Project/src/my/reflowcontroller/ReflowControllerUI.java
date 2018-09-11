/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package my.reflowcontroller;

import my.reflowcontroller.rs232.exceptions.ComPortInUseException;
import my.reflowcontroller.rs232.exceptions.ComPortException;
import my.reflowcontroller.rs232.exceptions.ComPortNotFoundException;
import my.reflowcontroller.rs232.events.DataReceivedRS232Event;
import my.reflowcontroller.rs232.events.DisconnectedRS232Event;
import my.reflowcontroller.rs232.events.IRS232EventObserver;
import my.reflowcontroller.rs232.events.ConnectedRS232Event;
import my.reflowcontroller.rs232.events.IRS232Events;
import java.io.File;
import java.io.*;
import javax.swing.JFileChooser;
import gnu.io.CommPortIdentifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import javax.swing.DefaultComboBoxModel;
import my.reflowcontroller.rs232.api.RS232ConnectionAPI;
import my.my.reflowcontroller.rs232.HexBinOctUtils;
import org.openide.util.Exceptions;
import java.io.FileWriter;
import java.io.IOException;
import java.text.*;
import javax.swing.Timer;
import javax.swing.table.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.io.FilenameUtils;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import java.awt.Desktop;
import javax.swing.text.JTextComponent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.ui.RectangleEdge;
import org.jfree.chart.ChartUtilities;
import java.awt.Color;
import javax.swing.text.DefaultCaret;

import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYSeries;
        
import org.apache.commons.lang.math.NumberUtils;

import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;

/**
 *
 * @author Nicholas Barnes
 */
public class ReflowControllerUI extends javax.swing.JFrame implements IRS232EventObserver {

    // Global variables
    char lastRequest = ' ';

    StringBuilder serialDataStr = new StringBuilder();
    int dataCount = 0;
    int lastEventNumber = 0;
    int updateCount = 0;
    boolean isConnected = false;
    boolean portFocused = false;
    int DELAY_SEND = 50; // Milliseconds to delay between settings send
    int readCount = 0;
    
    Timer timer;
    
    String chartTitle = "Oven Temperature";
    String xAxisLabel = "Time (Seconds)";
    String yAxisLabel = "Temperature (°C)";
    
    XYSeriesCollection dataset = new XYSeriesCollection();

    JFreeChart chart = ChartFactory.createXYLineChart(chartTitle,
            xAxisLabel, yAxisLabel, dataset);
    
    XYSeries seriesSetpoint = new XYSeries("Setpoint");
    XYSeries seriesActual = new XYSeries("Actual");
    
    // Set path to icon for app (in src/my/...)
    ImageIcon icon = new ImageIcon(getClass().getResource("project.png"));
    
    JFileChooser jfLoad = new JFileChooser();
    
    public class UploadThread implements Runnable {
        @Override
        public void run(){
            
            uploadToDevice();
        }
    }
    
    /**
     * Creates new form DataLoggerUI
     */
    public ReflowControllerUI() {

        initComponents();
        initCommPortComboBox();
        
        // Create graph and format it 
        ChartPanel chPanel = new ChartPanel(chart); //creating the chart panel, which extends JPanel
        pnlGraph.add(chPanel); //add the chart viewer to the JPanel
        dataset.addSeries(seriesSetpoint);
        dataset.addSeries(seriesActual);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        // Sets paint color for each series
        renderer.setSeriesPaint(0, Color.BLUE); // Setpoint
        renderer.setSeriesPaint(1, Color.RED); // Actual
        XYPlot plot = chart.getXYPlot();
        ValueAxis xaxis = plot.getDomainAxis();
        xaxis.setAutoRange(true);
        ValueAxis yaxis = plot.getRangeAxis();
        yaxis.setAutoRange(true);
        //yaxis.setRange(0.0, 300.0);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);
        LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.BOTTOM);
                
        // Set to user home directory
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home") + "/Documents"));
                
        addCutCopyPastePopUp(jLogArea);
        
        lblTempSetpoint.setVisible(false);
        spnTempSetpoint.setVisible(false);
        txtTempPost.setVisible(false);
        
        DefaultCaret caret = (DefaultCaret)jLogArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        // Timer to update GUI
        ActionListener actListner = new ActionListener() {
            @Override

            public void actionPerformed(ActionEvent event) {

                // Get PC Time
                SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
                String dateTime = sdf.format(System.currentTimeMillis());

                // Get logger info, only when idle
                if (isConnected == true) {
                    if (readCount >= 9) {
                        // Read data every 10 seconds
                        readCurrentData();
                        readCount = 0;
                    } else {
                        readCount += 1;
                    }
                } else {
                    // Rescan, not connected
                    // Rescan, not connected
                    if (!portFocused) {
                        initCommPortComboBox();
                    } else {
                        // Wait
                    }
                }

            }

        };

        timer = new Timer(1000, actListner);

        timer.start();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        pnlMaster = new javax.swing.JTabbedPane();
        pnlMain = new javax.swing.JPanel();
        pnlSettings = new javax.swing.JPanel();
        lblOvenMode = new javax.swing.JLabel();
        lbPreheatSetpoint = new javax.swing.JLabel();
        lblSoakSetpoint = new javax.swing.JLabel();
        lblReflowSetpoint = new javax.swing.JLabel();
        lblTempSetpoint = new javax.swing.JLabel();
        cmbOvenMode = new javax.swing.JComboBox<>();
        spnTempSetpoint = new javax.swing.JSpinner();
        spnPreheatSetpoint = new javax.swing.JSpinner();
        spnSoakSetpoint = new javax.swing.JSpinner();
        spnReflowSetpoint = new javax.swing.JSpinner();
        btnReadSettings = new javax.swing.JButton();
        btnSendSettings = new javax.swing.JButton();
        txtPreheatPost = new javax.swing.JLabel();
        txtSoakPost = new javax.swing.JLabel();
        txtTempPost = new javax.swing.JLabel();
        txtReflowPost = new javax.swing.JLabel();
        pnlGraph = new javax.swing.JPanel();
        pnlCommunication = new javax.swing.JPanel();
        btnConnect = new javax.swing.JButton();
        cmbCOMSelect = new javax.swing.JComboBox();
        cmbBaudRate = new javax.swing.JComboBox();
        btnRefresh = new javax.swing.JButton();
        pnlDateTime = new javax.swing.JPanel();
        edtRunTime = new javax.swing.JTextField();
        txtProcessSetpoint = new javax.swing.JLabel();
        edtProcessSetpoint = new javax.swing.JTextField();
        edtProcessActual = new javax.swing.JTextField();
        txtProcessActual = new javax.swing.JLabel();
        btnStartProcess = new javax.swing.JButton();
        txtRunTime1 = new javax.swing.JLabel();
        txtSeconds = new javax.swing.JLabel();
        txtDegSign5 = new javax.swing.JLabel();
        txtDegSign6 = new javax.swing.JLabel();
        btnViewData = new javax.swing.JButton();
        btnExportGraph = new javax.swing.JButton();
        pnlControllerInfo = new javax.swing.JPanel();
        lblFirmware = new javax.swing.JLabel();
        edtFirmware = new javax.swing.JTextField();
        jScrollPaneMsg = new javax.swing.JScrollPane();
        edtMessages = new javax.swing.JTextArea();
        pnlRawData = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblRawData = new javax.swing.JTable();
        btnClearTable = new javax.swing.JButton();
        btnExportCSV = new javax.swing.JButton();
        pnlSerial = new javax.swing.JPanel();
        pnlSerialConsole = new javax.swing.JPanel();
        btnSendSerial = new javax.swing.JButton();
        jSendField = new javax.swing.JTextField();
        jSendInFormatBox = new javax.swing.JComboBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        jLogArea = new javax.swing.JTextArea();
        jViewCodeAsBox = new javax.swing.JComboBox();
        pnlUploadFirmware = new javax.swing.JPanel();
        btnSelectHex = new javax.swing.JButton();
        btnUpload = new javax.swing.JButton();
        txtSelectedHex = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        mnuFile = new javax.swing.JMenu();
        mniRead = new javax.swing.JMenuItem();
        mniSend = new javax.swing.JMenuItem();
        mniDefault = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        mniExportGraph = new javax.swing.JMenuItem();
        mniExportCSV = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        mniExit = new javax.swing.JMenuItem();
        mnuEdit = new javax.swing.JMenu();
        mniLogin = new javax.swing.JMenuItem();
        mnuHelp = new javax.swing.JMenu();
        mnAbout = new javax.swing.JMenuItem();
        mniManual = new javax.swing.JMenuItem();
        mniDatasheet = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("NSB Reflow Controller V1.10");
        setIconImage(icon.getImage());
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        pnlMaster.setToolTipText("");
        pnlMaster.setEnabled(false);

        pnlMain.setMinimumSize(new java.awt.Dimension(800, 600));
        pnlMain.setPreferredSize(new java.awt.Dimension(800, 600));
        pnlMain.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        pnlSettings.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 12))); // NOI18N
        pnlSettings.setName("Thresholds"); // NOI18N
        pnlSettings.setPreferredSize(new java.awt.Dimension(400, 300));
        pnlSettings.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblOvenMode.setText("Controller Mode");
        pnlSettings.add(lblOvenMode, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, 100, -1));

        lbPreheatSetpoint.setText("Preheat Setpoint");
        pnlSettings.add(lbPreheatSetpoint, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 100, -1));

        lblSoakSetpoint.setText("Soak Setpoint");
        pnlSettings.add(lblSoakSetpoint, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 80, 100, -1));

        lblReflowSetpoint.setText("Reflow Setpoint");
        pnlSettings.add(lblReflowSetpoint, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 110, 100, -1));

        lblTempSetpoint.setText("Setpoint Temp");
        pnlSettings.add(lblTempSetpoint, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 140, 100, -1));

        cmbOvenMode.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Reflow", "Temperature" }));
        cmbOvenMode.setToolTipText("Select oven mode, either Reflow (default) or Temperature controller");
        cmbOvenMode.setEnabled(false);
        cmbOvenMode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbOvenModeActionPerformed(evt);
            }
        });
        pnlSettings.add(cmbOvenMode, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 20, 100, 20));

        spnTempSetpoint.setModel(new javax.swing.SpinnerNumberModel(40, 20, 200, 1));
        spnTempSetpoint.setToolTipText("Temperature setpoint range 20 - 200°C, increment 1°C");
        spnTempSetpoint.setName(""); // NOI18N
        spnTempSetpoint.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spnTempSetpointStateChanged(evt);
            }
        });
        pnlSettings.add(spnTempSetpoint, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 140, 60, 20));

        spnPreheatSetpoint.setModel(new javax.swing.SpinnerNumberModel(150, 100, 150, 1));
        spnPreheatSetpoint.setToolTipText("Preheat setpoint range 100 - 150°C, increment 1°C");
        spnPreheatSetpoint.setName(""); // NOI18N
        pnlSettings.add(spnPreheatSetpoint, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 50, 60, 20));

        spnSoakSetpoint.setModel(new javax.swing.SpinnerNumberModel(200, 150, 200, 1));
        spnSoakSetpoint.setToolTipText("Soak setpoint range 150 - 200°C, increment 1°C");
        spnSoakSetpoint.setName(""); // NOI18N
        pnlSettings.add(spnSoakSetpoint, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 80, 60, 20));

        spnReflowSetpoint.setModel(new javax.swing.SpinnerNumberModel(240, 200, 255, 1));
        spnReflowSetpoint.setToolTipText("Reflow setpoint range 200 - 255°C, increment 1°C");
        spnReflowSetpoint.setName(""); // NOI18N
        pnlSettings.add(spnReflowSetpoint, new org.netbeans.lib.awtextra.AbsoluteConstraints(110, 110, 60, 20));

        btnReadSettings.setText("Read Settings");
        btnReadSettings.setToolTipText("Read current settings from controller");
        btnReadSettings.setEnabled(false);
        btnReadSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReadSettingsActionPerformed(evt);
            }
        });
        pnlSettings.add(btnReadSettings, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 170, -1, -1));

        btnSendSettings.setText("Send Settings");
        btnSendSettings.setToolTipText("Send the current settings to the controller");
        btnSendSettings.setEnabled(false);
        btnSendSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendSettingsActionPerformed(evt);
            }
        });
        pnlSettings.add(btnSendSettings, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 170, -1, -1));

        txtPreheatPost.setText("°C");
        pnlSettings.add(txtPreheatPost, new org.netbeans.lib.awtextra.AbsoluteConstraints(175, 50, -1, -1));

        txtSoakPost.setText("°C");
        pnlSettings.add(txtSoakPost, new org.netbeans.lib.awtextra.AbsoluteConstraints(175, 80, -1, -1));

        txtTempPost.setText("°C");
        pnlSettings.add(txtTempPost, new org.netbeans.lib.awtextra.AbsoluteConstraints(175, 140, -1, -1));

        txtReflowPost.setText("°C");
        pnlSettings.add(txtReflowPost, new org.netbeans.lib.awtextra.AbsoluteConstraints(175, 110, -1, -1));

        pnlMain.add(pnlSettings, new org.netbeans.lib.awtextra.AbsoluteConstraints(320, 10, 230, 210));

        pnlGraph.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Temperature Graph", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 12))); // NOI18N
        pnlGraph.setName("Thresholds"); // NOI18N
        pnlGraph.setLayout(new java.awt.BorderLayout());
        pnlMain.add(pnlGraph, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 240, 780, 300));

        pnlCommunication.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Communication", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 12))); // NOI18N
        pnlCommunication.setToolTipText("");
        pnlCommunication.setName("Communication"); // NOI18N
        pnlCommunication.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnConnect.setText("Connect");
        btnConnect.setToolTipText("Connect/Disconnect controlelr");
        btnConnect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnConnectActionPerformed(evt);
            }
        });
        pnlCommunication.add(btnConnect, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 25, 100, 25));

        cmbCOMSelect.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "COM" }));
        cmbCOMSelect.setToolTipText("Communication port selected");
        cmbCOMSelect.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
                cmbCOMSelectPopupMenuWillBecomeInvisible(evt);
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                cmbCOMSelectPopupMenuWillBecomeVisible(evt);
            }
        });
        cmbCOMSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbCOMSelectActionPerformed(evt);
            }
        });
        pnlCommunication.add(cmbCOMSelect, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 25, 60, 25));

        cmbBaudRate.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "9600", "14400", "19200", "28800", "38400", "57600", "115200" }));
        cmbBaudRate.setToolTipText("Baud rate");
        cmbBaudRate.setEnabled(false);
        cmbBaudRate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbBaudRateActionPerformed(evt);
            }
        });
        pnlCommunication.add(cmbBaudRate, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 25, 60, 25));

        btnRefresh.setIcon(new javax.swing.ImageIcon(getClass().getResource("/my/reflowcontroller/rs232/icons/refresh.png"))); // NOI18N
        btnRefresh.setToolTipText("Refresh COM Port List");
        btnRefresh.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });
        pnlCommunication.add(btnRefresh, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 25, 25, 25));

        pnlMain.add(pnlCommunication, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 300, 60));

        pnlDateTime.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Process Details", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 12))); // NOI18N
        pnlDateTime.setToolTipText("");
        pnlDateTime.setName("DateTime"); // NOI18N
        pnlDateTime.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        edtRunTime.setEditable(false);
        edtRunTime.setBackground(new java.awt.Color(225, 225, 225));
        edtRunTime.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtRunTime.setToolTipText("Time process has been running");
        edtRunTime.setMinimumSize(new java.awt.Dimension(50, 25));
        edtRunTime.setPreferredSize(new java.awt.Dimension(50, 25));
        pnlDateTime.add(edtRunTime, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 30, 50, 20));

        txtProcessSetpoint.setText("Setpoint");
        pnlDateTime.add(txtProcessSetpoint, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 60, -1, -1));

        edtProcessSetpoint.setEditable(false);
        edtProcessSetpoint.setBackground(new java.awt.Color(225, 225, 225));
        edtProcessSetpoint.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtProcessSetpoint.setToolTipText("Current setpoint temperature");
        edtProcessSetpoint.setMinimumSize(new java.awt.Dimension(50, 25));
        edtProcessSetpoint.setPreferredSize(new java.awt.Dimension(50, 25));
        pnlDateTime.add(edtProcessSetpoint, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 60, 50, 20));

        edtProcessActual.setEditable(false);
        edtProcessActual.setBackground(new java.awt.Color(225, 225, 225));
        edtProcessActual.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtProcessActual.setToolTipText("Actual temperature reported by controller");
        edtProcessActual.setMinimumSize(new java.awt.Dimension(50, 25));
        edtProcessActual.setPreferredSize(new java.awt.Dimension(50, 25));
        pnlDateTime.add(edtProcessActual, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 90, 50, 20));

        txtProcessActual.setText("Actual Temp");
        pnlDateTime.add(txtProcessActual, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, -1, -1));

        btnStartProcess.setText("Start Process");
        btnStartProcess.setToolTipText("Start/Stop the process");
        btnStartProcess.setEnabled(false);
        btnStartProcess.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStartProcessActionPerformed(evt);
            }
        });
        pnlDateTime.add(btnStartProcess, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 30, -1, -1));

        txtRunTime1.setText("Run Time");
        pnlDateTime.add(txtRunTime1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, -1, -1));

        txtSeconds.setText("Seconds");
        pnlDateTime.add(txtSeconds, new org.netbeans.lib.awtextra.AbsoluteConstraints(135, 30, -1, -1));

        txtDegSign5.setText("°C");
        pnlDateTime.add(txtDegSign5, new org.netbeans.lib.awtextra.AbsoluteConstraints(135, 90, -1, -1));

        txtDegSign6.setText("°C");
        pnlDateTime.add(txtDegSign6, new org.netbeans.lib.awtextra.AbsoluteConstraints(135, 60, -1, -1));

        btnViewData.setText("View Data");
        btnViewData.setToolTipText("View raw data in table format");
        btnViewData.setEnabled(false);
        btnViewData.setPreferredSize(new java.awt.Dimension(97, 23));
        btnViewData.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnViewDataActionPerformed(evt);
            }
        });
        pnlDateTime.add(btnViewData, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 90, -1, -1));

        btnExportGraph.setText("Export Graph");
        btnExportGraph.setToolTipText("Export the graph to jpeg or png");
        btnExportGraph.setEnabled(false);
        btnExportGraph.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportGraphActionPerformed(evt);
            }
        });
        pnlDateTime.add(btnExportGraph, new org.netbeans.lib.awtextra.AbsoluteConstraints(190, 60, -1, -1));

        pnlMain.add(pnlDateTime, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 90, 300, 130));
        pnlDateTime.getAccessibleContext().setAccessibleName("DateTime");

        pnlControllerInfo.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Controller Info", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 12))); // NOI18N
        pnlControllerInfo.setName("Thresholds"); // NOI18N
        pnlControllerInfo.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        lblFirmware.setText("Firmware Version");
        lblFirmware.setToolTipText("");
        pnlControllerInfo.add(lblFirmware, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, -1, -1));

        edtFirmware.setEditable(false);
        edtFirmware.setBackground(new java.awt.Color(225, 225, 225));
        edtFirmware.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        edtFirmware.setText("?");
        edtFirmware.setToolTipText("Controller firmware version number");
        edtFirmware.setMinimumSize(new java.awt.Dimension(50, 25));
        edtFirmware.setPreferredSize(new java.awt.Dimension(50, 25));
        pnlControllerInfo.add(edtFirmware, new org.netbeans.lib.awtextra.AbsoluteConstraints(120, 30, 50, 20));

        jScrollPaneMsg.setBackground(new java.awt.Color(225, 225, 225));
        jScrollPaneMsg.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPaneMsg.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        jScrollPaneMsg.setEnabled(false);

        edtMessages.setEditable(false);
        edtMessages.setBackground(new java.awt.Color(225, 225, 225));
        edtMessages.setColumns(20);
        edtMessages.setFont(new java.awt.Font("Tahoma", 1, 13)); // NOI18N
        edtMessages.setLineWrap(true);
        edtMessages.setRows(2);
        edtMessages.setTabSize(1);
        edtMessages.setToolTipText("Information regarding controller state");
        edtMessages.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        jScrollPaneMsg.setViewportView(edtMessages);

        pnlControllerInfo.add(jScrollPaneMsg, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 60, 160, 40));

        pnlMain.add(pnlControllerInfo, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 10, 180, 120));

        pnlMaster.addTab("Main", pnlMain);

        pnlRawData.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel5.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jScrollPane2.setAutoscrolls(true);

        tblRawData.setAutoCreateRowSorter(true);
        tblRawData.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null}
            },
            new String [] {
                "Time (Seconds)", "Setpoint (°C)", "Temperature (°C)", "PWM Output (ms)"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblRawData.setToolTipText("Oven data table");
        jScrollPane2.setViewportView(tblRawData);

        jPanel5.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 0, 770, 430));

        pnlRawData.add(jPanel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 780, -1));

        btnClearTable.setText("Clear Table");
        btnClearTable.setEnabled(false);
        btnClearTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnClearTableActionPerformed(evt);
            }
        });
        pnlRawData.add(btnClearTable, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 480, -1, 20));

        btnExportCSV.setText("Export to CSV");
        btnExportCSV.setEnabled(false);
        btnExportCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportCSVActionPerformed(evt);
            }
        });
        pnlRawData.add(btnExportCSV, new org.netbeans.lib.awtextra.AbsoluteConstraints(400, 480, -1, 20));

        pnlMaster.addTab("Raw Data", pnlRawData);

        pnlSerial.setEnabled(false);
        pnlSerial.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        pnlSerialConsole.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Serial Console", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 12))); // NOI18N
        pnlSerialConsole.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnSendSerial.setText("Send");
        btnSendSerial.setToolTipText("Send raw data to controller");
        btnSendSerial.setEnabled(false);
        btnSendSerial.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSendSerialActionPerformed(evt);
            }
        });
        pnlSerialConsole.add(btnSendSerial, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 20, -1, -1));

        jSendField.setToolTipText("Raw data to be sent to controller");
        jSendField.setEnabled(false);
        jSendField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jSendFieldKeyPressed(evt);
            }
        });
        pnlSerialConsole.add(jSendField, new org.netbeans.lib.awtextra.AbsoluteConstraints(80, 20, 139, -1));

        jSendInFormatBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ASCII", "Hex" }));
        jSendInFormatBox.setToolTipText("Raw data format");
        jSendInFormatBox.setEnabled(false);
        pnlSerialConsole.add(jSendInFormatBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(230, 20, -1, -1));

        jLogArea.setEditable(false);
        jLogArea.setColumns(20);
        jLogArea.setRows(5);
        jLogArea.setToolTipText("Serial messages from controller");
        jScrollPane1.setViewportView(jLogArea);

        pnlSerialConsole.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 50, 730, 180));

        jViewCodeAsBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ASCII" }));
        jViewCodeAsBox.setToolTipText("Format of serial messages from controller");
        jViewCodeAsBox.setEnabled(false);
        jViewCodeAsBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jViewCodeAsBoxActionPerformed(evt);
            }
        });
        pnlSerialConsole.add(jViewCodeAsBox, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 240, -1, 22));

        pnlSerial.add(pnlSerialConsole, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 140, 750, 400));

        pnlUploadFirmware.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Upload Firmware", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 12))); // NOI18N
        pnlUploadFirmware.setName("Thresholds"); // NOI18N
        pnlUploadFirmware.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        btnSelectHex.setText("Select Hex to Upload");
        btnSelectHex.setToolTipText("Select Hex file to be uploaded to controller");
        btnSelectHex.setEnabled(false);
        btnSelectHex.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectHexActionPerformed(evt);
            }
        });
        pnlUploadFirmware.add(btnSelectHex, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 30, 140, 25));

        btnUpload.setText("Upload");
        btnUpload.setToolTipText("Upload selected Hex file to controller");
        btnUpload.setEnabled(false);
        btnUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUploadActionPerformed(evt);
            }
        });
        pnlUploadFirmware.add(btnUpload, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 70, 100, 25));

        txtSelectedHex.setEditable(false);
        txtSelectedHex.setBackground(new java.awt.Color(255, 255, 255));
        txtSelectedHex.setToolTipText("Path of selected Hex file");
        pnlUploadFirmware.add(txtSelectedHex, new org.netbeans.lib.awtextra.AbsoluteConstraints(170, 30, 570, 25));

        pnlSerial.add(pnlUploadFirmware, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 10, 750, 110));

        pnlMaster.addTab("Serial", pnlSerial);

        getContentPane().add(pnlMaster, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, 594));
        pnlMaster.getAccessibleContext().setAccessibleName("Serial");
        pnlMaster.getAccessibleContext().setAccessibleParent(pnlMaster);

        mnuFile.setText("File");

        mniRead.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        mniRead.setIcon(new javax.swing.ImageIcon(getClass().getResource("/my/reflowcontroller/open.png"))); // NOI18N
        mniRead.setText("Read Settings");
        mniRead.setEnabled(false);
        mniRead.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniReadActionPerformed(evt);
            }
        });
        mnuFile.add(mniRead);

        mniSend.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mniSend.setIcon(new javax.swing.ImageIcon(getClass().getResource("/my/reflowcontroller/save.png"))); // NOI18N
        mniSend.setText("Send Settings");
        mniSend.setEnabled(false);
        mniSend.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniSendActionPerformed(evt);
            }
        });
        mnuFile.add(mniSend);

        mniDefault.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        mniDefault.setText("Default Settings");
        mniDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniDefaultActionPerformed(evt);
            }
        });
        mnuFile.add(mniDefault);
        mnuFile.add(jSeparator1);

        mniExportGraph.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        mniExportGraph.setText("Export Graph");
        mniExportGraph.setEnabled(false);
        mniExportGraph.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniExportGraphActionPerformed(evt);
            }
        });
        mnuFile.add(mniExportGraph);

        mniExportCSV.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        mniExportCSV.setText("Export to CSV");
        mniExportCSV.setEnabled(false);
        mniExportCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniExportCSVActionPerformed(evt);
            }
        });
        mnuFile.add(mniExportCSV);
        mnuFile.add(jSeparator2);

        mniExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        mniExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/my/reflowcontroller/exit.png"))); // NOI18N
        mniExit.setText("Exit");
        mniExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniExitActionPerformed(evt);
            }
        });
        mnuFile.add(mniExit);

        jMenuBar1.add(mnuFile);

        mnuEdit.setText("Edit");

        mniLogin.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        mniLogin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/my/reflowcontroller/login.png"))); // NOI18N
        mniLogin.setText("Login");
        mniLogin.setEnabled(false);
        mniLogin.setIconTextGap(10);
        mniLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniLoginActionPerformed(evt);
            }
        });
        mnuEdit.add(mniLogin);

        jMenuBar1.add(mnuEdit);

        mnuHelp.setText("Help");
        mnuHelp.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                mnuHelpMenuSelected(evt);
            }
        });
        mnuHelp.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                mnuHelpMouseClicked(evt);
            }
        });
        mnuHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuHelpActionPerformed(evt);
            }
        });

        mnAbout.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        mnAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/my/reflowcontroller/about.png"))); // NOI18N
        mnAbout.setText("About");
        mnAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnAboutActionPerformed(evt);
            }
        });
        mnuHelp.add(mnAbout);

        mniManual.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        mniManual.setText("User Manual");
        mniManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniManualActionPerformed(evt);
            }
        });
        mnuHelp.add(mniManual);

        mniDatasheet.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
        mniDatasheet.setText("Datasheet");
        mniDatasheet.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mniDatasheetActionPerformed(evt);
            }
        });
        mnuHelp.add(mniDatasheet);

        jMenuBar1.add(mnuHelp);

        setJMenuBar(jMenuBar1);

        getAccessibleContext().setAccessibleDescription("");

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void initCommPortComboBox() {
        String strCurPort = "";
        
        if (cmbCOMSelect.getItemCount() > 0) {
            strCurPort = (String) (cmbCOMSelect.getSelectedItem());
        }
        
        ArrayList<String> ports = new ArrayList<>();

        try {
            Runtime rt = Runtime.getRuntime();
            
            // Read all the available COM ports
            Process prPort = rt.exec("wmic path Win32_SerialPort Get DeviceID");          
            BufferedReader inputPort = new BufferedReader(new InputStreamReader(prPort.getInputStream()));
            
            // Read all the available PnP device info
            Process prPnP = rt.exec("wmic path Win32_SerialPort Get PnPDeviceID");
            BufferedReader inputPnP = new BufferedReader(new InputStreamReader(prPnP.getInputStream()));

            String linePort = null;
            String linePnP = null;
            
            while ((linePort = inputPort.readLine()) != null) {
                linePnP = inputPnP.readLine();
                // Find Event Loggers
                int idxArduino = linePnP.indexOf("PID_8036"); // Leonardo
                if (idxArduino != -1) {
                    // Logger found, add to list
                    String comName = linePort;
                    comName = comName.replaceAll("\\s+","");
                    ports.add(comName);
                }
            }
//            int exitVal = pr.waitFor();
//            System.out.println("Exited with error code "+exitVal);

        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "List Comm Ports", JOptionPane.ERROR_MESSAGE);
            System.out.println("Error scanning COM ports! ");
            System.out.println(ex.getMessage());
            System.exit(0); // Close app
        }

        // Below adds all available com ports, replaced with only Event logger ports above
        // iterate through, looking for the port
//        while (portEnum.hasMoreElements()) {
//            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
//            // Check if it is Arduino, otherwise do not list
//            
//            ports.add(currPortId.getName());
//            System.out.println(currPortId.getName());
//        }
        
        Collections.sort(ports);
        // Update COM port list
        cmbCOMSelect.setModel(new DefaultComboBoxModel(ports.toArray()));
        
        // Use old selected port if still available
        if (!strCurPort.isEmpty()) {
            cmbCOMSelect.setSelectedItem(strCurPort);
        }
    }
    
    private void readCurrentData() {      
        //Send command to read setup from logger, must first be connected to serial
        try {
            byte[] sendBytes = new byte[]{};
            String input = "T";
            sendBytes = input.getBytes();
            RS232ConnectionAPI.getInstance().send(sendBytes);
            lastRequest = 'T';
            serialDataStr.setLength(0);
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Send", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error sending data!");
            System.err.println(ex);
        }
    }
    
    private void mniSendActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniSendActionPerformed
         btnSendSettingsActionPerformed(evt);
    }//GEN-LAST:event_mniSendActionPerformed

    private void delaySystem(int delayTime) {
        try {
            Thread.sleep(delayTime);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
    private void btnExportCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportCSVActionPerformed
        // TODO add your handling code here:

        // Need to stop timer, otherwise it interrupts write
        timer.stop();
        
        fileChooser.resetChoosableFileFilters();
        FileNameExtensionFilter filterCSV = new FileNameExtensionFilter("CSV Files", "csv");
        fileChooser.setFileFilter(filterCSV);
        
        // Choose where to save csv
        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("")) {
                // Filename has no extension, add
                if (fileChooser.getFileFilter() == filterCSV) {
                    file = new File(file.getParentFile(), FilenameUtils.getBaseName(file.getName()) + ".csv");
                } else {
                    // Default to csv
                    file = new File(file.getParentFile(), FilenameUtils.getBaseName(file.getName()) + ".csv");
                }
            } else {
                // Do nothing
            }

            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file", JOptionPane.YES_NO_CANCEL_OPTION);
                switch (result) {
                    case JOptionPane.YES_OPTION:
                        saveEventsToCSV(file.getAbsolutePath());
                        break;
                    case JOptionPane.NO_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                    case JOptionPane.CANCEL_OPTION:
                        System.out.println("File overwrite cancelled by user.");
                        break;
                }
            } else {
                saveEventsToCSV(file.getAbsolutePath());
            }

        } else {
            System.out.println("File save cancelled by user.");
        }

        // Restart timer
        timer.start();

    }//GEN-LAST:event_btnExportCSVActionPerformed

    private void saveEventsToCSV(String path) {
        try {
            FileWriter excel = new FileWriter(path);
            
            // Get PC Time
            SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
            String dateTime = sdf.format(System.currentTimeMillis());
                
            // Save Info
            String tmpStr;
            if (cmbOvenMode.getSelectedItem().equals("Reflow")) {
                tmpStr = "Reflow Controller,";
            } else {
                tmpStr = "Temperature Controller,";
            }
            
            tmpStr = tmpStr.concat(dateTime);
            tmpStr = tmpStr.concat("\n");
            excel.write(tmpStr);

            // Preheat/P Setting
            if (cmbOvenMode.getSelectedItem().equals("Reflow")) {
                tmpStr = "Preheat,";
            } else {
                tmpStr = "Proportional Gain,";
            }
            tmpStr = tmpStr.concat(spnPreheatSetpoint.getValue().toString());
            tmpStr = tmpStr.concat("\n");
            excel.write(tmpStr);

            // Soak/I Setting
            if (cmbOvenMode.getSelectedItem().equals("Reflow")) {
                tmpStr = "Soak,";
            } else {
                tmpStr = "Integral Gain,";
            }
            tmpStr = tmpStr.concat(spnSoakSetpoint.getValue().toString());
            tmpStr = tmpStr.concat("\n");
            excel.write(tmpStr);

            // Reflow/D Setting
            if (cmbOvenMode.getSelectedItem().equals("Reflow")) {
                tmpStr = "Reflow,";
            } else {
                tmpStr = "Derivative Gain,";
            }
            tmpStr = tmpStr.concat(spnReflowSetpoint.getValue().toString());
            tmpStr = tmpStr.concat("\n");
            excel.write(tmpStr);

            excel.write("\n");
            // Save column names
            for (int i = 0; i < tblRawData.getColumnCount(); i++) {
                excel.write(tblRawData.getColumnName(i) + ",");
            }
            excel.write("\n");

            // Save rows, last one will be empty, ignore
            for (int i = 0; i < tblRawData.getRowCount() - 1; i++) {
                for (int j = 0; j < tblRawData.getColumnCount(); j++) {
                    // Write to excel
                    tmpStr = tblRawData.getValueAt(i, j).toString();
                    excel.write(tmpStr + ",");
                }
                excel.write("\n");
            }

            excel.close();
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Export to CSV", JOptionPane.ERROR_MESSAGE);
        }

    }

    private void btnClearTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnClearTableActionPerformed
        // TODO add your handling code here:
        dataCount = 0;
        btnExportCSV.setEnabled(false);
        mniExportCSV.setEnabled(false);
        // Clear data table
        for (int i = tblRawData.getRowCount(); i > 1; i--) {
            ((DefaultTableModel) tblRawData.getModel()).removeRow(i - 1);
        }
        for (int j = 0; j < tblRawData.getColumnCount(); j++) {
            tblRawData.setValueAt("", 0, j);
        }
    }//GEN-LAST:event_btnClearTableActionPerformed

    private void mniLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniLoginActionPerformed
        
//        if (mniLogin.getText().equals("Logout")) {
//            edtLoggerSerial.setEditable(false);
//            edtLoggerSerial.setBackground(new Color(204,204,204));
//            pnlMaster.setEnabledAt(2, false); // Disable Serial panel
//            mniLogin.setText("Login");
//        } else {
//            // Open login panel
//            JTextField txtUsername = new JTextField(10);
//            JPasswordField txtPassword = new JPasswordField(10);
//
//            JPanel pnlLogin = new JPanel();
//            pnlLogin.add(new JLabel("Username:"));
//            pnlLogin.add(txtUsername);
//            pnlLogin.setLayout(new BoxLayout(pnlLogin, BoxLayout.Y_AXIS));
//            pnlLogin.add(new JLabel("Password:"));
//            pnlLogin.add(txtPassword);
//            
//            int result = JOptionPane.showConfirmDialog(this, pnlLogin, 
//                    "Please enter Username and Password", JOptionPane.OK_CANCEL_OPTION, 
//                    JOptionPane.INFORMATION_MESSAGE);
//            txtUsername.requestFocusInWindow();
//            
//            if ((result == 0) && (txtUsername.getText().equalsIgnoreCase("cljhb")) 
//                    && (txtPassword.getText().equalsIgnoreCase("cljhb"))) {
//                edtLoggerSerial.setEditable(true);
//                edtLoggerSerial.setBackground(Color.WHITE);
//                pnlMaster.setEnabledAt(2, true); // Enable Serial panel
//                mniLogin.setText("Logout");
//                System.out.println("Login successful");
//            } else {
//                edtLoggerSerial.setEditable(false);
//                edtLoggerSerial.setBackground(new Color(204,204,204));
//                pnlMaster.setEnabledAt(2, false); // Disable Serial panel
//                mniLogin.setText("Login");
//            } 
//        }
        
    }//GEN-LAST:event_mniLoginActionPerformed

    private void mnuHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuHelpActionPerformed
        
    }//GEN-LAST:event_mnuHelpActionPerformed

    private void mnuHelpMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_mnuHelpMenuSelected
       
    }//GEN-LAST:event_mnuHelpMenuSelected

    private void jViewCodeAsBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jViewCodeAsBoxActionPerformed
        jLogArea.setText("");
    }//GEN-LAST:event_jViewCodeAsBoxActionPerformed

    private void jSendFieldKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jSendFieldKeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jSendFieldKeyPressed

    private void btnSendSerialActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendSerialActionPerformed
        // TODO add your handling code here:
        try {
            byte[] sendBytes = new byte[]{};
            String input = jSendField.getText();

            if (jSendInFormatBox.getSelectedItem().equals("ASCII")) {
                sendBytes = input.getBytes();
            } else if (jSendInFormatBox.getSelectedItem().equals("Hex")) {
                sendBytes = HexBinOctUtils.hexStringToByteArray(input);
            }

            RS232ConnectionAPI.getInstance().send(sendBytes);

        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Send", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error sending data!");
            System.err.println(ex);
            return;
        }
        jSendField.setText("");
    }//GEN-LAST:event_btnSendSerialActionPerformed

    private void mnuHelpMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mnuHelpMouseClicked
        
    }//GEN-LAST:event_mnuHelpMouseClicked

    private void mniReadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniReadActionPerformed
         btnReadSettingsActionPerformed(evt);
    }//GEN-LAST:event_mniReadActionPerformed

    private void mniDefaultActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniDefaultActionPerformed
        // Load defaults
        if (cmbOvenMode.getSelectedItem().equals("Reflow")) {
            spnPreheatSetpoint.setValue(150);
            spnSoakSetpoint.setValue(180);
            spnReflowSetpoint.setValue(240);
        } else if (cmbOvenMode.getSelectedItem().equals("Temperature")) {
            spnPreheatSetpoint.setValue(5.0);
            spnSoakSetpoint.setValue(0.01);
            spnReflowSetpoint.setValue(10.0);
        }
    }//GEN-LAST:event_mniDefaultActionPerformed

    private void mniExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniExitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_mniExitActionPerformed

    private void mnAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnAboutActionPerformed
        JOptionPane.showMessageDialog(this,
                "Reflow Controller\n\n" +
                "The Reflow Controller is specifically designed to control the soldering\n" +
                "of surface mount technology (SMT) components onto pc‐boards using a small\n" +
                "batch compact oven. The controller follows the correct temperature profile\n" +
                "for SMT soldering. This allows for production quality prototyping or small\n" +
                "batch manufacturing to be completed effortlessly.\n\n" +
                "Features:\n" +
                "- Plug‐and‐solder\n" +
                "- LCD display of temperature and state\n" +
                "- LED state indication\n" +
                "- USB connectivity for monitoring/controlling\n" +
                "- Optional RF connectivity (433MHz transceiver)\n" +
                "- Can change mode to be a temperature controller",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_mnAboutActionPerformed

    private void mniManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniManualActionPerformed
        // Open help file
        if (Desktop.isDesktopSupported()) {
            try {
                File myFile = new File("./help/NSB006 Reflow Controller - GUI User Manual.pdf");
                Desktop.getDesktop().open(myFile);
            } catch (IOException ex) {
                // no application registered for PDFs
                javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Help", JOptionPane.ERROR_MESSAGE);
                System.err.println("Help! ");
                System.err.println(ex.getMessage());
            }
        }
    }//GEN-LAST:event_mniManualActionPerformed

    private void mniExportCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniExportCSVActionPerformed
        btnExportCSVActionPerformed(evt);
    }//GEN-LAST:event_mniExportCSVActionPerformed

    private void mniExportGraphActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniExportGraphActionPerformed
        btnExportGraphActionPerformed(evt);
    }//GEN-LAST:event_mniExportGraphActionPerformed

    private void btnSelectHexActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSelectHexActionPerformed
        // Open file chooser to select hex to upload
        // Change file extensions to hex
        fileChooser.resetChoosableFileFilters();
        FileNameExtensionFilter filterHex = new FileNameExtensionFilter("Hex Files", "hex");
        jfLoad.setFileFilter(filterHex);

        int returnVal = jfLoad.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String fileName = jfLoad.getSelectedFile().getAbsolutePath();
            txtSelectedHex.setText(fileName);
            btnUpload.setEnabled(true);
            jfLoad.setCurrentDirectory(new File(jfLoad.getSelectedFile().getAbsolutePath()));
        } else {
            System.out.println("File open cancelled by user.");
            txtSelectedHex.setText("");
            btnUpload.setEnabled(false);
        }

    }//GEN-LAST:event_btnSelectHexActionPerformed

    private void btnUploadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUploadActionPerformed

        // Start new thread to handle upload otherwise gui is not updated
        Thread thread = new Thread(new UploadThread());
        thread.start();

    }//GEN-LAST:event_btnUploadActionPerformed

    private void mniDatasheetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mniDatasheetActionPerformed
        // Open help file
        if (Desktop.isDesktopSupported()) {
            try {
                File myFile = new File("./help/NSB006 Reflow Controller - Spec.pdf");
                Desktop.getDesktop().open(myFile);
            } catch (IOException ex) {
                // no application registered for PDFs
                javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Help", JOptionPane.ERROR_MESSAGE);
                System.err.println("Help! ");
                System.err.println(ex.getMessage());
            }
        }
    }//GEN-LAST:event_mniDatasheetActionPerformed

    private void btnExportGraphActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportGraphActionPerformed
        // Ask user where to save the file and what format
        // Need to stop timer, otherwise it interrupts write
        timer.stop();

        fileChooser.resetChoosableFileFilters();

        FileNameExtensionFilter filterPNG = new FileNameExtensionFilter("PNG Image Files", "png");
        FileNameExtensionFilter filterJPG = new FileNameExtensionFilter("JPEG Image Files", "jpg", "jpeg");
        fileChooser.setFileFilter(filterPNG);
        fileChooser.setFileFilter(filterJPG);

        // Choose where to save csv
        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (FilenameUtils.getExtension(file.getName()).equalsIgnoreCase("")) {
                // Filename has no extension, add
                if (fileChooser.getFileFilter() == filterJPG) {
                    file = new File(file.getParentFile(), FilenameUtils.getBaseName(file.getName()) + ".jpg");
                } else if (fileChooser.getFileFilter() == filterPNG) {
                    file = new File(file.getParentFile(), FilenameUtils.getBaseName(file.getName()) + ".png");
                } else {
                    // Default to jpg
                    file = new File(file.getParentFile(), FilenameUtils.getBaseName(file.getName()) + ".jpg");
                }
            } else {
                // Do nothing
            }

            boolean saveConfirmed = false;

            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(this, "The file exists, overwrite?", "Existing file", JOptionPane.YES_NO_CANCEL_OPTION);
                switch (result) {
                    case JOptionPane.YES_OPTION:
                    saveConfirmed = true;
                    break;
                    case JOptionPane.NO_OPTION:
                    case JOptionPane.CLOSED_OPTION:
                    case JOptionPane.CANCEL_OPTION:
                    System.out.println("File overwrite cancelled by user.");
                    break;
                }
            } else {
                saveConfirmed = true;
            }

            if (saveConfirmed == true) {
                try {
                    String fileExt = FilenameUtils.getExtension(file.getName());
                    if (fileExt.equalsIgnoreCase("jpg") || fileExt.equalsIgnoreCase("jpeg")) {
                        ChartUtilities.saveChartAsJPEG(new File(file.getAbsolutePath()), 1.0f, chart, 800, 300);
                    } else {
                        ChartUtilities.saveChartAsPNG(new File(file.getAbsolutePath()), chart, 800, 300);
                    }
                } catch (IOException ex) {
                    System.err.println("Cannot save chart to jpeg");
                    System.err.println(ex);
                }
            }

        } else {
            System.out.println("File save cancelled by user.");
        }

        // Restart timer
        timer.start();
    }//GEN-LAST:event_btnExportGraphActionPerformed

    private void btnViewDataActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnViewDataActionPerformed
        pnlMaster.setSelectedIndex(1); // Focus on raw data tab
    }//GEN-LAST:event_btnViewDataActionPerformed

    private void btnStartProcessActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStartProcessActionPerformed
        try {
            if (btnStartProcess.getText().equals("Start Process")) {
                byte[] sendBytes = new byte[]{};
                String input = "O1";
                sendBytes = input.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                lastRequest = 'O';
                serialDataStr.setLength(0);

                // Clear the graph
                seriesSetpoint.clear();
                seriesActual.clear();

                dataCount = 0;
                // Clear data table
                for (int i = tblRawData.getRowCount(); i > 1; i--) {
                    ((DefaultTableModel) tblRawData.getModel()).removeRow(i - 1);
                }
                for (int j = 0; j < tblRawData.getColumnCount(); j++) {
                    tblRawData.setValueAt("", 0, j);
                }

            } else if (btnStartProcess.getText().equals("Stop Process")) {
                byte[] sendBytes = new byte[]{};
                String input = "O0";
                sendBytes = input.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                lastRequest = ' ';
                serialDataStr.setLength(0);
                btnStartProcess.setText("Start Process");

                btnReadSettings.setEnabled(true);
                btnSendSettings.setEnabled(true);
                mniRead.setEnabled(true);
                mniSend.setEnabled(true);
                cmbOvenMode.setEnabled(true);
                spnPreheatSetpoint.setEnabled(true);
                spnSoakSetpoint.setEnabled(true);
                spnReflowSetpoint.setEnabled(true);
                btnExportCSV.setEnabled(true);
                mniExportCSV.setEnabled(true);
                btnClearTable.setEnabled(true);
                btnSelectHex.setEnabled(true);
                if (!txtSelectedHex.getText().equals("")) {
                    // Enable upload button only if file has been selected
                    btnUpload.setEnabled(true);
                }
                dataCount = 0;
            }
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Send", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error sending data!");
            System.err.println(ex);
        }

    }//GEN-LAST:event_btnStartProcessActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        // TODO add your handling code here:
        initCommPortComboBox();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void cmbBaudRateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbBaudRateActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbBaudRateActionPerformed

    private void cmbCOMSelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbCOMSelectActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbCOMSelectActionPerformed

    private void btnConnectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnConnectActionPerformed
        // TODO add your handling code here:
        if (RS232ConnectionAPI.getInstance().isSessionConnected()) {
            RS232ConnectionAPI.getInstance().stopSession();
            componentClosed();
        } else {
            try {
                String str = (String) (cmbBaudRate.getSelectedItem());
                RS232ConnectionAPI.getInstance().setComPort((String) cmbCOMSelect.getSelectedItem());
                RS232ConnectionAPI.getInstance().setDataRate(Integer.parseInt(str));
                componentOpened();
                RS232ConnectionAPI.getInstance().startSession();

            } catch (ComPortInUseException ex) {
                Exceptions.printStackTrace(ex);
                javax.swing.JOptionPane.showMessageDialog(this, "COM port in use", "Connect", JOptionPane.ERROR_MESSAGE);
            } catch (ComPortNotFoundException ex) {
                Exceptions.printStackTrace(ex);
                javax.swing.JOptionPane.showMessageDialog(this, "Could not find COM port", "Connect", JOptionPane.ERROR_MESSAGE);
            } catch (ComPortException ex) {
                Exceptions.printStackTrace(ex);
                javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Connect", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                System.err.println(ex);
                javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Connect", JOptionPane.ERROR_MESSAGE);
            }

        }
    }//GEN-LAST:event_btnConnectActionPerformed

    private void btnSendSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSendSettingsActionPerformed
        //Send commands to send setup to logger, must first be connected to serial
        try {
            byte[] sendBytes = new byte[]{};
            String tmpSend = "";

            // Disable all buttons on GUI
            jSendField.setEnabled(false);
            btnSendSerial.setEnabled(false);
            jSendInFormatBox.setEnabled(false);
            jViewCodeAsBox.setEnabled(false);
            btnReadSettings.setEnabled(false);
            btnSendSettings.setEnabled(false);
            btnClearTable.setEnabled(false);
            btnExportCSV.setEnabled(false);
            mniExportCSV.setEnabled(false);
            pnlMaster.setEnabled(false);

            // Need to stop timer, otherwise it interrupts write
            timer.stop();

            if (cmbOvenMode.getSelectedItem().equals("Reflow")) {
                // Send Preheat
                tmpSend = "P" + spnPreheatSetpoint.getValue().toString() + "\r\n";
                sendBytes = tmpSend.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                // Wait for request to be processed by logger
                delaySystem(DELAY_SEND); // milliseconds

                // Send Soak
                tmpSend = "S" + spnSoakSetpoint.getValue().toString() + "\r\n";
                sendBytes = tmpSend.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                // Wait for request to be processed by logger
                delaySystem(DELAY_SEND); // milliseconds

                // Send Reflow
                tmpSend = "R" + spnReflowSetpoint.getValue().toString() + "\r\n";
                sendBytes = tmpSend.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                // Wait for request to be processed by logger
                delaySystem(DELAY_SEND); // milliseconds
            } else if (cmbOvenMode.getSelectedItem().equals("Temperature")) {
                // Send Preheat
                tmpSend = "P" + spnPreheatSetpoint.getValue().toString() + "\r\n";
                sendBytes = tmpSend.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                // Wait for request to be processed by logger
                delaySystem(DELAY_SEND); // milliseconds

                // Send Soak
                tmpSend = "I" + spnSoakSetpoint.getValue().toString() + "\r\n";
                sendBytes = tmpSend.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                // Wait for request to be processed by logger
                delaySystem(DELAY_SEND); // milliseconds

                // Send Reflow
                tmpSend = "D" + spnReflowSetpoint.getValue().toString() + "\r\n";
                sendBytes = tmpSend.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                // Wait for request to be processed by logger
                delaySystem(DELAY_SEND); // milliseconds

                // Send Setpoint
                tmpSend = "U" + spnTempSetpoint.getValue().toString() + "\r\n";
                sendBytes = tmpSend.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                // Wait for request to be processed by logger
                delaySystem(DELAY_SEND); // milliseconds
            }

            // Save new system settings on logger EEPROM
            tmpSend = "W";
            sendBytes = tmpSend.getBytes();
            RS232ConnectionAPI.getInstance().send(sendBytes);

            // Enable GUI panel
            jSendField.setEnabled(true);
            btnSendSerial.setEnabled(true);
            jSendInFormatBox.setEnabled(true);
            jViewCodeAsBox.setEnabled(true);
            btnReadSettings.setEnabled(true);
            btnSendSettings.setEnabled(true);
            btnClearTable.setEnabled(true);
            pnlMaster.setEnabled(true);

            // Restart timer
            timer.start();

            // Show message
            javax.swing.JOptionPane.showMessageDialog(this, "Done sending setup to controller",
                "Send Settings", JOptionPane.INFORMATION_MESSAGE);

            //lastRequest = 'S';
            //serialDataStr.setLength(0);
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Send", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error sending data!");
            System.err.println(ex);
        }
    }//GEN-LAST:event_btnSendSettingsActionPerformed

    private void btnReadSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReadSettingsActionPerformed
        //Send command to read setup from logger, must first be connected to serial
        try {
            byte[] sendBytes = new byte[]{};
            String input = "C";
            sendBytes = input.getBytes();
            RS232ConnectionAPI.getInstance().send(sendBytes);
            lastRequest = 'C';
            serialDataStr.setLength(0);
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Send", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error sending data!");
            System.err.println(ex);
        }
    }//GEN-LAST:event_btnReadSettingsActionPerformed

    private void spnTempSetpointStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnTempSetpointStateChanged

        try {
            if (cmbOvenMode.getSelectedItem().equals("Temperature")) {
                // Set the setpoint when value is changed
                byte[] sendBytes = new byte[]{};
                String tmpSend = "U" + spnTempSetpoint.getValue().toString() + "\r\n";
                sendBytes = tmpSend.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
            } else {
                // Do nothing in reflow mode
            }
        } catch (IOException ex) {
            System.err.println("Error sending data!");
            System.err.println(ex);
        }
    }//GEN-LAST:event_spnTempSetpointStateChanged

    private void cmbOvenModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbOvenModeActionPerformed
        try {
            if (cmbOvenMode.getSelectedItem().equals("Reflow")) {
                byte[] sendBytes = new byte[]{};
                String input = "M0";
                sendBytes = input.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                lbPreheatSetpoint.setText("Preheat Setpoint");
                lblSoakSetpoint.setText("Soak Setpoint");
                lblReflowSetpoint.setText("Reflow Setpoint");
                lblTempSetpoint.setText("Setpoint Temp");
                lbPreheatSetpoint.setToolTipText("Preheat setpoint range 100 - 150°C, increment 1°C");
                lblSoakSetpoint.setToolTipText("Soak setpoint range 150 - 200°C, increment 1°C");
                lblReflowSetpoint.setToolTipText("Reflow setpoint range 200 - 255°C, increment 1°C");
                lblTempSetpoint.setToolTipText("Temperature setpoint range 20 - 200°C, increment 1°C");
                txtPreheatPost.setText("°C");
                txtSoakPost.setText("°C");
                txtReflowPost.setText("°C");
                lblTempSetpoint.setVisible(false);
                spnTempSetpoint.setVisible(false);
                txtTempPost.setVisible(false);
                spnPreheatSetpoint.setModel(new SpinnerNumberModel(150, 100, 150, 1));
                spnSoakSetpoint.setModel(new SpinnerNumberModel(200, 150, 200, 1));
                spnReflowSetpoint.setModel(new SpinnerNumberModel(240, 200, 255, 1));
                spnTempSetpoint.setModel(new SpinnerNumberModel(40, 20, 200, 1));

                // Wait for request to be processed by logger
                delaySystem(DELAY_SEND); // milliseconds

                sendBytes = new byte[]{};
                input = "C";
                sendBytes = input.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                lastRequest = 'C';

            } else if (cmbOvenMode.getSelectedItem().equals("Temperature")) {
                byte[] sendBytes = new byte[]{};
                String input = "M1";
                sendBytes = input.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                lbPreheatSetpoint.setText("Proportional Gain");
                lblSoakSetpoint.setText("Integral Gain");
                lblReflowSetpoint.setText("Derivative Gain");
                lblTempSetpoint.setText("Setpoint Temp");
                lbPreheatSetpoint.setToolTipText("Proportional Gain range 0 - 2000, increment 0.01");
                lblSoakSetpoint.setToolTipText("Integral Gain range 0 - 2000, increment 0.01");
                lblReflowSetpoint.setToolTipText("Derivative Gain range 0 - 2000, increment 0.1");
                lblTempSetpoint.setToolTipText("Temperature setpoint range 20 - 200°C, increment 1°C");
                txtPreheatPost.setText("");
                txtSoakPost.setText("");
                txtReflowPost.setText("");
                lblTempSetpoint.setVisible(true);
                spnTempSetpoint.setVisible(true);
                txtTempPost.setVisible(true);
                spnPreheatSetpoint.setValue(0);
                spnSoakSetpoint.setValue(0);
                spnReflowSetpoint.setValue(0);
                spnTempSetpoint.setValue(40);
                spnPreheatSetpoint.setModel(new SpinnerNumberModel(5.0, 0.0, 2000.0, 0.1));
                spnSoakSetpoint.setModel(new SpinnerNumberModel(0.01, 0.0, 2000.0, 0.01));
                spnReflowSetpoint.setModel(new SpinnerNumberModel(10.0, 0.0, 2000.0, 0.1));
                spnTempSetpoint.setModel(new SpinnerNumberModel(40, 20, 200, 1));

                // Wait for request to be processed by logger
                delaySystem(DELAY_SEND); // milliseconds

                sendBytes = new byte[]{};
                input = "C";
                sendBytes = input.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
                lastRequest = 'C';
            }
        } catch (IOException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Send", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error sending data!");
            System.err.println(ex);
        }
    }//GEN-LAST:event_cmbOvenModeActionPerformed

    private void cmbCOMSelectPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_cmbCOMSelectPopupMenuWillBecomeVisible
        // Prevent update of list happening
        portFocused = true;
    }//GEN-LAST:event_cmbCOMSelectPopupMenuWillBecomeVisible

    private void cmbCOMSelectPopupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_cmbCOMSelectPopupMenuWillBecomeInvisible
        // Allow update of list happening
        portFocused = false;
    }//GEN-LAST:event_cmbCOMSelectPopupMenuWillBecomeInvisible
        
    private void uploadToDevice() {
        // Disable gui items
        btnUpload.setEnabled(false);
        btnSelectHex.setEnabled(false);
        btnSendSerial.setEnabled(false);
        jSendField.setEnabled(false);
        jSendInFormatBox.setEnabled(false);
        jViewCodeAsBox.setEnabled(false);
        pnlMaster.setEnabled(false);
        
        // First disconnect gui from COM port
        Object originalCOMPort = cmbCOMSelect.getSelectedItem();
        timer.stop();
        if (RS232ConnectionAPI.getInstance().isSessionConnected()) {
            RS232ConnectionAPI.getInstance().stopSession();
            componentClosed();
        } else {
            // Already disconnected for some odd reason
            System.err.println("Upload, COM already disconnected?");
        }
       
        
        // Forcing reset using 1200bps open/close on port
        Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
        ArrayList<String> oldPorts = new ArrayList();
        ArrayList<String> bootPort = new ArrayList();

        Runtime cmdRuntime;
        Process cmdProcess;
        BufferedReader cmdReturn;
        String line;


        String strCmd;
        boolean foundPort = false;
        int cntDelay = 0;

        jLogArea.setText("");
        
        delaySystem(1000);

        // Iterate through, looking for the port
        while (portEnum.hasMoreElements()) {
            CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
            oldPorts.add(currPortId.getName());
        }

        try {
            cmdRuntime = Runtime.getRuntime();

            System.out.println("Resetting Unit...");
            jLogArea.append("Resetting Unit...\n");

            strCmd = String.format("powershell \"$port= new-Object System.IO.Ports.SerialPort %s,1200,None,8,one; $port.open(); $port.Close()\"", cmbCOMSelect.getSelectedItem());
            System.out.println(strCmd);
            System.out.println();
            jLogArea.append(strCmd + "\n\n");
            cmdProcess = cmdRuntime.exec(strCmd);

            System.out.print("Searching for bootloader");
            jLogArea.append("Searching for bootloader");

            while ((foundPort == false) && (cntDelay < 10)) {
                delaySystem(1000);
                cntDelay += 1;
                System.out.print(".");
                jLogArea.append(".");
                portEnum = CommPortIdentifier.getPortIdentifiers();
                // iterate through, looking for the port
                while (portEnum.hasMoreElements()) {
                    CommPortIdentifier currPortId = (CommPortIdentifier) portEnum.nextElement();
                    bootPort.add(currPortId.getName());
                }

                // Remove all elements in firstList from secondList
                bootPort.removeAll(oldPorts);

                if (!bootPort.isEmpty()) {
                    // Found bootloader port, exit loop
                    foundPort = true;
                } else {
                    // Continue to wait for bootloader port
                }
            }
            System.out.println("");
            jLogArea.append("\n");

            if (foundPort == true) {
                // Upload new hex file
                System.out.println("Bootloader found, programming");
                jLogArea.append("Bootloader found, programming\n");
                strCmd = String.format("avrdude\\avrdude.exe -Cavrdude\\avrdude.conf -patmega32u4 -cavr109 -P%s -b57600 -D -Uflash:w:\"%s\":i", bootPort.get(0), txtSelectedHex.getText());
                System.out.println(strCmd);
                jLogArea.append(strCmd + "\n");
                cmdProcess = cmdRuntime.exec(strCmd);

                cmdReturn = new BufferedReader(new InputStreamReader(cmdProcess.getErrorStream()));

                while ((line = cmdReturn.readLine()) != null) {
                    System.out.println(line);
                    jLogArea.append(line + "\n");
                }
            } else {
                System.err.println("Timeout, could not find bootloader");
                jLogArea.append("Timeout, could not find bootloader\n"); 
            }

        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(null, ex.getMessage(), "Upload", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error uploading Hex file!");
            System.err.println(ex.getMessage());
        }
        
        // Enable gui items
        btnUpload.setEnabled(true);
        btnSelectHex.setEnabled(true);
        btnSendSerial.setEnabled(true);
        jSendField.setEnabled(true);
        jSendInFormatBox.setEnabled(true);
        jViewCodeAsBox.setEnabled(true);
        pnlMaster.setEnabled(true);
        
        timer.start();
        delaySystem(1000);
        
        // Reconnect to COM port
        try {
            String str = (String) (cmbBaudRate.getSelectedItem());
            RS232ConnectionAPI.getInstance().setComPort((String) originalCOMPort);
            RS232ConnectionAPI.getInstance().setDataRate(Integer.parseInt(str));
            componentOpened();
            RS232ConnectionAPI.getInstance().startSession();

        } catch (ComPortInUseException ex) {
            Exceptions.printStackTrace(ex);
            javax.swing.JOptionPane.showMessageDialog(this, "COM port in use", "Connect", JOptionPane.ERROR_MESSAGE);
        } catch (ComPortNotFoundException ex) {
            Exceptions.printStackTrace(ex);
            javax.swing.JOptionPane.showMessageDialog(this, "Could not find COM port", "Connect", JOptionPane.ERROR_MESSAGE);
        } catch (ComPortException ex) {
            Exceptions.printStackTrace(ex);
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Connect", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            System.err.println(ex);
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Connect", JOptionPane.ERROR_MESSAGE);
        }
        
    }
    
    // @Override
    public void componentOpened() {
        if (!RS232ConnectionAPI.getInstance().isSessionConnected()) {
            initCommPortComboBox();
        }
        if (!RS232ConnectionAPI.getInstance().isObserver(this)) {
            RS232ConnectionAPI.getInstance().registerObserver(this);
        }
    }

    //@Override
    public void componentClosed() {
        if (!RS232ConnectionAPI.getInstance().isSessionConnected()) {
            RS232ConnectionAPI.getInstance().unregisterObserver(this);
        }
    }

    @Override
    public void eventListener(IRS232Events ev) {
        if (ev instanceof ConnectedRS232Event) {
            btnConnect.setText("Disconnect");
            btnStartProcess.setText("Start Process");
            jLogArea.setText("");
            edtMessages.setText("\tOVEN IDLE");
            edtMessages.setForeground(Color.BLUE);
            cmbCOMSelect.setEnabled(false);
            cmbBaudRate.setEnabled(false);
            btnRefresh.setEnabled(false);
            jSendField.setEnabled(true);
            cmbOvenMode.setEnabled(true);
            btnSendSerial.setEnabled(true);
            jSendInFormatBox.setEnabled(true);
            jViewCodeAsBox.setEnabled(true);
            btnStartProcess.setEnabled(true);
            btnReadSettings.setEnabled(true);
            btnSendSettings.setEnabled(true);
            btnClearTable.setEnabled(true);
            mniRead.setEnabled(true);
            mniSend.setEnabled(true);
            //mniLogin.setEnabled(true);
            pnlMaster.setEnabled(true);
            // Clear the graph
            seriesSetpoint.clear();
            seriesActual.clear();
            btnSelectHex.setEnabled(true);
            if (!txtSelectedHex.getText().equals("")) {
                // Enable upload button only if file has been selected
                btnUpload.setEnabled(true);
            }
        
//            if (mniLogin.getText().equals("Logout")) {
//                pnlMaster.setEnabledAt(2, true); // Enable serial panel
//            } else {
//                pnlMaster.setEnabledAt(2, false); // Disable serial panel
//            }

            try {
                // Get firmware version
                byte[] sendBytes = new byte[]{};
                String input = "F";
                sendBytes = input.getBytes();
                RS232ConnectionAPI.getInstance().send(sendBytes);
            } catch (IOException ex) {
                System.err.println("Error sending data!");
                System.err.println(ex);
            }
            
            lastRequest = ' ';
            isConnected = true;
            dataCount = 0;

            // Clear events table
            for (int i = tblRawData.getRowCount(); i > 1; i--) {
                ((DefaultTableModel) tblRawData.getModel()).removeRow(i - 1);
            }
            for (int j = 0; j < tblRawData.getColumnCount(); j++) {
                tblRawData.setValueAt("", 0, j);
            }

            // Read the controller setup
            btnReadSettings.doClick();

        } else if (ev instanceof DisconnectedRS232Event) {
            btnConnect.setText("Connect");
            btnStartProcess.setText("Start Process");
            edtMessages.setText("");
            edtMessages.setForeground(Color.BLUE);
            cmbCOMSelect.setEnabled(true);
            cmbBaudRate.setEnabled(false);
            btnRefresh.setEnabled(true);
            jSendField.setEnabled(false);
            btnSendSerial.setEnabled(false);
            jSendInFormatBox.setEnabled(false);
            jViewCodeAsBox.setEnabled(false);
            btnStartProcess.setEnabled(false);
            cmbOvenMode.setEnabled(false);
            btnReadSettings.setEnabled(false);
            btnSendSettings.setEnabled(false);
            btnClearTable.setEnabled(false);
            btnExportCSV.setEnabled(false);
            mniExportCSV.setEnabled(false);
            pnlMaster.setEnabled(false);
            mniRead.setEnabled(false);
            mniSend.setEnabled(false);
            mniLogin.setEnabled(false);
            mniExportCSV.setEnabled(false);
            btnViewData.setEnabled(false);
            btnExportGraph.setEnabled(false);
            mniExportGraph.setEnabled(false);
            isConnected = false;
            dataCount = 0;
            // Clear the graph
            seriesSetpoint.clear();
            seriesActual.clear();
            btnUpload.setEnabled(false);
            btnSelectHex.setEnabled(false);
            btnUpload.setEnabled(false);
        } else if (ev instanceof DataReceivedRS232Event) {

            DataReceivedRS232Event evt = (DataReceivedRS232Event) ev;
            String data = new String(evt.getData());
            if (jViewCodeAsBox.getSelectedItem().equals("Hex")) {
                data = HexBinOctUtils.getHexString(evt.getData());
            } else if (jViewCodeAsBox.getSelectedItem().equals("Bin")) {
                data = HexBinOctUtils.getBinString(evt.getData());
            } else if (jViewCodeAsBox.getSelectedItem().equals("Oct")) {
                data = HexBinOctUtils.getOctString(evt.getData());
            }
            jLogArea.append(data);
            serialDataStr.append(data);
            int idxStr = 0;
            
            try {
                while (idxStr != -1) {
                    idxStr = serialDataStr.indexOf("\n");
                    if (idxStr != -1) {
                        String tmpStr = serialDataStr.substring(0, idxStr);
                        handleDataMessage(tmpStr);
                        updateCount = 0;
                        serialDataStr.delete(0, idxStr+1); // Plus Newline
                    }
                }
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Last Request", JOptionPane.ERROR_MESSAGE);
                System.err.println("Error Last Request! ");
                System.err.println(ex.getMessage());
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        // Modified to Windows, Nimbus moves gui items
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ReflowControllerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ReflowControllerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ReflowControllerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ReflowControllerUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ReflowControllerUI().setVisible(true);
            }
        });

    }

    public void handleCountEvent(String tmpString) {

        try {
            int tmpCount = Integer.parseInt(tmpString);

            // Send sw ver command
            byte[] sendBytes = new byte[]{};
            String tmpSend = "";

            // Get software version
            tmpSend = "s";
            sendBytes = tmpSend.getBytes();
            RS232ConnectionAPI.getInstance().send(sendBytes);
            lastRequest = 's';

        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Send Count", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error Reading Count! ");
            System.err.println(ex.getMessage());
        }
    }

    public void handleDataMessage(String tmpData) {
        try {
            // Extract data from string
            int idxTmp = -1;
            int cnt = 0;
            
            while((idxTmp = tmpData.indexOf(',', idxTmp+1)) > 0)
                cnt++;
            
            switch (cnt) {
                case 0:
                    
                    // Check if settings received on connection start
                    idxTmp = -1;
                    int cnt2 = 0;
                    while((idxTmp = tmpData.indexOf('-', idxTmp+1)) > 0)
                        cnt2++;
                    
                    if (cnt2 == 2) {
                        int idxStr1 = tmpData.indexOf('-');
                        int idxStr2 = tmpData.indexOf('-', idxStr1+1);
                        
                        String preheatStr = tmpData.substring(3, idxStr1);
                        String soakStr = tmpData.substring(idxStr1+1, idxStr2);
                        String reflowStr = tmpData.substring(idxStr2+1,tmpData.length()-1);
                        try {
                            spnPreheatSetpoint.setValue(Float.parseFloat(preheatStr));
                            spnSoakSetpoint.setValue(Float.parseFloat(soakStr));
                            spnReflowSetpoint.setValue(Float.parseFloat(reflowStr));
                        } catch (Exception ex) {
                            //javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Read Events", JOptionPane.ERROR_MESSAGE);
                            System.err.print("Error Reading Settings! ");
                            System.err.println(ex.getMessage());
                        }
                    }
                    
                    // Check firmware version
                    idxTmp = tmpData.indexOf("FW: ");
                    if (idxTmp != -1) {
                        String fwVersion = tmpData.substring(idxTmp+4);
                        edtFirmware.setText(fwVersion);
                    }
                    
                    // Check oven mode reflow
                    idxTmp = tmpData.indexOf("I: P-S-R");
                    if (idxTmp != -1) {
                        if (cmbOvenMode.getSelectedItem().equals("Temperature")) {
                            cmbOvenMode.setSelectedItem("Reflow");
                            edtMessages.setText("\tOVEN IDLE");
                            edtMessages.setForeground(Color.BLUE);
                        } else {
                            // Already in reflow mode, ignore
                        }
                    }
                    
                    // Check oven mode temperature
                    idxTmp = tmpData.indexOf("I: P-I-D");
                    if (idxTmp != -1) {
                        if (cmbOvenMode.getSelectedItem().equals("Reflow")) {
                            cmbOvenMode.setSelectedItem("Temperature");
                            edtMessages.setText("\tOVEN IDLE");
                            edtMessages.setForeground(Color.BLUE);
                        } else {
                            // Already in reflow mode, ignore
                        }
                    }
                    
                    // Check Idle
                    idxTmp = tmpData.indexOf("I: Idle");
                    if (idxTmp != -1) {
                        btnStartProcess.setText("Start Process");
                        cmbOvenMode.setEnabled(true);
                        spnPreheatSetpoint.setEnabled(true);
                        spnSoakSetpoint.setEnabled(true);
                        spnReflowSetpoint.setEnabled(true);
                        btnExportCSV.setEnabled(true);
                        mniExportCSV.setEnabled(true);
                        btnClearTable.setEnabled(true);
                        btnReadSettings.setEnabled(true);
                        btnSendSettings.setEnabled(true);
                        mniRead.setEnabled(true);
                        mniSend.setEnabled(true);
                        btnSelectHex.setEnabled(true);
                        if (!txtSelectedHex.getText().equals("")) {
                            // Enable upload button only if file has been selected
                            btnUpload.setEnabled(true);
                        }
                        edtMessages.setText("\tOVEN IDLE");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                    // Check Preheat
                    idxTmp = tmpData.indexOf("I: Preheat");
                    if (idxTmp != -1) {
                        edtMessages.setText("\tOVEN PREHEATING");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                    // Check Soak
                    idxTmp = tmpData.indexOf("I: Soak");
                    if (idxTmp != -1) {
                        edtMessages.setText("\tOVEN SOAKING");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                    // Check Reflow
                    idxTmp = tmpData.indexOf("I: Reflow");
                    if (idxTmp != -1) {
                        edtMessages.setText("\tOVEN REFLOWING");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                    // Check Cool
                    idxTmp = tmpData.indexOf("I: Cool");
                    if (idxTmp != -1) {
                        edtMessages.setText("\tOVEN COOLING\n\tOPEN OVEN DOOR!!");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                    // Check Complete
                    idxTmp = tmpData.indexOf("I: Complete");
                    if (idxTmp != -1) {
                        edtMessages.setText("\tPROCESS COMPLETE");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                     // Check Heating
                    idxTmp = tmpData.indexOf("I: Heating");
                    if (idxTmp != -1) {
                        edtMessages.setText("\tOVEN HEATING");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                    // Check Settling
                    idxTmp = tmpData.indexOf("I: Settling");
                    if (idxTmp != -1) {
                        edtMessages.setText("\tOVEN SETTLING");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                    // Check Steady
                    idxTmp = tmpData.indexOf("I: Steady");
                    if (idxTmp != -1) {
                        edtMessages.setText("\tOVEN STEADY");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                    // Check Wait,hot
                    idxTmp = tmpData.indexOf("I: Wait,hot");
                    if (idxTmp != -1) {
                        edtMessages.setText("\tOVEN HOT");
                        edtMessages.setForeground(Color.RED);
                    }
                    
                    // Check Error
                    idxTmp = tmpData.indexOf("I: Error");
                    if (idxTmp != -1) {
                        edtMessages.setText("\tOVEN ERROR");
                        edtMessages.setForeground(Color.RED);
                    }
                    
                    // Check if oven switched on
                    idxTmp = tmpData.indexOf("I: OVEN->ON");
                    if (idxTmp != -1) {
                        btnStartProcess.setText("Stop Process");
                        cmbOvenMode.setEnabled(false);
                        spnPreheatSetpoint.setEnabled(false);
                        spnSoakSetpoint.setEnabled(false);
                        spnReflowSetpoint.setEnabled(false);
                        btnExportCSV.setEnabled(false);
                        mniExportCSV.setEnabled(false);
                        btnClearTable.setEnabled(false);
                        btnReadSettings.setEnabled(false);
                        btnSendSettings.setEnabled(false);
                        mniRead.setEnabled(false);
                        mniSend.setEnabled(false);
                        btnExportGraph.setEnabled(false);
                        mniExportGraph.setEnabled(false);
                        btnViewData.setEnabled(false);
                        btnSelectHex.setEnabled(false);
                        btnUpload.setEnabled(false);
                        edtMessages.setText("\tOVEN ON");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                    // Check if oven switched off
                    idxTmp = tmpData.indexOf("I: OVEN->OFF");
                    if (idxTmp != -1) {
                        btnStartProcess.setText("Start Process");
                        cmbOvenMode.setEnabled(true);
                        spnPreheatSetpoint.setEnabled(true);
                        spnSoakSetpoint.setEnabled(true);
                        spnReflowSetpoint.setEnabled(true);
                        btnExportGraph.setEnabled(true);
                        mniExportGraph.setEnabled(true);
                        btnViewData.setEnabled(true);
                        btnExportCSV.setEnabled(true);
                        mniExportCSV.setEnabled(true);
                        btnClearTable.setEnabled(true);
                        btnReadSettings.setEnabled(true);
                        btnSendSettings.setEnabled(true);
                        mniRead.setEnabled(true);
                        mniSend.setEnabled(true);
                        btnSelectHex.setEnabled(true);
                        if (!txtSelectedHex.getText().equals("")) {
                            // Enable upload button only if file has been selected
                            btnUpload.setEnabled(true);
                        }
                        edtMessages.setText("\tOVEN IDLE");
                        edtMessages.setForeground(Color.BLUE);
                    }
                    
                    boolean errorOccurred = false;
                    // Check error message
                    idxTmp = tmpData.indexOf("E: Thermocouple Open");
                    if (idxTmp != -1) {
                        System.err.println("TC OPEN");
                        edtMessages.setText("\tERROR: TC OPEN!");
                        edtMessages.setForeground(Color.RED);
                        errorOccurred = true;
                    }
                    idxTmp = tmpData.indexOf("E: Thermocouple Short");
                    if (idxTmp != -1) {
                        System.err.println("TC SHORT");
                        edtMessages.setText("\tERROR: TC SHORT!");
                        edtMessages.setForeground(Color.RED);
                        errorOccurred = true;
                    }
                    idxTmp = tmpData.indexOf("E: Timeout to Reflow");
                    if (idxTmp != -1) {
                        System.err.println("TIMEOUT");
                        edtMessages.setText("\tERROR: TIMEOUT!");
                        edtMessages.setForeground(Color.RED);
                        errorOccurred = true;
                    }
                    idxTmp = tmpData.indexOf("E: Unknown");
                    if (idxTmp != -1) {
                        System.err.println("UNKNOWN");
                        edtMessages.setText("\tERROR: UNKNOWN!");
                        edtMessages.setForeground(Color.RED);
                        errorOccurred = true;
                    }
                    
                    if (errorOccurred == true) {
                        // Change button states
                        btnStartProcess.setText("Start Process");
                        cmbOvenMode.setEnabled(true);
                        spnPreheatSetpoint.setEnabled(true);
                        spnSoakSetpoint.setEnabled(true);
                        spnReflowSetpoint.setEnabled(true);
                        btnExportGraph.setEnabled(true);
                        mniExportGraph.setEnabled(true);
                        btnViewData.setEnabled(true);
                        btnExportCSV.setEnabled(true);
                        mniExportCSV.setEnabled(true);
                        btnClearTable.setEnabled(true);
                        btnReadSettings.setEnabled(true);
                        btnSendSettings.setEnabled(true);
                        mniRead.setEnabled(true);
                        mniSend.setEnabled(true);
                        btnSelectHex.setEnabled(true);
                        if (!txtSelectedHex.getText().equals("")) {
                            // Enable upload button only if file has been selected
                            btnUpload.setEnabled(true);
                        }
                    }
                    break;
                case 3:
                    int idxStr1 = tmpData.indexOf(',');
                    int idxStr2 = tmpData.indexOf(',', idxStr1+1);
                    int idxStr3 = tmpData.indexOf(',', idxStr2+1);
                    String timeStr = tmpData.substring(0, idxStr1);
                    String setpointStr = tmpData.substring(idxStr1+1, idxStr2);
                    String actualStr = tmpData.substring(idxStr2+1, idxStr3);
                    String pwmStr = tmpData.substring(idxStr3+1,tmpData.length()-1);

                    if (NumberUtils.isDigits(timeStr)) {
                        if ((timeStr.equals("0")) || (setpointStr.equals("0.00"))) {
                            // Only update the GUI fields, idle mode
                            // Update GUI fields
                            edtRunTime.setText(timeStr);
                            edtProcessSetpoint.setText(setpointStr);
                            edtProcessActual.setText(actualStr);
                        } else {
                            // Update GUI fields
                            edtRunTime.setText(timeStr);
                            edtProcessSetpoint.setText(setpointStr);
                            edtProcessActual.setText(actualStr);
                            // If process was running previously, update button text
                            if (btnStartProcess.getText().equals("Start Process")) {
                                btnStartProcess.setText("Stop Process");
                                edtMessages.setText("\tOVEN ON");
                                edtMessages.setForeground(Color.BLUE);
                                cmbOvenMode.setEnabled(false);
                                spnPreheatSetpoint.setEnabled(false);
                                spnSoakSetpoint.setEnabled(false);
                                spnReflowSetpoint.setEnabled(false);
                                btnExportCSV.setEnabled(false);
                                mniExportCSV.setEnabled(false);
                                btnClearTable.setEnabled(false);
                                btnReadSettings.setEnabled(false);
                                btnSendSettings.setEnabled(false);
                                mniRead.setEnabled(false);
                                mniSend.setEnabled(false);
                                btnExportGraph.setEnabled(false);
                                mniExportGraph.setEnabled(false);
                                btnViewData.setEnabled(false);
                                btnSelectHex.setEnabled(false);
                                btnUpload.setEnabled(false);
                            }

                            DefaultTableModel model = (DefaultTableModel) tblRawData.getModel();

                            if (model.getRowCount() <= dataCount+1) {
                                // Add new row
                                Object[] row = {"", "", "", ""};
                                model.addRow(row);
                            } else {
                                // Do nothing
                            }

                            tblRawData.setValueAt(timeStr, dataCount, 0);
                            tblRawData.setValueAt(setpointStr, dataCount, 1);
                            tblRawData.setValueAt(actualStr, dataCount, 2);
                            tblRawData.setValueAt(pwmStr, dataCount, 3);

                            seriesSetpoint.add(Integer.parseInt(timeStr), Float.parseFloat(setpointStr));
                            seriesActual.add(Integer.parseInt(timeStr), Float.parseFloat(actualStr));

                            dataCount += 1;
                        }
                    }
                    break;
            }

        } catch (Exception ex) {
            //javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(), "Read Events", JOptionPane.ERROR_MESSAGE);
            System.err.print("Error Reading Data! ");
            System.err.println(ex.getMessage());
        }
    }

    public static String hexSwap(String origHex) {
        String newHex = origHex;
        if (origHex.length() == 2) {
            // Int8
            newHex = origHex;
        } else {
            if (origHex.length() == 4) {
                // Int16
                newHex = origHex.substring(2, 4);
                newHex = newHex.concat(origHex.substring(0, 2));
            } else {
                if (origHex.length() == 8) {
                    // Int32
                    newHex = origHex.substring(6, 8);
                    newHex = newHex.concat(origHex.substring(4, 6));
                    newHex = newHex.concat(origHex.substring(2, 4));
                    newHex = newHex.concat(origHex.substring(0, 2));
                }
            }
        }
        return newHex;
    }
    
    // allows default cut copy paste popup menu actions
    private void addCutCopyPastePopUp(JTextComponent textComponent) {
       ActionMap am = textComponent.getActionMap();
       Action paste = am.get("paste-from-clipboard");
       Action copy = am.get("copy-to-clipboard");
       Action cut = am.get("cut-to-clipboard");

       cut.putValue(Action.NAME,   "Cut");
       copy.putValue(Action.NAME,  "Copy");
       paste.putValue(Action.NAME, "Paste");

       JPopupMenu popup = new JPopupMenu("Copy Paste Popup");
       textComponent.setComponentPopupMenu(popup);
       popup.add(new JMenuItem(cut));
       popup.add(new JMenuItem(copy));
       popup.add(new JMenuItem(paste));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnClearTable;
    private javax.swing.JButton btnConnect;
    private javax.swing.JButton btnExportCSV;
    private javax.swing.JButton btnExportGraph;
    private javax.swing.JButton btnReadSettings;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnSelectHex;
    private javax.swing.JButton btnSendSerial;
    private javax.swing.JButton btnSendSettings;
    private javax.swing.JButton btnStartProcess;
    private javax.swing.JButton btnUpload;
    private javax.swing.JButton btnViewData;
    private javax.swing.JComboBox cmbBaudRate;
    private javax.swing.JComboBox cmbCOMSelect;
    private javax.swing.JComboBox<String> cmbOvenMode;
    private javax.swing.JTextField edtFirmware;
    private javax.swing.JTextArea edtMessages;
    private javax.swing.JTextField edtProcessActual;
    private javax.swing.JTextField edtProcessSetpoint;
    private javax.swing.JTextField edtRunTime;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JTextArea jLogArea;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPaneMsg;
    private javax.swing.JTextField jSendField;
    private javax.swing.JComboBox jSendInFormatBox;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JComboBox jViewCodeAsBox;
    private javax.swing.JLabel lbPreheatSetpoint;
    private javax.swing.JLabel lblFirmware;
    private javax.swing.JLabel lblOvenMode;
    private javax.swing.JLabel lblReflowSetpoint;
    private javax.swing.JLabel lblSoakSetpoint;
    private javax.swing.JLabel lblTempSetpoint;
    private javax.swing.JMenuItem mnAbout;
    private javax.swing.JMenuItem mniDatasheet;
    private javax.swing.JMenuItem mniDefault;
    private javax.swing.JMenuItem mniExit;
    private javax.swing.JMenuItem mniExportCSV;
    private javax.swing.JMenuItem mniExportGraph;
    private javax.swing.JMenuItem mniLogin;
    private javax.swing.JMenuItem mniManual;
    private javax.swing.JMenuItem mniRead;
    private javax.swing.JMenuItem mniSend;
    private javax.swing.JMenu mnuEdit;
    private javax.swing.JMenu mnuFile;
    private javax.swing.JMenu mnuHelp;
    private javax.swing.JPanel pnlCommunication;
    private javax.swing.JPanel pnlControllerInfo;
    private javax.swing.JPanel pnlDateTime;
    private javax.swing.JPanel pnlGraph;
    private javax.swing.JPanel pnlMain;
    private javax.swing.JTabbedPane pnlMaster;
    private javax.swing.JPanel pnlRawData;
    private javax.swing.JPanel pnlSerial;
    private javax.swing.JPanel pnlSerialConsole;
    private javax.swing.JPanel pnlSettings;
    private javax.swing.JPanel pnlUploadFirmware;
    private javax.swing.JSpinner spnPreheatSetpoint;
    private javax.swing.JSpinner spnReflowSetpoint;
    private javax.swing.JSpinner spnSoakSetpoint;
    private javax.swing.JSpinner spnTempSetpoint;
    private javax.swing.JTable tblRawData;
    private javax.swing.JLabel txtDegSign5;
    private javax.swing.JLabel txtDegSign6;
    private javax.swing.JLabel txtPreheatPost;
    private javax.swing.JLabel txtProcessActual;
    private javax.swing.JLabel txtProcessSetpoint;
    private javax.swing.JLabel txtReflowPost;
    private javax.swing.JLabel txtRunTime1;
    private javax.swing.JLabel txtSeconds;
    private javax.swing.JTextField txtSelectedHex;
    private javax.swing.JLabel txtSoakPost;
    private javax.swing.JLabel txtTempPost;
    // End of variables declaration//GEN-END:variables

}
