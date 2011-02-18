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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.staxutils.StaxStreamFilter;
import org.apache.cxf.staxutils.StaxUtils;

public final class TransformUtils {
    private TransformUtils() {
        
    }
    
    public static XMLStreamReader createNewReaderIfNeeded(XMLStreamReader reader, InputStream is) {
        return reader == null ? StaxUtils.createXMLStreamReader(is) : reader;
    }
    
    public static XMLStreamWriter createNewWriterIfNeeded(XMLStreamWriter writer, OutputStream os) {
        return writer == null ? StaxUtils.createXMLStreamWriter(os) : writer;
    }
    
    public static XMLStreamWriter createTransformWriterIfNeeded(XMLStreamWriter writer,
                                                                OutputStream os,
                                                                Map<String, String> outElementsMap,
                                                                List<String> outDropElements,
                                                                Map<String, String> outAppendMap,
                                                                boolean attributesToElements) {
        if (outElementsMap != null || outDropElements != null 
            || outAppendMap != null || attributesToElements) {
            writer = createNewWriterIfNeeded(writer, os);
            writer = new OutTransformWriter(writer, outElementsMap, outAppendMap,
                                            outDropElements, attributesToElements);
        }
        return writer;
    }
    
    public static XMLStreamReader createTransformReaderIfNeeded(XMLStreamReader reader, 
                                                                InputStream is,
                                                                List<String> inDropElements,
                                                                Map<String, String> inElementsMap,
                                                                Map<String, String> inAppendMap,
                                                                boolean blockOriginalReader) {
        if (inDropElements != null) {
            Set<QName> dropElements = XMLUtils.convertStringsToQNames(inDropElements);
            reader = StaxUtils.createFilteredReader(createNewReaderIfNeeded(reader, is),
                                               new StaxStreamFilter(dropElements.toArray(new QName[]{})));    
        }
        if (inElementsMap != null || inAppendMap != null) {
            reader = new InTransformReader(createNewReaderIfNeeded(reader, is),
                                           inElementsMap, inAppendMap, blockOriginalReader);
        }
        return reader;
    }
    
    protected static void convertToQNamesMap(Map<String, String> map,
                                             QNamesMap elementsMap,
                                             Map<String, String> nsMap) {
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                QName lname = XMLUtils.convertStringToQName(entry.getKey());
                QName rname = XMLUtils.convertStringToQName(entry.getValue());
                elementsMap.put(lname, rname);
                if (nsMap != null) {
                    nsMap.put(lname.getNamespaceURI(), rname.getNamespaceURI());
                }
            }
        }
    }
    
    protected static void convertToMapOfQNames(Map<String, String> map,
                                               Map<QName, QName> elementsMap) {
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                QName lname = XMLUtils.convertStringToQName(entry.getKey());
                QName rname = XMLUtils.convertStringToQName(entry.getValue());
                elementsMap.put(lname, rname);
            }
        }
    }
    
}
