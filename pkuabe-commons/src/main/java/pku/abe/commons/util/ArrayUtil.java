package pku.abe.commons.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

public class ArrayUtil {

    private ArrayUtil() {}

    public static Long[] toLongArr(String[] strArr) {
        Long[] longArr = new Long[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            longArr[i] = Long.parseLong(strArr[i]);
        }
        return longArr;
    }

    public static Long[] toLongArr(long[] longArr) {
        Long[] rs = new Long[longArr.length];
        for (int i = 0; i < longArr.length; i++) {
            rs[i] = Long.valueOf(longArr[i]);
        }
        return rs;
    }

    public static long[] toLongArr(Long[] longArr) {
        long[] rs = new long[longArr.length];
        for (int i = 0; i < longArr.length; i++) {
            rs[i] = Long.valueOf(longArr[i]);
        }
        return rs;
    }


    public static long[] toRawLongArr(String[] strArr) {
        long[] longArr = new long[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            longArr[i] = Long.parseLong(strArr[i]);
        }
        return longArr;
    }

    public static int[] toRawIntArr(String strArr[]) {
        int[] intArr = new int[strArr.length];
        for (int i = 0; i < strArr.length; i++) {
            intArr[i] = Integer.parseInt(strArr[i]);
        }
        return intArr;
    }

    public static long[] toLongArr(Collection<Long> ids) {
        if (ids == null || ids.size() == 0) {
            return new long[0];
        }
        long[] idsArr = new long[ids.size()];
        int idx = 0;
        for (long id : ids) {
            idsArr[idx++] = id;
        }
        return idsArr;
    }

    public static int[] toIntArr(Collection<Integer> ids) {
        if (ids == null || ids.size() == 0) {
            return new int[0];
        }
        int[] idsArr = new int[ids.size()];
        int idx = 0;
        for (int id : ids) {
            idsArr[idx++] = id;
        }
        return idsArr;
    }

    public static String longArrToPrintableString(long[] longArr) {
        StringBuilder value = new StringBuilder();
        value.append("[");
        if (null != longArr && longArr.length > 0) {
            for (long l : longArr) {
                value.append(l).append(",");
            }
            value.setLength(value.length() - 1);
        }
        value.append("]");
        return value.toString();
    }

    public static String stringArrToPrintableString(String[] objectArr) {
        StringBuilder value = new StringBuilder();
        value.append("[");
        if (null != objectArr && objectArr.length > 0) {
            for (String s : objectArr) {
                value.append(s).append(",");
            }
            value.setLength(value.length() - 1);
        }
        value.append("]");
        return value.toString();
    }

    public static String arrayToString(Object[] arrs) {
        return arrayToString(arrs, ",");
    }

    public static String arrayToString(Object[] arrs, String split) {
        if (arrs == null) {
            return "";
        }
        int iMax = arrs.length - 1;
        if (iMax == -1) {
            return "";
        }

        StringBuilder b = new StringBuilder();
        for (int i = 0;; i++) {
            b.append(String.valueOf(arrs[i]));
            if (i == iMax) return b.toString();
            b.append(split);
        }
    }

    public static String toSimpleString(long[] arrs) {
        if (arrs == null) {
            return "";
        }
        int iMax = arrs.length - 1;
        if (iMax == -1) {
            return "";
        }

        StringBuilder b = new StringBuilder();
        for (int i = 0;; i++) {
            b.append(arrs[i]);
            if (i == iMax) return b.toString();
            b.append(",");
        }
    }

    /**
     *
     * @param strs
     * @return
     */
    public static String[] trimDuplicates(String[] strs) {
        if (strs == null || strs.length == 0) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        }
        Set<String> set = new LinkedHashSet<String>(strs.length);
        for (String str : strs) {
            if (str != null) {
                set.add(str);
            }
        }
        return set.toArray(new String[] {});
    }

    public static void reverse(long[][] b) {
        int left = 0; // index of leftmost element
        int right = b.length - 1; // index of rightmost element

        while (left < right) {
            // exchange the left and right elements
            long[] temp = b[left];
            b[left] = b[right];
            b[right] = temp;

            // move the bounds toward the center
            left++;
            right--;
        }
    }

    /**
     * @param original
     * @param newLength
     * @return
     */
    public static long[] reverseCopy(long[] original, int newLength) {
        // System.out.println("temp length:" + temp.length);
        long[] result = new long[newLength];
        int originalLimit = original.length - newLength;
        for (int i = original.length - 1, resultIdx = 0; i >= originalLimit; i--) {
            result[resultIdx++] = original[i];
        }
        // System.out.println("result length:" + result.length + ", result" +
        // Arrays.toString(result));
        return result;
    }

    public static long[] removeAll(long[] sourceArray, long[] removeArray) {
        int sourceLength = sourceArray.length;
        int removeLength = removeArray.length;
        if (sourceLength == 0 || removeLength == 0) {
            return sourceArray;
        }

        /*
         * if removeLength > 2 use removeSet
         */
        long[] temp = new long[sourceLength];
        int i = 0;
        if (removeLength > 2) {
            Set<Long> removeSet = new HashSet<Long>(removeLength);
            for (long remove : removeArray) {
                removeSet.add((Long) remove);
            }
            for (long source : sourceArray) {
                if (!removeSet.contains((Long) source)) { // not contains so push
                    temp[i++] = source;
                }
            }
        } else {
            for (long source : sourceArray) {
                if (!arrayContains(removeArray, source)) { // not contains so push
                    temp[i++] = source;
                }
            }
        }

        if (i < temp.length) {
            return Arrays.copyOf(temp, i);
        } else {
            return temp;
        }
    }

    private static boolean arrayContains(long[] arrs, long value) {
        for (long arr : arrs) {
            if (arr == value) {
                return true;
            }
        }
        return false;
    }

    public static <T> List<T> removeAll(List<T> sourceList, Collection<T> removeList) {
        List<T> leftList = new ArrayList<T>(sourceList.size() - removeList.size());
        Set<T> removeSet = new HashSet<T>(removeList.size());
        removeSet.addAll(removeList);
        for (T item : sourceList) {
            if (!removeSet.contains(item)) {
                leftList.add(item);
            }
        }
        return leftList;
    }

    public static long[] removeAll(long[] sourceArray, Set<Long> removeSet) {
        int sourceLength = sourceArray.length;
        int removeLength = removeSet.size();
        if (sourceLength == 0 || removeLength == 0) {
            return sourceArray;
        }

        long[] temp = new long[sourceLength];
        int i = 0;
        for (long source : sourceArray) {
            if (!removeSet.contains((Long) source)) { // not contains so push
                temp[i++] = source;
            }
        }

        if (i < temp.length) {
            return Arrays.copyOf(temp, i);
        } else {
            return temp;
        }
    }

    /**
     * result will be order by asc
     *
     * @param left
     * @param right
     * @return
     */
    public static long[] sort(long left[], long right[]) {
        long[] result = addTo(left, right);
        Arrays.sort(result);
        return result;
    }

    /**
     * 姹備氦闆嗚�屼笉鏀瑰彉椤哄簭
     *
     * @param arr1 浠ユ涓烘帓搴忎緷鎹�
     * @param arr2
     * @return
     */
    public static long[] intersectionOrder(long[] arr1, long[] arr2) {
        // 鑾峰彇浜ら泦鑰屼笉鎵撲贡arr1鐨勯『搴�
        HashSet<Long> tempSet = new HashSet<Long>();
        for (int i = 0; i < arr2.length; i++) {
            tempSet.add(arr2[i]);
        }
        long[] tmpResult = new long[arr1.length];
        int count = 0;
        for (int i = 0; i < arr1.length; i++) {
            if (tempSet.contains(arr1[i])) {
                tmpResult[count] = arr1[i];
                count++;
            }
        }
        // 鎴彇鎺夊浣欑殑
        return Arrays.copyOf(tmpResult, count);
    }

    public static long[] addTo(long[] left, long[] right) {
        if (left == null) {
            return ArrayUtils.clone(right);
        } else if (right == null) {
            return ArrayUtils.clone(left);
        }

        long[] result = new long[left.length + right.length];

        int pos = 0;
        System.arraycopy(left, 0, result, pos, left.length);
        pos += left.length;
        System.arraycopy(right, 0, result, pos, right.length);
        return result;
    }

    /**
     * @param arr
     * @param id
     * @return
     */
    public static long[] addTo(long[] arr, long id) {
        return addTo(arr, new long[] {id});
    }

    public static long[] addTo(long[] arr, long id, int limit) {
        if (ArrayUtils.contains(arr, id)) {
            return arr;
        }
        arr = addTo(arr, id);
        return getLimited(arr, limit);
    }

    public static long[] getLimited(long[] arr, int limit) {
        if (arr == null) {
            return arr;
        }

        if (arr.length > limit) {
            long[] result = new long[limit];
            System.arraycopy(arr, arr.length - limit, result, 0, limit);
            return result;
        } else {
            return arr;
        }
    }

    public static List<Long> arrayToList(long[] arr) {
        if (ArrayUtils.isEmpty(arr)) {
            return new ArrayList<Long>(0);
        }
        List<Long> result = new ArrayList<Long>(arr.length);
        for (long i : arr) {
            result.add(i);
        }
        return result;
    }

    public static Set<Long> arrayToSet(long[] arr) {
        if (ArrayUtils.isEmpty(arr)) {
            return new HashSet<Long>(0);
        }
        Set<Long> result = new HashSet<Long>(arr.length);
        for (long i : arr) {
            result.add(i);
        }
        return result;
    }

    /**
     * 顺序在List中查找指定对象position
     *
     * @param list
     * @param target
     * @return
     */
    public static <T> int indexOf(List<? extends Comparable<? super T>> list, T target) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).compareTo(target) == 0) {
                return i;
            }
        }
        return -1;
    }

    public static int binarySearch(long[] ids, long id, boolean asc) {
        int low = 0;
        int high = ids.length - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            long midVal = ids[mid];
            int cmp = (asc ? 1 : -1) * (midVal > id ? 1 : (midVal == id ? 0 : -1));
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1); // key not found
    }

    public static <T> int binarySearchForInsert(List<? extends Comparable<? super T>> list, T key) {
        if (list == null) {
            return -1;
        } else if (list.size() == 0) {
            return 0;
        }

        int position = Collections.binarySearch(list, key);
        return position >= 0 ? position : Math.abs(position + 1);
    }

    public static <T extends Comparable<T>> int binarySearchForInsert(T[] array, T key) {
        int position = Arrays.binarySearch(array, key, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return o1.compareTo(o2);
            }
        });
        return position >= 0 ? position : Math.abs(position + 1);
    }

    public static int binarySearchForInsert(long[] array, long target) {
        int position = Arrays.binarySearch(array, target);
        return position >= 0 ? position : Math.abs(position + 1);
    }

    public static String strArrToString(String[] strArr) {
        String result = "";
        if(strArr == null || strArr.length == 0) {
            return result;
        }
        for(String str: strArr) {
            result = result + str + ",";
        }
        if(result.length() > 0) {
            return result.substring(0, result.length() - 1);
        }
        return result;
    }
}

