package me.kbai.mfinstaller.tool.comparator;

/**
 * @author sean
 */
public interface IComparator<T> {

    int compare(T t1, T t2, boolean asc);
}