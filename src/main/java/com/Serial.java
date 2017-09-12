package com;

import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

public class Serial implements SerialInterface {
    private SerialPort port;
    private boolean opened;

    /**
     * Creating port with this name
     * @param name - the name of COM port to create
     */
    public Serial(String name) {
        this.port = new SerialPort(name);
        opened = false;
    }

    /**
     * writing all bytes to COM port
     * @param bytes - the bytes for writing
     * @return is writing finish successfully
     */
    public boolean write(byte[] bytes) {
        try {
            return port.writeBytes(bytes);
        }
        catch (SerialPortException ex) {
            ex.printStackTrace();
            return  false;
        }
    }

    /**
     * reading some bytes from port
     * @param byteCount - the count of bytes for reading
     * @return the bytes, that was readied
     */
    public byte[] read(int byteCount) {
        try {
            return port.readBytes(byteCount);
        }
        catch (SerialPortException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * connecting to port
     * @return is connecting was successfully
     */
    public boolean open() {
        try {
            opened = port.openPort();
            return opened;
        }
        catch (SerialPortException ex)
        {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * closing connection
     * @return is closing was successfully
     */
    public boolean close() {
        try {
            if (opened) {
                opened = false;
                return port.closePort();
            }
            return false;
        }
        catch (SerialPortException ex)
        {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * setting parameters to port
     * @param baudRate - the speed of COM port in bauds
     * @param dataBits - the count of bits for data
     * @param stopBits - the count of stop bits
     * @param parity - the settings of the parity bit
     */
    public void setParams(int baudRate, int dataBits, int stopBits, int parity) {
        try {
            port.setParams(baudRate, dataBits, stopBits, parity);
        }
        catch (SerialPortException ex){
            ex.printStackTrace();
        }
    }

    /**
     * adding listener to port, when it was using
     * @param listener - the listener of read event for adding
     */
    public void addListener(SerialPortEventListener listener) {
        try {
            port.setEventsMask(SerialPort.MASK_RXCHAR);
            port.addEventListener(listener);
        }
        catch (SerialPortException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * setting parameters for flow control
     * @param mask - the mask of parameters
     */
    public void setFlowControl(int mask){
        try {
            port.setFlowControlMode(mask);
        } catch (SerialPortException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * is port opened?
     * @return the current status of port
     */
    public boolean isOpen(){
        return opened;
    }
}