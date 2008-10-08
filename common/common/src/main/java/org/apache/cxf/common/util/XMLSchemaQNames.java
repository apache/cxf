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

package org.apache.cxf.common.util;

import javax.xml.namespace.QName;

/**
 * This class provides QNames for all of the XML Schema types. 
 */
public final class XMLSchemaQNames {
    public static final QName XSD_STRING = new QName(SOAPConstants.XSD, "string", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_LONG = new QName(SOAPConstants.XSD, "long", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_FLOAT = new QName(SOAPConstants.XSD, "float", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_DOUBLE = new QName(SOAPConstants.XSD, "double", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_INT = new QName(SOAPConstants.XSD, "int", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_SHORT = new QName(SOAPConstants.XSD, "short", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_BYTE = new QName(SOAPConstants.XSD, "byte", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_BOOLEAN = new QName(SOAPConstants.XSD,
                                                         "boolean", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_DATETIME = new QName(SOAPConstants.XSD, "dateTime",
                                                          SOAPConstants.XSD_PREFIX);
    public static final QName XSD_TIME = new QName(SOAPConstants.XSD, "dateTime", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_BASE64 = new QName(SOAPConstants.XSD, "base64Binary",
                                                        SOAPConstants.XSD_PREFIX);
    public static final QName XSD_DECIMAL = new QName(SOAPConstants.XSD,
                                                         "decimal", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_INTEGER = new QName(SOAPConstants.XSD,
                                                         "integer", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_URI = new QName(SOAPConstants.XSD, "anyURI", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_ANY = new QName(SOAPConstants.XSD, "anyType", SOAPConstants.XSD_PREFIX);

    public static final QName XSD_DATE = new QName(SOAPConstants.XSD, "date", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_DURATION = new QName(SOAPConstants.XSD, "duration",
                                                          SOAPConstants.XSD_PREFIX);
    public static final QName XSD_G_YEAR_MONTH = new QName(SOAPConstants.XSD, "gYearMonth",
                                                              SOAPConstants.XSD_PREFIX);
    public static final QName XSD_G_MONTH_DAY = new QName(SOAPConstants.XSD, "gMonthDay",
                                                             SOAPConstants.XSD_PREFIX);
    public static final QName XSD_G_YEAR = new QName(SOAPConstants.XSD, "gYear", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_G_MONTH = new QName(SOAPConstants.XSD, "gMonth", SOAPConstants.XSD_PREFIX);
    public static final QName XSD_G_DAY = new QName(SOAPConstants.XSD, "gDay", SOAPConstants.XSD_PREFIX);
    
    private XMLSchemaQNames() {
    }
}
