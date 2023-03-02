package me.kbai.mfinstaller.tool.comparator;

/**
 * @author sean
 */
public class AsciiComparator implements IComparator<Character> {

    AsciiComparator() {
    }

    @Override
    public int compare(Character c1, Character c2, boolean asc) {
        char upperC1 = Character.toUpperCase(c1);
        char upperC2 = Character.toUpperCase(c2);

        //对比字母忽略大小写
        if (upperC1 > upperC2) {
            return asc ? 1 : -1;
        } else if (upperC1 < upperC2) {
            return asc ? -1 : 1;
            //相同字母大写在前
        } else if (c1 > c2) {
            return 1;
        }
        return 0;
    }
}
