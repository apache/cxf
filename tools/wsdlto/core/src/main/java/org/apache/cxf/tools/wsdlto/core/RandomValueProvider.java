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

package org.apache.cxf.tools.wsdlto.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

public class RandomValueProvider implements DefaultValueProvider {
    Random random = new Random();

    public boolean getBooleanValue(String path) {
        return random.nextBoolean();
    }

    public byte getByteValue(String path) {
        return (byte)random.nextInt();
    }

    public char getCharValue(String path) {
        return (char)random.nextInt();
    }

    public double getDoubleValue(String path) {
        return random.nextDouble();
    }

    public float getFloatValue(String path) {
        return random.nextFloat();
    }

    public int getIntValue(String path) {
        return random.nextInt();
    }

    public long getLongValue(String path) {
        return random.nextLong();
    }

    public short getShortValue(String path) {
        return (short)random.nextInt();
    }

    public String getStringValue(String path) {
        return path.substring(path.lastIndexOf('/') + 1) + getIntValue(path);
    }
    
    public QName getQNameValue(String path) {
        return new QName("http://" + getStringValue(path) + ".com",
                         getStringValue(path));
    }

    public URI getURIValue(String path) {
        try {
            return new URI("http://" + getStringValue(path) + ".com/" + path);
        } catch (URISyntaxException e) {
            //ignore
        }
        return null;
    }

    public BigDecimal getBigDecimalValue(String path) {
        String s = Long.toString(random.nextLong());
        s += ".";
        s += Long.toString(Math.abs(random.nextLong()));
        return new BigDecimal(s);
    }
    
    public BigInteger getBigIntegerValue(String path) {
        String s = Long.toString(random.nextLong());
        s += Long.toString(Math.abs(random.nextLong()));
        return new BigInteger(s);
    }

    
    public String getXMLGregorianCalendarValueString(String path) {
        try {
            return javax.xml.datatype.DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat();
        } catch (DatatypeConfigurationException e) {
            //ignore
        }
        return null;
    }
    
    public String getDurationValueString(String path) {
        try {
            return javax.xml.datatype.DatatypeFactory.newInstance().newDuration(random.nextLong()).toString();
        } catch (DatatypeConfigurationException e) {
            //ignore
        }
        return "P1Y35DT60M60.500S";
    }

    public String chooseEnumValue(String path, Set<String> values) {
        int i = random.nextInt(values.size());
        for (String s : values) {
            if (i == 0) {
                return s;
            }
            --i;
        }
        return values.iterator().next();
    }

    public int getListLength(String path) {
        int cnt = path.split("/").length;
        return cnt > 5 ? 0 : 1;
    }

}
