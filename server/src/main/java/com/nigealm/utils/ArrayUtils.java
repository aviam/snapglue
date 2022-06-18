package com.nigealm.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.PredicateUtils;

public final class ArrayUtils
{
	/** single instance of an empty Integer array - for performance */
	public static final Integer[] EMPTY_ARRAY_INTEGER = new Integer[0];
	private static final int TYPE_MIN = 0;
	private static final int TYPE_MAX = 1;

	public static class XYvalues
	{
		public XYvalues(double[] _x, double[] _y)
		{
			super();
			this.x = _x;
			this.y = _y;
		}

		public final double[] x;
		public final double[] y;
	}

	private ArrayUtils()
	{
		super();
	}

	/**
	 * If array1 is, for example, Double[] then result will be also of type
	 * Double[].
	 */
	public static <T> T[] getCommonElements(T[] array1, T[] array2)
	{
		Debug.assertSG(array1);
		Debug.assertSG(array2);

		List<T> l1 = new ArrayList<T>(Arrays.asList(array1));
		l1.retainAll(Arrays.asList(array2));
		return l1.toArray(createArray(array1, l1.size()));
	}

	public static boolean isAllElementsInstanceOf(Object[] a, Class<?> c)
	{
		Debug.assertSG(a, 0);
		Debug.assertSG(c, 1);
		return countElementsByType(Arrays.asList(a), c) == a.length;
	}

	public static int countElementsByType(Collection<?> a, Class<?> c)
	{
		Debug.assertSG(a, 0);
		Debug.assertSG(c, 1);
		return CollectionUtils.countMatches(a, PredicateUtils.instanceofPredicate(c));
	}

	/**
	 * Returns all elements that are contained in array1 but not in array2 or
	 * that are contained in array2 but not in array1 If array1 is, for example,
	 * Double[] then result will be also of type Double[].
	 */
	public static Object[] getUnCommonElements(Object[] array1, Object[] array2)
	{
		Debug.assertSG(array1);
		Debug.assertSG(array2);

		Object[] commonElements = getCommonElements(array1, array2);
		List<Object> l = new ArrayList<Object>(Arrays.asList(join(array1, array2)));
		l.removeAll(Arrays.asList(commonElements));
		Object[] d = (Object[]) java.lang.reflect.Array.newInstance(array1.getClass().getComponentType(), 0);
		return l.toArray(d);
	}

	/**
	 * @return if both arrays contain the same elements (disregarding order)
	 */
	public static boolean haveEqualElements(Object[] array1, Object[] array2)
	{
		if (array1 == null && array2 == null)
			return true;
		if (array1 == null || array2 == null)
			return false;
		if (array1.length != array2.length)
			return false;
		return getCommonElements(array1, array2).length == array1.length;
	}

	/**
	 * @return copy of <code>source</code>
	 */
	public static double[] copy(double[] source)
	{
		if (source == null || source.length == 0)
		{
			return source;
		}
		double[] d = new double[source.length];
		System.arraycopy(source, 0, d, 0, source.length);
		return d;
	}

	public static double[][] copy(double[][] source)
	{
		if (source == null || source.length == 0)
		{
			return source;
		}
		double[][] d = new double[source.length][];
		for (int i = 0; i < d.length; i++)
		{
			if (source[i] != null)
			{
				d[i] = new double[source[i].length];
				System.arraycopy(source[i], 0, d[i], 0, source[i].length);
			}
		}
		return d;
	}

	/**
	 * @return copy of <code>source</code>
	 */
	public static int[] copy(int[] source)
	{
		if (source == null || source.length == 0)
		{
			return source;
		}

		int[] d = new int[source.length];
		System.arraycopy(source, 0, d, 0, source.length);
		return d;
	}

	public static void copy(double[] source, Double[] dest)
	{
		int s = Math.min(source.length, dest.length);
		for (int j = 0; j < s; j++)
		{
			dest[j] = new Double(source[j]);
		}
	}

	/**
	 * Converts elements of <code>Double[]</code> without <code>null</code>s to
	 * <code>double[]</code>.
	 * 
	 * @param source array of <code>Double</code> elements, none of which is
	 *            <code>null</code>
	 * @param dest array of doubles
	 */
	public static void copy(Double[] source, double[] dest)
	{
		int s = Math.min(source.length, dest.length);
		for (int j = 0; j < s; j++)
		{
			dest[j] = source[j].doubleValue();
		}
	}

	public static <T1> T1[] copy(T1[] source)
	{
		if (source == null || source.length == 0)
			return source;
		Object r = Array.newInstance(source.getClass().getComponentType(), source.length);
		System.arraycopy(source, 0, r, 0, source.length);
		return (T1[]) r;
	}

	public static boolean[] copy(boolean[] source)
	{
		if (source == null || source.length == 0)
		{
			return source;
		}
		boolean[] d = new boolean[source.length];
		System.arraycopy(source, 0, d, 0, source.length);
		return d;
	}

	/**
	 * Return min. value of the arrays
	 */
	public static double getMin(double[][] data)
	{
		double min = Double.POSITIVE_INFINITY;
		for (int i = 0; i < data.length; i++)
		{
			double m = getMin(data[i]);
			if (m < min)
				min = m;
		}
		return min;
	}

	public static double getMin(List<double[]> data)
	{
		double min = Double.POSITIVE_INFINITY;
		for (int i = 0; i < data.size(); i++)
		{
			double m = getMin(data.get(i));
			if (m < min)
				min = m;
		}
		return min;
	}

	/**
	 * Return min. element of the array
	 */
	public static double getMin(double[] data)
	{
		return getMinMax(data, TYPE_MIN);
	}

	/**
	 * Return max. value of the arrays If all elements are NaN - return NaN.
	 */
	public static double getMax(double[][] data)
	{
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < data.length; i++)
		{
			double m = getMax(data[i]);
			if (m > max)
				max = m;
		}
		return max;
	}

	public static double getMax(List<double[]> data)
	{
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < data.size(); i++)
		{
			double m = getMax(data.get(i));
			if (m > max)
				max = m;
		}
		return max;
	}

	/**
	 * Return max. element of the array. If all elements are NaN - return NaN.
	 */
	public static double getMax(double[] data)
	{
		return getMinMax(data, TYPE_MAX);
	}

	private static double getMinMax(double[] data, int type)
	{
		Debug.assertSG(data.length > 0);
		boolean allNaN = true;
		int firstNumberIndex = 0;
		for (int i = 0; i < data.length; i++)
		{
			if (!Double.isNaN(data[i]))
			{
				allNaN = false;
				firstNumberIndex = i;
				break;
			}
		}
		if (allNaN)
		{
			return Double.NaN;
		}
		double m = data[firstNumberIndex];
		for (int i = (firstNumberIndex + 1); i < data.length; i++)
		{
			if (!Double.isNaN(data[i]))
			{
				if (type == TYPE_MIN && data[i] < m)
				{
					m = data[i];
				}
				if (type == TYPE_MAX && data[i] > m)
				{
					m = data[i];
				}
			}
		}
		return m;
	}

	public static int getMinIndex(double[] data)
	{
		Debug.assertSG(data.length > 0);
		int index = 0;
		for (int i = 1; i < data.length; i++)
		{
			if (data[index] > data[i])
			{
				index = i;
			}
		}
		return index;
	}

	public static int getMinIndex(int[] data)
	{
		Debug.assertSG(data.length > 0);
		int index = 0;
		for (int i = 1; i < data.length; i++)
		{
			if (data[index] > data[i])
			{
				index = i;
			}
		}
		return index;
	}

	public static int getMin(int[] data)
	{
		Debug.assertSG(data.length > 0);
		int min = data[0];
		for (int i = 1; i < data.length; i++)
		{
			if (min > data[i])
			{
				min = data[i];
			}
		}
		return min;
	}

	public static int getMax(int[] data)
	{
		Debug.assertSG(data.length > 0);
		int max = data[0];
		for (int i = 1; i < data.length; i++)
		{
			if (max < data[i])
			{
				max = data[i];
			}
		}
		return max;
	}

	public static int getMaxIndex(int[] data)
	{
		Debug.assertSG(data.length > 0);
		int index = 0;
		for (int i = 1; i < data.length; i++)
		{
			if (data[index] < data[i])
			{
				index = i;
			}
		}
		return index;
	}

	/**
	 * Return index of max. element of the array
	 */
	public static int getMaxIndex(double[] data)
	{
		Debug.assertSG(data.length > 0);
		int index = 0;
		for (int i = 1; i < data.length; i++)
		{
			if (data[index] < data[i])
			{
				index = i;
			}
		}
		return index;
	}

	/**
	 * Checks if the array is <code>null</code> or empty.
	 * 
	 * @param <T> type of the array
	 * @param array array to check
	 * @return <code>true</code> if the array is <code>null</code> or empty
	 */
	public static <T> boolean nullOrEmpty(final T[] array)
	{
		return array == null || array.length == 0;
	}

	// public static <T, T1 extends T> T[] join(T[] s1, T1 s2)
	// {
	// Debug.assertSG(s1);
	// //
	// if (s2 == null)
	// return s1;
	// //
	// T[] d = createArray(s1, s1.length + 1);
	// System.arraycopy(s1, 0, d, 0, s1.length);
	// d[d.length - 1] = s2;
	// return d;
	// }
	//
	// public static <T> T[] join(T[] s1, T[] s2, T[] s3)
	// {
	// return join(join(s1, s2), s3);
	// }
	//
	public static <T> T[] join(T[] s1, T[] s2)
	{
		if (s1 == null)
			return s2;
		if (s2 == null)
			return s1;
		Class<?> s1type = s1.getClass().getComponentType();
		Class<?> s2type = s2.getClass().getComponentType();
		Debug.assertSG(s1type.equals(s2type));
		return join(s1, s2, s1type);
	}

	public static <T> T[] join(T[] s1, T[] s2, Class<?> elementsClass)
	{
		if (s1 == null)
			return s2;
		if (s2 == null)
			return s1;
		T[] d = (T[]) Array.newInstance(elementsClass, s1.length + s2.length);
		System.arraycopy(s1, 0, d, 0, s1.length);
		System.arraycopy(s2, 0, d, s1.length, s2.length);
		return d;
	}

	// public static int[] join(int[] s1, int[] s2)
	// {
	// if (s1 == null)
	// return s2;
	// if (s2 == null)
	// return s1;
	//
	// int[] d = new int[s1.length + s2.length];
	// System.arraycopy(s1, 0, d, 0, s1.length);
	// System.arraycopy(s2, 0, d, s1.length, s2.length);
	// return d;
	// }

	/**
	 * Sorts array of string, some of which may be <code>null</code>s, putting
	 * <code>null</code>s at the end.
	 * 
	 * @param strings array of string to sort - in-place
	 */
	public static void sortNullable(final String[] strings)
	{
		Arrays.sort(strings, new Comparator<String>()
		{
			@Override
			public int compare(final String object1, final String object2)
			{
				// nulls at the end
				if (object1 == null)
					return (object2 != null ? 1 : 0);
				if (object2 == null)
					return -1;

				return object1.compareTo(object2);
			}
		});
	}

	/**
	 * @return true if i is contained in the given array
	 */
	public static boolean contains(int[] array, int i)
	{
		return indexOf(array, i) != -1;
	}

	/**
	 * @return index of <code>i</code> in <code>d</code> or -1 if <code>d</code>
	 *         does not contain <code>i</code>
	 */
	public static int indexOf(int[] d, int i)
	{
		for (int j = 0; j < d.length; j++)
		{
			if (d[j] == i)
				return j;
		}
		return -1;
	}

	/**
	 * @return index of <code>i</code> in <code>d</code> or -1 if <code>d</code>
	 *         does not contain <code>i</code>
	 */
	public static int indexOf(double[] d, double i)
	{
		for (int j = 0; j < d.length; j++)
		{
			if ((d[j] == i) || (Double.isNaN(i) && Double.isNaN(d[j])))
				return j;
		}
		return -1;
	}

	public static int indexOf(char[] d, char i)
	{
		for (int j = 0; j < d.length; j++)
		{
			if (d[j] == i)
				return j;
		}
		return -1;
	}

	/**
	 * @return index of <code>element</code> in <code>d</code>; or -1 if
	 *         <code>d</code> does not contain <code>i</code>
	 */
	public static int indexOf(final boolean[] array, final boolean element)
	{
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == element)
				return i;
		}
		return -1;
	}

	public static int indexOf(Object[] d, Object o)
	{
		return indexOf(d, 0, d.length - 1, o);
	}

	public static boolean contains(Object[] d, Object o)
	{
		return indexOf(d, o) > -1;
	}

	/**
	 * Check only the range specified by index0 and index1 (inclusive).
	 */
	public static int indexOf(Object[] d, int index0, int index1, Object o)
	{
		for (int j = index0; j <= index1; j++)
		{
			if (equals(d[j], o))
				return j;
		}
		return -1;
	}

	/**
	 * Delete array element at index i and return the resulting array
	 */
	public static int[] delete(int[] d, int i)
	{
		Debug.assertSG(i > -1 && i < d.length);
		int[] p = new int[d.length - 1];
		int k = 0;
		for (int j = 0; j < d.length; j++)
		{
			if (j != i)
			{
				p[k] = d[j];
				k++;
			}
		}
		return p;
	}

	public static int[] deleteVal(int[] d, int val)
	{
		int index = indexOf(d, val);
		if (index == -1)
			return copy(d);
		return delete(d, index);
	}

	public static boolean[] delete(boolean[] d, int i)
	{
		Debug.assertSG(i > -1 && i < d.length);
		boolean[] p = new boolean[d.length - 1];
		int k = 0;
		for (int j = 0; j < d.length; j++)
		{
			if (j != i)
			{
				p[k] = d[j];
				k++;
			}
		}
		return p;
	}

	/**
	 * Delete array element at index i and return the resulting array
	 */
	public static double[] delete(double[] d, int i)
	{
		Debug.assertSG(i > -1 && i < d.length);
		double[] p = new double[d.length - 1];
		int k = 0;
		for (int j = 0; j < d.length; j++)
		{
			if (j != i)
			{
				p[k] = d[j];
				k++;
			}
		}
		return p;
	}

	public static <T> T[] delete(T[] d, int index)
	{
		Debug.assertSG(d);
		Debug.assertSG(index > -1, index);
		Debug.assertSG(index < d.length, index);
		//
		List<T> newData = new ArrayList<T>();
		for (int i = 0; i < d.length; i++)
		{
			if (i != index)
				newData.add(d[i]);
		}
		T[] newArray = (T[]) java.lang.reflect.Array.newInstance(d.getClass().getComponentType(), newData.size());
		Debug.assertSG(newArray.length == newData.size());
		for (int i = 0; i < newArray.length; i++)
			newArray[i] = newData.get(i);
		return newArray;
	}

	/**
	 * Insert object e (may be null) at the specified index into array d.
	 */
	public static <T> T[] insert(T[] d, T e, int index)
	{
		Debug.assertSG(d);
		Debug.assertSG(index > -1 && index <= d.length);
		Class<?> componentType = d.getClass().getComponentType();
		T[] newArray = (T[]) java.lang.reflect.Array.newInstance(componentType, d.length + 1);
		for (int i = 0; i < d.length; i++)
			newArray[i + (i < index ? 0 : 1)] = d[i];
		newArray[index] = e;
		return newArray;
	}

	/**
	 * Insert element e at the specified index into array d.
	 */
	public static boolean[] insert(boolean[] d, boolean e, int index)
	{
		Debug.assertSG(d);
		Debug.assertSG(index > -1 && index <= d.length);
		boolean[] newArray = new boolean[d.length + 1];
		for (int i = 0; i < d.length; i++)
			newArray[i + (i < index ? 0 : 1)] = d[i];
		newArray[index] = e;
		return newArray;
	}

	/**
	 * Insert element e at the specified index into array d.
	 */
	public static int[] insert(final int[] d, final int e, final int index)
	{
		Debug.assertSG(d);
		Debug.assertSG(index > -1 && index <= d.length);
		final int[] newArray = new int[d.length + 1];
		for (int i = 0; i < d.length; i++)
			newArray[i + (i < index ? 0 : 1)] = d[i];
		newArray[index] = e;
		return newArray;
	}

	/**
	 * Insert element e at the specified index into array d.
	 */
	public static double[] insert(final double[] d, final double e, final int index)
	{
		Debug.assertSG(d);
		Debug.assertSG(index > -1 && index <= d.length);
		final double[] newArray = new double[d.length + 1];
		for (int i = 0; i < d.length; i++)
			newArray[i + (i < index ? 0 : 1)] = d[i];
		newArray[index] = e;
		return newArray;
	}

	/**
	 * Delete ALL occurrences of elements of e from d. d and e must be non-null.
	 */
	public static <T, T1 extends T> T[] deleteAll(T[] d, T1[] e)
	{
		Debug.assertSG(d);
		Debug.assertSG(e);
		List<T> newData = new ArrayList<T>(Arrays.asList(d));
		newData.removeAll(Arrays.asList(e));
		return newData.toArray(createArray(d, newData.size()));
	}

	/**
	 * Delete ALL occurrences of e (may be null) in d (may not be null).
	 */
	public static <T, T1 extends T> T[] delete(T[] d, T1 e)
	{
		Debug.assertSG(d);
		List<T> newData = new ArrayList<T>(Arrays.asList(d));
		while (newData.contains(e))
			newData.remove(e);
		return newData.toArray(createArray(d, newData.size()));
	}

	private static <T> T[] createArray(T[] source, int size)
	{
		return (T[]) Array.newInstance(source.getClass().getComponentType(), size);
	}

	public static boolean equals(Object o1, Object o2)
	{
		if (o1 == null && o2 == null)
			return true;
		if ((o1 != null && o2 == null) || (o1 == null && o2 != null))
			return false;
		if ((o1 instanceof Object[]) && (o2 instanceof Object[]))
		{
			return Arrays.deepEquals((Object[]) o1, (Object[]) o2);
		} else if ((o1 instanceof int[]) && (o2 instanceof int[]))
		{
			return Arrays.equals((int[]) o1, (int[]) o2);
		} else if ((o1 instanceof double[]) && (o2 instanceof double[]))
		{
			return Arrays.equals((double[]) o1, (double[]) o2);
		} else if ((o1 instanceof boolean[]) && (o2 instanceof boolean[]))
		{
			return Arrays.equals((boolean[]) o1, (boolean[]) o2);
		} else if ((o1 instanceof long[]) && (o2 instanceof long[]))
		{
			return Arrays.equals((long[]) o1, (long[]) o2);
		} else if ((o1 instanceof char[]) && (o2 instanceof char[]))
		{
			return Arrays.equals((char[]) o1, (char[]) o2);
		} else if ((o1 instanceof byte[]) && (o2 instanceof byte[]))
		{
			return Arrays.equals((byte[]) o1, (byte[]) o2);
		} else if ((o1 instanceof float[]) && (o2 instanceof float[]))
		{
			return Arrays.equals((float[]) o1, (float[]) o2);
		} else if ((o1 instanceof short[]) && (o2 instanceof short[]))
		{
			return Arrays.equals((short[]) o1, (short[]) o2);
		} else
		{
			return o1.equals(o2);
		}
	}

	/**
	 * @return sum of all elements of the array
	 */
	public static double sum(double[] d)
	{
		return sum(d, 0, d.length - 1);
	}

	public static double sum(double[] d, int index0, int index1)
	{
		double sum = 0.0;
		for (int i = index0; i <= index1; i++)
		{
			if (Double.isNaN(d[i]))
			{
				return Double.NaN;
			}
			sum = sum + d[i];
		}
		return sum;
	}

	public static <T> List<T> asList(T[] data)
	{
		return new ArrayList<T>(Arrays.asList(data));
	}

	public static List<Integer> asList(int[] data)
	{
		ArrayList<Integer> l = new ArrayList<Integer>(data.length);
		for (int i = 0; i < data.length; i++)
		{
			l.add(new Integer(data[i]));
		}
		return l;
	}

	public static List<Double> asList(double[] data)
	{
		ArrayList<Double> l = new ArrayList<Double>(data.length);
		for (int i = 0; i < data.length; i++)
		{
			l.add(new Double(data[i]));
		}
		return l;
	}

	public static <T> ArrayList<T> asFlatList(T[][] data)
	{
		Debug.assertSG(data);
		//
		ArrayList<T> l = new ArrayList<T>();
		for (int i = 0; i < data.length; i++)
		{
			l.addAll(Arrays.asList(data[i]));
		}
		return l;
	}

	/**
	 * Returns list of {@link Short} objects as <code>short[]</code>. There
	 * should not be <code>null</code> values in the list.
	 * 
	 * @param list list to convert (without <code>null</code>s)
	 * @return array with short values
	 */
	public static short[] asShortArray(List<Short> list)
	{
		short[] result = new short[list.size()];
		for (int i = 0; i < result.length; i++)
		{
			result[i] = list.get(i).shortValue();
		}
		return result;
	}

	/**
	 * Returns list of {@link Integer} objects as <code>int[]</code>. There
	 * should not be <code>null</code> values in the list.
	 * 
	 * @param list list to convert (without <code>null</code>s)
	 * @return array with int values
	 */
	public static int[] asIntArray(List<Integer> list)
	{
		int[] result = new int[list.size()];
		for (int i = 0; i < result.length; i++)
		{
			result[i] = list.get(i).intValue();
		}
		return result;
	}

	public static int[] asIntArray(Integer[] d)
	{
		int[] result = new int[d.length];
		for (int i = 0; i < result.length; i++)
		{
			result[i] = d[i].intValue();
		}
		return result;
	}

	/**
	 * Returns list of {@link Double} objects as <code>double[]</code>. There
	 * should not be <code>null</code> values in the list.
	 * 
	 * @param list list to convert (without <code>null</code>s)
	 * @return array with double values
	 */
	public static double[] asDoubleArray(List<Double> list)
	{
		double[] result = new double[list.size()];
		for (int i = 0; i < result.length; i++)
		{
			result[i] = list.get(i).doubleValue();
		}
		return result;
	}

	public static String toString(int[] d)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		if (d != null && d.length > 0)
		{
			for (int i = 0; i < d.length; i++)
			{
				sb.append(Integer.toString(d[i]));
				if (i < (d.length - 1))
					sb.append("; ");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public static String toString(double[] d)
	{
		StringBuilder sb = new StringBuilder();
		if (d != null && d.length > 0)
		{
			sb.append("[");
			for (int i = 0; i < d.length; i++)
			{
				sb.append(Double.toString(d[i]));
				if (i < (d.length - 1))
					sb.append("; ");
			}
			sb.append("]");
		}
		return sb.toString();
	}

	/**
	 * Trims array's length to the specified value. If array is null or its
	 * length is less or equal than the specified value - returns the array
	 * itself.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] trim(T[] d, int newLength)
	{
		if (d == null || Array.getLength(d) <= newLength)
			return d;
		T[] r = (T[]) Array.newInstance(d.getClass().getComponentType(), newLength);
		System.arraycopy(d, 0, r, 0, newLength);
		return r;
	}

	/**
	 * Trims array's length to the specified value. If array is null or its
	 * length is less or equal than the specified value - returns the array
	 * itself.
	 */
	public static double[] trim(double[] d, int newLength)
	{
		if (d == null || d.length <= newLength)
			return d;
		double[] r = new double[newLength];
		System.arraycopy(d, 0, r, 0, newLength);
		return r;
	}

	/**
	 * Trims array's length by removing the first elements (as opposed to
	 * {@link #trim(double[], int)}, which removes the last elements) to the
	 * specified value. If array is null or its length is less or equal than the
	 * specified value - returns the array itself.
	 */
	public static double[] tail(double[] d, int newLength)
	{
		if (d == null || d.length <= newLength)
			return d;
		double[] r = new double[newLength];
		System.arraycopy(d, d.length - newLength, r, 0, newLength);
		return r;
	}

	public static void swap(Object[] d, int index1, int index2)
	{
		Object t = d[index1];
		d[index1] = d[index2];
		d[index2] = t;
	}

	public static void swap(int[] d, int index1, int index2)
	{
		int t = d[index1];
		d[index1] = d[index2];
		d[index2] = t;
	}

	public static double[] reverse(double[] d)
	{
		double[] d1 = new double[d.length];
		for (int i = 0; i < d1.length; i++)
			d1[i] = d[d.length - 1 - i];
		return d1;
	}

	public static int[] reverse(int[] d)
	{
		int[] d1 = new int[d.length];
		for (int i = 0; i < d1.length; i++)
			d1[i] = d[d.length - 1 - i];
		return d1;
	}

	/**
	 * Creates a reversed object array (new copy, rather than in-place).
	 */
	public static <T> T[] reverse(T[] array)
	{
		Class<?> componentType = array.getClass().getComponentType();
		@SuppressWarnings("unchecked")
		T[] newArray = (T[]) java.lang.reflect.Array.newInstance(componentType, array.length);
		for (int i = 0, j = newArray.length - 1; i < newArray.length; i++, j--)
		{
			newArray[i] = array[j];
		}
		return newArray;
	}

	public static boolean haveEqualElements(int[] array1, int[] array2)
	{
		if (array1 == null && array2 == null)
			return true;
		if (array1 == null || array2 == null)
			return false;
		if (array1.length != array2.length)
			return false;
		int[] a1 = copy(array1);
		Arrays.sort(a1);
		int[] a2 = copy(array2);
		Arrays.sort(a2);
		return Arrays.equals(a1, a2);
	}

	/**
	 * @return true, if _b is a subset of _a
	 */
	public static boolean isSubset(Object[] _a, Object[] _b)
	{
		return Arrays.asList(_a).containsAll(Arrays.asList(_b));
	}

	public static <T> Set<T> asSet(T[] array)
	{
		List<T> l = Arrays.asList(array);
		Set<T> set = new HashSet<T>();
		set.addAll(l);
		return set;
	}

	/**
	 * Converts an array to an ordered set (in the same order as in the array).
	 * 
	 * @param <T> type of the elements in the array
	 * @param array input array
	 * @return ordered set of the elements in the array
	 */
	public static <T> LinkedHashSet<T> asLinkedHashSet(final T[] array)
	{
		@SuppressWarnings("unchecked")
		final List<T> l = array != null ? Arrays.asList(array) : (List<T>) Collections.EMPTY_LIST;
		return new LinkedHashSet<T>(l);
	}

	/**
	 * returns array of those elements that exist in both arrays.
	 * 
	 * @param <T>
	 * @param firstArray
	 * @param secondArray
	 * @param emptyGenericArray used for casting set to string ('new
	 *            AttributeDefinition[0]' - for example)
	 * @return
	 */
	public static <T> T[] intersection(T[] firstArray, T[] secondArray, final T[] emptyGenericArray)
	{
		Set<T> from1stSet = asSet(firstArray);
		Set<T> commonSet = new HashSet<T>();
		for (T secondElement : secondArray)
			if (from1stSet.contains(secondElement))
				commonSet.add(secondElement);
		return commonSet.toArray(emptyGenericArray);
	}
}
