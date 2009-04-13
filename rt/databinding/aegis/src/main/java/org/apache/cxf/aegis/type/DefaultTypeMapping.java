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
package org.apache.cxf.aegis.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.type.basic.Base64Type;
import org.apache.cxf.aegis.type.basic.BigDecimalType;
import org.apache.cxf.aegis.type.basic.BigIntegerType;
import org.apache.cxf.aegis.type.basic.BooleanType;
import org.apache.cxf.aegis.type.basic.ByteType;
import org.apache.cxf.aegis.type.basic.CalendarType;
import org.apache.cxf.aegis.type.basic.CharacterAsStringType;
import org.apache.cxf.aegis.type.basic.CharacterType;
import org.apache.cxf.aegis.type.basic.DateTimeType;
import org.apache.cxf.aegis.type.basic.DoubleType;
import org.apache.cxf.aegis.type.basic.FloatType;
import org.apache.cxf.aegis.type.basic.IntType;
import org.apache.cxf.aegis.type.basic.LongType;
import org.apache.cxf.aegis.type.basic.ObjectType;
import org.apache.cxf.aegis.type.basic.ShortType;
import org.apache.cxf.aegis.type.basic.SqlDateType;
import org.apache.cxf.aegis.type.basic.StringType;
import org.apache.cxf.aegis.type.basic.TimeType;
import org.apache.cxf.aegis.type.basic.TimestampType;
import org.apache.cxf.aegis.type.basic.URIType;
import org.apache.cxf.aegis.type.mtom.AbstractXOPType;
import org.apache.cxf.aegis.type.mtom.DataHandlerType;
import org.apache.cxf.aegis.type.mtom.DataSourceType;
import org.apache.cxf.aegis.type.xml.DocumentType;
import org.apache.cxf.aegis.type.xml.JDOMDocumentType;
import org.apache.cxf.aegis.type.xml.JDOMElementType;
import org.apache.cxf.aegis.type.xml.SourceType;
import org.apache.cxf.aegis.type.xml.XMLStreamReaderType;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.common.util.XMLSchemaQNames;
import org.jdom.Element;

/**
 * Contains type mappings for java/qname pairs.
 */
public class DefaultTypeMapping implements TypeMapping {
    public  static final String DEFAULT_MAPPING_URI = "urn:org.apache.cxf.aegis.types";
    private static final Logger LOG = LogUtils.getL7dLogger(DefaultTypeMapping.class);
    private Map<Class, Type> class2Type;
    private Map<QName, Type> xml2Type;
    private Map<Class, QName> class2xml;
    private TypeMapping nextTM;
    private TypeCreator typeCreator;
    private String identifierURI;

    public DefaultTypeMapping(String identifierURI, TypeMapping defaultTM) {
        this(identifierURI);

        this.nextTM = defaultTM;
    }
    
    public DefaultTypeMapping() {
        this(DEFAULT_MAPPING_URI);
    }

    public DefaultTypeMapping(String identifierURI) {
        this.identifierURI = identifierURI;
        class2Type = Collections.synchronizedMap(new HashMap<Class, Type>());
        class2xml = Collections.synchronizedMap(new HashMap<Class, QName>());
        xml2Type = Collections.synchronizedMap(new HashMap<QName, Type>());
    }

    public boolean isRegistered(Class javaType) {
        boolean registered = class2Type.containsKey(javaType);

        if (!registered && nextTM != null) {
            registered = nextTM.isRegistered(javaType);
        }

        return registered;
    }

    public boolean isRegistered(QName xmlType) {
        boolean registered = xml2Type.containsKey(xmlType);

        if (!registered && nextTM != null) {
            registered = nextTM.isRegistered(xmlType);
        }

        return registered;
    }

    public void register(Class javaType, QName xmlType, Type type) {
        type.setSchemaType(xmlType);
        type.setTypeClass(javaType);

        register(type);
    }

    /**
     * {@inheritDoc}
     */
    public void register(Type type) {
        type.setTypeMapping(this);
        /*
         * -- prb@codehaus.org; changing this to only register the type for
         * actions that it supports, and it could be none.
         */
        if (type.getTypeClass() != null) {
            class2xml.put(type.getTypeClass(), type.getSchemaType());
            class2Type.put(type.getTypeClass(), type);
        }
        if (type.getSchemaType() != null) {
            xml2Type.put(type.getSchemaType(), type);
        }
        if (type.getTypeClass() == null && type.getSchemaType() == null) {
            LOG.warning("The type " + type.getClass().getName()
                     + " supports neither serialization (non-null TypeClass)"
                     + " nor deserialization (non-null SchemaType).");
        }
    }

    public void removeType(Type type) {
        if (!xml2Type.containsKey(type.getSchemaType())) {
            nextTM.removeType(type);
        } else {
            xml2Type.remove(type.getSchemaType());
            class2Type.remove(type.getTypeClass());
            class2xml.remove(type.getTypeClass());
        }
    }

    /**
     * @see org.apache.cxf.aegis.type.TypeMapping#getType(java.lang.Class)
     */
    public Type getType(Class javaType) {
        Type type = class2Type.get(javaType);

        if (type == null && nextTM != null) {
            type = nextTM.getType(javaType);
        }

        return type;
    }

    /**
     * @see org.apache.cxf.aegis.type.TypeMapping#getType(javax.xml.namespace.QName)
     */
    public Type getType(QName xmlType) {
        Type type = xml2Type.get(xmlType);

        if (type == null && nextTM != null) {
            type = nextTM.getType(xmlType);
        }

        return type;
    }

    /**
     * @see org.apache.cxf.aegis.type.TypeMapping#getTypeQName(java.lang.Class)
     */
    public QName getTypeQName(Class clazz) {
        QName qname = class2xml.get(clazz);

        if (qname == null && nextTM != null) {
            qname = nextTM.getTypeQName(clazz);
        }

        return qname;
    }

    public TypeCreator getTypeCreator() {
        return typeCreator;
    }

    public void setTypeCreator(TypeCreator typeCreator) {
        this.typeCreator = typeCreator;

        typeCreator.setTypeMapping(this);
    }

    public TypeMapping getParent() {
        return nextTM;
    }

    private static void defaultRegister(TypeMapping tm, boolean defaultNillable, Class class1, QName name,
                                        Type type) {
        if (!defaultNillable) {
            type.setNillable(false);
        }

        tm.register(class1, name, type);
    }

    private static void fillStandardMappings(TypeMapping tm, boolean defaultNillable, 
                                             boolean enableMtomXmime) {
        defaultRegister(tm, defaultNillable, BigDecimal.class, XMLSchemaQNames.XSD_DECIMAL,
                        new BigDecimalType());
        defaultRegister(tm, defaultNillable, BigInteger.class, XMLSchemaQNames.XSD_INTEGER,
                        new BigIntegerType());
        defaultRegister(tm, defaultNillable, Boolean.class, XMLSchemaQNames.XSD_BOOLEAN,
                        new BooleanType());
        defaultRegister(tm, defaultNillable, Calendar.class, XMLSchemaQNames.XSD_DATETIME,
                        new CalendarType());
        defaultRegister(tm, defaultNillable, Date.class, XMLSchemaQNames.XSD_DATETIME, new DateTimeType());
        defaultRegister(tm, defaultNillable, Document.class, XMLSchemaQNames.XSD_ANY, new DocumentType());
        defaultRegister(tm, defaultNillable, Element.class, XMLSchemaQNames.XSD_ANY,
                        new JDOMElementType());
        defaultRegister(tm, defaultNillable, Float.class, XMLSchemaQNames.XSD_FLOAT, new FloatType());
        defaultRegister(tm, defaultNillable, Double.class, XMLSchemaQNames.XSD_DOUBLE, new DoubleType());
        defaultRegister(tm, defaultNillable, Integer.class, XMLSchemaQNames.XSD_INT, new IntType());
        defaultRegister(tm, defaultNillable, Long.class, XMLSchemaQNames.XSD_LONG, new LongType());
        defaultRegister(tm, defaultNillable, Object.class, XMLSchemaQNames.XSD_ANY, new ObjectType());
        defaultRegister(tm, defaultNillable, Byte.class, XMLSchemaQNames.XSD_BYTE, new ByteType());
        defaultRegister(tm, defaultNillable, Short.class, XMLSchemaQNames.XSD_SHORT, new ShortType());
        defaultRegister(tm, defaultNillable, Source.class, XMLSchemaQNames.XSD_ANY, new SourceType());
        defaultRegister(tm, defaultNillable, String.class, XMLSchemaQNames.XSD_STRING, new StringType());
        defaultRegister(tm, defaultNillable, Time.class, XMLSchemaQNames.XSD_TIME, new TimeType());
        defaultRegister(tm, defaultNillable, Timestamp.class, XMLSchemaQNames.XSD_DATETIME,
                        new TimestampType());
        defaultRegister(tm, defaultNillable, URI.class, XMLSchemaQNames.XSD_URI, new URIType());
        defaultRegister(tm, defaultNillable, XMLStreamReader.class, XMLSchemaQNames.XSD_ANY,
                        new XMLStreamReaderType());
        
        defaultRegister(tm, defaultNillable, boolean.class, XMLSchemaQNames.XSD_BOOLEAN,
                        new BooleanType());
        defaultRegister(tm, defaultNillable, byte[].class, XMLSchemaQNames.XSD_BASE64, new Base64Type());
        defaultRegister(tm, defaultNillable, double.class, XMLSchemaQNames.XSD_DOUBLE, new DoubleType());
        defaultRegister(tm, defaultNillable, float.class, XMLSchemaQNames.XSD_FLOAT, new FloatType());
        defaultRegister(tm, defaultNillable, int.class, XMLSchemaQNames.XSD_INT, new IntType());
        defaultRegister(tm, defaultNillable, short.class, XMLSchemaQNames.XSD_SHORT, new ShortType());
        defaultRegister(tm, defaultNillable, byte.class, XMLSchemaQNames.XSD_BYTE, new ByteType());
        defaultRegister(tm, defaultNillable, long.class, XMLSchemaQNames.XSD_LONG, new LongType());

        defaultRegister(tm, defaultNillable, java.sql.Date.class, XMLSchemaQNames.XSD_DATETIME,
                        new SqlDateType());
        defaultRegister(tm, defaultNillable, org.jdom.Document.class, XMLSchemaQNames.XSD_ANY,
                        new JDOMDocumentType());
        
        QName mtomBase64 = XMLSchemaQNames.XSD_BASE64;
        if (enableMtomXmime) {
            mtomBase64 = AbstractXOPType.XML_MIME_BASE64;
        }

        defaultRegister(tm, defaultNillable, DataSource.class, mtomBase64,
                        new DataSourceType(enableMtomXmime, null));
        defaultRegister(tm, defaultNillable, DataHandler.class, mtomBase64,
                        new DataHandlerType(enableMtomXmime, null));
    }

    public static DefaultTypeMapping createSoap11TypeMapping(boolean defaultNillable, 
                                                             boolean enableMtomXmime) {
        // Create a Type Mapping for SOAP 1.1 Encoding
        DefaultTypeMapping soapTM = new DefaultTypeMapping(Soap11.SOAP_ENCODING_URI);
        fillStandardMappings(soapTM, defaultNillable, enableMtomXmime);

        defaultRegister(soapTM, defaultNillable, boolean.class, Soap11.ENCODED_BOOLEAN, new BooleanType());
        defaultRegister(soapTM, defaultNillable, char.class, Soap11.ENCODED_CHAR, new CharacterType());
        defaultRegister(soapTM, defaultNillable, int.class, Soap11.ENCODED_INT, new IntType());
        defaultRegister(soapTM, defaultNillable, short.class, Soap11.ENCODED_SHORT, new ShortType());
        defaultRegister(soapTM, defaultNillable, double.class, Soap11.ENCODED_DOUBLE, new DoubleType());
        defaultRegister(soapTM, defaultNillable, float.class, Soap11.ENCODED_FLOAT, new FloatType());
        defaultRegister(soapTM, defaultNillable, long.class, Soap11.ENCODED_LONG, new LongType());
        defaultRegister(soapTM, defaultNillable, char.class, Soap11.ENCODED_CHAR, new CharacterType());
        defaultRegister(soapTM, defaultNillable, Character.class, Soap11.ENCODED_CHAR, new CharacterType());
        defaultRegister(soapTM, defaultNillable, String.class, Soap11.ENCODED_STRING, new StringType());
        defaultRegister(soapTM, defaultNillable, Boolean.class, Soap11.ENCODED_BOOLEAN, new BooleanType());
        defaultRegister(soapTM, defaultNillable, Integer.class, Soap11.ENCODED_INT, new IntType());
        defaultRegister(soapTM, defaultNillable, Short.class, Soap11.ENCODED_SHORT, new ShortType());
        defaultRegister(soapTM, defaultNillable, Double.class, Soap11.ENCODED_DOUBLE, new DoubleType());
        defaultRegister(soapTM, defaultNillable, Float.class, Soap11.ENCODED_FLOAT, new FloatType());
        defaultRegister(soapTM, defaultNillable, Long.class, Soap11.ENCODED_LONG, new LongType());
        defaultRegister(soapTM, defaultNillable, Date.class, Soap11.ENCODED_DATETIME, new DateTimeType());
        defaultRegister(soapTM, defaultNillable, java.sql.Date.class, Soap11.ENCODED_DATETIME,
                        new SqlDateType());
        defaultRegister(soapTM, defaultNillable, Calendar.class, Soap11.ENCODED_DATETIME, new CalendarType());
        defaultRegister(soapTM, defaultNillable, byte[].class, Soap11.ENCODED_BASE64, new Base64Type());
        defaultRegister(soapTM, defaultNillable, BigDecimal.class, Soap11.ENCODED_DECIMAL,
                        new BigDecimalType());
        defaultRegister(soapTM, defaultNillable, BigInteger.class, Soap11.ENCODED_INTEGER,
                        new BigIntegerType());

        return soapTM;
    }

    public static DefaultTypeMapping createDefaultTypeMapping(boolean defaultNillable, 
                                                              boolean enableMtomXmime) {
        // by convention, the default mapping is against the XML schema URI.
        DefaultTypeMapping tm = new DefaultTypeMapping(SOAPConstants.XSD);
        fillStandardMappings(tm, defaultNillable, enableMtomXmime);
        defaultRegister(tm, defaultNillable, Character.class, 
                        CharacterAsStringType.CHARACTER_AS_STRING_TYPE_QNAME,
                        new CharacterAsStringType());
        defaultRegister(tm, defaultNillable, char.class, 
                        CharacterAsStringType.CHARACTER_AS_STRING_TYPE_QNAME,
                        new CharacterAsStringType());

        defaultRegister(tm, defaultNillable, javax.xml.datatype.Duration.class, XMLSchemaQNames.XSD_DURATION,
                            new org.apache.cxf.aegis.type.java5.DurationType());
        defaultRegister(tm, defaultNillable, javax.xml.datatype.XMLGregorianCalendar.class,
                            XMLSchemaQNames.XSD_DATE,
                            new org.apache.cxf.aegis.type.java5.XMLGregorianCalendarType());
        defaultRegister(tm, defaultNillable, javax.xml.datatype.XMLGregorianCalendar.class,
                            XMLSchemaQNames.XSD_TIME,
                            new org.apache.cxf.aegis.type.java5.XMLGregorianCalendarType());
        defaultRegister(tm, defaultNillable, javax.xml.datatype.XMLGregorianCalendar.class,
                            XMLSchemaQNames.XSD_G_DAY,
                            new org.apache.cxf.aegis.type.java5.XMLGregorianCalendarType());
        defaultRegister(tm, defaultNillable, javax.xml.datatype.XMLGregorianCalendar.class,
                            XMLSchemaQNames.XSD_G_MONTH,
                            new org.apache.cxf.aegis.type.java5.XMLGregorianCalendarType());
        defaultRegister(tm, defaultNillable, javax.xml.datatype.XMLGregorianCalendar.class,
                            XMLSchemaQNames.XSD_G_MONTH_DAY,
                            new org.apache.cxf.aegis.type.java5.XMLGregorianCalendarType());
        defaultRegister(tm, defaultNillable, javax.xml.datatype.XMLGregorianCalendar.class,
                            XMLSchemaQNames.XSD_G_YEAR,
                            new org.apache.cxf.aegis.type.java5.XMLGregorianCalendarType());
        defaultRegister(tm, defaultNillable, javax.xml.datatype.XMLGregorianCalendar.class,
                            XMLSchemaQNames.XSD_G_YEAR_MONTH,
                            new org.apache.cxf.aegis.type.java5.XMLGregorianCalendarType());
        defaultRegister(tm, defaultNillable, javax.xml.datatype.XMLGregorianCalendar.class,
                            XMLSchemaQNames.XSD_DATETIME,
                            new org.apache.cxf.aegis.type.java5.XMLGregorianCalendarType());
        return tm;
    }

    public String getMappingIdentifierURI() {
        return identifierURI;
    }

    public void setMappingIdentifierURI(String uri) {
        identifierURI = uri;
        
    }
}
