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
package org.apache.cxf.tools.common;

import java.util.Calendar;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

public final class DataTypeAdapter {

    private DataTypeAdapter() {
    }

    public static Date parseDate(String s) {
        if (s == null) {
            return null;
        }
        return DatatypeConverter.parseDate(s).getTime();
    }
    public static String printDate(Date dt) {
        if (dt == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        return DatatypeConverter.printDate(c);
    }

    public static Date parseTime(String s) {
        if (s == null) {
            return null;
        }
        return DatatypeConverter.parseTime(s).getTime();
    }
    public static String printTime(Date dt) {
        if (dt == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        return DatatypeConverter.printTime(c);
    }

    public static Date parseDateTime(String s) {
        if (s == null) {
            return null;
        }
        return DatatypeConverter.parseDateTime(s).getTime();
    }
    public static String printDateTime(Date dt) {
        if (dt == null) {
            return null;
        }
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        return DatatypeConverter.printDateTime(c);
    }
}