package me.kbai.mfinstaller.tool;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author sean on 2020/8/28.
 */
public class Zip4JUtils {

    public static void unzip(File file, String dest) throws ZipException {
        unzip(file, dest, null, null);
    }

    public static void unzip(File file, String dest, ZipNameFilter mapper) throws ZipException {
        unzip(file, dest, null, mapper);
    }

    public static void unzip(File file, String dest, String password, ZipNameFilter mapper) throws ZipException {
        ZipFile zipFile = openZipFile(file, password);
        File destDir = new File(dest);
        if (destDir.isDirectory() && !destDir.exists()) {
            if (!destDir.mkdir()) {
                throw new ZipException("failed to create directory.");
            }
        }
        if (mapper == null) {
            zipFile.extractAll(dest);
        } else {
            List<FileHeader> headerList = zipFile.getFileHeaders();
            for (FileHeader header : headerList) {
                if (mapper.accept(header.getFileName())) {
                    zipFile.extractFile(header, dest);
                }
            }
        }
    }

    public static ZipFile openZipFile(File file) throws ZipException {
        return openZipFile(file, null, Charset.forName("GBK"));
    }

    public static ZipFile openZipFile(File file, String password) throws ZipException {
        return openZipFile(file, password, Charset.forName("GBK"));
    }

    public static ZipFile openZipFile(File file, String password, Charset charset) throws ZipException {
        ZipFile zipFile = new ZipFile(file);
        zipFile.setCharset(charset);
        if (!zipFile.isValidZipFile()) {
            throw new ZipException("valid zip file.");
        }
        if (zipFile.isEncrypted()) {
            if (password == null) {
                throw new ZipException("password is required.");
            }
            zipFile.setPassword(password.toCharArray());
        }
        return zipFile;
    }

    public interface ZipNameFilter {
        boolean accept(String name);
    }
}
