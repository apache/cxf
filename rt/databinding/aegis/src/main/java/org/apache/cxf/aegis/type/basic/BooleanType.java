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

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;

/**
 * Aegis type for {@link java.lang.Boolean}. These can be null.
 */
public class BooleanType extends Type {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        if (reader.isXsiNil()) {
            reader.readToEnd();
            return null;
        }
        return Boolean.valueOf(reader.getValueAsBoolean());
    }

    @Override
    public void writeObject(Object object, MessageWriter writer, Context context) {
        if (object == null) {
            writer.writeXsiNil();
        } else {
            writer.writeValueAsBoolean(((Boolean)object).booleanValue());
        }
    }
}
