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
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;

/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class ShortType extends AegisType {
    @Override
    public Object readObject(MessageReader reader, Context context) {
        return new Short(reader.getValue().trim());
    }

    @Override
    public void writeObject(Object object, MessageWriter writer, Context context) {
        if (object instanceof Short) {
            writer.writeValueAsShort((Short)object);
        } else {
            writer.writeValueAsShort(new Short(((Number)object).shortValue()));
        }
    }
}
