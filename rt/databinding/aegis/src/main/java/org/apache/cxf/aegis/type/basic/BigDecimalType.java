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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;

/**
 * <code>AegisType</code> for a <code>BigDecimal</code>
 */
public class BigDecimalType extends AegisType {
    public BigDecimalType() {
        super();
    }

    @Override
    public Object readObject(final MessageReader reader, final Context context) {
        final String value = reader.getValue();

        return null == value ? null : new BigDecimal(value.trim());
    }

    @Override
    public void writeObject(final Object object, final MessageWriter writer, final Context context) {
        final BigDecimal d;
        if (object instanceof BigDecimal) {
            d = (BigDecimal)object;
        } else if (object instanceof BigInteger) {
            d = new BigDecimal((BigInteger)object);
        } else if (object instanceof AtomicInteger || object instanceof Integer) {
            d = new BigDecimal(((Number)object).intValue());
        } else if (object instanceof Long || object instanceof AtomicLong) {
            d = new BigDecimal(((Number)object).longValue());
        } else if (object instanceof Double) {
            d = new BigDecimal(((Number)object).doubleValue());
        } else if (object instanceof Float) {
            d = new BigDecimal(((Number)object).floatValue());
        } else if (object instanceof Short) {
            d = new BigDecimal(((Number)object).shortValue());
        } else if (object instanceof Byte) {
            d = new BigDecimal(((Number)object).byteValue());
        } else {
            d = new BigDecimal(((Number)object).doubleValue());
        }
        writer.writeValue(d.toPlainString());
    }
}
