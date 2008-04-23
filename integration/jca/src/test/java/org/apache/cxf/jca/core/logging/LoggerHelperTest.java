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

import java.io.IOException;
import java.io.Writer;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class LoggerHelperTest extends Assert {
    public static final String TEST_LOGGER_NAME = "test.logger";


    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testEnableDisableConsoleLogging() {
        Logger rootLogger = LogUtils.getLogger(this.getClass(), null, "");
        Handler handler;
        
        /*Handler handler = LoggerHelper.getHandler(rootLogger, LoggerHelper.CONSOLE_HANDLER);
        assertNotNull("default console appender is there", handler);*/

        LoggerHelper.enableConsoleLogging();

        handler = LoggerHelper.getHandler(rootLogger, LoggerHelper.CONSOLE_HANDLER);
        assertNotNull("default console appender is not there", handler);

        LoggerHelper.disableConsoleLogging();

        handler = LoggerHelper.getHandler(rootLogger, LoggerHelper.CONSOLE_HANDLER);
        assertNull("Unexpected appender after disable", handler);
    }

    @Test
    public void testSettingLogLevel() {
        LoggerHelper.setRootLoggerName(TEST_LOGGER_NAME);
        LoggerHelper.setLogLevel("INFO");
        assertEquals("incorrect log level", "INFO", LoggerHelper.getLogLevel());
        assertEquals("log level not set on IONA logger", "INFO",
                     LogUtils.getLogger(this.getClass(), null, TEST_LOGGER_NAME)
                         .getLevel().toString());
    }

    @Test
    public void testSetWriter() {
        // setup an dummy writer
        DummyWriter writer = new DummyWriter();
        assertTrue("The DummyWriter init error", !writer.writed);
        LoggerHelper.initializeLoggingOnWriter(writer);
        LoggerHelper.setLogLevel("INFO");
        LoggerHelper.getRootCXFLogger().severe("Test String");
        assertTrue("The DummyWriter didn't be setup", writer.writed);
    }

    class DummyWriter extends Writer {
        boolean writed;
        boolean flushed;
        boolean closed;

        public void write(char[] cbuf, int off, int len) throws IOException {

            writed = true;
        }

        @Override
        public void flush() throws IOException {
            flushed = true;
        }

        @Override
        public void close() throws IOException {
            closed = true;
        }

    }

    
}
