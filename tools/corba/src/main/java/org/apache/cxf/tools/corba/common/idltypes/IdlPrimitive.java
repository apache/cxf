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

package org.apache.cxf.tools.corba.common.idltypes;

import java.io.PrintWriter;

public final class IdlPrimitive extends IdlDefnImplBase implements IdlType {
    static final short MINIMUM = 1;
    static final short LONG = MINIMUM;
    static final short ULONG = 2;
    static final short LONGLONG = 3;
    static final short ULONGLONG = 4;
    static final short SHORT = 5;
    static final short USHORT = 6;
    static final short FLOAT = 7;
    static final short DOUBLE = 8;
    static final short LONGDOUBLE = 9;
    static final short CHAR = 10;
    static final short WCHAR = 11;
    static final short BOOLEAN = 12;
    static final short OCTET = 13;
    static final short ANY = 14;
    static final short MAXIMUM = 15;
    static final short DATETIME = MAXIMUM;
    private String wsdlName;
    private short type;

    private IdlPrimitive(IdlScopeBase parent, String name, String wsdlFileName, short typeValue) {
        super(parent, name);
        this.wsdlName = new String(wsdlFileName);
        this.type = typeValue;
    }    
    
    public static IdlPrimitive create(IdlScopeBase parent, short type) {
        String name = null;
        String wsdlName = null;

        switch (type) {
        case LONG:
            name = new String("long");
            wsdlName = new String("long");

            break;

        case ULONG:
            name = new String("unsigned long");
            wsdlName = new String("ulong");

            break;

        case LONGLONG:
            name = new String("long long");
            wsdlName = new String("longlong");

            break;

        case ULONGLONG:
            name = new String("unsigned long long");
            wsdlName = new String("ulonglong");

            break;

        case SHORT:
            name = new String("short");
            wsdlName = new String("short");

            break;

        case USHORT:
            name = new String("unsigned short");
            wsdlName = new String("ushort");

            break;

        case FLOAT:
            name = new String("float");
            wsdlName = new String("float");

            break;

        case DOUBLE:
            name = new String("double");
            wsdlName = new String("double");

            break;

        case LONGDOUBLE:
            name = new String("long double");
            wsdlName = new String("longdouble");

            break;

        case CHAR:
            name = new String("char");
            wsdlName = new String("char");

            break;

        case WCHAR:
            name = new String("wchar");
            wsdlName = new String("wchar");

            break;

        case BOOLEAN:
            name = new String("boolean");
            wsdlName = new String("boolean");

            break;

        case OCTET:
            name = new String("octet");
            wsdlName = new String("octet");

            break;

        case ANY:
            name = new String("any");
            wsdlName = new String("any");

            break;

        case DATETIME:
            name = new String("TimeBase::UtcT");
            wsdlName = new String("dateTime");

            break;
            
        default:
            break;
        }

        return new IdlPrimitive(parent, name, wsdlName, type);
    }


    public String fullName() {
        return localName();
    }


    public String fullName(IdlScopedName rel) {
        return localName();
    }

    public IdlScopedName scopeName() {
        return null;
    }


    short primitiveType() {
        return type;
    }


    String wsdlName() {
        return this.wsdlName;
    }


    public void write(PrintWriter pw) {
        pw.print(localName());
    }
    
}

