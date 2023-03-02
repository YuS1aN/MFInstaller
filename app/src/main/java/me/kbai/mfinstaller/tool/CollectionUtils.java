package me.kbai.mfinstaller.tool;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * @author Sean on 2019/11/13.
 */
public class CollectionUtils {
    /**
     * 查找
     *
     * @param list   source
     * @param filter filter
     * @param <T>    type
     * @return object
     */
    public static <T> T find(Collection<T> list, Filter<T> filter) {
        for (T object : list) {
            if (filter.accept(object)) {
                return object;
            }
        }
        return null;
    }

    /**
     * 筛选
     *
     * @param list   source
     * @param filter filter
     * @param <T>    type
     * @return filtered list
     */
    public static <T> List<T> filter(Collection<T> list, Filter<T> filter) {
        List<T> result = new ArrayList<>();
        for (T object : list) {
            if (filter.accept(object)) {
                result.add(object);
            }
        }
        return result;
    }

    /**
     * 从 B 中选择一个字段
     *
     * @param list          source
     * @param fieldProvider field provider
     * @param distinct      去重
     * @param <B>           Bean
     * @param <F>           Field
     * @return field list
     */
    public static <B, F> Collection<F> select(Collection<B> list, FieldProvider<B, F> fieldProvider, boolean distinct) {
        Collection<F> result;
        if (distinct) {
            result = new HashSet<>();
        } else {
            result = new ArrayList<>();
        }
        for (B item : list) {
            result.add(fieldProvider.provide(item));
        }
        return result;
    }

    /**
     * 根据某字段分组
     *
     * @param list          source
     * @param fieldProvider field provider
     * @param <B>           B
     * @param <F>           F
     * @return grouped
     */
    public static <B, F> List<List<B>> groupBy(List<B> list, FieldProvider<B, F> fieldProvider) {
        if (list == null || list.size() == 0) {
            return null;
        }
        HashSet<F> fieldValueSet = new HashSet<>();
        for (B item : list) {
            fieldValueSet.add(fieldProvider.provide(item));
        }
        List<List<B>> result = new ArrayList<>();
        for (F field : fieldValueSet) {
            List<B> group = new ArrayList<>();
            for (B item : list) {
                F fieldItem = fieldProvider.provide(item);
                boolean equals;
                if (fieldItem != null) {
                    equals = fieldItem.equals(field);
                } else {
                    equals = field == null;
                }
                if (equals) {
                    group.add(item);
                }
            }
            result.add(group);
        }
        return result;
    }

    /**
     * 通过指定规则删除元素
     *
     * @param list   source
     * @param filter filter
     * @param <T>    T
     */
    public static <T> void removeIf(Collection<T> list, Filter<T> filter) {
        Iterator<T> iterator = list.iterator();
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (filter.accept(item)) {
                iterator.remove();
            }
        }
    }

    /**
     * 计算 B list 中 某数字字段的和
     *
     * @param list     source
     * @param provider field provider
     * @param <B>      B
     * @param <N>      Number
     * @return sum
     */
    public static <B, N extends Number> BigDecimal sum(Collection<B> list, FieldProvider<B, N> provider) {
        BigDecimal result = BigDecimal.ZERO;
        boolean highPrecision = list.iterator().next() instanceof BigDecimal;
        for (B item : list) {
            N n = provider.provide(item);
            if (n != null) {
                if (highPrecision) {
                    result = result.add((BigDecimal) n);
                } else {
                    result = result.add(BigDecimal.valueOf(n.doubleValue()));
                }
            }
        }
        return result;
    }

    /**
     * 平均值
     */
    public static <B, N extends Number> BigDecimal average(Collection<B> list, FieldProvider<B, N> provider) {
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal size = BigDecimal.ZERO;
        for (B item : list) {
            N n = provider.provide(item);
            if (n != null) {
                sum = sum.add(BigDecimal.valueOf(n.doubleValue()));
                size = size.add(BigDecimal.valueOf(1));
            }
        }
        if (size.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return sum.divide(size, 6, RoundingMode.HALF_UP);
    }

    /**
     * 类型转换
     *
     * @param list      source
     * @param converter converter
     * @param <T>       type
     * @param <R>       result
     * @return result list
     */
    public static <T, R> List<R> convert(Collection<T> list, Converter<T, R> converter) {
        List<R> result = new ArrayList<>();
        for (T item : list) {
            R converted = converter.convert(item);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T, R> R[] convert(T[] array, Class<R> clazz, Converter<T, R> converter) {
        if (array == null) {
            return null;
        }
        R[] result = (R[]) Array.newInstance(clazz, array.length);
        for (int i = 0; i < array.length; i++) {
            result[i] = converter.convert(array[i]);
        }
        return result;
    }

    /**
     * 选择器
     *
     * @param <T> Type
     */
    public interface Filter<T> {
        /**
         * 筛选是否通过
         *
         * @param item obj
         * @return is accept?
         */
        boolean accept(T item);
    }

    /**
     * 转换器
     *
     * @param <T> input type
     * @param <R> result
     */
    public interface Converter<T, R> {
        /**
         * 转换类型
         *
         * @param item obj
         * @return converted
         */
        R convert(T item);
    }

    /**
     * 字段选择器
     *
     * @param <B> Bean
     * @param <F> Field
     */
    public interface FieldProvider<B, F> {
        /**
         * 提供 Bean 中一个字段
         *
         * @param item B
         * @return F
         */
        F provide(B item);
    }
}