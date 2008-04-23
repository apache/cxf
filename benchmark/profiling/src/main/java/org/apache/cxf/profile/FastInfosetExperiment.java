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

package org.apache.cxf.profile;

import com.ctc.wstx.sax.WstxSAXParserFactory;
import com.sun.xml.fastinfoset.dom.DOMDocumentParser;
import com.sun.xml.fastinfoset.sax.SAXDocumentParser;
import com.sun.xml.fastinfoset.sax.SAXDocumentSerializer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;

import org.jvnet.fastinfoset.FastInfosetException;

import org.w3c.dom.Document;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

public class FastInfosetExperiment {
    
    private DocumentBuilder documentBuilder;
    TransformerFactory transformerFactory;
    private File fiFile;
    private final static int iterCount = 10000;
    
    private FastInfosetExperiment() throws ParserConfigurationException {
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        transformerFactory = TransformerFactory.newInstance();
        fiFile = new File("fiTest.fixml");
    }
    
    private void dehydrate(InputStream input, OutputStream output) throws ParserConfigurationException, SAXException, IOException {
        // Create Fast Infoset SAX serializer
        SAXDocumentSerializer saxDocumentSerializer = new SAXDocumentSerializer();
        // Set the output stream
        saxDocumentSerializer.setOutputStream(output);

        // Instantiate JAXP SAX parser factory
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        /* Set parser to be namespace aware
         * Very important to do otherwise invalid FI documents will be
         * created by the SAXDocumentSerializer
         */
        saxParserFactory.setNamespaceAware(true);
        // Instantiate the JAXP SAX parser
        SAXParser saxParser = saxParserFactory.newSAXParser();
        // Set the lexical handler
        saxParser.setProperty("http://xml.org/sax/properties/lexical-handler", saxDocumentSerializer);
        // Parse the XML document and convert to a fast infoset document
        saxParser.parse(input, saxDocumentSerializer);
    }
    
    private void readWithWoodstox() throws SAXException, TransformerConfigurationException, TransformerException, IOException {
        InputStream is = getClass().getResourceAsStream("/META-INF/cxf/cxf.xml");
        WstxSAXParserFactory woodstoxParserFactory;
        woodstoxParserFactory = new WstxSAXParserFactory();
        woodstoxParserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", 
                                         true);
        SAXParser parser = woodstoxParserFactory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        SAXSource saxSource = new SAXSource(reader, new InputSource(is));
        Document document;
        document = documentBuilder.newDocument();
        DOMResult domResult = new DOMResult(document);
        transformerFactory.newTransformer().transform(saxSource, domResult);
        is.close();
    }
    
    private void readWithFI() throws TransformerConfigurationException, TransformerException, IOException {
        InputStream is = new FileInputStream(fiFile);
        XMLReader saxReader = new SAXDocumentParser();
        InputStream in = new BufferedInputStream(is);
        SAXSource saxSource = new SAXSource(saxReader, new InputSource(in));
        Document document;
        document = documentBuilder.newDocument();
        DOMResult domResult = new DOMResult(document);
        transformerFactory.newTransformer().transform(saxSource, domResult);
        is.close();
    }
    
    private void readWithFIDom() throws FastInfosetException, IOException {
        InputStream is = new FileInputStream(fiFile);
        DOMDocumentParser ddp = new DOMDocumentParser();
        Document document;
        document = documentBuilder.newDocument();
        ddp.parse(document, is);
        is.close();
    }
    
    private void benchmark() throws ParserConfigurationException, SAXException, IOException, TransformerConfigurationException, TransformerException, FastInfosetException {
        InputStream is = getClass().getResourceAsStream("/META-INF/cxf/cxf.xml");
        OutputStream os = new FileOutputStream(fiFile);
        dehydrate(is, os);
        is.close();
        os.close();
        
        long totalTime = 0;
        
        for(int x = 0; x < iterCount; x ++) {
            long startTime = System.nanoTime();
            readWithWoodstox();
            long endTime = System.nanoTime();
            totalTime += endTime - startTime;
        }
        
        double averageNanos = totalTime / iterCount;
        System.out.println("Woodstox average us: " + averageNanos / 1000);

        totalTime = 0;
        
        for(int x = 0; x < iterCount; x ++) {
            long startTime = System.nanoTime();
            readWithFI();
            long endTime = System.nanoTime();
            totalTime += endTime - startTime;
        }
        
        averageNanos = totalTime / iterCount;
        System.out.println("FastInfoset average us: " + averageNanos / 1000);
        
        totalTime = 0;

        for(int x = 0; x < iterCount; x ++) {
            long startTime = System.nanoTime();
            readWithFIDom();
            long endTime = System.nanoTime();
            totalTime += endTime - startTime;
        }
        
        averageNanos = totalTime / iterCount;
        System.out.println("FastInfoset DOM average us: " + averageNanos / 1000);
    }
    
    public static void main(String[] args) throws Exception {
        FastInfosetExperiment that = new FastInfosetExperiment();
        that.benchmark();
    }
}
