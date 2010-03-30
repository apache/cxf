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
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;


/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class StaxOutInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String OUTPUT_STREAM_HOLDER = StaxOutInterceptor.class.getName() + ".outputstream";
    public static final String FORCE_START_DOCUMENT = "org.apache.cxf.stax.force-start-document";
    public static final StaxOutEndingInterceptor ENDING = new StaxOutEndingInterceptor();
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(StaxOutInterceptor.class);
    private static Map<Object, XMLOutputFactory> factories = new HashMap<Object, XMLOutputFactory>();

    
    public StaxOutInterceptor() {
        super(Phase.PRE_STREAM);
        addAfter(AttachmentOutInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        OutputStream os = message.getContent(OutputStream.class);
        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        if (os == null || writer != null) {
            return;
        }

        String encoding = getEncoding(message);
        
        try {
            XMLOutputFactory factory = getXMLOutputFactory(message);
            if (factory == null) {
                writer = StaxUtils.createXMLStreamWriter(os, encoding);
            } else {
                synchronized (factory) {
                    writer = factory.createXMLStreamWriter(os, encoding);
                }
            }
            if (Boolean.TRUE.equals(message.getContextualProperty(FORCE_START_DOCUMENT))) {
                writer.writeStartDocument(encoding, "1.0");
                message.removeContent(OutputStream.class);
                message.put(OUTPUT_STREAM_HOLDER, os);
            }
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STREAM_CREATE_EXC", BUNDLE), e);
        }
        message.setContent(XMLStreamWriter.class, writer);

        // Add a final interceptor to write end elements
        message.getInterceptorChain().add(ENDING);
    }
    @Override
    public void handleFault(Message message) {
        super.handleFault(message);
        OutputStream os = (OutputStream)message.get(OUTPUT_STREAM_HOLDER);
        if (os != null) {
            message.setContent(OutputStream.class, os);
        }
    }

    private String getEncoding(Message message) {
        Exchange ex = message.getExchange();
        String encoding = (String)message.get(Message.ENCODING);
        if (encoding == null && ex.getInMessage() != null) {
            encoding = (String) ex.getInMessage().get(Message.ENCODING);
            message.put(Message.ENCODING, encoding);
        }
        
        if (encoding == null) {
            encoding = "UTF-8";
            message.put(Message.ENCODING, encoding);
        }
        return encoding;
    }

    public static XMLOutputFactory getXMLOutputFactory(Message m) throws Fault {
        Object o = m.getContextualProperty(XMLOutputFactory.class.getName());
        if (o instanceof XMLOutputFactory) {
            m.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION,
                        Boolean.TRUE);
            m.put(FORCE_START_DOCUMENT, Boolean.TRUE);
            return (XMLOutputFactory)o;
        } else if (o != null) {
            XMLOutputFactory xif = (XMLOutputFactory)factories.get(o);
            if (xif == null) {
                Class cls;
                if (o instanceof Class) {
                    cls = (Class)o;
                } else if (o instanceof String) {
                    try {
                        cls = ClassLoaderUtils.loadClass((String)o, StaxInInterceptor.class);
                    } catch (ClassNotFoundException e) {
                        throw new Fault(e);
                    }
                } else {
                    throw new Fault(new org.apache.cxf.common.i18n.Message("INVALID_INPUT_FACTORY", 
                                                                           BUNDLE, o));
                }

                try {
                    xif = (XMLOutputFactory)(cls.newInstance());
                    factories.put(o, xif);
                } catch (InstantiationException e) {
                    throw new Fault(e);
                } catch (IllegalAccessException e) {
                    throw new Fault(e);
                }
            }
            m.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION,
                  Boolean.TRUE);
            m.put(FORCE_START_DOCUMENT, Boolean.TRUE);
            return xif;
        }
        return null;
    }
    
    public static class StaxOutEndingInterceptor extends AbstractPhaseInterceptor<Message> {
        public StaxOutEndingInterceptor() {
            super(Phase.PRE_STREAM_ENDING);
            getAfter().add(AttachmentOutInterceptor.AttachmentOutEndingInterceptor.class.getName());
        }

        public void handleMessage(Message message) throws Fault {
            try {
                XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
                if (xtw != null) {
                    xtw.writeEndDocument();
                    xtw.flush();
                    xtw.close();
                }

                OutputStream os = (OutputStream)message.get(OUTPUT_STREAM_HOLDER);
                if (os != null) {
                    message.setContent(OutputStream.class, os);
                }
                message.removeContent(XMLStreamWriter.class);
            } catch (XMLStreamException e) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_WRITE_EXC", BUNDLE), e);
            }
        }

    }    
}
