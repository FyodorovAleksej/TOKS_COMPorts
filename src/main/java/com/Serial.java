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
    public boolean write(byte[] bytes, String address, String source) {
        try {
            return port.writeBytes(incapsulate(bytes, address, source));
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
            byte[] in = port.readBytes(byteCount);
            return decapsulate(in);
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
    public void setFlowControl(int mask) {
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

    /**
     * transform bytes into string format
     * @param bytes - the byte for transforming
     * @return - the string format of bytes
     */
    private String toBinary( byte[] bytes ) {
        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
        for( int i = 0; i < Byte.SIZE * bytes.length; i++ )
            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
        return sb.toString();
    }

    /**
     * transform string into bytes
     * @param s - the string format
     * @return - the bytes
     */
    private byte[] fromBinary( String s ) {
        int sLen = s.length();
        byte[] toReturn = new byte[(sLen + Byte.SIZE - 1) / Byte.SIZE];
        char c;
        for (int i = 0; i < sLen; i++ )
            if( (c = s.charAt(i)) == '1' )
                toReturn[i / Byte.SIZE] = (byte) (toReturn[i / Byte.SIZE] | (0x80 >>> (i % Byte.SIZE)));
            else if ( c != '0' )
                throw new IllegalArgumentException();
        return toReturn;
    }

    private byte[] incapsulate(byte[] raw, String address, String source) {
        String resString = toBinary(raw).replaceAll("11111","111110");
        resString = "01111110" + toBinary(address.getBytes()).replaceAll("11111","111110") + "01111110" + toBinary(source.getBytes()).replaceAll("11111","111110") + "01111110" + resString;
        boolean parity = false;
        for (int i = 0; i < resString.length(); i++) {
            if (resString.charAt(i) == '1') {
                parity = !parity;
            }
        }
        if (parity) {
            resString = resString + "1";
        }
        else {
            resString = resString + "0";
        }
        return fromBinary(resString);
    }

    private byte[] decapsulate(byte[] complex) {
        String temp = toBinary(complex);
        boolean parity = false;
        for (int i = 0; i < temp.length(); i++) {
            if (temp.charAt(i) == '1') {
                parity = !parity;
            }
        }
        if (!parity) {
            temp = temp.substring(0,temp.length() - Byte.SIZE);
            int start = temp.indexOf("01111110");
            if (start >= 0) {
                temp = temp.substring(start + 8);
                String[] reading = temp.split("01111110");
                if (reading.length > 1) {
                    reading[0] = reading[0].replaceAll("111110", "11111");
                    String address = new String(fromBinary(reading[0]));
                    reading[1] = reading[1].replaceAll("111110", "11111");
                    String source = new String(fromBinary(reading[1]));
                    System.out.println("source = " + source);
                    if (address.equals(port.getPortName())) {
                        return fromBinary(reading[2].replaceAll("111110", "11111"));
                    }
                }
            }
        }
        return null;
    }
}