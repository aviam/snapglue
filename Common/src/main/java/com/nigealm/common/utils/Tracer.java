package com.nigealm.common.utils;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.net.URL;


/**
 * Implements tracing functionality
 */
public final class Tracer {
    private static final String DEBUG_OWNER = "com.nigealm";
    private Logger logger;
    private String owner;
    private Level defaultLevel;

    /**
     * Constructs a Tracer for the specified owner class. Normally should be
     * used for initialization of private static final Tracer variables.
     */
    public Tracer(Class owner) {
        this(owner.getName(), Level.INFO);
    }

    /**
     * To be used only by Debug.
     */
    public Tracer() {
        this(DEBUG_OWNER, Level.TRACE);
    }

    /**
     * Constructs a Tracer with the specified name. This constructor should be
     * used only in special cases, e.g. when we inform our customers about
     * specific tracing capabilities.
     *
     * @param name The name of the tracer
     */
    public Tracer(String name, Level defaultLevel) {
        this.owner = name;
        this.logger = Logger.getLogger(name);
        this.defaultLevel = defaultLevel;
        URL fileUrl = getClass().getResource("/log4j.properties");
        PropertyConfigurator.configure(fileUrl);

        // Let verify the log messages
        logger.trace("logger created: " + name + ", " + defaultLevel.toString());
    }

	/**
	 * Traces at the DEBUG level (most verbose)
	 */
	public void trace(Object obj)
	{
		String text = "null";
		if (text != null)
			text = obj.toString();

		trace(text);
	}

	/**
	 * Traces at the DEBUG level (most verbose)
	 */
	public void trace(String text)
	{
		trace(text, new Object[0]);
	}

    /**
     * Traces at the DEBUG level (most verbose)
     */
    public void trace(String text, Object[] params) {
        if (logger.isEnabledFor(Level.TRACE)) {
            // notice that the params array may be actually Double[] not
            // Object[], so we copy it for the Double -> String conversion to
            // work
            Object[] res = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                res[i] = params[i];

                // if the user didn't specify the {i} placeholders in the text,
                // we add them to the end
                if (text.indexOf("{" + i) == -1) {
                    // NOTICE: we perform the Double -> String switch only if
                    // the pattern was not given, otherwise it is the user's
                    // responsibility
                    text = text + " Param_" + i + ": {" + i + "}";
                    if (res[i] instanceof Double)
                        // this is to get the full length of double rather than
                        // trimmed formatted one
                        res[i] = res[i].toString();
                }
            }
            log(text, null);
        }
    }

    private void log(String text, Throwable t) {
        logger.log(owner, defaultLevel, text, t);
    }

    /**
     * Traces at an info level.
     */
    public void info(String text) {
        if (logger.isEnabledFor(Level.INFO))
            log(text, null);
    }

    /**
     * Traces at an WARN level.
     */
    public void warn(String text) {
        if (logger.isEnabledFor(Level.WARN))
            logger.warn(text);
    }

    /**
     * Traces ENTRY to a function
     */
    public void entry(String methodName) {
        if (logger.isEnabledFor(Level.TRACE))
            logger.log(owner, defaultLevel, "entering method: " + methodName, null);
    }

    /**
     * Traces EXIT from a function
     */
    public void exit(String methodName) {
        if (logger.isEnabledFor(Level.TRACE))
            log("exit method: " + methodName, null);
    }

    /**
     * Traces occurrence of an exception at the FINE level
     */
    public void exception(String methodName, Throwable exception) {
        if (logger.isEnabledFor(defaultLevel)) {
            logger.error(methodName + ": Exception of the class " + exception.getClass().getName(), exception);
        }
    }

    /**
     * Returns whether the trace settings allow tracing at DEBUG level (most
     * verbose)
     */
    public boolean isDebugLoggable() {
        return logger.isEnabledFor(Level.DEBUG);
    }

    /**
     * Specifies whether the tracer should trace all (if false - disabled)
     */
    public void setLogging(boolean trace) {
        logger.setLevel(trace ? Level.ALL : Level.OFF);
    }

    /**
     * @return the logger
     */
    public org.apache.log4j.Logger getLogger() {
        return logger;
    }

}
