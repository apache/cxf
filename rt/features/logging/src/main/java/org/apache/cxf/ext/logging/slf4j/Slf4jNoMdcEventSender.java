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
package org.apache.cxf.ext.logging.slf4j;

import org.apache.cxf.ext.logging.event.AbstractPrintLogEventSender;
import org.apache.cxf.ext.logging.event.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jNoMdcEventSender extends AbstractPrintLogEventSender {
    private final String logCategory;

    public Slf4jNoMdcEventSender(String logCategory) {
        this.logCategory = logCategory;
    }

    public Slf4jNoMdcEventSender() {
        this(null);
    }

    @Override
    public void send(LogEvent event) {
        String cat = logCategory != null ? logCategory
            : "org.apache.cxf.services." + event.getPortTypeName().getLocalPart() + "." + event.getType();
        Logger log = LoggerFactory.getLogger(cat);
        
        StringBuilder b = new StringBuilder();
        prepareBuilder(b, event);
        log.info(b.toString());
    }

    
}
