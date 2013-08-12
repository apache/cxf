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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class StaxInInterceptor extends AbstractPhaseInterceptor<Message> {
    
    private static final Logger LOG = LogUtils.getL7dLogger(StaxInInterceptor.class);    

    private static Map<Object, XMLInputFactory> factories = new HashMap<Object, XMLInputFactory>();        

    public StaxInInterceptor() {
        super(Phase.POST_STREAM);
    }
    public StaxInInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(Message message) {
        if (isGET(message) || message.getContent(XMLStreamReader.class) != null) {
            LOG.fine("StaxInInterceptor skipped.");
            return;
        }
        InputStream is = message.getContent(InputStream.class);
        Reader reader = null;
        if (is == null) {
            reader = message.getContent(Reader.class);
            if (reader == null) {
                return;
            }
        }
        String contentType = (String)message.get(Message.CONTENT_TYPE);
        
        if (contentType != null && contentType.contains("text/html")) {
            String htmlMessage = null;
            try {
                htmlMessage = IOUtils.toString(is, 500);
            } catch (IOException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("INVALID_HTML_RESPONSETYPE",
                        LOG, "(none)"));
            }
            throw new Fault(new org.apache.cxf.common.i18n.Message("INVALID_HTML_RESPONSETYPE",
                    LOG, (htmlMessage == null || htmlMessage.length() == 0) ? "(none)" : htmlMessage));
        }
        if (contentType == null) {
            //if contentType is null, this is likely a an empty post/put/delete/similar, lets see if it's
            //detectable at all
            Map<String, List<String>> m = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
            if (m != null) {
                List<String> contentLen = HttpHeaderHelper
                    .getHeader(m, HttpHeaderHelper.CONTENT_LENGTH);
                List<String> contentTE = HttpHeaderHelper
                    .getHeader(m, HttpHeaderHelper.CONTENT_TRANSFER_ENCODING);
                if ((StringUtils.isEmpty(contentLen) || "0".equals(contentLen.get(0)))
                    && StringUtils.isEmpty(contentTE)) {
                    return;
                }
            }
        }

        String encoding = (String)message.get(Message.ENCODING);

        XMLStreamReader xreader;
        try {
            XMLInputFactory factory = getXMLInputFactory(message);
            if (factory == null) {
                if (reader != null) {
                    xreader = StaxUtils.createXMLStreamReader(reader);
                } else {
                    xreader = StaxUtils.createXMLStreamReader(is, encoding);
                }
            } else {
                synchronized (factory) {
                    if (reader != null) {
                        xreader = factory.createXMLStreamReader(reader);
                    } else {
                        xreader = factory.createXMLStreamReader(is, encoding);
                    }
                }                
            }
            xreader = configureRestrictions(xreader, message);
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STREAM_CREATE_EXC",
                                                                   LOG,
                                                                   encoding), e);
        }
        message.setContent(XMLStreamReader.class, xreader);
        message.getInterceptorChain().add(StaxInEndingInterceptor.INSTANCE);
    }

    private XMLStreamReader configureRestrictions(XMLStreamReader xreader, Message message) throws XMLStreamException {
        Integer maxChildElements = getInteger(message, StaxUtils.MAX_CHILD_ELEMENTS);
        Integer maxElementDepth = getInteger(message, StaxUtils.MAX_ELEMENT_DEPTH);
        Integer maxAttributeCount = getInteger(message, StaxUtils.MAX_ATTRIBUTE_COUNT); 
        Integer maxAttributeSize = getInteger(message, StaxUtils.MAX_ATTRIBUTE_SIZE);
        Integer maxTextLength = getInteger(message, StaxUtils.MAX_TEXT_LENGTH); 
        Long maxElementCount = getLong(message, StaxUtils.MAX_ELEMENT_COUNT);
        Long maxXMLCharacters = getLong(message, StaxUtils.MAX_XML_CHARACTERS);
        return StaxUtils.configureReader(xreader, maxChildElements, maxElementDepth,
                                         maxAttributeCount, maxAttributeSize, maxTextLength,
                                         maxElementCount, maxXMLCharacters);
    }
    private Long getLong(Message message, String key) {
        Object o = message.getContextualProperty(key);
        if (o instanceof Long) {
            return (Long)o;
        } else if (o instanceof Number) {
            return ((Number)o).longValue();
        } else if (o instanceof String) {
            return Long.valueOf(o.toString());
        }
        return null;
    }
    private Integer getInteger(Message message, String key) {
        Object o = message.getContextualProperty(key);
        if (o instanceof Integer) {
            return (Integer)o;
        } else if (o instanceof Number) {
            return ((Number)o).intValue();
        } else if (o instanceof String) {
            return Integer.valueOf((String)o);
        }
        return null;
    }
    public static XMLInputFactory getXMLInputFactory(Message m) throws Fault {
        Object o = m.getContextualProperty(XMLInputFactory.class.getName());
        if (o instanceof XMLInputFactory) {
            return (XMLInputFactory)o;
        } else if (o != null) {
            XMLInputFactory xif = factories.get(o);
            if (xif == null) {
                Class<?> cls;
                if (o instanceof Class) {
                    cls = (Class<?>)o;
                } else if (o instanceof String) {
                    try {
                        cls = ClassLoaderUtils.loadClass((String)o, StaxInInterceptor.class);
                    } catch (ClassNotFoundException e) {
                        throw new Fault(e);
                    }
                } else {
                    throw new Fault(
                                    new org.apache.cxf.common.i18n.Message("INVALID_INPUT_FACTORY", 
                                                                           LOG, o));
                }

                try {
                    xif = (XMLInputFactory)(cls.newInstance());
                    factories.put(o, xif);
                } catch (InstantiationException e) {
                    throw new Fault(e);
                } catch (IllegalAccessException e) {
                    throw new Fault(e);
                }
            }
            return xif;
        } 
        return null;
    }
}
