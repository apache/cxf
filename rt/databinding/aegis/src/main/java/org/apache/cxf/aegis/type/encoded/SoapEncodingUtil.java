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
package org.apache.cxf.aegis.type.encoded;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.binding.soap.Soap12;

/**
 * Utilitiy methods for SOAP reading and writing encoded mesages.
 */
public final class SoapEncodingUtil {
    private static final String SOAP_ENCODING_NS_1_2 = Soap12.getInstance().getSoapEncodingStyle();
    private static final QName SOAP_ENCODING_ID_1_2 = new QName(SOAP_ENCODING_NS_1_2, "id");
    private static final QName SOAP_ENCODING_ID_1_1 = new QName("id");
    private static final QName SOAP_ENCODING_REF_1_2 = new QName(SOAP_ENCODING_NS_1_2, "ref");
    private static final QName SOAP_ENCODING_REF_1_1 = new QName("href");

    private SoapEncodingUtil() {
    }

    /**
     * Reads the SOAP 1.2 or SOAP 1.1 id attribute.
     *
     * @param reader the stream to read; must not be null
     * @return the id or null of neither attribute was present
     */
    public static String readId(MessageReader reader) {
        String id = readAttributeValue(reader, SOAP_ENCODING_ID_1_2);
        if (id == null) {
            id = readAttributeValue(reader, SOAP_ENCODING_ID_1_1);
        }
        return id;
    }

    /**
     * Writes a SOAP 1.1 id attribute.
     *
     * @param writer the stream to which the id should be written; must not be null
     * @param id the id to write; must not be null
     */
    public static void writeId(MessageWriter writer, String id) {
        if (id == null) {
            throw new NullPointerException("id is null");
        }
        writeAttribute(writer, SOAP_ENCODING_ID_1_1, id);
    }

    /**
     * Reads the SOAP 1.2 or SOAP 1.1 reference attribute.
     *
     * @param reader the stream to read; must not be null
     * @return the reference id or null of neither attribute was present
     */
    public static String readRef(MessageReader reader) {
        String ref = readAttributeValue(reader, SOAP_ENCODING_REF_1_2);
        if (ref == null) {
            ref = readAttributeValue(reader, SOAP_ENCODING_REF_1_1);
        }
        return ref;
    }

    /**
     * Writes a SOAP 1.1 ref attribute.
     *
     * @param writer the stream to which the id should be written; must not be null
     * @param refId the reference id to write; must not be null
     */
    public static void writeRef(MessageWriter writer, String refId) {
        if (refId == null) {
            throw new NullPointerException("refId is null");
        }
        writeAttribute(writer, SOAP_ENCODING_REF_1_1, refId);
    }

    public static String readAttributeValue(MessageReader reader, QName name) {
        if (reader == null) {
            throw new NullPointerException("reader is null");
        }
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        MessageReader attributeReader = reader.getAttributeReader(name);
        if (attributeReader != null) {
            String value = attributeReader.getValue();
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    public static void writeAttribute(MessageWriter writer, QName name, String value) {
        if (writer == null) {
            throw new NullPointerException("writer is null");
        }
        if (name == null) {
            throw new NullPointerException("name is null");
        }
        if (value == null) {
            throw new NullPointerException("value is null");
        }
        MessageWriter attributeWriter = writer.getAttributeWriter(name);
        attributeWriter.writeValue(value);
        attributeWriter.close();
    }
}
