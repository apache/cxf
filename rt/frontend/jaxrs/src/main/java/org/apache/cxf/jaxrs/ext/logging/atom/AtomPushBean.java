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
package org.apache.cxf.jaxrs.ext.logging.atom;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.commons.lang.Validate;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.logging.LogLevel;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.Converter;
import org.apache.cxf.jaxrs.ext.logging.atom.deliverer.Deliverer;

/**
 * Bean used to configure {@link AtomPushHandler JUL handler} with Spring instead of properties file. See
 * {@link AtomPushHandler} class for detailed description of parameters. Next to configuration of handler,
 * Spring bean offers simple configuration of associated loggers that share ATOM push-style handler.
 * <p>
 * General rules:
 * <ul>
 * <li>When {@link #setDeliverer(Deliverer) deliverer} property is not set explicitly, URL must be set to
 * create default deliverer.</li>
 * <li>When {@link #setConverter(Converter) converter} property is not set explicitly, default converter is
 * created.</li>
 * <li>When {@link #setLoggers(String) loggers} property is used, it overrides pair of
 * {@link #setLogger(String) logger} and {@link #setLevel(String) level} properties; and vice versa.</li>
 * <li>When logger is not set, handler is attached to root logger (named ""); when level is not set for
 * logger, default "INFO" level is used.</li>
 * <li>When {@link #setBatchSize(String) batchSize} property is not set or set to wrong value, default batch
 * size of "1" is used.</li>
 * <li>When deliverer property is NOT set, use of "retryXxx" properties causes creation of retrying default
 * deliverer.</li>
 * </ul>
 * Examples:
 * <p>
 * ATOM push handler with registered with root logger for all levels or log events, pushing one feed per event
 * to specified URL, using default delivery and conversion methods:
 * 
 * <pre>
 *   &lt;bean class=&quot;org.apache.cxf.jaxrs.ext.logging.atom.AtomPushBean&quot; 
 *     init-method=&quot;init&quot;&gt;
 *       &lt;property name=&quot;url&quot; value=&quot;http://localhost:9080/feed&quot;/&gt;
 *       &lt;property name=&quot;level&quot; value=&quot;ALL&quot; /&gt;
 *   &lt;/bean&gt;
 * </pre>
 * 
 * ATOM push handler registered with multiple loggers and listening for different levels (see
 * {@link #setLoggers(String) loggers} property description for syntax details). Custom deliverer will take
 * care of feeds, each of which carries batch of 10 log events:
 * 
 * <pre>
 *   &lt;bean id=&quot;soapDeliverer&quot; ...
 *   ...
 *   &lt;bean class=&quot;org.apache.cxf.jaxrs.ext.logging.atom.AtomPushBean&quot; 
 *     init-method=&quot;init&quot;&gt;
 *       &lt;property name=&quot;deliverer&quot;&gt;
 *           &lt;ref bean=&quot;soapDeliverer&quot;/&gt;
 *       &lt;/property&gt;
 *       &lt;property name=&quot;loggers&quot; value=&quot;
 *           org.apache.cxf:DEBUG,
 *           org.apache.cxf.jaxrs,
 *           org.apache.cxf.bus:ERROR&quot; /&gt;
 *       &lt;property name=&quot;batchSize&quot; value=&quot;10&quot; /&gt;
 *   &lt;/bean&gt;
 * </pre>
 */
public final class AtomPushBean {

    private AtomPushEngineConfigurator conf = new AtomPushEngineConfigurator();
    private List<LoggerLevel> loggers = new ArrayList<LoggerLevel>();
    private boolean initialized;

    /**
     * Creates unconfigured and uninitialized bean. To configure setters must be used, then {@link #init()}
     * must be called.
     */
    public AtomPushBean() {
        initSingleLogger();
    }

    private void initSingleLogger() {
        loggers = new ArrayList<LoggerLevel>();
        loggers.add(new LoggerLevel("", "INFO"));
    }

    /**
     * Set URL used when custom deliverer is not set (default deliverer is being created).
     */
    public void setUrl(String url) {
        checkInit();
        Validate.notNull(url, "url is null");
        conf.setUrl(url);
    }

    /**
     * Set initialized deliverer.
     */
    public void setDeliverer(Deliverer deliverer) {
        checkInit();
        Validate.notNull(deliverer, "deliverer is null");
        conf.setDeliverer(deliverer);
    }

    /**
     * Set initialized converter.
     */
    public void setConverter(Converter converter) {
        checkInit();
        Validate.notNull(converter, "converter is null");
        conf.setConverter(converter);
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
     * Size of batch; empty string for default one element batch.
     */
    public void setBatchSize(String batchSize) {
        checkInit();
        Validate.notNull(batchSize, "batchSize is null");
        conf.setBatchSize(batchSize);
    }

    /**
     * Retry pause calculation strategy, either "linear" or "exponential".
     */
    public void setRetryPause(String retryPause) {
        checkInit();
        Validate.notNull(retryPause, "retryPause is null");
        conf.setRetryPause(retryPause);
    }

    /**
     * Retry pause time (in seconds).
     */
    public void setRetryPauseTime(String time) {
        checkInit();
        Validate.notNull(time, "time is null");
        conf.setRetryPauseTime(time);
    }

    /**
     * Retry timeout (in seconds).
     */
    public void setRetryTimeout(String timeout) {
        checkInit();
        Validate.notNull(timeout, "timeout is null");
        conf.setRetryTimeout(timeout);
    }

    /**
     * Conversion output type: "feed" or "entry".
     */
    public void setOutput(String output) {
        checkInit();
        Validate.notNull(output, "output is null");
        conf.setOutput(output);
    }

    /**
     * Multiplicity of subelement of output: "one" or "many".
     */
    public void setMultiplicity(String multiplicity) {
        checkInit();
        Validate.notNull(multiplicity, "multiplicity is null");
        conf.setMultiplicity(multiplicity);
    }

    /**
     * Entry data format: "content" or "extension".
     */
    public void setFormat(String format) {
        checkInit();
        Validate.notNull(format, "format is null");
        conf.setFormat(format);
    }

    /**
     * Initializes bean; creates ATOM push handler based on current properties state, and attaches handler to
     * logger(s).
     */
    public void init() {
        checkInit();
        initialized = true;
        Handler h = new AtomPushHandler(conf.createEngine());
        for (int i = 0; i < loggers.size(); i++) {
            Logger l = LogUtils.getL7dLogger(AtomPushBean.class, null, loggers.get(i).getLogger());
            l.addHandler(h);
            l.setLevel(LogLevel.toJUL(LogLevel.valueOf(loggers.get(i).getLevel())));
        }
    }

    private void checkInit() {
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

    private static class LoggerLevel {
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
