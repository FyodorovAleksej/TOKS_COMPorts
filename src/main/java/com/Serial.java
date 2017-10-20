package com;
import jssc.SerialPort;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import java.util.Random;

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
    public boolean write(byte[] bytes, boolean hemming) {
        try {
            if (hemming) {
                return port.writeBytes(hemingCoding(bytes, 5));
            }
            else {
                return port.writeBytes(crcCoding(bytes));
            }
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
    public byte[] read(int byteCount, boolean hemming) {
        try {
            byte[] in = port.readBytes(byteCount);
            if (hemming) {
                return hemingDecode(in);
            }
            else {
                return crcDecoding(in);
            }
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
    public static String toBinary( byte[] bytes ) {
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
    public static byte[] fromBinary( String s ) {
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

    /**
     * incapsulate bytes into package for sending
     * @param raw - raw bytes
     * @param address - the address for sending
     * @param source - the source of sending
     * @return - incapsulated package
     */
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

    /**
     * getting bytes from incapsulated package
     * @param complex - incapsulated package
     * @return - the message from package
     */
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
                    if (address.equals(port.getPortName())) {
                        return fromBinary(reading[2].replaceAll("111110", "11111"));
                    }
                }
            }
        }
        return null;
    }


    /**
     * incapsulating bytes with Hemming code for repair error bit
     * @param raw - raw bytes
     * @param error - the position of error (-1) - without error
     * @return - the incapsulated package with Hemming code
     */
    public static byte[] hemingCoding(byte[] raw, int error) {
        String rs = toBinary(raw);
        StringBuilder finalString = new StringBuilder();
        String rawStr;
        rawStr = rs;
        StringBuilder r = new StringBuilder();
        StringBuilder result = new StringBuilder();
        String[] temp = new String[5];
        int pow = 1;
        for (int j = 0; j < 5; j++) {
            temp[j] = "";
            for (int i = 1; i < 22; i++) {
                if ((i & pow) != 0) {
                    temp[j] = temp[j] + "1";
                } else {
                    temp[j] = temp[j] + "0";
                }
            }
            pow *= 2;
        }

        pow = 1;
        int j = 0;
        for (int i = 1; i < 22; i++) {
            if (i == pow) {
                result.append("0");
                pow *= 2;
            } else {
                result.append(rawStr.charAt(j));
                j++;
            }
        }
        for (j = 0; j < 5; j++) {
            boolean currentR = false;
            for (int i = 0; i < 21; i++) {
                if (temp[j].charAt(i) == '1' && result.charAt(i) == '1') {
                    currentR = !currentR;
                }
            }
            if (currentR) {
                r.append("1");
            } else {
                r.append("0");
            }
        }
        pow = 2;
        j = 1;
        result.deleteCharAt(0);
        result.insert(0, r.charAt(0));
        for (int i = 1; i < 21; i++) {
            if (i == pow) {
                result.deleteCharAt(i - 1);
                result.insert(i - 1, r.charAt(j));
                j++;
                pow *= 2;
            }
        }
        if (error != -1) {
            int errorIndex = error + 1;
            if (result.charAt(errorIndex) == '1') {
                result.deleteCharAt(errorIndex - 1);
                result.insert(errorIndex - 1, '0');
            } else {
                result.deleteCharAt(errorIndex - 1);
                result.insert(errorIndex - 1, '1');
            }
        }
        finalString.append(result);
        return fromBinary(finalString.toString());
    }


    /**
     * decoding Hemming package
     * @param hem - package with Hemming code
     * @return - the right package
     */
    public static byte[] hemingDecode(byte[] hem) {
        String raw = toBinary(hem);
        StringBuilder finalRes = new StringBuilder();
        StringBuilder s = new StringBuilder();
        String[] temp = new String[5];
        StringBuilder input;
        input = new StringBuilder(raw.substring(0, 21));

        int pow = 1;
        int j;
        for (j = 0; j < 5; j++) {
            temp[j] = "";
            for (int i = 1; i < 22; i++) {
                if ((i & pow) != 0) {
                    temp[j] = temp[j] + "1";
                } else {
                    temp[j] = temp[j] + "0";
                }
            }
            pow *= 2;
        }

        for (j = 0; j < 5; j++) {
            boolean currentS = false;
            for (int i = 0; i < 21; i++) {
                if (temp[j].charAt(i) == '1' && input.charAt(i) == '1') {
                    currentS = !currentS;
                }
            }
            if (currentS) {
                s.append("1");
            } else {
                s.append("0");
            }
        }
        if (!s.toString().equals("00000")) {
            pow = 1;
            int error = 0;
            for (int i = 0; i < s.length(); i++) {
                error += (s.charAt(i) - '0') * pow;
                pow *= 2;
            }

            if (input.charAt(error - 1) == '1') {
                input.deleteCharAt(error - 1);
                input.insert(error - 1, '0');
            } else {
                input.deleteCharAt(error - 1);
                input.insert(error - 1, '1');
            } /// 0011000100110010
        }
        pow = 1;
        while (pow < input.length()) {
            pow *= 2;
        }
        while (pow > 1) {
            pow /= 2;
            input.deleteCharAt(pow - 1);
        }
        finalRes.append(input);
        return fromBinary(finalRes.toString());
    }

    /**
     * incapsulating into package with CRC code
     * @param bytes - bytes for incapsulating
     * @return - the package with CRC code
     */
    public static byte[] crcCoding(byte[] bytes) {
        String raw = toBinary(bytes);
        raw = raw + getMod(raw + "000","1101");
        return fromBinary(raw);
    }

    /**
     * decoding from package with CRC code
     * @param bytes - raw bytes with CRC code
     * @return - bytes with fixed error
     */
    public static byte[] crcDecoding(byte[] bytes) {
        String s = toBinary(bytes);
        s = s.substring(0, s.length() - 5);
        s = repair(s);
        return fromBinary(s.substring(0, s.length() - 3));
    }

    /**
     * getting mod for generating CRC code
     * @param raw - raw bits for coding
     * @param div - divider of CRC
     * @return - the bits with mod
     */
    public static String getMod(String raw, String div) {
        StringBuilder sb = new StringBuilder(raw);
        for (int i = 0; i <= raw.length() - div.length(); i++) {
            String s = divide(sb.substring(i, i + div.length()),div);
            if (s != null) {
                sb.delete(i, i + div.length());
                sb.insert(i, s);
            }
        }
        return sb.substring(sb.length() - 3);
    }


    /**
     * xor command for 2 bit Strings if this is possible
     * @param s1 - the 1-st bit String
     * @param s2 - the 2-nd bit String
     * @return - the result of xor operation
     *         - or null, if this is impossible
     */
    public static String divide(String s1, String s2) {
        if (s1.length() == s2.length()) {
            if (s1.charAt(0) == '1') {
                return xorString(s1,s2);
            }
        }
        return null;
    }

    /**
     * cycle left shift of bits
     * @param raw - bits for shifting
     * @return - the result of shifting
     */
    public static String shiftLeft(String raw) {
        StringBuilder res = new StringBuilder(raw);
        char temp = res.charAt(0);
        res.deleteCharAt(0);
        res.append(temp);
        return res.toString();
    }

    /**
     * cycle right shift of bits
     * @param raw - bits for shifting
     * @return - the result of shifting
     */
    public static String shiftRight(String raw) {
        StringBuilder res = new StringBuilder(raw);
        char temp = res.charAt(res.length() - 1);
        res.deleteCharAt(res.length() - 1);
        res.insert(0,temp);
        return res.toString();
    }

    /**
     * repairing bits with CRC code
     * @param raw - raw bits
     * @return - bits after repairing
     */
    public static String repair(String raw) {
        String mod = getMod(raw,"1101");
        if (countOf1(mod) > 1) {
            return shiftRight(repair(shiftLeft(raw)));
        }
        else {
            return xorEndsString(mod,raw);
        }
    }

    /**
     * count of 1 in bit String
     * @param raw - raw string
     * @return - the count of '1' in String
     */
    public static int countOf1(String raw) {
        int count = 0;
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == '1') {
                count++;
            }
        }
        return count;
    }

    /**
     * xor operation between 2 bit Strings with equal length
     * @param s1 - 1-st string
     * @param s2 - 2-nd string
     * @return - the result of xor command
     */
    public static String xorString(String s1, String s2) {
        StringBuilder sb = new StringBuilder();
        int len = s1.length() > s2.length()? s2.length() : s1.length();
        for (int i = 0; i < len; i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                sb.append('1');
            }
            else {
                sb.append('0');
            }
        }
        return sb.toString();
    }

    /**
     * xor operation between 2 bit Strings with not equal length
     * if length are not equals - add '0' to start of less string
     * @param s1 - 1-st string
     * @param s2 - 2-nd string
     * @return - the result of xor command
     */
    public static String xorEndsString(String s1, String s2) {
        StringBuilder sb1 = new StringBuilder(s1);
        StringBuilder sb2 = new StringBuilder(s2);
        int delta;
        if (sb1.length() > sb2.length()) {
            delta = sb1.length() - sb2.length();
            for (int i = 0; i < delta; i++) {
                sb2.insert(0,'0');
            }
        }
        else {
            delta = sb2.length() - sb1.length();
            for (int i = 0; i < delta; i++) {
                sb1.insert(0,'0');
            }
        }
        return xorString(sb1.toString(), sb2.toString());
    }
}