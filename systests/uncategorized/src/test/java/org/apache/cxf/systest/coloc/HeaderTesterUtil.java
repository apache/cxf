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

package org.apache.cxf.systest.coloc;

public final class HeaderTesterUtil {
    public static final String IN_MESSAGE = "in message";
    public static final String IN_ORIGINATOR = "in originator";
    public static final String IN_REQUEST_TYPE = "in request type";

    public static final String OUT_MESSAGE_IN = "out message in";
    public static final String OUT_MESSAGE_OUT = "out message out";
    public static final String OUT_ORIGINATOR_IN = "out orginator in";
    public static final String OUT_ORIGINATOR_OUT = "out orginator out";
    public static final String OUT_REQUEST_TYPE = "out request test";
    public static final String OUT_RESPONSE_TYPE = "out Header type";

    public static final String INOUT_MESSAGE_IN = "inout message in";
    public static final String INOUT_MESSAGE_OUT = "inout message out";
    public static final String INOUT_ORIGINATOR_IN = "inout originator in";
    public static final String INOUT_ORIGINATOR_OUT = "inout orginator out";
    public static final String INOUT_REQUEST_TYPE_IN = "inout request type in";
    public static final String INOUT_REQUEST_TYPE_OUT = "inout request type out";
    static final String EX_STRING = "CXF RUNTIME EXCEPTION";
    
    
    private HeaderTesterUtil() {
        //utility class
    }
}