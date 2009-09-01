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
package org.apache.cxf.aegis.type.java5;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;

/**
 * @author Dan Diephouse
 */
public class XMLGregorianCalendarType extends AegisType {
    private DatatypeFactory dtFactory;

    public XMLGregorianCalendarType() {
        try {
            dtFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new DatabindingException("Couldn't load DatatypeFactory.", e);
        }

        setTypeClass(XMLGregorianCalendar.class);
    }

    @Override
    public Object readObject(MessageReader reader, Context context) {
        return dtFactory.newXMLGregorianCalendar(reader.getValue().trim());
    }

    @Override
    public void writeObject(Object object, MessageWriter writer, Context context) {
        writer.writeValue(((XMLGregorianCalendar)object).toXMLFormat());
    }
}
