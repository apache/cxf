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
package org.apache.cxf.aegis.type.basic;

import java.util.Calendar;
import java.util.Date;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.common.util.XMLSchemaQNames;
import org.junit.Test;



public class DateTimezoneTest extends AbstractAegisTest {

    TypeMapping mapping;
    private AegisContext context;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        context = new AegisContext();
        context.initialize();
        mapping = context.getTypeMapping();
    }
    
    @Test
    public void testTimezoneLessCalendar() throws Exception {
        BeanTypeInfo info = new BeanTypeInfo(CalendarBean.class, "urn:Bean");
        mapping.register(Calendar.class, XMLSchemaQNames.XSD_DATETIME, new TimezoneLessDateType());
        mapping.register(Calendar.class, XMLSchemaQNames.XSD_DATE, new TimezoneLessDateType());        
        info.setTypeMapping(mapping);
  
        BeanType type = new BeanType(info);
        type.setTypeClass(CalendarBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));
  
        CalendarBean bean = new CalendarBean();
        bean.setCalendar(Calendar.getInstance());
        // Test writing
        Element element = writeObjectToElement(type, bean, getContext());
  
        assertTimezoneLessString(element.getTextContent());
    }
    
    @Test
    public void testTimezoneLessDate() throws Exception {
        BeanTypeInfo info = new BeanTypeInfo(DateBean.class, "urn:Bean");
        mapping.register(Date.class, XMLSchemaQNames.XSD_DATETIME, new TimezoneLessDateType());
        mapping.register(Date.class, XMLSchemaQNames.XSD_DATE, new TimezoneLessDateType());        
        info.setTypeMapping(mapping);
  
        BeanType type = new BeanType(info);
        type.setTypeClass(DateBean.class);
        type.setTypeMapping(mapping);
        type.setSchemaType(new QName("urn:Bean", "bean"));
  
        DateBean bean = new DateBean();
        bean.setDate(Calendar.getInstance().getTime());
        // Test writing
        Element element = writeObjectToElement(type, bean, getContext());
        assertTimezoneLessString(element.getTextContent());
        
    }
    
    private void assertTimezoneLessString(String dateString) {
        assertTrue(dateString.length() <= 10);
        assertFalse(dateString.contains("+"));
        assertFalse(dateString.contains("Z"));
    }
    
    //TODO add tests with Timezones
    
    public static class CalendarBean {
        private Calendar calendar;
    
        public Calendar getCalendar() {
            return calendar;
        }
    
        public void setCalendar(Calendar calendar) {
            this.calendar = calendar;
        }
    }
    
    public static class DateBean {
        private Date date;
  
        public Date getDate() {
            return date;
        }
  
        public void setDate(Date date) {
            this.date = date;
        }
    }    
}
