package me.kbai.mfinstaller.tool.comparator;

import java.text.Collator;
import java.util.Locale;

/**
 * @author sean
 */
public class ChineseComparator implements IComparator<Character> {
    ChineseComparator() {
    }

    @Override
    public int compare(Character c1, Character c2, boolean asc) {
        int result = Collator.getInstance(Locale.CHINA).compare(String.valueOf(c1), String.valueOf(c2));
        if (result == 0) {
            return 0;
        } else {
            return asc ? result : -result;
        }
    }
}
