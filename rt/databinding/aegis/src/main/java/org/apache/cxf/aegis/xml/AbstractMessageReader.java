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
package org.apache.cxf.aegis.xml;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.common.util.SOAPConstants;

/**
 * Basic type conversions for reading messages.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public abstract class AbstractMessageReader implements MessageReader {
    private static final QName XSI_NIL = new QName(SOAPConstants.XSI_NS, "nil", SOAPConstants.XSI_PREFIX);

    public AbstractMessageReader() {
    }

    public void readToEnd() {
        readToEnd(this);
    }

    private void readToEnd(MessageReader childReader) {
        while (childReader.hasMoreElementReaders()) {
            readToEnd(childReader.getNextElementReader());
        }
    }

    public boolean isXsiNil() {
        MessageReader nilReader = getAttributeReader(XSI_NIL);
        boolean nil = false;
        if (nilReader != null) {
            String value = nilReader.getValue();
            if (value != null && (value.trim().equals("true") || value.trim().equals("1"))) {
                return true;
            }
        }

        return nil;
    }

    public boolean hasValue() {
        return getValue() != null;
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageReader#getValueAsCharacter()
     */
    public char getValueAsCharacter() {
        if (getValue() == null) {
            return 0;
        }
        return getValue().charAt(0);
    }

    public int getValueAsInt() {
        if (getValue() == null) {
            return 0;
        }

        return Integer.parseInt(getValue().trim());
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageReader#getValueAsLong()
     */
    public long getValueAsLong() {
        if (getValue() == null) {
            return 0L;
        }

        return Long.parseLong(getValue().trim());
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageReader#getValueAsDouble()
     */
    public double getValueAsDouble() {
        if (getValue() == null) {
            return 0d;
        }

        return Double.parseDouble(getValue().trim());
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageReader#getValueAsFloat()
     */
    public float getValueAsFloat() {
        if (getValue() == null) {
            return 0f;
        }

        return Float.parseFloat(getValue().trim());
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageReader#getValueAsBoolean()
     */
    public boolean getValueAsBoolean() {
        String value = getValue();
        if (value == null) {
            return false;
        }
        value = value.trim();
        if ("true".equalsIgnoreCase(value) || "1".equalsIgnoreCase(value)) {
            return true;
        }

        if ("false".equalsIgnoreCase(value) || "0".equalsIgnoreCase(value)) {
            return false;
        }

        throw new DatabindingException("Invalid boolean value: " + value);
    }

    public XMLStreamReader getXMLStreamReader() {
        throw new UnsupportedOperationException();
    }
}
