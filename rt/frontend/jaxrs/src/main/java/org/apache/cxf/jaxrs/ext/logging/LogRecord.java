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
package org.apache.cxf.jaxrs.ext.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Log entry serializable to XML. Based on common set of {@link java.util.logging.LogRecord} and
 * {@link org.apache.log4j.spi.LoggingEvent} attributes.
 * <p>
 * LogRecord are never null; if some attributes are not set (e.g. logger name, or rendered cause taken from
 * Throwable) empty strings are returned.
 */
@XmlRootElement
public class LogRecord {

    private Date eventTimestamp = new Date();
    private LogLevel level = LogLevel.INFO;
    private String message = "";
    private String loggerName = "";
    private String threadName = "";
    private String throwable = "";

    /**
     * Creates this object from JUL LogRecord. Most attributes are copied, others are converted as follows:
     * raw {@link java.util.logging.LogRecord#getMessage() message} is formatted with
     * {@link java.util.logging.LogRecord#getParameters() parameters} using {@link MessageFormat}, attached
     * {@link java.util.logging.LogRecord#getThrown() throwable} has full stack trace dumped, and log levels
     * are mapped as specified in {@link LogRecord}.
     * 
     * @param julRecord log record to convert.
     * @return conversion result.
     */
    public static LogRecord fromJUL(java.util.logging.LogRecord julRecord) {
        Validate.notNull(julRecord, "julRecord is null");
        LogRecord record = new LogRecord();
        record.setEventTimestamp(new Date(julRecord.getMillis()));
        record.setLevel(LogLevel.fromJUL(julRecord.getLevel()));
        record.setLoggerName(julRecord.getLoggerName());
        if (julRecord.getThrown() != null) {
            record.setThrowable(julRecord.getThrown());
        }
        if (julRecord.getParameters() != null) {
            record.setMessage(MessageFormat.format(julRecord.getMessage(), julRecord.getParameters()));
        } else {
            record.setMessage(julRecord.getMessage());
        }
        record.setThreadName(Integer.toString(julRecord.getThreadID()));
        return record;
    }

    @XmlElement
    public Date getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(Date eventTimestamp) {
        Validate.notNull(eventTimestamp, "eventTimestamp is null");
        this.eventTimestamp = eventTimestamp;
    }

    @XmlElement
    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        Validate.notNull(level, "level is null");
        this.level = level;
    }

    /**
     * Formatted message with parameters filled in.
     */
    @XmlElement
    public String getMessage() {
        return message;
    }

    public void setMessage(String renderedMessage) {
        Validate.notNull(level, "message is null");
        this.message = renderedMessage;
    }

    @XmlElement
    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        Validate.notNull(level, "loggerName is null");
        this.loggerName = loggerName;
    }

    @XmlElement
    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        Validate.notNull(level, "threadName is null");
        this.threadName = threadName;
    }

    /**
     * Full stack trace of {@link Throwable} associated with log record.
     */
    @XmlElement
    public String getThrowable() {
        return throwable;
    }

    public void setThrowable(String throwable) {
        Validate.notNull(throwable, "throwable is null");
        this.throwable = throwable;
    }

    public void setThrowable(Throwable thr) {
        Validate.notNull(thr, "throwable is null");
        StringWriter sw = new StringWriter();
        thr.printStackTrace(new PrintWriter(sw));
        this.throwable = sw.getBuffer().toString();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(obj, this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
