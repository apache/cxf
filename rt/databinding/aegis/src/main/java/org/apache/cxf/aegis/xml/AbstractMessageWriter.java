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

import org.apache.cxf.common.util.SOAPConstants;

/**
 * Basic type conversion functionality for writing messages.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public abstract class AbstractMessageWriter implements MessageWriter {
    
    private boolean xsiTypeWritten;
    
    /**
     * Create a LiteralWriter but without writing an element name.
     * 
     * @param writer
     */
    public AbstractMessageWriter() {
    }

    public void writeXsiType(QName type) {
        if (xsiTypeWritten) {
            return;
        }
        xsiTypeWritten = true;

        /*
         * Do not assume that the prefix supplied with the QName should be used
         * in this case.
         */
        String prefix = getPrefixForNamespace(type.getNamespaceURI(), type.getPrefix());
        String value;
        if (prefix != null && prefix.length() > 0) {
            StringBuffer sb = new StringBuffer(prefix.length() + 1 + type.getLocalPart().length());
            sb.append(prefix);
            sb.append(':');
            sb.append(type.getLocalPart());
            value = sb.toString();
        } else {
            value = type.getLocalPart();
        }
        getAttributeWriter("type", SOAPConstants.XSI_NS).writeValue(value);
    }

    public void writeXsiNil() {
        MessageWriter attWriter = getAttributeWriter("nil", SOAPConstants.XSI_NS);
        attWriter.writeValue("true");
        attWriter.close();
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageWriter#writeValueAsInt(java.lang.Integer)
     */
    public void writeValueAsInt(Integer i) {
        writeValue(i.toString());
    }
    
    public void writeValueAsByte(Byte b) {
        writeValue(b.toString());
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageWriter#writeValueAsDouble(java.lang.Double)
     */
    public void writeValueAsDouble(Double d) {
        writeValue(d.toString());
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageWriter#writeValueAsCharacter(java.lang.Character)
     */
    public void writeValueAsCharacter(Character char1) {
        writeValue(char1.toString());
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageWriter#writeValueAsLong(java.lang.Long)
     */
    public void writeValueAsLong(Long l) {
        writeValue(l.toString());
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageWriter#writeValueAsFloat(java.lang.Float)
     */
    public void writeValueAsFloat(Float f) {
        writeValue(f.toString());
    }

    /**
     * @see org.apache.cxf.aegis.xml.MessageWriter#writeValueAsBoolean(boolean)
     */
    public void writeValueAsBoolean(boolean b) {
        writeValue(b ? "true" : "false");
    }

    public void writeValueAsShort(Short s) {
        writeValue(s.toString());
    }
}
