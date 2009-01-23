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

package org.apache.cxf.interceptor;


import java.io.OutputStream;
import java.io.PrintWriter;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;


/**
 *
 */
public class PrettyLoggingOutInterceptor extends AbstractPhaseInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(PrettyLoggingOutInterceptor.class);
    private PrintWriter writer;


    public PrettyLoggingOutInterceptor(PrintWriter w) {
        super(Phase.PRE_STREAM);
        addBefore(StaxOutInterceptor.class.getName());
        writer = w;
    }

    public void handleMessage(Message message) throws Fault {
        final OutputStream os = message.getContent(OutputStream.class);
        if (os == null) {
            return;
        }
        if (!LOG.isLoggable(Level.ALL)) {
            return;
        }

//     Write the output while caching it for the log message
        final CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
        message.setContent(OutputStream.class, newOut);
        newOut.registerCallback(new LoggingCallback());
    }

    public class LoggingCallback implements CachedOutputStreamCallback {

        public void onFlush(CachedOutputStream cos) {

        }

        public void onClose(CachedOutputStream cos) {
 
            try {
                DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
                DOMImplementationLS domLS = (DOMImplementationLS) registry.getDOMImplementation("LS");
                LSParser lsParser = domLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);

                LSInput lsInput = domLS.createLSInput();
                lsInput.setByteStream(cos.getInputStream());
                org.w3c.dom.Document doc = lsParser.parse(lsInput);

                LSSerializer lsSerializer = domLS.createLSSerializer();
                DOMConfiguration config = lsSerializer.getDomConfig();
                config.setParameter("format-pretty-print", true);
                            
                String prettyStr = lsSerializer.writeToString(doc.getDocumentElement());
                if (writer != null) {
                    writer.println(prettyStr);
                    writer.close();
                } else if (LOG.isLoggable(Level.INFO)) {
                    System.out.println("writer is null " + prettyStr);
                    LOG.info(prettyStr);
                }
         
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}