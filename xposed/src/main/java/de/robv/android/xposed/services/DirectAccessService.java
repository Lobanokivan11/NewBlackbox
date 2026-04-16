package de.robv.android.xposed.services;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class DirectAccessService extends BaseService {
    
    @Override
    public boolean checkFileAccess(String paramString, int paramInt) {
        File file = new File(paramString);
        if (paramInt == 0) return file.exists();
        if ((paramInt & 0x4) != 0 && !file.canRead()) return false;
        if ((paramInt & 0x2) != 0 && !file.canWrite()) return false;
        if ((paramInt & 0x1) != 0 && !file.canExecute()) return false;
        return file.exists();
    }

    @Override
    public boolean checkFileExists(String paramString) {
        return (new File(paramString)).exists();
    }

    @Override
    public boolean hasDirectFileAccess() {
        return true;
    }

    @Override
    public FileResult getFileInputStream(String paramString, long paramLong1, long paramLong2) throws IOException {
        File file = new File(paramString);
        long l1 = file.length();
        long l2 = file.lastModified();
        if (paramLong1 == l1 && paramLong2 == l2) {
            return new FileResult(l1, l2);
        }
        return new FileResult(new BufferedInputStream(new FileInputStream(file), 16384), l1, l2);
    }

    @Override
    public InputStream getFileInputStream(String paramString) throws IOException {
        return new BufferedInputStream(new FileInputStream(paramString), 16384);
    }

    @Override
    public FileResult statFile(String paramString) throws IOException {
        File file = new File(paramString);
        if (!file.exists()) throw new java.io.FileNotFoundException(paramString);
        return new FileResult(file.length(), file.lastModified());
    }

    @Override
    public byte[] readFile(String paramString) throws IOException {
        File file = new File(paramString);
        byte[] arrayOfByte = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            fis.read(arrayOfByte);
        }
        return arrayOfByte;
    }

    @Override
    public FileResult readFile(String paramString, long paramLong1, long paramLong2) throws IOException {
        File file = new File(paramString);
        long l1 = file.length();
        long l2 = file.lastModified();
        if (paramLong1 == l1 && paramLong2 == l2) {
            return new FileResult(l1, l2);
        }
        return new FileResult(readFile(paramString), l1, l2);
    }

    @Override
    public FileResult readFile(String paramString, int offset, int length, long previousSize, long previousTime) throws IOException {
        File file = new File(paramString);
        long currentSize = file.length();
        long currentTime = file.lastModified();

        if (previousSize == currentSize && previousTime == currentTime) {
            return new FileResult(currentSize, currentTime);
        }

        if (offset <= 0 && length <= 0) {
            return new FileResult(readFile(paramString), currentSize, currentTime);
        }

        if (offset >= currentSize) {
            throw new IllegalArgumentException("Offset " + offset + " is out of range for " + paramString);
        }

        int readLength = length;
        if (readLength <= 0 || (offset + readLength) > currentSize) {
            readLength = (int) (currentSize - offset);
        }

        byte[] arrayOfByte = new byte[readLength];
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            fileInputStream.skip(offset);
            fileInputStream.read(arrayOfByte);
        }
        return new FileResult(arrayOfByte, currentSize, currentTime);
    }
}
