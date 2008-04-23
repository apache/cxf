/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jca.core.logging;

import java.io.Writer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.Log4jLogger;
import org.apache.cxf.common.logging.LogUtils;


public final class LoggerHelper {
    public static final Level DEFAULT_LOG_LEVEL = Level.WARNING;    
    public static final String CONSOLE_HANDLER = "ConsoleHandler";
    public static final String WRITER_HANDLER = "WriterHandler";
    private static String rootLoggerName = "org.apache.cxf";
    private static boolean initComplete;
    private static Level currentLogLevel = Level.WARNING;

    private LoggerHelper() {
        //do nothing here
    }

    public static void initializeLoggingOnWriter(final Writer writer) {
        if (writer != null) {
            if (writer.getClass().getName().startsWith("org.jboss")) {
                // jboss writer will redirect to log4j which will cause an
                // infinite loop if we install an appender over this writer.
                // Continue logging via log4j and ignore this writer.
                LogUtils.setLoggerClass(Log4jLogger.class);
                return;
            }

            Logger cxfLogger = getRootCXFLogger();

            // test if the stream handler were setted
            if (getHandler(cxfLogger, WRITER_HANDLER) == null) {
                final WriterHandler handler = new WriterHandler(writer);
                cxfLogger.addHandler(handler);
            }

        }
    }
    
    public static void deleteLoggingOnWriter() {
        Logger cxfLogger = getRootCXFLogger();
        Handler handler = getHandler(cxfLogger, WRITER_HANDLER);
        
        if (handler != null) {
            cxfLogger.removeHandler(handler);
        }
        enableConsoleLogging();
    }

    // true if log output is already going somewhere
    public static boolean loggerInitialisedOutsideConnector() {       
        final Handler[] handlers = getConsoleLogger().getHandlers(); //NOPMD        
        return handlers != null && handlers.length > 0;
    }

    static Handler getHandler(Logger log, String handlerName) {
        Handler[] handlers = log.getHandlers();
        Handler result = null;
        for (int i = 0; i < handlers.length; i++) {
            if (handlers[i].getClass().getName().endsWith(handlerName)) {
                result = handlers[i];
            }
        }
        return result;
    }

    public static void disableConsoleLogging() {        
        final Handler handler = getHandler(getConsoleLogger(), CONSOLE_HANDLER);  //NOPMD
        getConsoleLogger().removeHandler(handler);  //NOPMD
    }

    public static void enableConsoleLogging() {        
        if (getHandler(getConsoleLogger(), CONSOLE_HANDLER) == null) {  //NOPMD
            final Handler console = new ConsoleHandler();
            getConsoleLogger().addHandler(console);  //NOPMD
        }
    }

    public static void setLogLevel(String logLevel) {
        init();
        try {
            currentLogLevel = Level.parse(logLevel);
        } catch (IllegalArgumentException ex) {
            currentLogLevel = DEFAULT_LOG_LEVEL;
        }
        getRootCXFLogger().setLevel(currentLogLevel);
    }

    public static String getLogLevel() {
        return currentLogLevel.toString();
    }

    public static Logger getRootCXFLogger() {
        return LogUtils.getLogger(LoggerHelper.class, null, getRootLoggerName());
    }
    
    public static Logger getConsoleLogger() {
        return LogUtils.getLogger(LoggerHelper.class, null, "");
    }

    public static void init() {
        if (!initComplete) {
            initComplete = true;
            if (!loggerInitialisedOutsideConnector()) {
                enableConsoleLogging();
            }
        }
    }

    public static String getRootLoggerName() {
        return rootLoggerName;
    }

    public static void setRootLoggerName(String loggerName) {
        LoggerHelper.rootLoggerName = loggerName;
    }
}
