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
package org.apache.cxf.management.web.logging.atom;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.commons.lang.Validate;
import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.management.web.logging.LogLevel;


public abstract class AbstractAtomBean {

    private List<LoggerLevel> loggers = new ArrayList<LoggerLevel>();
    private boolean initialized;
    private Bus bus;
    
    /**
     * Creates unconfigured and uninitialized bean. To configure setters must be used, then {@link #init()}
     * must be called.
     */
    public AbstractAtomBean() {
        initSingleLogger();
    }

    private void initSingleLogger() {
        loggers = new ArrayList<LoggerLevel>();
        loggers.add(new LoggerLevel("", "INFO"));
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }
    
    public Bus getBus() {
        return bus;
    }

    /**
     * Set one or more loggers and levels descriptor. <br>
     * Parsed input syntax is:
     * 
     * <pre>
     * loggers   := &lt;logger&gt;(&lt;separator&gt;&lt;logger&gt;)*
     * logger    := &lt;name&gt;[&quot;:&quot;&lt;level&gt;]
     * separator := &quot;,&quot; | &quot; &quot; | &quot;\n&quot;
     * </pre>
     * 
     * Examples:
     * <p>
     * Two loggers and two levels: <br>
     * <tt>org.apache.cxf:INFO, org.apache.cxf.jaxrs:DEBUG</tt>
     * <p>
     * Three loggers, first with default "INFO" level: <br>
     * <tt>org.apache.cxf, org.apache.cxf.jaxrs:DEBUG, namedLogger:ERROR</tt><br>
     * <p>
     * One logger with default "INFO" level: <br>
     * <tt>org.apache.cxf</tt><br>
     */
    public void setLoggers(String loggers) {
        checkInit();
        Validate.notNull(loggers, "loggers is null");
        parseLoggers(loggers);
    }

    /**
     * Name of logger to associate with ATOM push handler; empty string for root logger.
     */
    public void setLogger(String logger) {
        checkInit();
        Validate.notNull(logger, "logger is null");
        if (loggers.size() != 1) {
            initSingleLogger();
        }
        loggers.get(0).setLogger(logger);
    }

    /**
     * Name of level that logger will use publishing log events to ATOM push handler; empty string for default
     * "INFO" level.
     */
    public void setLevel(String level) {
        checkInit();
        Validate.notNull(level, "level is null");
        if (loggers.size() != 1) {
            initSingleLogger();
        }
        loggers.get(0).setLevel(level);
    }


    /**
     * Initializes bean; creates ATOM handler based on current properties state, and attaches handler to
     * logger(s).
     */
    public void init() {
        checkInit();
        initialized = true;
        Handler h = createHandler();
        for (int i = 0; i < loggers.size(); i++) {
            Logger l = LogUtils.getL7dLogger(AbstractAtomBean.class, null, loggers.get(i).getLogger());
            l.addHandler(h);
            l.setLevel(LogLevel.toJUL(LogLevel.valueOf(loggers.get(i).getLevel())));
        }
    }
    
    protected abstract Handler createHandler();
    
    protected void checkInit() {
        if (initialized) {
            throw new IllegalStateException("Bean is already initialized");
        }
    }

    private void parseLoggers(String param) {
        loggers = new ArrayList<LoggerLevel>();
        StringTokenizer st1 = new StringTokenizer(param, ", \t\n\r\f");
        while (st1.hasMoreTokens()) {
            String tok = st1.nextToken();
            int idx = tok.indexOf(":");
            if (idx != -1) {
                loggers.add(new LoggerLevel(tok.substring(0, idx), tok.substring(idx + 1, tok.length())));
            } else {
                loggers.add(new LoggerLevel(tok, "INFO"));
            }
        }
    }

    protected List<LoggerLevel> getLoggers() {
        return loggers;
    }

    protected static class LoggerLevel {
        private String logger;
        private String level;

        public LoggerLevel(String logger, String level) {
            this.logger = logger;
            this.level = level;
        }

        public String getLogger() {
            return logger;
        }

        public void setLogger(String logger) {
            this.logger = logger;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

    }
}
