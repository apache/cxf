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

import java.util.logging.Handler;

import org.apache.cxf.management.web.logging.LogRecord;


public final class AtomPullHandler extends Handler {

    private AtomPullServer engine;
    /**
     * Creates handler using (package private).
     * 
     * @param engine configured engine.
     */
    AtomPullHandler(AtomPullServer engine) {
        this.engine = engine;
    }

    @Override
    public void publish(java.util.logging.LogRecord record) {
        if (LoggingThread.isSilent()) {
            return;
        }
        LoggingThread.markSilent(true);
        try {
            LogRecord rec = LogRecord.fromJUL(record);
            engine.publish(rec);
        } finally {
            LoggingThread.markSilent(false);
        }
    }

    @Override
    public synchronized void close() throws SecurityException {
        engine.close();
    }

    @Override
    public synchronized void flush() {
        // no-op
    }
}
