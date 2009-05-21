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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
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
        if (isGET(message) || message.getContentFormats().contains(XMLStreamReader.class)) {
            LOG.fine("StaxInInterceptor skipped.");
            return;
        }
        InputStream is = message.getContent(InputStream.class);
        assert is != null;

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
        
        String encoding = (String)message.get(Message.ENCODING);

        XMLStreamReader reader;
        try {
            XMLInputFactory factory = getXMLInputFactory(message);
            synchronized (factory) {
                reader = factory.createXMLStreamReader(is, encoding);
            }
        } catch (XMLStreamException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STREAM_CREATE_EXC",
                                                                   LOG,
                                                                   encoding), e);
        }

        message.setContent(XMLStreamReader.class, reader);
    }

    public static XMLInputFactory getXMLInputFactory(Message m) throws Fault {
        Object o = m.getContextualProperty(XMLInputFactory.class.getName());
        if (o instanceof XMLInputFactory) {
            return (XMLInputFactory)o;
        } else if (o != null) {
            XMLInputFactory xif = (XMLInputFactory)factories.get(o);
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
        } else {
            return StaxUtils.getXMLInputFactory();
        }
    }
}
