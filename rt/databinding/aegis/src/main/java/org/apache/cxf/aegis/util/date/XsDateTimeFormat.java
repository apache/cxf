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
package org.apache.cxf.aegis.util.date;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * <p>
 * An instance of {@link java.text.Format}, which may be used to parse and
 * format <code>xs:dateTime</code> values.
 * </p>
 */
public class XsDateTimeFormat extends Format {
    private static final long serialVersionUID = 3258131340871479609L;

    final boolean parseDate;

    final boolean parseTime;
    
    final boolean parseTimezone;

    XsDateTimeFormat(boolean pParseDate, boolean pParseTime) {
      this(pParseDate, pParseTime, true);
    }
    
    XsDateTimeFormat(boolean pParseDate, boolean pParseTime, boolean pParseTimezone) {
        parseDate = pParseDate;
        parseTime = pParseTime;
        parseTimezone = pParseTimezone;
    }

    /**
     * Creates a new instance.
     */
    public XsDateTimeFormat() {
        this(true, true, true);
    }


    public Object parseObject(String pString, ParsePosition pParsePosition) {
        if (pString == null) {
            throw new NullPointerException("The String argument must not be null.");
        }
        if (pParsePosition == null) {
            throw new NullPointerException("The ParsePosition argument must not be null.");
        }
        int offset = pParsePosition.getIndex();
        int idxSpc = pString.indexOf(' ', offset);
        int idxCom = pString.indexOf(',', offset);

        if (idxCom != -1 && idxCom < idxSpc) {
            idxSpc = idxCom;
        }
        String newVal = null;
        if (idxSpc == -1) {
            newVal = pString.substring(offset);
        } else {
            newVal = pString.substring(offset, idxSpc);
        }
        DatatypeFactory factory;
        try {
            factory = DatatypeFactory.newInstance();
            XMLGregorianCalendar cal = factory.newXMLGregorianCalendar(newVal);

            pParsePosition.setIndex(idxSpc);
            return cal.toGregorianCalendar();
        } catch (DatatypeConfigurationException e) {
            pParsePosition.setErrorIndex(offset);
        }
        return null;
    }
        

    private void append(StringBuffer pBuffer, int pNum, int pMinLen) {
        String s = Integer.toString(pNum);
        for (int i = s.length(); i < pMinLen; i++) {
            pBuffer.append('0');
        }
        pBuffer.append(s);
    }

    @Override
    public StringBuffer format(Object pCalendar, StringBuffer pBuffer, FieldPosition pPos) {
        if (pCalendar == null) {
            throw new NullPointerException("The Calendar argument must not be null.");
        }
        if (pBuffer == null) {
            throw new NullPointerException("The StringBuffer argument must not be null.");
        }
        if (pPos == null) {
            throw new NullPointerException("The FieldPosition argument must not be null.");
        }

        Calendar cal = (Calendar)pCalendar;
        if (parseDate) {
            int year = cal.get(Calendar.YEAR);
            if (year < 0) {
                pBuffer.append('-');
                year = -year;
            }
            append(pBuffer, year, 4);
            pBuffer.append('-');
            append(pBuffer, cal.get(Calendar.MONTH) + 1, 2);
            pBuffer.append('-');
            append(pBuffer, cal.get(Calendar.DAY_OF_MONTH), 2);
            if (parseTime) {
                pBuffer.append('T');
            }
        }
        if (parseTime) {
            append(pBuffer, cal.get(Calendar.HOUR_OF_DAY), 2);
            pBuffer.append(':');
            append(pBuffer, cal.get(Calendar.MINUTE), 2);
            pBuffer.append(':');
            append(pBuffer, cal.get(Calendar.SECOND), 2);
            int millis = cal.get(Calendar.MILLISECOND);
            if (millis > 0) {
                pBuffer.append('.');
                append(pBuffer, millis, 3);
            }
        }
        if (parseTimezone) {
            TimeZone tz = cal.getTimeZone();
            // JDK 1.4: int offset = tz.getOffset(cal.getTimeInMillis());
            int offset = cal.get(Calendar.ZONE_OFFSET);
            if (tz.inDaylightTime(cal.getTime())) {
                offset += cal.get(Calendar.DST_OFFSET);
            }
            if (offset == 0) {
                pBuffer.append('Z');
            } else {
                if (offset < 0) {
                    pBuffer.append('-');
                    offset = -offset;
                } else {
                    pBuffer.append('+');
                }
                int minutes = offset / (60 * 1000);
                int hours = minutes / 60;
                minutes -= hours * 60;
                append(pBuffer, hours, 2);
                pBuffer.append(':');
                append(pBuffer, minutes, 2);
            }
        }
        return pBuffer;
    }
}
