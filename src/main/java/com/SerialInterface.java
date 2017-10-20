package com;

public interface SerialInterface {

    boolean write(byte[] bytes, boolean hemming);

    byte[] read(int bytesCount, boolean hemming);

    boolean open();

    boolean close();

    void setParams(int baudRate, int dataBits, int stopBits, int parity);
}
