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

package org.apache.cxf.bus.spring;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.sax.SAXSource;

import org.w3c.dom.Document;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.sun.xml.fastinfoset.stax.StAXDocumentParser;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.springframework.beans.factory.xml.DefaultDocumentLoader;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * A Spring DocumentLoader that uses WoodStox when we are not validating to speed up the process. 
 */
class TunedDocumentLoader extends DefaultDocumentLoader {
    private static final Logger LOG = LogUtils.getL7dLogger(TunedDocumentLoader.class); 
    
    private static boolean hasFastInfoSet;
    
    static {
        try { 
            ClassLoaderUtils
                .loadClass("com.sun.xml.fastinfoset.stax.StAXDocumentParser", 
                           TunedDocumentLoader.class); 
            hasFastInfoSet = true;
        } catch (Throwable e) { 
            LOG.fine("FastInfoset not found on classpath. Disabling context load optimizations.");
            hasFastInfoSet = false;
        } 
    }
    private SAXParserFactory saxParserFactory;
    private SAXParserFactory nsasaxParserFactory;
    
    TunedDocumentLoader() {
        try {
            Class<?> cls = ClassLoaderUtils.loadClass("com.ctc.wstx.sax.WstxSAXParserFactory",
                                                      TunedDocumentLoader.class);
            saxParserFactory = (SAXParserFactory)cls.newInstance();
            nsasaxParserFactory = (SAXParserFactory)cls.newInstance();
        } catch (Throwable e) {
            //woodstox not found, use any other Stax parser
            saxParserFactory = SAXParserFactory.newInstance();
            nsasaxParserFactory = SAXParserFactory.newInstance();
        }

        try {
            nsasaxParserFactory.setFeature("http://xml.org/sax/features/namespaces", true); 
            nsasaxParserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", 
                                           true);
        } catch (Throwable e) {
            //ignore
        }
        
    }
    
    public static boolean hasFastInfoSet() {
        return hasFastInfoSet;
    }

    @Override
    public Document loadDocument(InputSource inputSource, EntityResolver entityResolver,
                                 ErrorHandler errorHandler, int validationMode, boolean namespaceAware)
        throws Exception {
        if (validationMode == XmlBeanDefinitionReader.VALIDATION_NONE) {
            SAXParserFactory parserFactory = 
                namespaceAware ? nsasaxParserFactory : saxParserFactory;
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setEntityResolver(entityResolver);
            reader.setErrorHandler(errorHandler);
            SAXSource saxSource = new SAXSource(reader, inputSource);
            W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
            StaxUtils.copy(saxSource, writer);
            return writer.getDocument();
        } else {
            return super.loadDocument(inputSource, entityResolver, errorHandler, validationMode,
                                      namespaceAware);
        }
    }

    @Override
    protected DocumentBuilderFactory createDocumentBuilderFactory(int validationMode, boolean namespaceAware)
        throws ParserConfigurationException {
        DocumentBuilderFactory factory = super.createDocumentBuilderFactory(validationMode, namespaceAware);
        try {
            factory.setFeature("http://apache.org/xml/features/dom/defer-node-expansion", false);
        } catch (Throwable e) {
            // we can get all kinds of exceptions from this
            // due to old copies of Xerces and whatnot.
        }
        
        return factory;
    }
    
    static Document loadFastinfosetDocument(URL url) 
        throws IOException, ParserConfigurationException, XMLStreamException {
        InputStream is = url.openStream();
        InputStream in = new BufferedInputStream(is);
        XMLStreamReader staxReader = new StAXDocumentParser(in);
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        StaxUtils.copy(staxReader, writer);
        in.close();
        return writer.getDocument();
    }

}
