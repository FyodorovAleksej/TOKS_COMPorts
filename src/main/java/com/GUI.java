package com;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static java.lang.Thread.sleep;

public class GUI {

    private static JLabel responseLabel;
    private static JLabel infoLabel;
    private static JButton sendButton;
    private static JButton connectButton;
    private static JButton refreshButton;
    private static JTextArea textField;
    private static Serial serialPort;
    private static JComboBox<String> speedBox;
    private static JComboBox<String> portsBox;
    private static JComboBox<String> codingsBox;


    /**
     * init GUI elements and Listeners
     */
    public void init() {
        responseLabel = new JLabel("Response: ");
        infoLabel = new JLabel("Info: ");
        sendButton = new JButton("SEND");
        connectButton = new JButton("connect");
        refreshButton = new JButton("refresh");
        textField = new JTextArea();
        textField.setLineWrap(true);

        JFrame frame = new JFrame("hello");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 200);



        String[] ports = SerialPortList.getPortNames();
        JPanel allPanel = new JPanel();

        String[] codings = {"Hemming", "CRC"};
        codingsBox = new JComboBox<String>(codings);

        allPanel.setLayout(new BorderLayout());
        portsBox = new JComboBox<String>(ports);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridLayout(2,1));
        leftPanel.add(portsBox);
        leftPanel.add(codingsBox);

        allPanel.add(leftPanel, BorderLayout.WEST);

        String[] bounds = {"110", "300", "600", "1200", "4800", "9600", "14400", "19200", "38400", "57600", "115200", "128000", "256000"};
        speedBox = new JComboBox<String>(bounds);
        allPanel.add(speedBox, BorderLayout.EAST);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new GridLayout(2,1));

        JPanel downPanel = new JPanel();
        downPanel.setLayout(new GridLayout(1,3));

        downPanel.add(refreshButton);
        downPanel.add(sendButton);
        downPanel.add(connectButton);

        southPanel.add(downPanel);
        southPanel.add(infoLabel);

        allPanel.add(textField, BorderLayout.CENTER);
        allPanel.add(southPanel, BorderLayout.SOUTH);
        allPanel.add(responseLabel, BorderLayout.NORTH);

        frame.add(allPanel);
        frame.setVisible(true);

        if (ports.length == 0) {
            sendButton.setEnabled(false);
        }

        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendActionPerformed();
            }
        });

        refreshButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                refreshActionPerformed();
            }
        });

        connectButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                connectActionPerformed();
            }
        });

    }

    private static class PortReader implements SerialPortEventListener {
        /**
         * Listener for reading from SerialPort how fast, such information was write
         * @param event - the event of writing
         */
        public void serialEvent(SerialPortEvent event) {
            if(event.isRXCHAR() && event.getEventValue() > 0) {
                byte[] readBytes = serialPort.read(event.getEventValue(), codingsBox.getSelectedItem().toString().equals("Hemming"));
                if (readBytes != null) {
                    String result = new String(readBytes);
                    responseLabel.setText(responseLabel.getText() + result);
                    Calendar calendar = new GregorianCalendar();
                    infoLabel.setText("Info: " + Long.toString(calendar.getTimeInMillis()) + " - reading");
                }
            }
        }
    }

    /**
     * refresh list of available COM ports
     */
    private void refreshActionPerformed() {
        String[] ports = SerialPortList.getPortNames();
        portsBox.removeAllItems();
        for (String s : ports) {
            portsBox.addItem(s);
        }
        if (ports.length == 0) {
            sendButton.setEnabled(false);
        }
        else {
            sendButton.setEnabled(true);
        }
        infoLog("port list was refreshed");
    }

    /**
     * send text of textArea to COM port
     */
    private void sendActionPerformed() {
        connectActionPerformed();
        String text = textField.getText();
        int  i;
        if (codingsBox.getSelectedItem().toString().equals("Hemming")) {
            for (i = 0; i < text.length() - 2; ) {
                StringBuilder builder = new StringBuilder();
                builder.append(text.charAt(i));
                i++;
                builder.append(text.charAt(i));
                i++;
                try {
                    sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                serialPort.write(builder.toString().getBytes(), true);
            }
            if (i + 1 >= text.length()) {
                serialPort.write((text.substring(i) + " ").getBytes(), true);
            } else {
                serialPort.write(text.substring(i).getBytes(), codingsBox.getSelectedItem().toString().equals("Hemming"));
            }
        }
        else {
            for (i = 0; i < text.length(); i++) {
                try {
                    sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                serialPort.write(String.valueOf(text.charAt(i)).getBytes(), false);
            }
        }
        Calendar calendar = new GregorianCalendar();
        infoLog(Long.toString(calendar.getTimeInMillis()) + " - sending");
    }

    /**
     * connecting to selected port
     */
    private void connectActionPerformed() {
        if (serialPort != null) {
            serialPort.close();
        }

        serialPort = new Serial(portsBox.getSelectedItem().toString());
        boolean flag = serialPort.open();
        serialPort.setParams(Integer.valueOf(speedBox.getSelectedItem().toString()),
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
        //serialPort.setFlowControl(SerialPort.FLOWCONTROL_RTSCTS_IN |
        //        SerialPort.FLOWCONTROL_RTSCTS_OUT);

        serialPort.addListener(new PortReader());
        if (flag) {
            infoLog("was connected");
            responseLabel.setBackground(Color.GREEN);
            responseLabel.setOpaque(true);
        }
        else {
            infoLog("can't connected");
            responseLabel.setBackground(null);
            responseLabel.setOpaque(false);
        }
    }

    /**
     * logging message to information label on GUI and console
     * @param message - the message for logging
     */
    private void infoLog(String message) {
        if (infoLabel != null) {
            infoLabel.setText("Info: " + message);
        }
        System.out.println(message);
    }
}
