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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.staxutils.PrettyPrintXMLStreamWriter;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * A simple logging handler which outputs the bytes of the message to the
 * Logger.
 */
public abstract class AbstractLoggingInterceptor extends AbstractPhaseInterceptor<Message> {
    
    /**
    *   
    * {@linkplain StringBuilder} writer for use with {@linkplain XMLStreamWriter}.
    * 
    */
    
    static class ClosableStringBuilderWriter extends Writer {
        private boolean ignoreWrites;
        private final StringBuilder buffer;

        public ClosableStringBuilderWriter(StringBuilder buffer) {
            this.buffer = buffer;
        }

        public void write(char[] chars, int off, int len) throws IOException {
            if (!ignoreWrites) {
                this.buffer.append(chars, off, len);
            }
        }

        public void flush() {

        }
        
        public void ignoreWrites() {
            ignoreWrites = true;
        }

        public void close()  {
            // XMLStreamWrite.close() specifies that underlying resource not be closed, 
            // so this should never be called
        }
    }
    
    /**
     * {@linkplain StringBuffer} reader for use with {@linkplain XMLStreamReader}.
     *
     */
    
    static class StringBufferReader extends Reader {

        private final StringBuffer buffer;
        private final int contentLength;
        private int count;
        
        public StringBufferReader(StringBuffer buffer) {
            this.buffer = buffer;
            this.contentLength = buffer.length();
        }
        
        @Override
        public int read(char[] cbuf, int offset, int length) throws IOException {
            if (count >= contentLength) {
                return -1;
            }
            
            int n = Math.min(contentLength - count, length);
            buffer.getChars(count, count + n, cbuf, offset);
            count += n;
            return n;
        }

        @Override
        public void close() throws IOException {
            // XMLStreamReader.close() specifies that underlying resource not be closed, 
            // so this should never be called
        }
    }
    
    
    public static final int DEFAULT_LIMIT = 48 * 1024;
    protected static final String BINARY_CONTENT_MESSAGE = "--- Binary Content ---";
    protected static final String MULTIPART_CONTENT_MESSAGE = "--- Multipart Content ---";
    private static final String MULTIPART_CONTENT_MEDIA_TYPE = "multipart";
    private static final List<String> BINARY_CONTENT_MEDIA_TYPES;
    static {
        BINARY_CONTENT_MEDIA_TYPES = new ArrayList<String>();
        BINARY_CONTENT_MEDIA_TYPES.add("application/octet-stream");
        BINARY_CONTENT_MEDIA_TYPES.add("image/png");
        BINARY_CONTENT_MEDIA_TYPES.add("image/jpeg");
        BINARY_CONTENT_MEDIA_TYPES.add("image/gif");
    }
    
    protected int limit = DEFAULT_LIMIT;
    protected long threshold = -1;
    protected PrintWriter writer;
    protected boolean prettyLogging;
    private boolean showBinaryContent;
    private boolean showMultipartContent = true;
    
    public AbstractLoggingInterceptor(String phase) {
        super(phase);
    }
    public AbstractLoggingInterceptor(String id, String phase) {
        super(id, phase);
    }
    
    protected abstract Logger getLogger();
    
    Logger getMessageLogger(Message message) {
        Endpoint ep = message.getExchange().getEndpoint();
        if (ep == null || ep.getEndpointInfo() == null) {
            return getLogger();
        }
        EndpointInfo endpoint = ep.getEndpointInfo();
        if (endpoint.getService() == null) {
            return getLogger();
        }
        Logger logger = endpoint.getProperty("MessageLogger", Logger.class);
        if (logger == null) {
            String serviceName = endpoint.getService().getName().getLocalPart();
            InterfaceInfo iface = endpoint.getService().getInterface();
            String portName = endpoint.getName().getLocalPart();
            String portTypeName = iface.getName().getLocalPart();
            String logName = "org.apache.cxf.services." + serviceName + "." 
                + portName + "." + portTypeName;
            logger = LogUtils.getL7dLogger(this.getClass(), null, logName);
            endpoint.setProperty("MessageLogger", logger);
        }
        return logger;
    }

    public void setOutputLocation(String s) {
        if (s == null || "<logger>".equals(s)) {
            writer = null;
        } else if ("<stdout>".equals(s)) {
            writer = new PrintWriter(System.out, true);
        } else if ("<stderr>".equals(s)) {
            writer = new PrintWriter(System.err, true);  
        } else {
            try {
                URI uri = new URI(s);
                File file = new File(uri);
                writer = new PrintWriter(new FileWriter(file, true), true);
            } catch (Exception ex) {
                getLogger().log(Level.WARNING, "Error configuring log location " + s, ex);
            }
        }
    }
    
    public void setPrintWriter(PrintWriter w) {
        writer = w;
    }
    
    public PrintWriter getPrintWriter() {
        return writer;
    }
    
    public void setLimit(int lim) {
        limit = lim;
    }
    
    public int getLimit() {
        return limit;
    }
    
    public void setPrettyLogging(boolean flag) {
        prettyLogging = flag;
    }
    
    public boolean isPrettyLogging() {
        return prettyLogging;
    }

    public void setInMemThreshold(long t) {
        threshold = t;
    }

    public long getInMemThreshold() {
        return threshold;
    }
    
    /**
     * Write payload as characters. 
     * 
     * @param builder output 
     * @param cos cached content as bytes
     * @param encoding character encoding
     * @param contentType content type 
     * @throws Exception the default implementation will only throw {@linkplain IOException}}
     */

    protected void writePayload(StringBuilder builder, CachedOutputStream cos,
                                String encoding, String contentType) throws Exception  {
        // Pretty-print the XML message when the CachedOutputStream has content
        if (isPrettyLogging() && (contentType != null && contentType.indexOf("xml") >= 0 
            && contentType.toLowerCase().indexOf("multipart/related") < 0) && cos.size() > 0) {

            // This implementation does not enforce the limit directly in the pretty-printing operation,
            // so somewhat more work than necessary might be carried out. However in return the distinction 
            // between valid and invalid XML can always be made.

            // Save input size so we can clear or limit the output
            int inputBuilderLength = builder.length();

            // Roughly estimate pretty-printed message to twice the size of raw XML size
            builder.ensureCapacity(inputBuilderLength + (int)(cos.size() * 2));
            
            // Write directly to the output builder
            ClosableStringBuilderWriter swriter = new ClosableStringBuilderWriter(builder);

            XMLStreamWriter xwriter = new PrettyPrintXMLStreamWriter(StaxUtils.createXMLStreamWriter(swriter), 2);
            
            InputStream in = cos.getInputStream();
            
            // Passing the InputStream to the readers is optimal performance-wise as some readers
            // work on the byte stream directly and so will be slowed down if a InputStreamReader
            // was used as a wrapper.

            XMLStreamReader xreader;
            if (StringUtils.isEmpty(encoding)) {
                xreader = StaxUtils.createXMLStreamReader(in);
            } else {
                xreader = StaxUtils.createXMLStreamReader(in, encoding);
            }
                        
            try {
                StaxUtils.copy(xreader, xwriter);
                
                xwriter.flush();
                
                // Check number of characters added compared to input size
                if (limit != -1 && builder.length() - inputBuilderLength > limit) {
                    // truncate content
                    builder.setLength(inputBuilderLength + limit);
                }
            } catch (XMLStreamException xse) {
                // Reset builder to original size, discarding whatever was written
                builder.setLength(inputBuilderLength);
                
                // Fall back to capture as raw XML
                if (StringUtils.isEmpty(encoding)) {
                    cos.writeCacheTo(builder, limit);
                } else {
                    cos.writeCacheTo(builder, encoding, limit);
                }
            } finally {
                // Ensure nothing more is written to the builder
                swriter.ignoreWrites();
                
                // Free up resources
                try {
                    xwriter.close();
                } catch (XMLStreamException xse2) {
                    //ignore
                }
                try {
                    xreader.close();
                } catch (XMLStreamException xse2) {
                    //ignore
                }
                try {
                    in.close();
                } catch (IOException e) {
                    //ignore
                }
            }
        } else {
            if (StringUtils.isEmpty(encoding)) {
                cos.writeCacheTo(builder, limit);
            } else {
                cos.writeCacheTo(builder, encoding, limit);
            }
        }
    }
    protected void writePayload(StringBuilder builder, 
                                StringWriter stringWriter,
                                String contentType) 
        throws Exception {
        // Just transform the XML message when the cos has content
        if (isPrettyLogging() 
            && contentType != null 
            && contentType.indexOf("xml") >= 0 
            && stringWriter.getBuffer().length() > 0) {

            // save input size so we can clear or limit the output
            int inputBuilderLength = builder.length();

            // Write directly to the output builder
            ClosableStringBuilderWriter swriter = new ClosableStringBuilderWriter(builder);
            
            XMLStreamWriter xwriter = new PrettyPrintXMLStreamWriter(StaxUtils.createXMLStreamWriter(swriter), 2);
            
            // read directly from the StringWriter buffer
            XMLStreamReader xreader = StaxUtils.createXMLStreamReader(new StringBufferReader(stringWriter.getBuffer()));
            
            try {
                StaxUtils.copy(xreader, xwriter);
                
                xwriter.flush();
                
                // Check number of characters added compared to input size
                if (limit != -1 && builder.length() - inputBuilderLength > limit) {
                    // truncate content
                    builder.setLength(inputBuilderLength + limit);
                }
            } catch (XMLStreamException xse) {
                // Reset builder to original size, discarding whatever was written
                builder.setLength(inputBuilderLength);
                
                // Fall back to capture as raw XML
                StringBuffer buffer = stringWriter.getBuffer();
                if (limit == -1 || buffer.length() < limit) {
                    builder.append(buffer);
                } else {
                    // subsequence so that the underlying implementation uses System.arrayCopy(..)
                    // rather than a for loop - appending with offset + length is not really supported
                    // for StringBuffer on the StringBuilder
                    builder.append(buffer.subSequence(0, limit));
                }
            } finally {
                // Ensure nothing more is written to the builder
                swriter.ignoreWrites();
                
                // Free up resources
                try {
                    xwriter.close();
                } catch (XMLStreamException xse2) {
                    //ignore
                }
                try {
                    xreader.close();
                } catch (XMLStreamException xse2) {
                    //ignore
                }

            }

        } else {
            StringBuffer buffer = stringWriter.getBuffer();
            if (limit == -1 || buffer.length() < limit) {
                builder.append(buffer);
            } else {
                // subsequence so that the underlying implementation uses System.arrayCopy(..)
                // rather than a for loop - appending with offset + length is not really supported
                // for StringBuffer on the StringBuilder
                builder.append(buffer.subSequence(0, limit));
            }
        }
    }


    /**
     * Transform the string before display. The implementation in this class 
     * does nothing. Override this method if you wish to change the contents of the 
     * logged message before it is delivered to the output. 
     * For example, you can use this to mask out sensitive information.
     * @param originalLogString the raw log message.
     * @return transformed data
     */
    protected String transform(String originalLogString) {
        return originalLogString;
    } 

    protected void log(Logger logger, String message) {
        message = transform(message);
        if (writer != null) {
            writer.println(message);
            // Flushing the writer to make sure the message is written
            writer.flush();
        } else if (logger.isLoggable(Level.INFO)) {
            LogRecord lr = new LogRecord(Level.INFO, message);
            lr.setSourceClassName(logger.getName());
            lr.setSourceMethodName(null);
            lr.setLoggerName(logger.getName());
            logger.log(lr);
        }
    }
    public void setShowBinaryContent(boolean showBinaryContent) {
        this.showBinaryContent = showBinaryContent;
    }
    public boolean isShowBinaryContent() {
        return showBinaryContent;
    }
    protected boolean isBinaryContent(String contentType) {
        return contentType != null && BINARY_CONTENT_MEDIA_TYPES.contains(contentType);
    }
    public boolean isShowMultipartContent() {
        return showMultipartContent;
    }
    public void setShowMultipartContent(boolean showMultipartContent) {
        this.showMultipartContent = showMultipartContent;
    }
    protected boolean isMultipartContent(String contentType) {
        return contentType != null && contentType.startsWith(MULTIPART_CONTENT_MEDIA_TYPE);
    }
    
}
