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

import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class CorbaUtils {
    
    protected static final Set<Object> IDL_RESERVED_WORDS = new TreeSet<Object>();
    protected static final Set<Object> IGNORED_MODULES = new TreeSet<Object>();    
    protected static final Set<Object> TIMEBASE_IDL_DEFS = new TreeSet<Object>();
    
    static {
        /* IDL Key Words */
        IDL_RESERVED_WORDS.add("abstract");
        IDL_RESERVED_WORDS.add("double");
        IDL_RESERVED_WORDS.add("local");
        IDL_RESERVED_WORDS.add("raises");
        IDL_RESERVED_WORDS.add("typedef");
        IDL_RESERVED_WORDS.add("any");
        IDL_RESERVED_WORDS.add("exception");
        IDL_RESERVED_WORDS.add("long");
        IDL_RESERVED_WORDS.add("readonly");
        IDL_RESERVED_WORDS.add("unsigned");
        IDL_RESERVED_WORDS.add("attribute");
        IDL_RESERVED_WORDS.add("enum");
        IDL_RESERVED_WORDS.add("module");
        IDL_RESERVED_WORDS.add("sequence");
        IDL_RESERVED_WORDS.add("union");
        IDL_RESERVED_WORDS.add("boolean");
        IDL_RESERVED_WORDS.add("factory");
        IDL_RESERVED_WORDS.add("native");
        IDL_RESERVED_WORDS.add("short");
        IDL_RESERVED_WORDS.add("ValueBase");
        IDL_RESERVED_WORDS.add("case");
        IDL_RESERVED_WORDS.add("FALSE");
        IDL_RESERVED_WORDS.add("Object");
        IDL_RESERVED_WORDS.add("string");
        IDL_RESERVED_WORDS.add("valuetype");
        IDL_RESERVED_WORDS.add("char");
        IDL_RESERVED_WORDS.add("fixed");
        IDL_RESERVED_WORDS.add("octet");
        IDL_RESERVED_WORDS.add("struct");
        IDL_RESERVED_WORDS.add("void");
        IDL_RESERVED_WORDS.add("const");
        IDL_RESERVED_WORDS.add("float");
        IDL_RESERVED_WORDS.add("oneway");
        IDL_RESERVED_WORDS.add("supports");
        IDL_RESERVED_WORDS.add("wchar");
        IDL_RESERVED_WORDS.add("context");
        IDL_RESERVED_WORDS.add("in");
        IDL_RESERVED_WORDS.add("out");
        IDL_RESERVED_WORDS.add("switch");
        IDL_RESERVED_WORDS.add("wstring");
        IDL_RESERVED_WORDS.add("custom");
        IDL_RESERVED_WORDS.add("inout");
        IDL_RESERVED_WORDS.add("private");
        IDL_RESERVED_WORDS.add("TRUE");
        IDL_RESERVED_WORDS.add("default");
        IDL_RESERVED_WORDS.add("interface");
        IDL_RESERVED_WORDS.add("public");
        IDL_RESERVED_WORDS.add("truncatable");

        /**
         * Well known IDL Definitions         
         */
        IGNORED_MODULES.add("TimeBase");

        //Time Base Definitions as defined in <omg/TimeBase.idl>
        /*TIMEBASE_IDL_DEFS.add("TimeBase.TimeT");
        TIMEBASE_IDL_DEFS.add("TimeBase.InaccuracyT");
        TIMEBASE_IDL_DEFS.add("TimeBase.TdfT");
        TIMEBASE_IDL_DEFS.add("TimeBase.IntervalT");
        TIMEBASE_IDL_DEFS.add("TimeBase.UtcT");*/
    }
    
    protected CorbaUtils() {        
    }

    
    public static String mangleName(String cname) {
        while (isCollideWithKeyWord(cname)) {
            cname = "_" + cname;
        }

        if (cname.indexOf("..") != -1) {
            cname = cname.replace('.', '_');
        } else if (cname.indexOf("$") != -1) {
            cname = cname.replace('$', '_');
        } else if (cname.indexOf("?") != -1) {
            if (cname.length() == 1) {
                cname = "u0063";
            } else {
                StringTokenizer tokenizer = new StringTokenizer(cname, "?");        
                String str = "";

                while (tokenizer.hasMoreTokens()) {
                    String s = tokenizer.nextToken();
                    str = s + "u0063";
                }
                cname = str;
            }
        } else if (cname.indexOf("-") != -1) {
            cname = cname.replace('-', '_');
        } else if (cname.indexOf("/") != -1) {
            cname = cname.replace('/', '_');
        }

        return cname;
    }


    public static String mangleEnumIdentifier(String identifier) {
        String value = mangleName(identifier);

        value = value.replace(' ', '_');
        value = value.replace('.', '_');

        if (value.indexOf(":") != -1) {
            value = value.substring(value.lastIndexOf(":") + 1);
        }
        String fletter = value.substring(0, 1);

        try {
            Integer.parseInt(fletter);
            value = "e_" + value;
        } catch (NumberFormatException e) {
            //
        }

        if (value.startsWith("__")) {
            value = value.substring(1);
        }
        return value;
    }


    public static boolean isReservedWord(String name) {
        return IDL_RESERVED_WORDS.contains(name);
    }
    
    public static boolean isCollideWithKeyWord(String name) {
        Iterator it = IDL_RESERVED_WORDS.iterator();
        while (it.hasNext()) {
            String key = (String)it.next();
            if (key.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTimeBaseDef(String name) {
        return TIMEBASE_IDL_DEFS.contains(name);
    }
    
    public static boolean ignoreModule(String name) {
        return IGNORED_MODULES.contains(name);
    }

}
