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

package org.apache.cxf.ws.eventing.shared.utils;

import java.util.GregorianCalendar;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.cxf.ws.eventing.ExpirationType;

public final class DurationAndDateUtil {

    private static DatatypeFactory factory;

    static {
        try {
            factory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
            throw new RuntimeException(
                    "Cannot instantiate a DatatypeFactory required for unmarshalling "
                            + "to XMLGregorianCalendar and Duration",
                    ex);
        }
    }

    private DurationAndDateUtil() {

    }

    public static Duration parseDuration(String input) throws IllegalArgumentException {
        return factory.newDuration(input);
    }

    public static XMLGregorianCalendar parseXMLGregorianCalendar(String input)
        throws IllegalArgumentException {
        return factory.newXMLGregorianCalendar(input);
    }

    public static boolean isXMLGregorianCalendar(String input) {
        try {
            factory.newXMLGregorianCalendar(input);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean isDuration(String input) {
        try {
            factory.newDuration(input);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static Object parseDurationOrTimestamp(String input) throws IllegalArgumentException {
        Object ret;
        try {
            ret = factory.newDuration(input);
        } catch (Exception e) {
            ret = factory.newXMLGregorianCalendar(input);
        }
        return ret;
    }

    public static String convertToXMLString(Object input) {
        if (input instanceof XMLGregorianCalendar) {
            return ((XMLGregorianCalendar)input).toXMLFormat();
        }
        if (input instanceof Duration) {
            return ((Duration)input).toString();
        }
        throw new IllegalArgumentException(
                "convertToXMLString requires either an instance of XMLGregorianCalendar or Duration");
    }

    public static ExpirationType toExpirationTypeContainingGregorianCalendar(XMLGregorianCalendar date) {
        ExpirationType et = new ExpirationType();
        et.setValue(date.toXMLFormat());
        return et;
    }

    public static ExpirationType toExpirationTypeContainingDuration(XMLGregorianCalendar date) {
        ExpirationType et = new ExpirationType();
        XMLGregorianCalendar now = factory.newXMLGregorianCalendar(new GregorianCalendar());
        XMLGregorianCalendar then = factory.newXMLGregorianCalendar(date.toGregorianCalendar());
        long durationMillis = then.toGregorianCalendar().getTimeInMillis()
                - now.toGregorianCalendar().getTimeInMillis();
        Duration duration = factory.newDuration(durationMillis);
        et.setValue(duration.toString());
        return et;
    }

    public static boolean isPT0S(Duration duration) {
        return "PT0S".equals(duration.toString());
    }

}
