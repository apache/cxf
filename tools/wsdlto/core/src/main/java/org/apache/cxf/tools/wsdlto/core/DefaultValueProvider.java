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
import java.util.Set;

import javax.xml.namespace.QName;

public interface DefaultValueProvider {
    
    byte getByteValue(String path);
    short getShortValue(String path);
    int getIntValue(String path);
    long getLongValue(String path);
    
    float getFloatValue(String path);
    double getDoubleValue(String path);
    
    char getCharValue(String path);
    
    String getStringValue(String path);
    boolean getBooleanValue(String path);
    
    QName getQNameValue(String path);
    URI getURIValue(String path);
    
    BigInteger getBigIntegerValue(String path);
    BigDecimal getBigDecimalValue(String path);
    
    String getXMLGregorianCalendarValueString(String path);
    String getDurationValueString(String path);
    
    String chooseEnumValue(String path, Set<String> values);
    
    int getListLength(String path);
}
