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
package org.apache.cxf.staxutils;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

/**
 * A StreamReaderDelegate that expands property references in element and attribute values.
 *
 */
public class PropertiesExpandingStreamReader extends StreamReaderDelegate {

    public static final String DELIMITER = "@";

    private final Map<String, String> props;

    public PropertiesExpandingStreamReader(XMLStreamReader reader, Map<String, String> props) {
        super(reader);
        this.props = props;
    }

    protected String expandProperty(String value) {
        if (isEmpty(value)) {
            return value;
        }

        final int startIndx = value.indexOf(DELIMITER);
        if (startIndx > -1) {
            final int endIndx = value.lastIndexOf(DELIMITER);
            if (endIndx > -1 && startIndx + 1 < endIndx) {
                final String propName = value.substring(startIndx + 1, endIndx);
                if (!isEmpty(propName)) {
                    final String envValue = props.get(propName);
                    if (!isEmpty(envValue)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(value.substring(0, startIndx));
                        sb.append(envValue);
                        sb.append(value.substring(endIndx + 1));
                        value = sb.toString();
                    }
                }
            }
        }

        return value;
    }

    private static boolean isEmpty(String str) {
        if (str != null) {
            int len = str.length();
            for (int x = 0; x < len; ++x) {
                if (str.charAt(x) > ' ') {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public String getElementText() throws XMLStreamException {
        return expandProperty(super.getElementText());
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        return expandProperty(super.getAttributeValue(namespaceURI, localName));
    }

    @Override
    public String getAttributeValue(int index) {
        return expandProperty(super.getAttributeValue(index));
    }

    @Override
    public String getText() {
        return expandProperty(super.getText());
    }
}
