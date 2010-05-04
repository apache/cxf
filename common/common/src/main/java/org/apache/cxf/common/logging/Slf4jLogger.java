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

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * <p>
 * java.util.logging.Logger implementation delegating to SLF4J.
 * </p>
 * <p>
 * Methods {@link java.util.logging.Logger#setParent(Logger)}, {@link java.util.logging.Logger#getParent()},
 * {@link java.util.logging.Logger#setUseParentHandlers(boolean)} and
 * {@link java.util.logging.Logger#getUseParentHandlers()} are not overrriden.
 * </p>
 * <p>
 * Level mapping inspired by {@link org.slf4j.bridge.SLF4JBridgeHandler}:
 * </p>
 * 
 * <pre>
 * FINEST  -&gt; TRACE
 * FINER   -&gt; DEBUG
 * FINE    -&gt; DEBUG
 * CONFIG  -&gt; DEBUG
 * INFO    -&gt; INFO
 * WARN ING -&gt; WARN
 * SEVER   -&gt; ERROR
 * </pre>
 */
public class Slf4jLogger extends AbstractDelegatingLogger {

    private final org.slf4j.Logger logger;

    public Slf4jLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
        logger = org.slf4j.LoggerFactory.getLogger(name);
    }

    @Override
    public Level getLevel() {
        Level level;
        // Verify from the wider (trace) to the narrower (error)
        if (logger.isTraceEnabled()) {
            level = Level.FINER; // FINEST
        } else if (logger.isDebugEnabled()) {
            // map to the lowest between FINER, FINE and CONFIG
            level = Level.FINER;
        } else if (logger.isInfoEnabled()) {
            level = Level.INFO;
        } else if (logger.isWarnEnabled()) {
            level = Level.WARNING;
        } else if (logger.isErrorEnabled()) {
            level = Level.SEVERE;
        } else {
            level = Level.OFF;
        }
        return level;
    }

    @Override
    protected void internalLogFormatted(String msg, LogRecord record) {

        Level level = record.getLevel();
        Throwable t = record.getThrown();

        /*
         * As we can not use a "switch ... case" block but only a "if ... else if ..." block, the order of the
         * comparisons is important. We first try log level FINE then INFO, WARN, FINER, etc
         */
        if (Level.FINE.equals(level)) {
            logger.debug(msg, t);
        } else if (Level.INFO.equals(level)) {
            logger.info(msg, t);
        } else if (Level.WARNING.equals(level)) {
            logger.warn(msg, t);
        } else if (Level.FINER.equals(level)) {
            logger.trace(msg, t);
        } else if (Level.FINEST.equals(level)) {
            logger.trace(msg, t);
        } else if (Level.ALL.equals(level)) {
            // should never occur, all is used to configure java.util.logging
            // but not accessible by the API Logger.xxx() API
            logger.error(msg, t);
        } else if (Level.SEVERE.equals(level)) {
            logger.error(msg, t);
        } else if (Level.CONFIG.equals(level)) {
            logger.debug(msg, t);
        } else if (Level.OFF.equals(level)) {
            // don't log
        }
    }
}
