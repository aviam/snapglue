package com.nigealm.utils;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class StringUtils
{
	public static final String EOL = Character.toString('\n');
	public static final String backslash = Character.toString('\\');
	public static final String HTML_TAG = "<html>";

	private StringUtils()
	{
		super();
	}

	public static boolean contains(String[] array, String str)
	{
		for (int i = 0; i < array.length; i++)
			if (GeneralUtils.equalsNullable(array[i], str))
				return true;
		return false;
	}

	public static int getMaxLength(String[] strs)
	{
		Debug.assertSG(strs.length > 0);
		int maxLen = strs[0].length();
		for (int j = 1; j < strs.length; j++)
		{
			int len = strs[j].length();
			if (len > maxLen)
			{
				maxLen = len;
			}
		}
		return maxLen;
	}

	/**
	 * Replace in <code>buf</code> first occurrence of <code>a</code> by
	 * <code>b</code>.
	 */
	public static void replace(StringBuffer buf, String a, String b)
	{
		Debug.assertSG(buf);
		Debug.assertSG(a.length() > 0);
		Debug.assertSG(b);
		int i = buf.toString().indexOf(a);
		if (i > -1)
		{
			buf.replace(i, i + a.length(), b);
		}
	}

	/**
	 * Returns the result of replacement in <code>src</code> of the first
	 * (non-overlapping) occurrence of <code>a</code> by <code>b</code>.
	 */
	public static String replaceFirst(String src, String a, String b)
	{
		StringBuffer buf = new StringBuffer(src);
		replace(buf, a, b);
		return buf.toString();
	}

	/**
	 * Returns the result of replacement in <code>src</code> of all
	 * (non-overlapping) occurrences of <code>a</code> by <code>b</code>.
	 */
	public static String replaceAll(String src, String a, String b)
	{
		int srcLength = src.length();
		int aLength = a.length();
		Debug.assertSG((aLength > 0) && (b != null));

		// first occurrence of a
		int nextOccurrence = src.indexOf(a);
		if (nextOccurrence < 0)
			return src;

		// jump through occurrences of a
		StringBuilder res = new StringBuilder();
		int pos = 0;
		do
		{
			if (nextOccurrence >= 0)
			{
				if (nextOccurrence > pos)
				{
					res.append(src.substring(pos, nextOccurrence));
				}
				res.append(b);
				pos = nextOccurrence + aLength;
				nextOccurrence = src.indexOf(a, pos);
			} else
			{
				res.append(src.substring(pos));
				pos = srcLength;
			}
		} while (pos < srcLength);

		return res.toString();
	}

	/**
	 * @return <code>array</code> merged to <code>String</code> and separated by
	 *         spaces (" ").
	 */
	public static String toString(String[] array)
	{
		return toString(array, " ");
	}

	/**
	 * @return <code>array</code> merged to <code>String</code> and separated by
	 *         <code>separator</code>.
	 */
	public static String toString(String[] array, String separator)
	{
		return toString(Arrays.asList(array), 0, array.length - 1, separator);
	}

	public static String toString(Collection<String> l, String separator)
	{
		return toString(l.toArray(new String[l.size()]), separator);
	}

	public static String toString(String[] array, int index0, int index1, String separator)
	{
		return toString(Arrays.asList(array), index0, index1, separator);
	}

	/**
	 * Returns concatenated toString() results for each object in the list
	 * separated by the provided separator. If object is null, it represented by
	 * "null".
	 */
	public static String toString(List<? extends Object> list, int index0, int index1, String separator)
	{
		StringBuilder sb = new StringBuilder();
		for (int j = index0; j <= index1; j++)
		{
			sb.append(String.valueOf(list.get(j)));
			if (j < index1)
				sb.append(separator);
		}
		return sb.toString();
	}

	/**
	 * Convert first <code>c</code> letters of <code>str</code> to upper case
	 * and return result.
	 */
	public static String toUpperCase(String str, int c)
	{
		Debug.assertSG(str);
		Debug.assertSG(c > 0);
		StringBuilder buf = new StringBuilder();
		for (int j = 0; j < str.length(); j++)
		{
			if (j < c)
			{
				buf.append(Character.toUpperCase(str.charAt(j)));
			} else
			{
				buf.append(str.charAt(j));
			}
		}
		return buf.toString();
	}

	/**
	 * Search for a String in a String array. If found a match returns the index
	 * if not, returns -1.
	 * 
	 * @param arr java.lang.String[] the array in which to look
	 * @param s java.lang.String the String to look for
	 * @param ignoreCase boolean whether to ignore case
	 * @return int the String index in the array, -1 if not found.
	 */
	public static int findStringIndexInArray(String[] arr, String s, boolean ignoreCase)
	{
		for (int i = 0; i < arr.length; i++)
		{
			if (arr[i].equals(s) || (ignoreCase && arr[i].equalsIgnoreCase(s)))
				return i;
		}
		return -1;
	}

	/**
	 * Simple method to remove HTML tags from a string. Example:
	 * "<html><b>ABC</b>" -> "ABC"
	 */
	public static String stripHTMLTags(String s)
	{
		Debug.assertSG(s);
		//
		StringBuilder sb = new StringBuilder();
		int index = 0;
		int inTagDepth = 0;
		String[] SPECIAL = new String[]
		{ "&nbsp;", "&quot;", "&gt;", "&lt;", "&amp;", "&apos;", "&#39;" };
		String[] SUBSTITUTE = new String[]
		{ " ", "\"", ">", "<", "&", "'", "'" };
		//
		P_1: while (index < s.length())
		{
			char c = s.charAt(index);
			//
			if (inTagDepth == 0 && c == '&')
			{
				for (int i = 0; i < SPECIAL.length; i++)
				{
					if ((index + SPECIAL[i].length() - 1) < s.length())
					{
						String subStr = s.substring(index, index + SPECIAL[i].length());
						if (subStr.equalsIgnoreCase(SPECIAL[i]))
						{
							sb.append(SUBSTITUTE[i]);
							index = index + SPECIAL[i].length();
							continue P_1;
						}
					}
				}
			}
			//
			if (c == '<')
			{
				inTagDepth++;
			}
			if (inTagDepth == 0)
			{
				sb.append(c);
			}
			if (c == '>')
			{
				if (inTagDepth == 0)
					return s; // Invalid HTML ?
				inTagDepth--;
			}
			//
			index++;
		}
		return sb.toString();
	}

	public static String trimRight(String text)
	{
		if (text == null)
			return null;
		int firstTrailingSpace = -1;
		for (int i = text.length() - 1; i > -1; i--)
		{
			if (text.charAt(i) == ' ')
				firstTrailingSpace = i;
			else
				break;
		}
		return (firstTrailingSpace > 0) ? text.substring(0, firstTrailingSpace) : text;
	}

	/**
	 * Returns the byte length of the given string for UTF-8 encoding
	 */
	public static int getStringLengthInUTF8(String str)
	{
		byte[] utf8Bytes = null;
		try
		{
			utf8Bytes = str.getBytes("UTF-8");
			return utf8Bytes.length;
		} catch (UnsupportedEncodingException e1)
		{
			Debug.abort(e1, "failure trying to get String Length In UTF8");
			return -1;
		}
	}

	public static boolean hasDuplicates(String[] a)
	{
		Debug.assertSG(a);

		return removeDuplicates(a).length < a.length;
	}

	public static String[] removeDuplicates(String[] a)
	{
		Debug.assertSG(a);

		Set<String> set = new HashSet<String>(Arrays.asList(a));
		return set.toArray(new String[0]);
	}

	/**
	 * returns true if all values within valsToCheck are also within
	 * originalVals
	 * 
	 * @param originalVals
	 * @param valsToCheck
	 */
	public static boolean isSubSet(String[] originalVals, String[] valsToCheck)
	{
		Set<String> temp = new HashSet<String>();
		for (int i = 0; i < originalVals.length; i++)
			temp.add(originalVals[i]);
		for (int i = 0; i < valsToCheck.length; i++)
			if (!(temp.contains(valsToCheck[i])))
				return false;
		return true;
	}

	/**
	 * returns values common to vals1 and vals2
	 */
	public static String[] getSubSet(String[] vals1, String[] vals2)
	{
		if (vals1 == null || vals2 == null)
			return new String[0];
		Set<String> vals1Set = new HashSet<String>();
		for (int i = 0; i < vals1.length; i++)
			vals1Set.add(vals1[i]);

		Set<String> subSet = new HashSet<String>();
		for (int i = 0; i < vals2.length; i++)
			if (vals1Set.contains(vals2[i]))
				subSet.add(vals2[i]);

		return subSet.toArray(new String[]
		{});
	}

	/**
	 * returns superset of vals1 and vals2
	 */
	public static String[] getSuperSet(String[] vals1, String[] vals2)
	{
		Set<String> temp = new HashSet<String>();
		for (int i = 0; i < vals1.length; i++)
			temp.add(vals1[i]);
		for (int i = 0; i < vals2.length; i++)
			temp.add(vals2[i]);
		return temp.toArray(new String[]
		{});
	}

	/**
	 * removes the aVal element from the array (if it is present)
	 * 
	 * @param vals
	 * @param aVal
	 * @return
	 */
	public static String[] removeElement(String[] vals, String aVal)
	{
		Set<String> set = new HashSet<String>();
		for (int i = 0; i < vals.length; i++)
		{
			set.add(vals[i]);
		}
		set.remove(aVal);
		return set.toArray(new String[]
		{});
	}

	/**
	 * Returns a String array containing all strings from both given arrays.
	 * Supports null in all possibilities, e.g. the array might be null and
	 * objects inside it.
	 * 
	 * @param a1
	 * @param a2
	 */
	public static String[] append(String[] a1, String[] a2)
	{
		if (a1 == null && a2 == null)
		{
			return null;
		} else if (a1 == null)
		{
			return a2;
		} else if (a2 == null)
		{
			return a1;
		} else
		{
			String[] res = new String[a1.length + a2.length];
			System.arraycopy(a1, 0, res, 0, a1.length);
			System.arraycopy(a2, 0, res, a1.length, a2.length);
			return res;
		}
	}
}