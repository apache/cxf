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
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * 
 */
public class LoggingOutInterceptor extends AbstractPhaseInterceptor {
    private static final String LOG_SETUP = LoggingOutInterceptor.class.getName() + ".log-setup";
    
    private static final Logger LOG = LogUtils.getL7dLogger(LoggingOutInterceptor.class); 

    private int limit = 100 * 1024;
    private PrintWriter writer;
    private boolean prettyLogging;
    
    public LoggingOutInterceptor(String phase) {
        super(phase);
        addBefore(StaxOutInterceptor.class.getName());
    }
    public LoggingOutInterceptor() {
        this(Phase.PRE_STREAM);
    }    
    public LoggingOutInterceptor(int lim) {
        this();
        limit = lim;
    }

    public LoggingOutInterceptor(PrintWriter w) {
        this();
        this.writer = w;
    }
    
    public void setPrettyLogging(boolean flag) {
        prettyLogging = flag;
    }
    
    public boolean isPrettyLogging() {
        return prettyLogging;
    }
    
    public void setLimit(int lim) {
        limit = lim;
    }
    
    public int getLimit() {
        return limit;
    }

    public void handleMessage(Message message) throws Fault {
        final OutputStream os = message.getContent(OutputStream.class);
        if (os == null) {
            return;
        }

        if (LOG.isLoggable(Level.INFO) || writer != null) {
            // Write the output while caching it for the log message
            boolean hasLogged = message.containsKey(LOG_SETUP);
            if (!hasLogged) {
                message.put(LOG_SETUP, Boolean.TRUE);
                final CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
                message.setContent(OutputStream.class, newOut);
                newOut.registerCallback(new LoggingCallback(message, os));
            }
        }
    }
    
    /**
     * Transform the string before display. The implementation in this class 
     * does nothing. Override this method if you want to change the contents of the 
     * logged message before it is delivered to the output. 
     * For example, you can use this to masking out sensitive information.
     * @param originalLogString the raw log message.
     * @return transformed data
     */
    protected String transform(String originalLogString) {
        return originalLogString;
    }
    
    protected void writePayload(StringBuilder builder, CachedOutputStream cos,
                                String encoding, String contentType)
        throws Exception {
        if (isPrettyLogging() && (contentType != null && contentType.indexOf("xml") >= 0)) {
            Transformer serializer = XMLUtils.newTransformer(2);
            // Setup indenting to "pretty print"
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter swriter = new StringWriter();
            serializer.transform(new StreamSource(cos.getInputStream()), new StreamResult(swriter));
            String result = swriter.toString();
            if (result.length() < limit || limit == -1) {
                builder.append(swriter.toString());
            } else {
                builder.append(swriter.toString().substring(0, limit));
            }

        } else {
            if (StringUtils.isEmpty(encoding)) {
                cos.writeCacheTo(builder, limit);
            } else {
                cos.writeCacheTo(builder, encoding, limit);
            }

        }
    }

    class LoggingCallback implements CachedOutputStreamCallback {
        
        private final Message message;
        private final OutputStream origStream;
        
        public LoggingCallback(final Message msg, final OutputStream os) {
            this.message = msg;
            this.origStream = os;
        }

        public void onFlush(CachedOutputStream cos) {  
            
        }
        
        public void onClose(CachedOutputStream cos) {
            String id = (String)message.getExchange().get(LoggingMessage.ID_KEY);
            if (id == null) {
                id = LoggingMessage.nextId();
                message.getExchange().put(LoggingMessage.ID_KEY, id);
            }
            final LoggingMessage buffer 
                = new LoggingMessage("Outbound Message\n---------------------------",
                                     id);
            
            Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
            if (responseCode != null) {
                buffer.getResponseCode().append(responseCode);
            }
            
            String encoding = (String)message.get(Message.ENCODING);

            if (encoding != null) {
                buffer.getEncoding().append(encoding);
            }            
            
            String address = (String)message.get(Message.ENDPOINT_ADDRESS);
            if (address != null) {
                buffer.getAddress().append(address);
            }
            String ct = (String)message.get(Message.CONTENT_TYPE);
            if (ct != null) {
                buffer.getContentType().append(ct);
            }
            Object headers = message.get(Message.PROTOCOL_HEADERS);
            if (headers != null) {
                buffer.getHeader().append(headers);
            }

            if (cos.getTempFile() == null) {
                //buffer.append("Outbound Message:\n");
                if (cos.size() > limit) {
                    buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
                }
            } else {
                buffer.getMessage().append("Outbound Message (saved to tmp file):\n");
                buffer.getMessage().append("Filename: " + cos.getTempFile().getAbsolutePath() + "\n");
                if (cos.size() > limit) {
                    buffer.getMessage().append("(message truncated to " + limit + " bytes)\n");
                }
            }
            try {
                writePayload(buffer.getPayload(), cos, encoding, ct); 
            } catch (Exception ex) {
                //ignore
            }

            if (writer != null) {
                writer.println(transform(buffer.toString()));
            } else if (LOG.isLoggable(Level.INFO)) {
                LOG.info(transform(buffer.toString()));
            }
            try {
                //empty out the cache
                cos.lockOutputStream();
                cos.resetOut(null, false);
            } catch (Exception ex) {
                //ignore
            }
            message.setContent(OutputStream.class, 
                               origStream);
        }
    }

}
