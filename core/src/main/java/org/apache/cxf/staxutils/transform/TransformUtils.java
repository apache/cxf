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

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;

public final class TransformUtils {
    private TransformUtils() {

    }

    public static XMLStreamReader createNewReaderIfNeeded(XMLStreamReader reader, InputStream is) {
        return reader == null ? StaxUtils.createXMLStreamReader(is) : reader;
    }

    public static XMLStreamWriter createNewWriterIfNeeded(XMLStreamWriter writer, OutputStream os) {
        return createNewWriterIfNeeded(writer, os, null);
    }

    public static XMLStreamWriter createNewWriterIfNeeded(XMLStreamWriter writer, OutputStream os, String encoding) {
        if (writer != null) {
            return writer;
        } else if (encoding != null) {
            return StaxUtils.createXMLStreamWriter(os, encoding);
        }
        return StaxUtils.createXMLStreamWriter(os);
    }

    public static XMLStreamWriter createTransformWriterIfNeeded(XMLStreamWriter writer,
                                                                OutputStream os,
                                                                Map<String, String> outElementsMap,
                                                                List<String> outDropElements,
                                                                Map<String, String> outAppendMap,
                                                                boolean attributesToElements,
                                                                String defaultNamespace) {
        if (outElementsMap != null || outDropElements != null
            || outAppendMap != null || attributesToElements) {
            writer = new OutTransformWriter(createNewWriterIfNeeded(writer, os), outElementsMap, outAppendMap,
                                            outDropElements, null, attributesToElements, defaultNamespace);
        }
        return writer;
    }

    //CHECKSTYLE:OFF ParameterNumber
    public static XMLStreamWriter createTransformWriterIfNeeded(XMLStreamWriter writer,
                                                                OutputStream os,
                                                                Map<String, String> outElementsMap,
                                                                List<String> outDropElements,
                                                                Map<String, String> outAppendMap,
                                                                Map<String, String> outAttributesMap,
                                                                boolean attributesToElements,
                                                                String defaultNamespace,
                                                                String encoding) {
        if (outElementsMap != null || outDropElements != null
            || outAppendMap != null || attributesToElements) {
            writer = new OutTransformWriter(createNewWriterIfNeeded(writer, os, encoding), outElementsMap, outAppendMap,
                                            outDropElements, outAttributesMap, attributesToElements, defaultNamespace);
        }
        return writer;
    }
    //CHECKSTYLE:ON

    public static XMLStreamReader createTransformReaderIfNeeded(XMLStreamReader reader,
                                                                InputStream is,
                                                                List<String> inDropElements,
                                                                Map<String, String> inElementsMap,
                                                                Map<String, String> inAppendMap,
                                                                boolean blockOriginalReader) {
        return createTransformReaderIfNeeded(reader, is,
                          inDropElements, inElementsMap, inAppendMap, null, blockOriginalReader);
    }

    public static XMLStreamReader createTransformReaderIfNeeded(XMLStreamReader reader,
                                                                InputStream is,
                                                                List<String> inDropElements,
                                                                Map<String, String> inElementsMap,
                                                                Map<String, String> inAppendMap,
                                                                Map<String, String> inAttributesMap,
                                                                boolean blockOriginalReader) {
        if (inElementsMap != null || inAppendMap != null || inDropElements != null
            || inAttributesMap != null) {
            reader = new InTransformReader(createNewReaderIfNeeded(reader, is),
                                           inElementsMap, inAppendMap, inDropElements,
                                           inAttributesMap, blockOriginalReader);
        }

        return reader;
    }

    protected static void convertToQNamesMap(Map<String, String> map,
                                             QNamesMap elementsMap,
                                             Map<String, String> nsMap) {
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                QName lname = DOMUtils.convertStringToQName(entry.getKey());
                QName rname = DOMUtils.convertStringToQName(entry.getValue());
                elementsMap.put(lname, rname);
                if (nsMap != null && !isEmptyQName(rname)
                    && ("*".equals(lname.getLocalPart()) && "*".equals(rname.getLocalPart()))) {
                    nsMap.put(lname.getNamespaceURI(), rname.getNamespaceURI());
                }
            }
        }
    }

    static void convertToMapOfElementProperties(Map<String, String> map,
                                                Map<QName, ElementProperty> elementsMap) {
        if (map != null) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                String text = null;
                boolean child = false;

                // if the content delimiter is present in the value, extract the content
                int d = value.indexOf('}');
                d = value.indexOf('=', d < 0 ? 0 : d);
                if (d > 0) {
                    text = value.substring(d + 1);
                    value = value.substring(0, d);
                }

                // if the trailer delimiter is present in the key, remove it
                if (key.endsWith("/")) {
                    key = key.substring(0, key.length() - 1);
                    child = true;
                }
                QName lname = DOMUtils.convertStringToQName(key);
                QName rname = DOMUtils.convertStringToQName(value);

                ElementProperty desc = new ElementProperty(rname, text, child);
                elementsMap.put(lname, desc);
            }
        }
    }

    protected static void convertToSetOfQNames(List<String> set,
                                               Set<QName> elementsSet) {
        if (set != null) {
            for (String entry : set) {
                QName name = DOMUtils.convertStringToQName(entry);
                elementsSet.add(name);
            }
        }
    }

    static boolean isEmptyQName(QName qname) {
        return XMLConstants.NULL_NS_URI.equals(qname.getNamespaceURI()) && qname.getLocalPart().isEmpty();
    }

    static ParsingEvent createStartElementEvent(QName name) {
        return new ParsingEvent(XMLStreamConstants.START_ELEMENT, name, null);
    }

    static ParsingEvent createEndElementEvent(QName name) {
        return new ParsingEvent(XMLStreamConstants.END_ELEMENT, name, null);
    }

    static ParsingEvent createCharactersEvent(String value) {
        return new ParsingEvent(XMLStreamConstants.CHARACTERS, null, value);
    }

}
