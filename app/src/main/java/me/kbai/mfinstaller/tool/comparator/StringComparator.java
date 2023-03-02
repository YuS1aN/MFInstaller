package me.kbai.mfinstaller.tool.comparator;

import java.util.regex.Pattern;

/**
 * @author sean
 */
public class StringComparator implements IComparator<String> {
    private static final Pattern CHINESE_CHARACTER_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private final ChineseComparator mChineseComparator;

    private static boolean isChineseCharacter(char c) {
        return CHINESE_CHARACTER_PATTERN.matcher(String.valueOf(c)).find();
    }

    StringComparator() {
        mChineseComparator = ComparatorManager.getInstance().getChineseComparator();
    }

    @Override
    public int compare(String s1, String s2, boolean asc) {
        if (s1 == null || s2 == null) {
            return 0;
        }
        int minLength = Math.min(s1.length(), s2.length());
        for (int i = 0; i < minLength; i++) {
            char c1 = s1.charAt(i);
            char c2 = s2.charAt(i);
            boolean c1IsChinese = isChineseCharacter(c1);
            boolean c2IsChinese = isChineseCharacter(c2);
            if (c1IsChinese & c2IsChinese) {
                int result = mChineseComparator.compare(c1, c2, asc);
                if (result != 0) {
                    return result;
                }
            } else if (!(c1IsChinese | c2IsChinese)) {
                int result = mChineseComparator.compare(c1, c2, asc);
                if (result != 0) {
                    return result;
                }
            } else {
                return (asc ? c1IsChinese : c2IsChinese) ? 1 : -1;
            }
        }
        return 0;
    }
}
