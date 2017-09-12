package com;

public interface SerialInterface {

    boolean write(byte[] bytes);

    byte[] read(int bytesCount);

    boolean open();

    boolean close();

    void setParams(int baudRate, int dataBits, int stopBits, int parity);
}
