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
package org.apache.cxf.binding.corba.interceptors;

import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;

public final class SystemExceptionHelper 
    implements org.omg.CORBA.portable.Streamable {
    
    private static final int BAD_CONTEXT = 0;
    private static final int BAD_INV_ORDER = 1;
    private static final int BAD_OPERATION = 2;
    private static final int BAD_PARAM = 3;
    private static final int BAD_QOS = 4;
    private static final int BAD_TYPECODE = 5;
    private static final int CODESET_INCOMPATIBLE = 6;
    private static final int COMM_FAILURE = 7;
    private static final int DATA_CONVERSION = 8;
    private static final int FREE_MEM = 9;
    private static final int IMP_LIMIT = 10;
    private static final int INITIALIZE = 11;
    private static final int INTERNAL = 12;
    private static final int INTF_REPOS = 13;
    private static final int INVALID_TRANSACTION = 14;
    private static final int INV_FLAG = 15;
    private static final int INV_IDENT = 16;
    private static final int INV_OBJREF = 17;
    private static final int INV_POLICY = 18;
    private static final int MARSHAL = 19;
    private static final int NO_IMPLEMENT = 20;
    private static final int NO_MEMORY = 21;
    private static final int NO_PERMISSION = 22;
    private static final int NO_RESOURCES = 23;
    private static final int NO_RESPONSE = 24;
    private static final int OBJECT_NOT_EXIST = 25;
    private static final int OBJ_ADAPTER = 26;
    private static final int PERSIST_STORE = 27;
    private static final int REBIND = 28;
    private static final int TIMEOUT = 29;
    private static final int TRANSACTION_MODE = 30;
    private static final int TRANSACTION_REQUIRED = 31;
    private static final int TRANSACTION_ROLLEDBACK = 32;
    private static final int TRANSACTION_UNAVAILABLE = 33;
    private static final int TRANSIENT = 34;
    private static final int UNKNOWN = 35;
    
    private static final String[] IDS = {
        "IDL:omg.org/CORBA/BAD_CONTEXT:1.0",
        "IDL:omg.org/CORBA/BAD_INV_ORDER:1.0",
        "IDL:omg.org/CORBA/BAD_OPERATION:1.0", "IDL:omg.org/CORBA/BAD_PARAM:1.0",
        "IDL:omg.org/CORBA/BAD_QOS:1.0", "IDL:omg.org/CORBA/BAD_TYPECODE:1.0",
        "IDL:omg.org/CORBA/CODESET_INCOMPATIBLE:1.0",
        "IDL:omg.org/CORBA/COMM_FAILURE:1.0",
        "IDL:omg.org/CORBA/DATA_CONVERSION:1.0",
        "IDL:omg.org/CORBA/FREE_MEM:1.0", "IDL:omg.org/CORBA/IMP_LIMIT:1.0",
        "IDL:omg.org/CORBA/INITIALIZE:1.0", "IDL:omg.org/CORBA/INTERNAL:1.0",
        "IDL:omg.org/CORBA/INTF_REPOS:1.0",
        "IDL:omg.org/CORBA/INVALID_TRANSACTION:1.0",
        "IDL:omg.org/CORBA/INV_FLAG:1.0", "IDL:omg.org/CORBA/INV_IDENT:1.0",
        "IDL:omg.org/CORBA/INV_OBJREF:1.0", "IDL:omg.org/CORBA/INV_POLICY:1.0",
        "IDL:omg.org/CORBA/MARSHAL:1.0", "IDL:omg.org/CORBA/NO_IMPLEMENT:1.0",
        "IDL:omg.org/CORBA/NO_MEMORY:1.0", "IDL:omg.org/CORBA/NO_PERMISSION:1.0",
        "IDL:omg.org/CORBA/NO_RESOURCES:1.0",
        "IDL:omg.org/CORBA/NO_RESPONSE:1.0",
        "IDL:omg.org/CORBA/OBJECT_NOT_EXIST:1.0",
        "IDL:omg.org/CORBA/OBJ_ADAPTER:1.0",
        "IDL:omg.org/CORBA/PERSIST_STORE:1.0", "IDL:omg.org/CORBA/REBIND:1.0",
        "IDL:omg.org/CORBA/TIMEOUT:1.0",
        "IDL:omg.org/CORBA/TRANSACTION_MODE:1.0",
        "IDL:omg.org/CORBA/TRANSACTION_REQUIRED:1.0",
        "IDL:omg.org/CORBA/TRANSACTION_ROLLEDBACK:1.0",
        "IDL:omg.org/CORBA/TRANSACTION_UNAVAILABLE:1.0",
        "IDL:omg.org/CORBA/TRANSIENT:1.0", "IDL:omg.org/CORBA/UNKNOWN:1.0"};


    SystemException value;
    TypeCode typeCode;
    
    private SystemExceptionHelper() {
    }
    private SystemExceptionHelper(SystemException ex) {
        value = ex;
    }
    
    

    private static int binarySearch(String[] arr, String value) {
        int left = 0;
        int right = arr.length;
        int index = -1;

        while (left < right) {
            int m = (left + right) / 2;
            int res = arr[m].compareTo(value);
            if (res == 0) {
                index = m;
                break;
            } else if (res > 0) {
                right = m;
            } else {
                left = m + 1;
            }
        }

        return index;
    }


    public static void insert(Any any, SystemException val) {
        any.insert_Streamable(new SystemExceptionHelper(val));
    }

    
    
    //CHECKSTYLE:OFF 
    //NCSS is to high for this due to the massive switch statement
    public static SystemException read(org.omg.CORBA.portable.InputStream in) {

        String id = in.read_string();
        int minor = in.read_ulong();
        org.omg.CORBA.CompletionStatus status = org.omg.CORBA.CompletionStatus.from_int(in.read_ulong());

        int n = binarySearch(IDS, id);
        SystemException ex = null;
        switch (n) {
        case BAD_CONTEXT:
            ex = new org.omg.CORBA.BAD_CONTEXT(minor, status);
            break;
        case BAD_INV_ORDER:
            ex = new org.omg.CORBA.BAD_INV_ORDER(minor, status);
            break;
        case BAD_OPERATION:
            ex = new org.omg.CORBA.BAD_OPERATION(minor, status);
            break;
        case BAD_PARAM:
            ex = new org.omg.CORBA.BAD_PARAM(minor, status);
            break;
        case BAD_QOS:
            ex = new org.omg.CORBA.BAD_QOS(minor, status);
            break;
        case BAD_TYPECODE:
            ex = new org.omg.CORBA.BAD_TYPECODE(minor, status);
            break;
        case CODESET_INCOMPATIBLE:
            ex = new org.omg.CORBA.CODESET_INCOMPATIBLE(minor, status);
            break;
        case COMM_FAILURE:
            ex = new org.omg.CORBA.COMM_FAILURE(minor, status);
            break;
        case DATA_CONVERSION:
            ex = new org.omg.CORBA.DATA_CONVERSION(minor, status);
            break;
        case FREE_MEM:
            ex = new org.omg.CORBA.FREE_MEM(minor, status);
            break;
        case IMP_LIMIT:
            ex = new org.omg.CORBA.IMP_LIMIT(minor, status);
            break;
        case INITIALIZE:
            ex = new org.omg.CORBA.INITIALIZE(minor, status);
            break;
        case INTERNAL:
            ex = new org.omg.CORBA.INTERNAL(minor, status);
            break;
        case INTF_REPOS:
            ex = new org.omg.CORBA.INTF_REPOS(minor, status);
            break;
        case INVALID_TRANSACTION:
            ex = new org.omg.CORBA.INVALID_TRANSACTION(minor, status);
            break;
        case INV_FLAG:
            ex = new org.omg.CORBA.INV_FLAG(minor, status);
            break;
        case INV_IDENT:
            ex = new org.omg.CORBA.INV_IDENT(minor, status);
            break;
        case INV_OBJREF:
            ex = new org.omg.CORBA.INV_OBJREF(minor, status);
            break;
        case INV_POLICY:
            ex = new org.omg.CORBA.INV_POLICY(minor, status);
            break;
        case MARSHAL:
            ex = new org.omg.CORBA.MARSHAL(minor, status);
            break;
        case NO_IMPLEMENT:
            ex = new org.omg.CORBA.NO_IMPLEMENT(minor, status);
            break;
        case NO_MEMORY:
            ex = new org.omg.CORBA.NO_MEMORY(minor, status);
            break;
        case NO_PERMISSION:
            ex = new org.omg.CORBA.NO_PERMISSION(minor, status);
            break;
        case NO_RESOURCES:
            ex = new org.omg.CORBA.NO_RESOURCES(minor, status);
            break;
        case NO_RESPONSE:
            ex = new org.omg.CORBA.NO_RESPONSE(minor, status);
            break;
        case OBJECT_NOT_EXIST:
            ex = new org.omg.CORBA.OBJECT_NOT_EXIST(minor, status);
            break;
        case OBJ_ADAPTER:
            ex = new org.omg.CORBA.OBJ_ADAPTER(minor, status);
            break;
        case PERSIST_STORE:
            ex = new org.omg.CORBA.PERSIST_STORE(minor, status);
            break;
        case REBIND:
            ex = new org.omg.CORBA.REBIND(minor, status);
            break;
        case TIMEOUT:
            ex = new org.omg.CORBA.TIMEOUT(minor, status);
            break;
        case TRANSACTION_MODE:
            ex = new org.omg.CORBA.TRANSACTION_MODE(minor, status);
            break;
        case TRANSACTION_REQUIRED:
            ex = new org.omg.CORBA.TRANSACTION_REQUIRED(minor, status);
            break;
        case TRANSACTION_ROLLEDBACK:
            ex = new org.omg.CORBA.TRANSACTION_ROLLEDBACK(minor, status);
            break;
        case TRANSACTION_UNAVAILABLE:
            ex = new org.omg.CORBA.TRANSACTION_UNAVAILABLE(minor, status);
            break;
        case TRANSIENT:
            ex = new org.omg.CORBA.TRANSIENT(minor, status);
            break;
        case UNKNOWN:
        default:
            ex = new org.omg.CORBA.UNKNOWN(minor, status);
        }
        return ex;
    }
    //CHECKSTYLE:ON
    
    
    
    public void _read(InputStream instream) {
        value = read(instream);
    }
    
    public TypeCode _type() {
        if (typeCode == null) {
            ORB orb = ORB.init();
            StructMember[] smBuf = new StructMember[2];
            TypeCode minortc = orb.get_primitive_tc(TCKind.tk_long);
            smBuf[0] = new StructMember("minor", minortc, null);

            String csLabels[] = {"COMPLETED_YES", "COMPLETED_NO", "COMPLETED_MAYBE"};
            TypeCode completedtc = orb
                .create_enum_tc("IDL:omg.org/CORBA/CompletionStatus:1.0",
                              "CompletionStatus", csLabels);

            smBuf[1] = new StructMember("completed", completedtc, null);
            String id;
            String name;
            if (value == null) {
                name = "SystemException";
                id = "IDL:omg.org/CORBA/SystemException:1.0";
            } else {
                String className = value.getClass().getName();
                name = className.substring(className.lastIndexOf('.') + 1);
                id = "IDL:omg.org/CORBA/" + name + ":1.0";
            }
            
            typeCode = orb.create_exception_tc(id, name, smBuf);
        }
        return typeCode;
    }
    public void _write(OutputStream outstream) {
        String id;
        if (value == null) {
            value = new org.omg.CORBA.UNKNOWN();
            id = "IDL:omg.org/CORBA/UNKNOWN";
        } else {
            String className = value.getClass().getName();
            id = "IDL:omg.org/CORBA/" 
                + className.substring(className.lastIndexOf('.') + 1) + ":1.0";
        }

        outstream.write_string(id);
        outstream.write_ulong(value.minor);
        outstream.write_ulong(value.completed.value());
    }

}
