package com;

public interface SerialInterface {

    boolean write(byte[] bytes, String address, String source);

    byte[] read(int bytesCount);

    boolean open();

    boolean close();

    void setParams(int baudRate, int dataBits, int stopBits, int parity);
}
