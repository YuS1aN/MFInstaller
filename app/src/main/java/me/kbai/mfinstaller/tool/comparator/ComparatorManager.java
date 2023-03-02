package me.kbai.mfinstaller.tool.comparator;

/**
 * @author sean
 */
public class ComparatorManager {
    private volatile ChineseComparator mChineseComparator;
    private volatile AsciiComparator mAsciiComparator;
    private volatile StringComparator mStringComparator;
    private volatile FileModifyTimeComparator mFileModifyTimeComparator;

    private ComparatorManager() {
    }

    private static class ComparatorManagerHolder {
        private static final ComparatorManager INSTANCE = new ComparatorManager();
    }

    public static ComparatorManager getInstance() {
        return ComparatorManagerHolder.INSTANCE;
    }

    public ChineseComparator getChineseComparator() {
        if (mChineseComparator == null) {
            synchronized (ComparatorManager.class) {
                if (mChineseComparator == null) {
                    mChineseComparator = new ChineseComparator();
                }
            }
        }
        return mChineseComparator;
    }

    public AsciiComparator getAsciiComparator() {
        if (mAsciiComparator == null) {
            synchronized (ComparatorManager.class) {
                if (mAsciiComparator == null) {
                    mAsciiComparator = new AsciiComparator();
                }
            }
        }
        return mAsciiComparator;
    }

    public StringComparator getStringComparator() {
        if (mStringComparator == null) {
            synchronized (ComparatorManager.class) {
                if (mStringComparator == null) {
                    mStringComparator = new StringComparator();
                }
            }
        }
        return mStringComparator;
    }

    public FileModifyTimeComparator getFileModifyTimeComparator() {
        if (mFileModifyTimeComparator == null) {
            synchronized (ComparatorManager.class) {
                if (mFileModifyTimeComparator == null) {
                    mFileModifyTimeComparator = new FileModifyTimeComparator();
                }
            }
        }
        return mFileModifyTimeComparator;
    }
}
