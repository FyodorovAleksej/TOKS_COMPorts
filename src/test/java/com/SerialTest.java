package com;
import jssc.SerialPort;
import jssc.SerialPortList;
import junit.framework.TestCase;

public class SerialTest extends TestCase {
    static Serial serialPort1 = null;
    static Serial serialPort2 = null;
    static boolean start;
    public void setUp() throws Exception {
        String[] names = SerialPortList.getPortNames();
        if (names.length >= 2) {
            start = true;
            serialPort1 = new Serial(names[0]);
            serialPort2 = new Serial(names[1]);
        }
        else {
            start = false;
        }

        if (start) {
            if (!serialPort1.isOpen()) {
                serialPort1.open();
            }
            if (!serialPort2.isOpen()) {
                serialPort2.open();
            }
            serialPort1.setParams(SerialPort.BAUDRATE_9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            serialPort1.setFlowControl(SerialPort.FLOWCONTROL_RTSCTS_IN |
                    SerialPort.FLOWCONTROL_RTSCTS_OUT);

            serialPort2.setParams(SerialPort.BAUDRATE_9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);

            serialPort2.setFlowControl(SerialPort.FLOWCONTROL_RTSCTS_IN |
                    SerialPort.FLOWCONTROL_RTSCTS_OUT);
        }
        super.setUp();
    }

    public void tearDown() throws Exception {
        serialPort1.close();
        serialPort2.close();
    }

    public void testStart() throws Exception {
        assertTrue(start);
    }

    public void testWrite() throws Exception {
        if (start) {
            String test = "Test String\nTest String2";
            serialPort1.write(test.getBytes(), "COM2", "COM1");
            assertEquals(new String(serialPort2.read(test.length())), test);
        }
    }

    public void testRead() throws Exception {
        if (start) {
            String test = "Test String\nTest String2";
            serialPort1.write(test.getBytes(), "COM2", "COM2");
            serialPort1.write(test.getBytes(), "COM2", "COM1");
            assertEquals(new String(serialPort2.read(2 * test.length())), test + test);
        }
    }

    public void testClose() throws Exception {
        if (start) {
            assertEquals(serialPort1.close(), true);
            assertEquals(serialPort1.close(), false);
        }
    }

    public void testIsOpen() throws Exception {
        if (start) {
            assertEquals(serialPort1.isOpen(), true);
            serialPort1.close();
            assertEquals(serialPort1.isOpen(), false);
        }
    }

}