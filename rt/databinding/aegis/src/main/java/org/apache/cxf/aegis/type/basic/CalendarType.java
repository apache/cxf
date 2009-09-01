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
package org.apache.cxf.aegis.type.basic;

import java.text.ParseException;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.util.date.XsDateTimeFormat;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;

/**
 * AegisType for the Calendar class.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class CalendarType extends AegisType {
    private static XsDateTimeFormat format = new XsDateTimeFormat();

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        String value = reader.getValue();

        if (value == null) {
            return null;
        }

        try {
            return format.parseObject(value.trim());
        } catch (ParseException e) {
            throw new DatabindingException("Could not parse xs:dateTime: " + e.getMessage(), e);
        }
    }

    @Override
    public void writeObject(Object object, MessageWriter writer, Context context) {
        writer.writeValue(format.format(object));
    }
}
