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

package org.apache.cxf.staxutils.transform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.staxutils.StaxUtils;

import org.junit.Assert;

/**
 * 
 */
public final class TransformTestUtils {
    private static final Logger LOG = LogUtils.getLogger(TransformTestUtils.class);

    private TransformTestUtils() {
    }
    
    // test utilities methods 
    
    static void transformInStreamAndCompare(String inname, String outname, 
                                           Map<String, String> transformElements,
                                           Map<String, String> appendElements,
                                           List<String> dropElements,
                                           Map<String, String> transformAttributes,
                                           Map<String, String> appendAttributes) 
        throws XMLStreamException {
        
        XMLStreamReader reader = createInTransformedStreamReader(inname,
                                                                 transformElements,
                                                                 appendElements,
                                                                 dropElements,
                                                                 transformAttributes);
        
        XMLStreamReader teacher = 
            StaxUtils.createXMLStreamReader(
                      TransformTestUtils.class.getResourceAsStream(outname));
        
        verifyReaders(teacher, reader, false, true);
    }

    static void transformOutStreamAndCompare(String inname, String outname, 
                                           Map<String, String> transformElements,
                                           Map<String, String> appendElements,
                                           List<String> dropElements,
                                           Map<String, String> transformAttributes,
                                           Map<String, String> appendAttributes) 
        throws XMLStreamException {
        
        XMLStreamReader reader = createOutTransformedStreamReader(inname, 
                                                                  transformElements, appendElements, 
                                                                  dropElements, transformAttributes, 
                                                                  false, null);
        XMLStreamReader teacher = 
            StaxUtils.createXMLStreamReader(
                      TransformTestUtils.class.getResourceAsStream(outname));
 
        verifyReaders(teacher, reader, false, true);
    }

    static XMLStreamReader createInTransformedStreamReader(
        String file,
        Map<String, String> emap, 
        Map<String, String> eappend,
        List<String> dropEls,
        Map<String, String> amap) throws XMLStreamException {

        return new InTransformReader(StaxUtils.createXMLStreamReader(
            TransformTestUtils.class.getResourceAsStream(file)),
            emap, eappend, dropEls, amap, false);
    }
    
    static XMLStreamReader createOutTransformedStreamReader(
        String file,
        Map<String, String> emap, 
        Map<String, String> append,
        List<String> dropEls,
        Map<String, String> amap,
        boolean attributesToElements,
        String defaultNamespace) throws XMLStreamException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLStreamWriter writer = 
            new OutTransformWriter(StaxUtils.createXMLStreamWriter(os, "UTF-8"), 
                                   emap, append, dropEls, amap, attributesToElements, defaultNamespace);
        StaxUtils.copy(new StreamSource(TransformTestUtils.class.getResourceAsStream(file)), writer);
        writer.flush();

        return StaxUtils.createXMLStreamReader(new ByteArrayInputStream(os.toByteArray()));
    }

    /**
     * Verifies the two stream events are equivalent and throws an assertion 
     * exception at the first mismatch.
     * @param teacher
     * @param reader
     * @param eec
     * @throws XMLStreamException
     */
    static void verifyReaders(XMLStreamReader teacher, XMLStreamReader reader, 
                               boolean eec, boolean pfx) throws XMLStreamException {
        // compare the elements and attributes while ignoring comments, line breaks, etc
        for (;;) {
            int revent = getNextEvent(reader);
            int tevent = getNextEvent(teacher);
            
            if (revent == -1 && tevent == -1) {
                break;
            }
            LOG.fine("Event: " + tevent + " ? " + revent);
            Assert.assertEquals(tevent, revent);

            switch (revent) {
            case XMLStreamConstants.START_ELEMENT:
                LOG.fine("Start Element " + teacher.getName() + " ? " + reader.getName());
                Assert.assertEquals(teacher.getName(), reader.getName());
                if (pfx) {
                    // verify if the namespace prefix are preserved
                    Assert.assertEquals(teacher.getPrefix(), reader.getPrefix());
                }
                verifyAttributes(teacher, reader);
                break;
            case XMLStreamConstants.END_ELEMENT:
                LOG.fine("End Element " + teacher.getName() + " ? " + reader.getName());
                if (eec) {
                    // perform end-element-check
                    Assert.assertEquals(teacher.getName(), reader.getName());
                }
                break;
            case XMLStreamConstants.CHARACTERS:
                LOG.fine("Characters " + teacher.getText() + " ? " + reader.getText());
                Assert.assertEquals(teacher.getText(), reader.getText());
                break;
            default:
            }
        }
    }

    private static void verifyAttributes(XMLStreamReader teacher, XMLStreamReader reader) {
        int acount = teacher.getAttributeCount();
        Assert.assertEquals(acount, reader.getAttributeCount());
        Map<QName, String> attributesMap = new HashMap<QName, String>();
        // temporarily store all the attributes
        for (int i = 0; i < acount; i++) {
            attributesMap.put(reader.getAttributeName(i), reader.getAttributeValue(i));
        }
        // compares each attribute
        for (int i = 0; i < acount; i++) {
            String avalue = attributesMap.remove(teacher.getAttributeName(i));
            Assert.assertEquals(avalue, teacher.getAttributeValue(i));
        }
        // attributes must be exhausted
        Assert.assertTrue(attributesMap.isEmpty());
    }

    /**
     * Returns the next relevant reader event.
     *  
     * @param reader
     * @return
     * @throws XMLStreamException
     */
    private static int getNextEvent(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int e = reader.next();
            if (e == XMLStreamConstants.END_DOCUMENT) {
                return e;
            }
            if (e == XMLStreamConstants.START_ELEMENT || e == XMLStreamConstants.END_ELEMENT) {
                return e;
            } else if (e == XMLStreamConstants.CHARACTERS) {
                String text = reader.getText();
                if (text.trim().length() == 0) {
                    continue;
                }
                return e;
            }
        }
        return -1;
    }
}
