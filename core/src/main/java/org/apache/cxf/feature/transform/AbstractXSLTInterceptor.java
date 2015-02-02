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


import java.io.InputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.staxutils.StaxUtils;


/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public abstract class AbstractXSLTInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final TransformerFactory TRANSFORM_FACTORIY = TransformerFactory.newInstance();

    private String contextPropertyName;
    private final Templates xsltTemplate;
        
    public AbstractXSLTInterceptor(String phase, Class<?> before, Class<?> after, String xsltPath) {
        super(phase);
        if (before != null) {
            addBefore(before.getName());
        }
        if (after != null) {
            addAfter(after.getName());
        }
        
        try {
            InputStream xsltStream = ClassLoaderUtils.getResourceAsStream(xsltPath, this.getClass());
            if (xsltStream == null) {
                throw new IllegalArgumentException("Cannot load XSLT from path: " + xsltPath);
            }
            Document doc = StaxUtils.read(xsltStream);
            xsltTemplate = TRANSFORM_FACTORIY.newTemplates(new DOMSource(doc));
        } catch (TransformerConfigurationException e) {
            throw new IllegalArgumentException(
                                               String.format("Cannot create XSLT template from path: %s, error: ",
                                                             xsltPath, e.getException()), e);
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException(
                                               String.format("Cannot create XSLT template from path: %s, error: ",
                                                             xsltPath, e.getNestedException()), e);
        }        
    }

    public void setContextPropertyName(String propertyName) {
        contextPropertyName = propertyName;
    }

    protected boolean checkContextProperty(Message message) {
        return contextPropertyName != null 
            && !MessageUtils.getContextualBoolean(message, contextPropertyName, false);
    }
    
    protected Templates getXSLTTemplate() {
        return xsltTemplate;
    }
}
