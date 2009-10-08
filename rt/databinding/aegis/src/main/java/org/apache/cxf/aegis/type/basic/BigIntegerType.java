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

import java.math.BigInteger;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;

/**
 * <code>Type</code> for a <code>BigInteger</code>
 * 
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class BigIntegerType extends Type {
    public BigIntegerType() {
        super();
    }

    @Override
    public Object readObject(final MessageReader reader, final Context context) {
        final String value = reader.getValue();

        return null == value ? null : new BigInteger(value.trim());
    }

    @Override
    public void writeObject(final Object object, final MessageWriter writer, final Context context) {
        writer.writeValue(object.toString());
    }
}
