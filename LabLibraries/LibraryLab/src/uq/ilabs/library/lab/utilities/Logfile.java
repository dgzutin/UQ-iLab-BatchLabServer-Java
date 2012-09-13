package uq.ilabs.library.lab.utilities;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author payne
 */
public class Logfile {

    private static final String STR_ClassName = Logfile.class.getName();

    /*
     * String constants
     */
    private static final String STR_DefaultFilename = "Logfile.log";
    public static final String STRLOG_Newline = System.getProperty("line.separator");
    public static final String STRLOG_Spacer = "  ";
    public static final String STRLOG_Quote = "'";
    /*
     * String constants for logfile messages
     */
    private static final String STRLOG_LogfileName_arg = "Logfile Name: %s";
    private static final String STRLOG_LoggingLevel_arg = "Logging Level: %s";
    private static final String STRLOG_Called = "(): Called";
    private static final String STRLOG_CalledMarker = " >> ";
    private static final String STRLOG_Completed = "(): Completed";
    private static final String STRLOG_CompletedMarker = " << ";
    /*
     * Local variables
     */
    private static FileHandler loggerFileHandler = null;
//    private static final Logger logger = Logger.getLogger(STR_ClassName);
    private static final Logger logger = Logger.getAnonymousLogger();
    private static String absoluteFilename;

    /**
     * Create a logger to write log information to the specified filename. The filename may be specified with a relative
     * or absolute path.
     *
     * @param filename Filename for the log file.
     */
    public static Logger CreateLogger(String filename) {
        try {
            /*
             * Get the absolute path for the specified filename
             */
            File file = new File(filename);
            absoluteFilename = file.getAbsolutePath();

            /*
             * Send logger output to a file handler
             */
            loggerFileHandler = new FileHandler(filename);
            loggerFileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(loggerFileHandler);
            logger.setLevel(Level.ALL);

            /*
             * Log information
             */
            String logMessage = STRLOG_Newline
                    + String.format(STRLOG_LogfileName_arg, absoluteFilename) + STRLOG_Newline
                    + String.format(STRLOG_LoggingLevel_arg, logger.getLevel().getName());
            logger.info(logMessage);
        } catch (IOException | SecurityException ex) {
            logger.log(Level.SEVERE, null, ex);
        }

        return logger;
    }

    /**
     * Find the logger that has already been created with a given filename. If the logger has not been created, a new
     * logger is created with a default filename.
     *
     * @return Logger
     */
    public static Logger GetLogger() {
        if (loggerFileHandler == null) {
            CreateLogger(STR_DefaultFilename);
        }

        return logger;
    }

    public static String Write() {
        return Write("");
    }

    public static String Write(String message) {
        return Write(Level.INFO, message);
    }

    public static String Write(Level level, String message) {
        String logMessage = message;
        log(level, "", null, logMessage);

        return logMessage;
    }

    public static String WriteError(String message) {
        log(Level.SEVERE, "", null, message);
        return message;
    }

    public static void WriteException(String sourceClass, String sourceMethod, Throwable thrown) {
        logger.throwing(sourceClass, sourceMethod, thrown);
    }

    public static String WriteCalled(String sourceClass, String sourceMethod) {
        return WriteCalled(Level.INFO, sourceClass, sourceMethod);
    }

    public static String WriteCalled(String sourceClass, String sourceMethod, String message) {
        return WriteCalled(Level.INFO, sourceClass, sourceMethod, message);
    }

    public static String WriteCalled(Level level, String sourceClass, String sourceMethod) {
        return WriteCalled(level, sourceClass, sourceMethod, "");
    }

    public static String WriteCalled(Level level, String sourceClass, String sourceMethod, String message) {
        String logMessage = null;

        /*
         * If the source method exists then add it to the log
         */
        if (sourceMethod != null && sourceMethod.length() > 0) {
            logMessage = sourceMethod + STRLOG_Called;
        }

        /*
         * If the source class exists then prepend it to the log
         */
        if (sourceClass != null && sourceClass.length() > 0 && logMessage != null) {
            logMessage = sourceClass + "." + logMessage;
        }

        /*
         * Check that the class name and/or method name have been specified
         */
        if (logMessage != null) {
            /*
             * If the message exists then add it to the log
             */
            if (message != null && message.length() > 0) {
                logMessage += STRLOG_Newline + STRLOG_CalledMarker + message;
            }
            log(level, "", null, logMessage);
        }

        return logMessage;
    }

    public static String WriteCompleted(String sourceClass, String sourceMethod) {
        return WriteCompleted(Level.INFO, sourceClass, sourceMethod, "");
    }

    public static String WriteCompleted(String sourceClass, String sourceMethod, String message) {
        return WriteCompleted(Level.INFO, sourceClass, sourceMethod, message);
    }

    public static String WriteCompleted(Level level, String sourceClass, String sourceMethod) {
        return WriteCompleted(level, sourceClass, sourceMethod, "");
    }

    public static String WriteCompleted(Level level, String sourceClass, String sourceMethod, String message) {
        String logMessage = null;

        /*
         * If the source method exists then add it to the log
         */
        if (sourceMethod != null && sourceMethod.length() > 0) {
            logMessage = sourceMethod + STRLOG_Completed;
        }

        /*
         * If the source class exists then prepend it to the log
         */
        if (sourceClass != null && sourceClass.length() > 0 && logMessage != null) {
            logMessage = sourceClass + "." + logMessage;
        }

        /*
         * Check that the class name and/or method name have been specified
         */
        if (logMessage != null) {
            /*
             * If the message exists then add it to the log
             */
            if (message != null && message.length() > 0) {
                logMessage += STRLOG_Newline + STRLOG_CompletedMarker + message;
            }
            log(level, "", null, logMessage);
        }

        return logMessage;
    }

    /**
     *
     * @param level
     * @param sourceClass
     * @param sourceMethod
     * @param msg
     */
    private static synchronized void log(Level level, String sourceClass, String sourceMethod, String msg) {
        logger.logp(level, sourceClass, sourceMethod, msg);
    }
}
