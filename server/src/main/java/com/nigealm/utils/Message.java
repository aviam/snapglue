package com.nigealm.utils;

import com.nigealm.common.utils.Tracer;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Message holds a generic formatted text with arguments.
 */
public final class Message implements Serializable, MessageEntry
{
	private static final long serialVersionUID = 112116181038221545L;

    private final static Tracer tracer = new Tracer(Message.class);

    private static final String DEFAULT_BUNDLE = "ServerMessages";
	
	private String formatKey;
	private Object[] arguments;
	
	/**
	 * Constructor for Message.
	 * @param formatKey Key to the message found in the resource bundle.
	 */
	public Message(String formatKey)
	{
		this.formatKey = formatKey;
	}

	/**
	 * Constructor for Message.
	 * @param formatKey Key to the message found in the resource bundle.
	 * @param arguments Parameters of the message (sub-types of MessageEntry will be treated specially).
	 */
	public Message(String formatKey, Object... arguments)
	{
		this.formatKey = formatKey;
		this.arguments = arguments;
	}
	
	public String toString()
	{
		return getText();
	}
	
	public String getText()
	{	
		return Message.getText(DEFAULT_BUNDLE, Locale.US, formatKey, arguments);
	}
	
    /**
	 * Returns the pattern from resource bundle populated with the arguments.
	 * @param bundle Path to the bundle to use
	 * @param locale Locale of the bundle to choose
	 * @param formatKey Key to the pattern in the bundle
	 * @param arguments Arguments to populate the patter with
	 */
	public static String getText(String bundle, Locale locale, String formatKey, Object[] arguments)
    {
    	if (formatKey==null)
    		return null;
    		
    	ResourceBundle rb = ResourceBundle.getBundle(bundle, locale);
    	
    	String msg = null;
    	try
    	{
    		msg = rb.getString(formatKey);
    	}
    	catch (MissingResourceException e)
    	{
    		tracer.warn("The key " + formatKey + " not found in the bundle " + bundle + " for the locale " + locale);
    	}
    	
   		Object[] args = arguments;
    	if (arguments != null && arguments.length>0)
    	{
    		args = new Object[arguments.length];
	    	for (int i=0; i<args.length; i++)
	    	{
	    		if (arguments[i] == null)
					args[i] = null;
	    		else if (arguments[i] instanceof MessageEntry)
	    		{
					args[i] = ((MessageEntry)arguments[i]).getText();
	    		}
				else if (arguments[i] instanceof Double)
				{
					double x = ((Double)arguments[i]).doubleValue();
					if (x==(int)x)
						args[i] = new Integer((int)x);
					else
						args[i] = arguments[i];
				}
				else if (arguments[i] instanceof Number || arguments[i] instanceof Date)
					args[i] = arguments[i];
				else if (arguments[i] instanceof Locale)
				{
					args[i] = ((Locale)arguments[i]).getDisplayName(locale);
				}
				else if (arguments[i] instanceof Throwable)
				{
					args[i] = getExceptionMessage((Throwable)arguments[i]);
				}
				else
					args[i] = arguments[i];
	    	}
    	}
    	
		MessageFormat mf = new MessageFormat(msg);
		mf.setLocale(locale);
		
		Format[] formats=mf.getFormatsByArgumentIndex();
		if (args!=null && args.length==formats.length)
		{
			for(int i=0; i < args.length; i++)
			{
				if (args[i] instanceof Integer &&
			            formats[i]!=null && formats[i] instanceof DecimalFormat) // to check it is not a choice
				{
				        ((DecimalFormat)formats[i]).setGroupingUsed(false);
			    }
				
				// the default formatting for doubles (unless otherwise specified in the message,
				// will round it at the third fractional digit,
				// so we should increase it (to 10)
				if (args[i] instanceof Double && formats[i]==null)
				{
					NumberFormat f = DecimalFormat.getNumberInstance(locale);
					f.setGroupingUsed(false);
					f.setMaximumFractionDigits(10);
					mf.setFormatByArgumentIndex(i, f);
				}
			}
		}
		return mf.format(args);
    }
    
    /**
     * Retrieves a message describing the argument
     */
	public static String getExceptionMessage(final Throwable argument)
    {
		String result = "";
		Throwable exception = argument;

		if (argument instanceof org.omg.CORBA.SystemException)
		{
			// we cannot present a user-friendly exception for CORBA exceptions (cause is not there);
			// so just print out its name
			return argument.getClass().getName();
		}

		final String tmp = exception.getLocalizedMessage();
		if (tmp != null)
			return result + tmp;

		return result + exception.toString();
    }

    /**
     * Returns the arguments array
     */
    public Object[] getArguments()
    {
    	return arguments;
    }
    
    /**
     * Returns the message format key
     */
    public String getKey()
    {
    	return formatKey;
    }
    
	public static String text(String _key)
	{
		return (new Message(_key)).getText();
	}

	public static String text(String _key, Object... arguments)
	{
		return (new Message(_key, arguments)).getText();
	}

	public static void main(String[] args)
	{
		//example 1
		String example1 = Message.text("example_list_of_arguments", new Object[]{"a", Integer.valueOf(1), Calendar.getInstance().getTime()});
		System.out.println(example1);
		
		String example2 = Message.text("example_message_as_argument", new Object[]{example1});
		System.out.println(example2);

		String example3 = Message.getExceptionMessage(new NullPointerException());
		System.out.println(example3);
		
	}
}
