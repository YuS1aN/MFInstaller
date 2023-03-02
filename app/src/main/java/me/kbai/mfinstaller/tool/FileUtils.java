package me.kbai.mfinstaller.tool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * @author Sean
 */
public class FileUtils {

    public static boolean copy(String src, String dest) {
        File srcFile = new File(src);
        File destFile = new File(dest);
        if (!srcFile.exists()) {
            return false;
        }
        if (destFile.exists()) {
            boolean deleted = destFile.delete();
            if (!deleted) {
                return false;
            }
        }
        try {
            File parent = destFile.getParentFile();
            if (parent == null) {
                return false;
            }
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
            boolean created = destFile.createNewFile();
            if (!created) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        FileChannel srcChannel;
        FileChannel dstChannel;

        try {
            srcChannel = new FileInputStream(src).getChannel();
            dstChannel = new FileOutputStream(destFile).getChannel();
            srcChannel.transferTo(0, srcChannel.size(), dstChannel);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            if (srcChannel.isOpen()) {
                srcChannel.close();
            }
            if (dstChannel.isOpen()) {
                dstChannel.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Delete a file or directory
     *
     * @param file file or directory
     * @return Whether the file was successfully deleted
     */
    public static boolean delete(File file) {
        if (!file.exists()) {
            return false;
        }
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            if (listFiles == null) {
                return false;
            }
            for (File child : listFiles) {
                if (child.isDirectory()) {
                    if (!delete(child)) {
                        return false;
                    }
                } else {
                    if (!child.delete()) {
                        return false;
                    }
                }
            }
        }
        return file.delete();
    }

    public static byte[] read(File file) {
        RandomAccessFile randomFile = null;
        FileChannel fileChannel = null;
        try {
            randomFile = new RandomAccessFile(file, "r");
            fileChannel = randomFile.getChannel();
            long fileSize = fileChannel.size();
            MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            byte[] result = new byte[(int) fileSize];
            mappedByteBuffer.get(result, 0, mappedByteBuffer.remaining());
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (randomFile != null) {
                try {
                    randomFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static String readString(File file) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            long fileSize = randomAccessFile.length();
            byte[] result = new byte[(int) fileSize];
            randomAccessFile.read(result, 0, (int) fileSize);
            return new String(result, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean write(File dest, String content) {
        if (dest.exists()) {
            boolean deleted = dest.delete();
            if (!deleted) {
                return false;
            }
        }
        try {
            File parent = dest.getParentFile();
            if (parent == null) {
                return false;
            }
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
            boolean created = dest.createNewFile();
            if (!created) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try (FileChannel channel = new FileOutputStream(dest).getChannel()) {
            ByteBuffer buffer = StandardCharsets.UTF_8.encode(content);
            channel.write(buffer);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getFileName(String filePath) {
        int index = filePath.lastIndexOf(File.separator);
        return index == -1 ? filePath : filePath.substring(index + 1);
    }
}
