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


package org.apache.cxf.binding.corba.utils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;

import org.apache.cxf.binding.corba.CorbaMessage;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.ServiceModelUtil;


/**
 * Holder for utility methods relating to contexts.
 */
public final class ContextUtils {
    private ContextUtils() {
        //utility class
    }
    
    public static boolean isRequestor(Message message) {
        return Boolean.TRUE.equals(message.containsKey(Message.REQUESTOR_ROLE));
    }

    /**
     * Determine if message is outbound.
     *
     * @param message the current Message
     * @return true iff the message direction is outbound
     */
    public static boolean isOutbound(Message message) {
        Exchange exchange = message.getExchange();
        return message != null
               && exchange != null
               && message == exchange.getOutMessage();
    }
        
    public static DataWriter<XMLEventWriter> getDataWriter(CorbaMessage message) {
        Service service = ServiceModelUtil.getService(message.getExchange());

        DataWriter<XMLEventWriter> dataWriter = service.getDataBinding().createWriter(XMLEventWriter.class);

        if (dataWriter == null) {
            //throw a fault
            //throw new Fault(new org.apache.cxf.common.i18n.Message("NO_DATAWRITER", BUNDLE, service
            //    .getName()));
        }

        return dataWriter;
    }

    public static DataReader<XMLEventReader> getDataReader(CorbaMessage message) {
        Service service = ServiceModelUtil.getService(message.getExchange());

        DataReader<XMLEventReader> dataReader = service.getDataBinding().createReader(XMLEventReader.class);

        if (dataReader == null) {
            //throw a fault
            //throw new Fault(new org.apache.cxf.common.i18n.Message("NO_DATAREADER", BUNDLE, service
            //    .getName()));
        }

        return dataReader;
    }

}
