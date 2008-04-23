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
import org.omg.CORBA.CompletionStatusHelper;
import org.omg.CORBA.ORB;
import org.omg.CORBA.StructMember;
import org.omg.CORBA.SystemException;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

public final class SystemExceptionHelper {
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
    
    

    private static final String[] CLASSES = {
        "org.omg.CORBA.BAD_CONTEXT", "org.omg.CORBA.BAD_INV_ORDER",
        "org.omg.CORBA.BAD_OPERATION", "org.omg.CORBA.BAD_PARAM",
        "org.omg.CORBA.BAD_QOS", "org.omg.CORBA.BAD_TYPECODE",
        "org.omg.CORBA.CODESET_INCOMPATIBLE", "org.omg.CORBA.COMM_FAILURE",
        "org.omg.CORBA.DATA_CONVERSION", "org.omg.CORBA.FREE_MEM",
        "org.omg.CORBA.IMP_LIMIT", "org.omg.CORBA.INITIALIZE",
        "org.omg.CORBA.INTERNAL", "org.omg.CORBA.INTF_REPOS",
        "org.omg.CORBA.INVALID_TRANSACTION", "org.omg.CORBA.INV_FLAG",
        "org.omg.CORBA.INV_IDENT", "org.omg.CORBA.INV_OBJREF",
        "org.omg.CORBA.INV_POLICY", "org.omg.CORBA.MARSHAL",
        "org.omg.CORBA.NO_IMPLEMENT", "org.omg.CORBA.NO_MEMORY",
        "org.omg.CORBA.NO_PERMISSION", "org.omg.CORBA.NO_RESOURCES",
        "org.omg.CORBA.NO_RESPONSE", "org.omg.CORBA.OBJECT_NOT_EXIST",
        "org.omg.CORBA.OBJ_ADAPTER", "org.omg.CORBA.PERSIST_STORE",
        "org.omg.CORBA.REBIND", "org.omg.CORBA.TIMEOUT",
        "org.omg.CORBA.TRANSACTION_MODE",
        "org.omg.CORBA.TRANSACTION_REQUIRED",
        "org.omg.CORBA.TRANSACTION_ROLLEDBACK",
        "org.omg.CORBA.TRANSACTION_UNAVAILABLE", "org.omg.CORBA.TRANSIENT",
        "org.omg.CORBA.UNKNOWN"};

    private static final String[] NAMES = {
        "BAD_CONTEXT", "BAD_INV_ORDER", "BAD_OPERATION", "BAD_PARAM",
        "BAD_QOS", "BAD_TYPECODE", "CODESET_INCOMPATIBLE", "COMM_FAILURE",
        "DATA_CONVERSION", "FREE_MEM", "IMP_LIMIT", "INITIALIZE", "INTERNAL",
        "INTF_REPOS", "INVALID_TRANSACTION", "INV_FLAG", "INV_IDENT",
        "INV_OBJREF", "INV_POLICY", "MARSHAL", "NO_IMPLEMENT", "NO_MEMORY",
        "NO_PERMISSION", "NO_RESOURCES", "NO_RESPONSE", "OBJECT_NOT_EXIST",
        "OBJ_ADAPTER", "PERSIST_STORE", "REBIND", "TIMEOUT",
        "TRANSACTION_MODE", "TRANSACTION_REQUIRED", "TRANSACTION_ROLLEDBACK",
        "TRANSACTION_UNAVAILABLE", "TRANSIENT", "UNKNOWN"};

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

    private static TypeCode typeCode;

    private SystemExceptionHelper() {
        //utility class
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

    private static TypeCode createTypeCode(String id, String name) {
        ORB orb = ORB.init();
        StructMember[] members = new StructMember[2];
        members[0] = new StructMember();
        members[0].name = "minor";
        members[0].type = orb.get_primitive_tc(TCKind.tk_ulong);
        members[1] = new StructMember();
        members[1].name = "completed";
        members[1].type = CompletionStatusHelper.type();
        return orb.create_exception_tc(id, name, members);
    }

    private static void writeImpl(org.omg.CORBA.portable.OutputStream out, 
                                  SystemException val, String id) {
        out.write_string(id);
        out.write_ulong(val.minor);
        out.write_ulong(val.completed.value());
    }

    public static void insert(Any any, SystemException val) {
        String className = val.getClass().getName();
        int index = binarySearch(CLASSES, className);

        String id;
        if (index == -1) {
            id = IDS[UNKNOWN];
        } else {
            id = IDS[index];
        }

        org.omg.CORBA.portable.OutputStream out = any.create_output_stream();
        writeImpl(out, val, id);
        any.read_value(out.create_input_stream(), createTypeCode(id, NAMES[index]));
    }

    public static SystemException extract(Any any) {
        try {
            TypeCode tc = any.type();
            String id = tc.id();
            if (tc.kind() == TCKind.tk_except && (id.length() == 0 || binarySearch(IDS, id) != -1)) {
                return read(any.create_input_stream());
            }
        } catch (org.omg.CORBA.TypeCodePackage.BadKind ex) {
            //ignore
        }

        throw new org.omg.CORBA.BAD_OPERATION();
    }

    public static synchronized TypeCode type() {
        if (typeCode == null) {
            typeCode = createTypeCode(id(), "SystemException");
        }

        return typeCode;
    }

    public static String id() {
        return "IDL:omg.org/CORBA/SystemException:1.0";
    }
    public static void write(org.omg.CORBA.portable.OutputStream out, SystemException val) {
        String className = val.getClass().getName();
        int index = binarySearch(CLASSES, className);

        String id;
        if (index == -1) {
            id = IDS[UNKNOWN];
        } else {
            id = IDS[index];
        }

        writeImpl(out, val, id);
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

}
