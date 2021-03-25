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
package org.apache.cxf.common.logging;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * java.util.logging.Logger implementation delegating to another framework.
 * All methods can be used except:
 *   setLevel
 *   addHandler / getHandlers
 *   setParent / getParent
 *   setUseParentHandlers / getUseParentHandlers
 */
public abstract class AbstractDelegatingLogger extends Logger {

    protected AbstractDelegatingLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }

    @Override
    public void log(LogRecord record) {
        if (isLoggable(record.getLevel())) {
            doLog(record);
        }
    }

    @Override
    public void log(Level level, String msg) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            doLog(lr);
        }
    }

    @Override
    public void log(Level level, String msg, Object param1) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            Object[] params = {param1 };
            lr.setParameters(params);
            doLog(lr);
        }
    }

    @Override
    public void log(Level level, String msg, Object[] params) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            lr.setParameters(params);
            doLog(lr);
        }
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            lr.setThrown(thrown);
            doLog(lr);
        }
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            doLog(lr);
        }
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            Object[] params = {param1 };
            lr.setParameters(params);
            doLog(lr);
        }
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object[] params) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setParameters(params);
            doLog(lr);
        }
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setThrown(thrown);
            doLog(lr);
        }
    }

  
    @Override
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            doLog(lr, bundleName);
        }
    }

   
    @Override
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                      String bundleName, String msg, Object param1) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            Object[] params = {param1 };
            lr.setParameters(params);
            doLog(lr, bundleName);
        }
    }

  
    @Override
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                      String bundleName, String msg, Object[] params) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setParameters(params);
            doLog(lr, bundleName);
        }
    }

    @Override
    @Deprecated
    public void logrb(Level level, String sourceClass, String sourceMethod,
                      String bundleName, String msg, Throwable thrown) {
        if (isLoggable(level)) {
            LogRecord lr = new LogRecord(level, msg);
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setThrown(thrown);
            doLog(lr, bundleName);
        }
    }

    @Override
    public void entering(String sourceClass, String sourceMethod) {
        if (isLoggable(Level.FINER)) {
            logp(Level.FINER, sourceClass, sourceMethod, "ENTRY");
        }
    }

    @Override
    public void entering(String sourceClass, String sourceMethod, Object param1) {
        if (isLoggable(Level.FINER)) {
            Object[] params = {param1 };
            logp(Level.FINER, sourceClass, sourceMethod, "ENTRY {0}", params);
        }
    }

    @Override
    public void entering(String sourceClass, String sourceMethod, Object[] params) {
        if (isLoggable(Level.FINER)) {
            String msg = "ENTRY";
            if (params == null) {
                logp(Level.FINER, sourceClass, sourceMethod, msg);
                return;
            }
            StringBuilder builder = new StringBuilder(msg);
            for (int i = 0; i < params.length; i++) {
                builder.append(" {");
                builder.append(Integer.toString(i));
                builder.append('}');
            }
            logp(Level.FINER, sourceClass, sourceMethod, builder.toString(), params);
        }
    }

    @Override
    public void exiting(String sourceClass, String sourceMethod) {
        if (isLoggable(Level.FINER)) {
            logp(Level.FINER, sourceClass, sourceMethod, "RETURN");
        }
    }

    @Override
    public void exiting(String sourceClass, String sourceMethod, Object result) {
        if (isLoggable(Level.FINER)) {
            Object[] params = {result };
            logp(Level.FINER, sourceClass, sourceMethod, "RETURN {0}", params);
        }
    }

    @Override
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        if (isLoggable(Level.FINER)) {
            LogRecord lr = new LogRecord(Level.FINER, "THROW");
            lr.setSourceClassName(sourceClass);
            lr.setSourceMethodName(sourceMethod);
            lr.setThrown(thrown);
            doLog(lr);
        }
    }

    @Override
    public void severe(String msg) {
        if (isLoggable(Level.SEVERE)) {
            LogRecord lr = new LogRecord(Level.SEVERE, msg);
            doLog(lr);
        }
    }

    @Override
    public void warning(String msg) {
        if (isLoggable(Level.WARNING)) {
            LogRecord lr = new LogRecord(Level.WARNING, msg);
            doLog(lr);
        }
    }

    @Override
    public void info(String msg) {
        if (isLoggable(Level.INFO)) {
            LogRecord lr = new LogRecord(Level.INFO, msg);
            doLog(lr);
        }
    }

    @Override
    public void config(String msg) {
        if (isLoggable(Level.CONFIG)) {
            LogRecord lr = new LogRecord(Level.CONFIG, msg);
            doLog(lr);
        }
    }

    @Override
    public void fine(String msg) {
        if (isLoggable(Level.FINE)) {
            LogRecord lr = new LogRecord(Level.FINE, msg);
            doLog(lr);
        }
    }

    @Override
    public void finer(String msg) {
        if (isLoggable(Level.FINER)) {
            LogRecord lr = new LogRecord(Level.FINER, msg);
            doLog(lr);
        }
    }

    @Override
    public void finest(String msg) {
        if (isLoggable(Level.FINEST)) {
            LogRecord lr = new LogRecord(Level.FINEST, msg);
            doLog(lr);
        }
    }

    @Override
    public void setLevel(Level newLevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public abstract Level getLevel();

    @Override
    public boolean isLoggable(Level level) {
        Level l = getLevel();
        return level.intValue() >= l.intValue() && l != Level.OFF;
    }

    protected boolean supportsHandlers() {
        return false;
    }

    @Override
    public synchronized void addHandler(Handler handler) {
        if (supportsHandlers()) {
            super.addHandler(handler);
            return;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void removeHandler(Handler handler) {
        if (supportsHandlers()) {
            super.removeHandler(handler);
            return;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Handler[] getHandlers() {
        if (supportsHandlers()) {
            return super.getHandlers();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void setUseParentHandlers(boolean useParentHandlers) {
        if (supportsHandlers()) {
            super.setUseParentHandlers(useParentHandlers);
            return;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized boolean getUseParentHandlers() {
        if (supportsHandlers()) {
            return super.getUseParentHandlers();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Logger getParent() {
        return null;
    }

    @Override
    public void setParent(Logger parent) {
        throw new UnsupportedOperationException();
    }

    protected void doLog(LogRecord lr) {
        lr.setLoggerName(getName());
        String rbname = getResourceBundleName();
        if (rbname != null) {
            lr.setResourceBundleName(rbname);
            lr.setResourceBundle(getResourceBundle());
        }
        internalLog(lr);
    }

    protected void doLog(LogRecord lr, String rbname) {
        lr.setLoggerName(getName());
        if (rbname != null) {
            lr.setResourceBundleName(rbname);
            lr.setResourceBundle(loadResourceBundle(rbname));
        }
        internalLog(lr);
    }

    protected void internalLog(LogRecord record) {
        Filter filter = getFilter();
        if (filter != null && !filter.isLoggable(record)) {
            return;
        }
        String msg = formatMessage(record);
        internalLogFormatted(msg, record);
    }

    protected abstract void internalLogFormatted(String msg, LogRecord record);

    protected String formatMessage(LogRecord record) {
        String format = record.getMessage();
        ResourceBundle catalog = record.getResourceBundle();
        if (catalog != null) {
            try {
                format = catalog.getString(record.getMessage());
            } catch (MissingResourceException ex) {
                format = record.getMessage();
            }
        }
        try {
            Object[] parameters = record.getParameters();
            if (parameters == null || parameters.length == 0) {
                return format;
            }
            if (format.indexOf("{0") >= 0 || format.indexOf("{1") >= 0
                        || format.indexOf("{2") >= 0 || format.indexOf("{3") >= 0) {
                return java.text.MessageFormat.format(format, parameters);
            }
            return format;
        } catch (Exception ex) {
            return format;
        }
    }

    /**
     * Load the specified resource bundle
     *
     * @param resourceBundleName
     *            the name of the resource bundle to load, cannot be null
     * @return the loaded resource bundle.
     * @throws java.util.MissingResourceException
     *             If the specified resource bundle can not be loaded.
     */
    static ResourceBundle loadResourceBundle(String resourceBundleName) {
        // try context class loader to load the resource
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (null != cl) {
            try {
                return ResourceBundle.getBundle(resourceBundleName, Locale.getDefault(), cl);
            } catch (MissingResourceException e) {
                // Failed to load using context classloader, ignore
            }
        }
        // try system class loader to load the resource
        cl = ClassLoader.getSystemClassLoader();
        if (null != cl) {
            try {
                return ResourceBundle.getBundle(resourceBundleName, Locale.getDefault(), cl);
            } catch (MissingResourceException e) {
                // Failed to load using system classloader, ignore
            }
        }
        return null;
    }

}