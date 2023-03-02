package me.kbai.mfinstaller.tool;

import android.content.Context;
import android.net.Uri;
import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * @author Sean on 2021/12/28
 */
public class ObbAccessUtils {
    public static final String OBB_TREE_URI = "content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fobb";
    /**
     * PATH_DOCUMENT    primary:{RELATIVE PATH}
     */
    public static DocumentFile getSingleDocumentFile(Context context, String path) {
        String contentUriPath = OBB_TREE_URI + "/document/primary%3A"
                + path.replace("/storage/emulated/0/", "").replace("/", "%2F");
        return DocumentFile.fromSingleUri(context, Uri.parse(contentUriPath));
    }

    /**
     * PATH_TREE:    primary:Android/obb
     */
    public static DocumentFile getTreeDocumentFile(Context context) {
        return DocumentFile.fromTreeUri(context, Uri.parse(OBB_TREE_URI));
    }

    public static boolean copyBySAF(Context context, String src, String dest) {
        File srcFile = new File(src);
        File destFile = new File(dest);
        if (!srcFile.exists()) {
            return false;
        }
        DocumentFile destDoc = getSingleDocumentFile(context, dest);
        if (destDoc.exists()) {
            boolean deleted = destDoc.delete();
            if (!deleted) {
                return false;
            }
        }
        try {
            File parent = new File(dest).getParentFile();
            if (parent == null) return false;
            // Android/obb/package
            DocumentFile packageDoc = getSingleDocumentFile(context, parent.getAbsolutePath());
            if (!packageDoc.exists()) {
                // Android/obb
                DocumentFile obbDoc = getTreeDocumentFile(context);
                if (!obbDoc.exists()) return false;
                obbDoc.createDirectory(parent.getName());
            }
            DocumentsContract.createDocument(context.getContentResolver(), packageDoc.getUri(), "", destFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        try (FileChannel srcChannel = new FileInputStream(src).getChannel();
             FileChannel dstChannel = ((FileOutputStream) context
                     .getContentResolver()
                     .openOutputStream(destDoc.getUri()))
                     .getChannel()
        ) {
            srcChannel.transferTo(0, srcChannel.size(), dstChannel);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
