package com.nigealm.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * General utility functions.
 * 
 */
public final class GeneralUtils
{
	/**
	 * This is 2^53 - the largest long convertible without loss to/from double
	 */
	public static final long MAX_LONG_CASTABLE_TO_DOUBLE = 9007199254740992L;

	public static final String LN = System.getProperty("line.separator");

	/**
	 * Separator of the paths, usually ; or :
	 */
	public static final String PATH_SEP = System.getProperty("path.separator");

	/**
	 * Separator of the files and directories, usually \ or /
	 */
	public static final String FILE_SEP = System.getProperty("file.separator");

	private static final String SYSTEM_PROPERTY_OPERATION_SYSTEM_NAME = "os.name";
	private static final String SYSTEM_PROPERTY_OPERATION_SYSTEM_VERSION = "os.version";
	private static final String SYSTEM_PROPERTY_OPERATION_SYSTEM_ARCH = "os.arch";
	private static final String SYSTEM_PROPERTY_JAVA_VERSION_INFO = "java.vm.info";


	public final static char[] digits = new char[]
	{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * The function compares two byte strings lexicographically. Description of
	 * this function is similar to String.compareTo()
	 */
	public static int compareTo(byte[] one, byte[] another)
	{
		int n = Math.min(one.length, another.length);

		for (int i = 0; i < n; i++)
		{
			if (one[i] != another[i])
				return one[i] - another[i];
		}

		return one.length - another.length;
	}

	/**
	 * Decodes the given character from Base64 (RFC-1521)
	 */
	private static int decodeBase64(char c) throws IOException
	{
		if (c >= 'A' && c <= 'Z')
			return c - 'A';

		if (c >= 'a' && c <= 'z')
			return 26 + c - 'a';

		if (c >= '0' && c <= '9')
			return 52 + c - '0';

		if (c == '+')
			return 62;

		if (c == '/')
			return 63;

		if (c == '=')
			return 0;

		throw new IOException("Bad base64 character encountered: " + c);
	}

	/**
	 * Decodes the given string from Base64 (RFC-1521)
	 */
	public static byte[] decodeBase64(String s) throws IOException
	{
		if (s.length() % 4 != 0)
			throw new IOException("Bad Base64 string - length should be a multiple of 4");

		int res_length = s.length() / 4 * 3;

		if (s.charAt(s.length() - 1) == '=')
		{
			if (s.charAt(s.length() - 2) == '=') // '==' trailed - one byte in
													// the last tripple
				res_length -= 2;
			else
				res_length -= 1; // '=' trailed - two bytes in the last tripple
		}

		byte[] res = new byte[res_length];
		int block;

		for (int i = 0, j = 0; i < s.length();)
		{
			block = decodeBase64(s.charAt(i++));
			block <<= 6;
			block |= decodeBase64(s.charAt(i++));
			block <<= 6;
			block |= decodeBase64(s.charAt(i++));
			block <<= 6;
			block |= decodeBase64(s.charAt(i++));

			res[j++] = (byte) ((block & 0xFF0000) >>> 16);
			if (j < res.length)
				res[j++] = (byte) ((block & 0xFF00) >>> 8);
			if (j < res.length)
				res[j++] = (byte) (block & 0xFF);
		}

		return res;
	}

	/**
	 * Decodes the given Base64 string and deserializes an object from it
	 */
	public static Object deserializeBase64(String base64enc) throws IOException
	{
		try
		{
			byte[] in = decodeBase64(base64enc);
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(in));
			return ois.readObject();
		} catch (ClassNotFoundException e)
		{
			Debug.abort(e, "failure in deserializeBase64");
			return null;
		}
	}

	/**
	 * Encodes the input byte array into a Base64 (RFC-1521) string
	 */
	public static String encodeBase64(byte[] in)
	{
		StringBuffer res = new StringBuffer(in.length / 3 * 4);
		int i;
		int blocks = in.length / 3;
		int block;

		for (i = 0; i < blocks * 3;)
		{
			block = in[i++] & 0xFF;
			block <<= 8;
			block |= in[i++] & 0xFF;
			block <<= 8;
			block |= in[i++] & 0xFF;

			res.append(encodeBase64(block));
		}

		if (i == in.length - 1) // one byte left
		{
			block = in[i++] & 0xFF;
			block <<= 4;
			res.append(encodeBase64(block), 2, 2);
			res.append("==");
		} else if (i == in.length - 2) // two bytes left
		{
			block = in[i++] & 0xFF;
			block <<= 8;
			block |= in[i++] & 0xFF;
			block <<= 2;
			res.append(encodeBase64(block), 1, 3);
			res.append("=");
		}

		return res.toString();
	}

	/**
	 * Encodes the 6 bits input into a char of Base64
	 */
	private static char encodeBase64(byte in)
	{
		if (in >= 0 && in <= 25)
			return (char) ('A' + in);

		if (in >= 26 && in <= 51)
			return (char) ('a' + in - 26);

		if (in >= 52 && in <= 61)
			return (char) ('0' + in - 52);

		if (in == 62)
			return '+';

		if (in == 63)
			return '/';

		Debug.abort("Bad Base64 byte");
		return 0;
	}

	/**
	 * Encodes the 3 bytes input into a 4 char Base64 (RFC-1521) string
	 */
	private static char[] encodeBase64(int in)
	{
		char[] res = new char[4];

		res[3] = encodeBase64((byte) (in & 0x3F));
		in >>>= 6;
		res[2] = encodeBase64((byte) (in & 0x3F));
		in >>>= 6;
		res[1] = encodeBase64((byte) (in & 0x3F));
		in >>>= 6;
		res[0] = encodeBase64((byte) (in & 0x3F));

		return res;
	}

	/**
	 * Compares two objects. If both references are <code>null</code> the
	 * objects are considered equal, otherwise equals() method is used to
	 * determine the equality.
	 * 
	 * @param a first object or <code>null</code>
	 * @param b second object or <code>null</code>
	 * @return <code>true</code> iff both refs are <code>null</code> or they
	 *         both are not <code>null</code> and equal
	 */
	public static boolean equalsNullable(final Object a, final Object b)
	{
		if (a == b)
			return true;
		if (a != null && b != null)
			return a.equals(b);
		return false;
	}

	/**
	 * Serializes the given object and encodes the result into Base64 string.
	 */
	public static String serializeBase64(Object o)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
			oos.flush();
			oos.close();

			return encodeBase64(baos.toByteArray());
		} catch (IOException e)
		{
			Debug.abort(e, "failure serializeBase64");
			return null;
		}
	}

	public static String getHostName()
	{
		try
		{
			return InetAddress.getLocalHost().getHostName();
		} 
		catch (UnknownHostException e)
		{
			Debug.abort(e, "failure looking for the host name");
			return null;
		}
	}

	public static String getHostIP()
	{
		try
		{
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e)
		{
			Debug.abort(e, "failure looking for the host ip");
			return null;
		}
	}

	/**
	 * Returns the server IP.
	 * 
	 * @throws UnknownHostException
	 */
	public static String getServerIP() throws UnknownHostException
	{
		return InetAddress.getLocalHost().getHostAddress();
	}

	public static String getOperationSystemName()
	{
		return System.getProperty(SYSTEM_PROPERTY_OPERATION_SYSTEM_NAME);
	}

	public static String getOperationSystemVersion()
	{
		return System.getProperty(SYSTEM_PROPERTY_OPERATION_SYSTEM_VERSION);
	}

	public static String getOperationSystemArch()
	{
		return System.getProperty(SYSTEM_PROPERTY_OPERATION_SYSTEM_ARCH);
	}

	/**
	 * Returns the total amount of memory in the Java virtual machine. Returned
	 * result is measured in megabytes.
	 */
	public static long getTotalMemory()
	{
		Runtime runt = Runtime.getRuntime();
		return runt.totalMemory() / (1024 * 1024);
	}

	/**
	 * Returns the amount of free memory in the Java Virtual Machine, measured
	 * in megabytes.
	 * 
	 */
	public static long getFreeMemory()
	{
		Runtime runt = Runtime.getRuntime();
		return runt.freeMemory() / (1024 * 1024);
	}

	public static String getJavaVersion()
	{
		String version = System.getProperty(SYSTEM_PROPERTY_JAVA_VERSION_INFO);
		String res;
		int index = version.indexOf("J9VM");
		if (index != -1)
		{
			res = version.substring(0, index - 1);
		} else
		{
			String runtimeName = System.getProperty("java.runtime.name");
			String runtimeVersion = System.getProperty("java.runtime.version");
			String vmName = System.getProperty("java.vm.name");
			String vendor = System.getProperty("java.specification.vendor");

			res = runtimeName + ", " + runtimeVersion + ", " + vmName + ", " + vendor;
		}
		return res;
	}

	/**
	 * Method serializeObject Serialize the given Object and returns the byte[]
	 * outcome
	 * 
	 * @param object
	 * @throws IOException
	 */
	public static byte[] serializeObject(Serializable object) throws IOException
	{
		// Serialize to a byte array
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput oos = new ObjectOutputStream(bos);
		oos.writeObject(object);
		oos.close();

		// Get the bytes of the serialized object
		return bos.toByteArray();
	}

	/**
	 * Method deserializeObject de-serialize the given byte[]
	 * 
	 * @param buf
	 * @throws ClassNotFoundException is deserialized Class is not recognizeble
	 * @throws IOException
	 */
	public static Object deserializeObject(byte[] buf) throws ClassNotFoundException, IOException
	{
		// Serialize to a byte array
		ByteArrayInputStream bis = new ByteArrayInputStream(buf);
		ObjectInput ois = new ObjectInputStream(bis);
		Object object = ois.readObject();
		ois.close();

		return object;
	}

	/**
	 * Clone the given array. Each of the objects in the array is copied(not
	 * cloned) to the new array.
	 * 
	 * @param a The array
	 * @return a clone of the given array.
	 */
	public static Object cloneArray(Object a)
	{
		int length = Array.getLength(a);
		Object res = Array.newInstance(a.getClass().getComponentType(), length);
		System.arraycopy(a, 0, res, 0, length);
		return res;

	}

	/**
	 * Get system property as an integer.
	 * 
	 * @param propName property name
	 * @param defaultVal default value in case the property is not defined or
	 *            has non-integer value
	 */
	public static int getIntProperty(String propName, int defaultVal)
	{
		String pS = System.getProperty(propName);
		if (pS != null && pS.length() > 0)
		{
			try
			{
				return Integer.parseInt(pS);
			} 
			catch (NumberFormatException e)
			{
				// return default value
				Debug.trace("Property \"" + propName + "\" should have integer value", e);
			}
		}
		return defaultVal;
	}

	/**
	 * Get system property as a double.
	 * 
	 * @param propName property name
	 * @param defaultVal default value in case the property is not defined or
	 *            has non-number value
	 */
	public static double getDoubleProperty(String propName, double defaultVal)
	{
		String pS = System.getProperty(propName);
		if (pS != null && pS.length() > 0)
			try
			{
				return Double.parseDouble(pS);
			} catch (NumberFormatException e)
			{
				// return the default value
				Debug.trace("Property \"" + propName + "\" should be a number", e);
			}
		return defaultVal;
	}

	/**
	 * Get Boolean system property.
	 * 
	 * @param propName
	 * @param defaultVal
	 */
	public static boolean getBooleanProperty(String propName, boolean defaultVal)
	{
		String pS = System.getProperty(propName);
		if (pS != null && pS.length() > 0)
			return Boolean.valueOf(pS).booleanValue();
		else
			return defaultVal;
	}

	/**
	 * Get Boolean system property.
	 * 
	 * @param propName property name
	 * @param defaultVal default value
	 */
	public static Boolean getBooleanProperty(String propName, Boolean defaultVal)
	{
		String pS = System.getProperty(propName);
		if (pS != null && pS.length() > 0)
			return Boolean.valueOf(pS);
		else
			return defaultVal;
	}

	/**
	 * Get system property.
	 * 
	 * @param propName
	 * @param defaultVal
	 */
	public static String getStringProperty(String propName, String defaultVal)
	{
		String pS = System.getProperty(propName);
		if (pS != null && pS.length() > 0)
			return pS;

		return defaultVal;
	}

	/**
	 * Performs 'deep' copy of the specified object.
	 * 
	 */
	public static <T extends Serializable> T copyObject(T o)
	{
		if (o == null)
			return null;
		try
		{
			ByteArrayOutputStream arrayStream = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(arrayStream);
			out.writeObject(o);
			byte[] objectData = arrayStream.toByteArray();
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(objectData));
			T result = (T) in.readObject();
			out.close();
			in.close();
			return result;
		}
		catch (Exception e)
		{
			Debug.abort(e, "failure to copy object");
			return null;
		}
	}

}