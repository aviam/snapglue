package com.nigealm.utils;

import com.nigealm.common.utils.Tracer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The class includes a number of debugging methods: - abort methods used to
 * quit the application on various error conditions; - assert methods used to
 * check a condition that should always hold true (invariants). If the condition
 * is false the methods will quit the application. - trace methods are used for
 * debug print-outs.
 */
public final class Debug
{
	private static final Tracer tracer = new Tracer();

	private static final String LINE_SEPERATOR = System.getProperty("line.separator");

	/**
	 * The class is used for implementation of the Debug class.
	 */
	public static final class Failure extends Error
	{
		private static final long serialVersionUID = -2734213067158162218L;

		private Failure(String text, Throwable e)
		{
			super(text, e);
		}
	}

	/**
	 * The class is a static name space and is not intended for instantiation.
	 */
	private Debug()
	{
	}

	/**
	 * Traces the text and the call stack and aborts the application.
	 * 
	 * @param text The text to be traced.
	 */
	public static void abort(String text)
	{
		abort(null, text);
	}

	/**
	 * Traces the text, the exception (including its call stack) and aborts the
	 * application.
	 * 
	 * @param e The exception to be traced.
	 * @param text The text to be traced.
	 */
	public static void abort(Throwable e, String text)
	{
		event("ABORT CALLED", "abort", e, text);
	}

	/**
	 * Checks whether the argument reference is not null. If the reference is
	 * null traces the stack and aborts the application.
	 * 
	 * @param cond The reference to be checked.
	 */
	public static void assertSG(Object cond)
	{
		// for performance
		if (cond != null)
			return;

		assertSG(false, "null assert argument");
	}

	public static void assertArraySG(Object[] _array, String _txt)
	{
		assertSG(_array, _txt);
		for (int i = 0; i < _array.length; i++)
			assertSG(_array[i] != null, _txt, i);
	}

	public static void assertArraySG(Object[] _array)
	{
		assertSG(_array);
		for (int i = 0; i < _array.length; i++)
			assertSG(_array[i], i);
	}

	/**
	 * Assert the the collection is not null and doesn't contain null elements.
	 */
	public static void assertCollectionSG(Collection<?> c)
	{
		assertSG(c);
		Iterator<?> iterator = c.iterator();
		while (iterator.hasNext())
		{
			assertSG(iterator.next());
		}
	}

	/**
	 * Checks whether the argument reference is not null. If the reference is
	 * null traces the stack and aborts the application.
	 * 
	 * @param cond The reference to be checked.
	 * @param id Identifier for multiple asserts in a function
	 */
	public static void assertSG(Object cond, int id)
	{
		assertSG(cond != null, id);
	}

	/**
	 * Checks whether the argument reference is not null. If the reference is
	 * null traces the stack and aborts the application.
	 * 
	 * @param cond The reference to be checked.
	 * @param text Message to print to console in case the cond is null
	 */
	public static void assertSG(Object cond, String text)
	{
		assertSG(cond != null, text);
	}

	/**
	 * Checks whether the condition is true. If the condition is false traces
	 * the stack and aborts the application.
	 * 
	 * @param cond The condition to be checked.
	 */
	public static void assertSG(boolean cond)
	{
		// for performance
		if (cond)
			return;

		assertSG(cond, "");
	}

	/**
	 * Checks whether the condition is true. If the condition is false traces
	 * the stack and aborts the application.
	 * 
	 * @param cond The condition to be checked.
	 * @param text Message to print to console in case the condition is false
	 */
	public static void assertSG(boolean cond, String text)
	{
		if (cond)
			return;

		event("ASSERTION FAILED", "assert", null, text);
	}

	public static void assertSG(boolean cond, String text1, String text2)
	{
		if (cond)
			return;

		event("ASSERTION FAILED", "assert", null, text1 + ", " + text2);
	}

	public static void assertSG(boolean cond, String text, long n)
	{
		if (cond)
			return;

		event("ASSERTION FAILED", "assert", null, text + " " + n);
	}

	public static void assertSG(boolean cond, String text, double d)
	{
		if (cond)
			return;

		event("ASSERTION FAILED", "assert", null, text + " " + d);
	}

	/**
	 * Checks whether the condition is true. If the condition is false traces
	 * the stack and aborts the application.
	 * 
	 * @param cond The condition to be checked.
	 * @param d double number(s) to print to console in case the condition is
	 *            false
	 */
	public static void assertSG(boolean cond, double... d)
	{
		if (cond)
			return;

		event("ASSERTION FAILED", "assert", null, ArrayUtils.toString(d));
	}

	/**
	 * Checks whether the condition is true. If the condition is false traces
	 * the stack and aborts the application.
	 * 
	 * @param cond The condition to be checked.
	 * @param n number to print to console in case the condition is false
	 */
	public static void assertSG(boolean cond, int n)
	{
		if (cond)
			return;

		event("ASSERTION FAILED", "assert", null, Integer.toString(n));
	}

	/**
	 * Prints a "Not implemented yet!" message and aborts the application.
	 */
	public static void notImplemented()
	{
		abort("Not implemented yet!");
	}

	/**
	 * Prints the text to the log stream.
	 * 
	 * @param text The text to be traced.
	 */
	public static void trace(String... text)
	{
		trace(StringUtils.toString(text), null);
	}

	/**
	 * Prints the content of the exception to the log stream.
	 * 
	 * @param e The exception to be traced.
	 */
	public static void trace(Throwable e)
	{
		trace(null, e);
	}

	/**
	 * Prints the text and the exception content to the log stream.
	 * 
	 * @param text The text to be traced.
	 * @param e The exception to be traced.
	 */
	public static void trace(String text, Throwable e)
	{
		String out = getHeader("TRACE", text);
		out = appendStack(out, e, "EXCEPTION TRACED");
		tracer.trace(out);
	}

	private static String getStack(Failure f)
	{
		StringWriter sw = new StringWriter();
		f.printStackTrace(new PrintWriter(sw));
		String stack = sw.getBuffer().toString();
		return stack;
	}

	private static void event(String title, String method, Throwable e, String text)
	{
		String header = getHeader(title, text);
		Failure f = new Failure(header, e);
		String out = header + "  " + getStack(f);
		out = appendStack(out, e, "ORIGIN EXCEPTION");
		tracer.trace(out);
		throw f;
	}

	private static String appendStack(String out, Throwable e, String title)
	{
		if (e != null)
			out += LINE_SEPERATOR + " " + title + ":" + LINE_SEPERATOR + getExceptionLog(e);

		return out;
	}

	private static String getHeader(String title, String text)
	{
		return title + ((text != null) ? (" - " + text + " - ") : "");
	}

	/**
	 * Creates a printout of the exception and all its internal (nested)
	 * exceptions.
	 */
	private static String getExceptionLog(Throwable e)
	{
		StringWriter sw = new StringWriter();
		PrintWriter out = new PrintWriter(sw);

		boolean have_internal_exception;
		int counter = 1;

		do
		{
			counter++;
			e.printStackTrace(out);

			have_internal_exception = false;
			Class<? extends Throwable> cl = e.getClass();

			// trying to find accessible fields holding a nested exception
			Field[] public_fields = cl.getFields();
			for (int i = 0; i < public_fields.length; i++)
			{
				if (Throwable.class.isAssignableFrom(public_fields[i].getType()))
				{
					try
					{
						e = (Throwable) public_fields[i].get(e);
					} catch (IllegalAccessException ex)
					{
						continue;
					}

					have_internal_exception = true;

					out.println();
					if (e != null)
						out.println("   NESTED EXCEPTION #" + counter + " [" + public_fields[i].getName() + "]:");

					break;
				}
			}

			// trying to call a method returning a nested exception
			Method[] public_methods = cl.getMethods();
			for (int i = 0; i < public_methods.length; i++)
			{
				if (Throwable.class.isAssignableFrom(public_methods[i].getReturnType())
						&& public_methods[i].getParameterTypes().length == 0
						&& !public_methods[i].getName().equals("fillInStackTrace"))
				{
					try
					{
						Throwable tmp = (Throwable) public_methods[i].invoke(e, new Object[0]);
						if (tmp != null)
							e = tmp;
						else
							continue;
					} catch (Exception ex)
					{
						continue;
					}

					have_internal_exception = true;

					out.println();
					if (e != null)
						out.println("   NESTED EXCEPTION #" + counter + " [" + public_methods[i].getName() + "()]:");

					break;
				}
			}

			if (e != null && have_internal_exception && counter > 10)
			{
				out.println("   NESTING LIMIT REACHED - FURTHER NESTED EXCEPTIONS OMITTED");
				break;
			}
		} while (e != null && have_internal_exception);

		out.println(" END ORIGIN EXCEPTION");
		out.close();
		return sw.getBuffer().toString();
	}

	public static void assertMethodExistence(Class<?> _class, List<String> _methods)
	{
		// check that the cached methods exist:
		Method[] cdMethods = _class.getMethods();
		ArrayList<String> sv = new ArrayList<String>();
		for (int i = 0; i < cdMethods.length; i++)
		{
			sv.add(cdMethods[i].getName());
		}
		for (String m : _methods)
		{
			Debug.assertSG(sv.contains(m), m);
		}
	}

	public static void main(String[] s)
	{
		trace("false");
	}

}
