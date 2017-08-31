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

package org.apache.cxf.feature.transform;


import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import sun.awt.CharsetString;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;


/** Class provides XSLT transformation of incoming message.
 * Actually it breaks streaming (can be fixed in further versions when XSLT engine supports XML stream)
 */
public class CharsetAwareXSLTInInterceptor extends AbstractXSLTInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(CharsetAwareXSLTInInterceptor.class);

    public CharsetAwareXSLTInInterceptor(String xsltPath) {
        super(Phase.POST_STREAM, StaxInInterceptor.class, null, xsltPath);
    }

    public CharsetAwareXSLTInInterceptor(String phase, Class<?> before, Class<?> after, String xsltPath) {
        super(phase, before, after, xsltPath);
    }

    @Override
    public void handleMessage(Message message) {
        if (!isRequestor(message) && isGET(message) || checkContextProperty(message)) {
            return;
        }

        String encoding = (String)message.getContextualProperty(Message.ENCODING);
        if (encoding == null) {
            encoding = StandardCharsets.UTF_8.name();
        }

        // 1. Try to get and transform XMLStreamReader message content
        XMLStreamReader xReader = message.getContent(XMLStreamReader.class);
        if (xReader != null) {
            transformXReader(message, xReader, encoding);
        } else {
            // 2. Try to get and transform InputStream message content
            InputStream is = message.getContent(InputStream.class);
            if (is != null) {
                transformIS(message, is, encoding);
            } else {
                // 3. Try to get and transform Reader message content (actually used for JMS TextMessage)
                Reader reader = message.getContent(Reader.class);
                if (reader != null) {
                    transformReader(message, reader); // reader already encapsulates encoding
                }
            }
        }
    }

    protected void transformXReader(Message message, XMLStreamReader xReader, String encoding) {
        CachedOutputStream cachedOS = new CachedOutputStream();
        try {
            StaxUtils.copy(xReader, cachedOS, encoding);
            InputStream transformedIS = XSLTUtils.transform(getXSLTTemplate(), cachedOS.getInputStream(), encoding);
            XMLStreamReader transformedReader = StaxUtils.createXMLStreamReader(transformedIS, encoding);
            message.setContent(XMLStreamReader.class, transformedReader);
        } catch (XMLStreamException e) {
            throw new Fault("STAX_COPY", LOG, e, e.getMessage());
        } catch (IOException e) {
            throw new Fault("GET_CACHED_INPUT_STREAM", LOG, e, e.getMessage());
        } finally {
            try {
                StaxUtils.close(xReader);
            } catch (XMLStreamException ex) {
                throw new Fault(ex);
            }
            try {
                cachedOS.close();
            } catch (IOException e) {
                LOG.warning("Cannot close stream after transformation: " + e.getMessage());
            }
        }
    }
    
    protected void transformIS(Message message, InputStream is, String encoding) {
        try {
            InputStream transformedIS = XSLTUtils.transform(getXSLTTemplate(), is, encoding);
            message.setContent(InputStream.class, transformedIS);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                LOG.warning("Cannot close stream after transformation: " + e.getMessage());
            }
        }
    }

    protected void transformReader(Message message, Reader reader) {
        try {
            Reader transformedReader = XSLTUtils.transform(getXSLTTemplate(), reader); // reader already encapsulates encoding
            message.setContent(Reader.class, transformedReader);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                LOG.warning("Cannot close stream after transformation: " + e.getMessage());
            }
        }
    }
}
