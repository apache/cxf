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
package org.apache.cxf.management.web.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Log level of {@link LogRecord}. Based on SLF4J being popular facade for loggers like JUL, Log4J, JCL and so
 * on. Severities order is: FATAL > ERROR > WARN > INFO > DEBUG > TRACE.
 * <p>
 * Mapping of levels:
 * <ol>
 * <li>JUL - same as <a href="http://www.slf4j.org/apidocs/org/slf4j/bridge/SLF4JBridgeHandler.html">SLF4J
 * approach</a>.</li>
 * <li>Log4J - levels are identical</li>
 * </ol>
 */
@XmlEnum
public enum LogLevel {
    ALL,
    FATAL,
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE,
    OFF;

    @XmlTransient
    private static Map<Level, LogLevel> fromJul = new HashMap<Level, LogLevel>();

    @XmlTransient
    private static Map<LogLevel, Level> toJul = new HashMap<LogLevel, Level>();

    static {        
        fromJul.put(Level.ALL, LogLevel.ALL);
        fromJul.put(Level.SEVERE, LogLevel.ERROR);
        fromJul.put(Level.WARNING, LogLevel.WARN);
        fromJul.put(Level.INFO, LogLevel.INFO);
        fromJul.put(Level.FINE, LogLevel.DEBUG);
        fromJul.put(Level.FINER, LogLevel.DEBUG);
        fromJul.put(Level.FINEST, LogLevel.TRACE);
        fromJul.put(Level.OFF, LogLevel.OFF);

        toJul.put(LogLevel.ALL, Level.ALL);
        toJul.put(LogLevel.FATAL, Level.SEVERE);
        toJul.put(LogLevel.ERROR, Level.SEVERE);
        toJul.put(LogLevel.WARN, Level.WARNING);
        toJul.put(LogLevel.INFO, Level.INFO);
        toJul.put(LogLevel.DEBUG, Level.FINE);
        toJul.put(LogLevel.TRACE, Level.FINEST);
        toJul.put(LogLevel.OFF, Level.OFF);
    }

    /**
     * Creates this enum from JUL {@link Level}.
     */
    public static LogLevel fromJUL(Level level) {
        return fromJul.get(level);
    }

    /**
     * Creates this JUL {@link Level} from this enum.
     */
    public static Level toJUL(LogLevel level) {
        return toJul.get(level);
    }
}
