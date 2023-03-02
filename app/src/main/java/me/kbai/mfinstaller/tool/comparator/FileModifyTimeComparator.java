package me.kbai.mfinstaller.tool.comparator;

import java.io.File;

/**
 * @author sean
 */
public class FileModifyTimeComparator implements IComparator<File> {
    FileModifyTimeComparator() {
    }

    @Override
    public int compare(File f1, File f2, boolean asc) {

        if (f1.exists() & f2.exists()) {
            long l1 = f1.lastModified();
            long l2 = f2.lastModified();

            if (l1 > l2) {
                return asc ? 1 : -1;
            } else if (l1 < l2) {
                return asc ? -1 : 1;
            }
        }
        return 0;
    }
}
