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
package org.apache.cxf.aegis.type.encoded;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;

import static org.apache.cxf.aegis.type.encoded.SoapEncodingUtil.readAttributeValue;

public class ArrayTypeInfo {
    private static final String SOAP_ENCODING_NS_1_1 = Soap11.getInstance().getSoapEncodingStyle();
    private static final QName SOAP_ARRAY_TYPE = new QName(SOAP_ENCODING_NS_1_1, "arrayType");
    private static final QName SOAP_ARRAY_OFFSET = new QName(SOAP_ENCODING_NS_1_1, "offset");

    private Type type;
    private QName typeName;
    private int ranks;
    private final List<Integer> dimensions = new ArrayList<Integer>();
    private int offset;

    public ArrayTypeInfo(QName typeName, int ranks, Integer... dimensions) {
        this.typeName = typeName;
        this.ranks = ranks;
        this.dimensions.addAll(Arrays.asList(dimensions));
    }

    public ArrayTypeInfo(MessageReader reader, TypeMapping tm) {
        this(reader.getXMLStreamReader().getNamespaceContext(),
             readAttributeValue(reader, SOAP_ARRAY_TYPE), readAttributeValue(reader, SOAP_ARRAY_OFFSET));

        // if type is xsd:ur-type replace it with xsd:anyType
        String namespace = reader.getNamespaceForPrefix(typeName.getPrefix());
        if (!StringUtils.isEmpty(namespace)) {
            if (SOAPConstants.XSD.equals(namespace) && "ur-type".equals(typeName.getLocalPart())) {
                typeName = new QName(namespace, "anyType", typeName.getPrefix());
            } else {
                typeName = new QName(namespace, typeName.getLocalPart(), typeName.getPrefix());
            }
        }
        
        if (tm != null) {
            type = tm.getType(typeName);

            if (ranks > 0) {
                Class componentType = type.getTypeClass();
                for (int i = 1; i < ranks + dimensions.size(); i++) {
                    componentType = Array.newInstance(componentType, 0).getClass();
                }

                SoapArrayType arrayType = new SoapArrayType();
                arrayType.setTypeClass(componentType);
                arrayType.setTypeMapping(type.getTypeMapping());
                type = arrayType;
            }
        }
    }

    public ArrayTypeInfo(NamespaceContext namespaceContext, String arrayTypeValue) {
        this(namespaceContext, arrayTypeValue, null);
    }

    public ArrayTypeInfo(NamespaceContext namespaceContext, String arrayTypeValue, String offsetString) {
        if (arrayTypeValue == null) {
            throw new NullPointerException("arrayTypeValue is null");
        }

        // arrayTypeValue = atype , asize ;
        // atype          = QName , [ rank ] ;
        // rank           = "[" , { "," } , "]" ;
        // asize          = "[" , length , { ","  length} , "]" ;
        // length         = DIGIT , { DIGIT } ;
        //
        // x:foo[,,,][1,2,3,4]

        StringTokenizer tokenizer = new StringTokenizer(arrayTypeValue, "[],:", true);
        List<String> tokens = CastUtils.cast(Collections.list(tokenizer));

        // ArrayType QName
        if (tokens.size() < 3) {
            throw new DatabindingException("Invalid ArrayType value " + arrayTypeValue);
        }
        if (tokens.get(1).equals(":")) {
            typeName = 
                new QName(namespaceContext.getNamespaceURI(tokens.get(0)), tokens.get(2), tokens.get(0));
            tokens = tokens.subList(3, tokens.size());
        } else {
            typeName = new QName("", tokens.get(0));
            tokens = tokens.subList(1, tokens.size());
        }

        if (!tokens.get(0).equals("[")) {
            throw new DatabindingException("Invalid ArrayType value " + arrayTypeValue);
        }

        // Rank: [,,,,]
        boolean hasRank = tokens.subList(1, tokens.size()).contains("[");
        if (hasRank) {
            // there are atleast [] there is one rank
            ranks = 1;
            for (String token : tokens.subList(1, tokens.size())) {
                if ("]".equals(token)) {
                    if (tokens.size() < ranks + 1) {
                        throw new DatabindingException("Invalid ArrayType value " + arrayTypeValue);
                    }
                    tokens = tokens.subList(ranks + 1, tokens.size());
                    break;
                } else if (",".equals(token)) {
                    ranks++;
                } else {
                    throw new DatabindingException("Invalid ArrayType value " + arrayTypeValue);
                }
            }
        }

        // Dimensions [1,2,3,4]
        for (int i = 1; i < tokens.size(); i = i + 2) {
            String dimension = tokens.get(i);
            if ("]".equals(dimension)) {
                if (i + 1 != tokens.size()) {
                    throw new DatabindingException("Invalid ArrayType value " + arrayTypeValue);
                }
                break;
            }

            int value;
            try {
                value = Integer.parseInt(dimension);
            } catch (NumberFormatException e) {
                throw new DatabindingException("Invalid ArrayType value " + arrayTypeValue);
            }
            if (value < 1) {
                throw new DatabindingException("Invalid ArrayType value " + arrayTypeValue);
            }
            dimensions.add(value);

            // verify next token is a ',' or ']'
            String next = tokens.get(i + 1);
            if (!",".equals(next) && !"]".equals(next)) {
                throw new DatabindingException("Invalid ArrayType value " + arrayTypeValue);
            }
        }

        if (dimensions.isEmpty()) {
            throw new DatabindingException("Invalid ArrayType value " + arrayTypeValue);
        }

        if (offsetString != null) {
            // offset = "[" , length , "]" ;
            tokens = CastUtils.cast(Collections.list(new StringTokenizer(offsetString, "[]", true)));
            if (tokens.size() != 3 || !"[".equals(tokens.get(0)) || !"]".equals(tokens.get(2))) {
                throw new DatabindingException("Invalid Array offset value " + offsetString);
            }
            try {
                offset = Integer.parseInt(tokens.get(1));
            } catch (NumberFormatException e) {
                throw new DatabindingException("Invalid Array offset value " + offsetString);
            }
        }
    }

    public QName getTypeName() {
        return typeName;
    }

    public Type getType() {
        return type;
    }

    public int getRanks() {
        return ranks;
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public int getTotalDimensions() {
        return ranks + dimensions.size();
    }

    public int getOffset() {
        return offset;
    }

    public void writeAttribute(MessageWriter writer) {
        String value = toString();
        SoapEncodingUtil.writeAttribute(writer, SOAP_ARRAY_TYPE, value);
    }

    public String toString() {
        StringBuilder string = new StringBuilder();
        
        // no prefix handed to us by someone else ...
        if ("".equals(typeName.getPrefix()) && !"".equals(typeName.getNamespaceURI())) {
            throw new RuntimeException("No prefix provided in QName for " + typeName.getNamespaceURI());
        }

        // typeName: foo:bar
        if (typeName.getPrefix() != null && typeName.getPrefix().length() > 0) {
            string.append(typeName.getPrefix()).append(":");
        }
        string.append(typeName.getLocalPart());

        // ranks: [,,,,]
        if (ranks > 0) {
            string.append("[");
            for (int i = 1; i < ranks; i++) {
                string.append(",");
            }
            string.append("]");
        }

        // dimensions: [2,3,4]
        string.append("[");
        string.append(dimensions.get(0));
        for (int dimension : dimensions.subList(1, dimensions.size())) {
            string.append(",").append(dimension);

        }
        string.append("]");

        return string.toString();
    }
}
