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
package org.apache.cxf.management.interceptor;


import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.management.counters.Counter;
import org.apache.cxf.management.counters.CounterRepository;
import org.apache.cxf.management.counters.MessageHandlingTimeRecorder;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.FaultMode;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.OperationInfo;

public abstract class AbstractMessageResponseTimeInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractMessageResponseTimeInterceptor.class);
    
    AbstractMessageResponseTimeInterceptor(String phase) {
        super(phase);
    }
    
    protected boolean isClient(Message msg) {
        return msg == null ? false : Boolean.TRUE.equals(msg.get(Message.REQUESTOR_ROLE));        
    }
   
    protected void beginHandlingMessage(Exchange ex) {
        if (null == ex) {
            return;
        }
        MessageHandlingTimeRecorder mhtr = ex.get(MessageHandlingTimeRecorder.class);        
        if (null != mhtr) {
            mhtr.beginHandling();
        } else {
            mhtr = new MessageHandlingTimeRecorder(ex);
            mhtr.beginHandling();
        }        
    }
    
    protected void endHandlingMessage(Exchange ex) {                
        if (null == ex) {
            return;
        }
        MessageHandlingTimeRecorder mhtr = ex.get(MessageHandlingTimeRecorder.class);
        if (null != mhtr) {
            mhtr.endHandling();
            mhtr.setFaultMode(ex.get(FaultMode.class));
            increaseCounter(ex, mhtr);
                        
        } // else can't get the MessageHandling Infor  
    }
    
    protected void setOneWayMessage(Exchange ex) {
        MessageHandlingTimeRecorder mhtr = ex.get(MessageHandlingTimeRecorder.class);        
        if (null == mhtr) {
            mhtr = new MessageHandlingTimeRecorder(ex);            
        } else {
            mhtr.endHandling();
        }
        mhtr.setOneWay(true);
        increaseCounter(ex, mhtr);
    }    
    
    private void increaseCounter(Exchange ex, MessageHandlingTimeRecorder mhtr) {
        Bus bus = ex.get(Bus.class);
        if (null == bus) {
            LOG.log(Level.INFO, "CAN_NOT_GET_BUS_FROM_EXCHANGE");
            BusFactory.getThreadDefaultBus();
        }
        
        Message message = ex.getOutMessage();
        
        CounterRepository cr = bus.getExtension(CounterRepository.class);
        
        if (null == cr) {
            LOG.log(Level.WARNING, "NO_COUNTER_REPOSITORY");
            return;
        } else {            
            Service service = ex.get(Service.class);            
            OperationInfo opInfo = ex.get(OperationInfo.class);
            Endpoint endpoint = ex.get(Endpoint.class);
            
            String serviceName = "\"" + service.getName() + "\"";            
            String portName = "\"" + endpoint.getEndpointInfo().getName().getLocalPart() + "\"";
            String operationName = opInfo == null ? null : "\"" + opInfo.getName().getLocalPart() + "\"";
            
            StringBuffer buffer = new StringBuffer();
            buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":");
            buffer.append(ManagementConstants.BUS_ID_PROP + "=" + bus.getId() + ",");
            if (isClient(message)) {
                buffer.append(ManagementConstants.TYPE_PROP + "=" + Counter.PERFORMANCE_COUNTER + ".Client,");
            } else {
                buffer.append(ManagementConstants.TYPE_PROP + "=" + Counter.PERFORMANCE_COUNTER + ".Server,");
            }
            buffer.append(ManagementConstants.SERVICE_NAME_PROP + "=" + serviceName + ",");
           
            buffer.append(ManagementConstants.PORT_NAME_PROP + "=" + portName);
            String serviceCounterName = buffer.toString();
            
            try {           
                ObjectName serviceCounter = 
                    new ObjectName(serviceCounterName);                
                cr.increaseCounter(serviceCounter, mhtr);
                if (operationName != null) {
                    buffer.append("," + ManagementConstants.OPERATION_NAME_PROP + "=" + operationName);
                    String operationCounterName = buffer.toString();
                    ObjectName operationCounter = new ObjectName(operationCounterName);
                    cr.increaseCounter(operationCounter, mhtr);                
                }
            } catch (Exception exception) {
                LOG.log(Level.WARNING, "CREATE_COUNTER_OBJECTNAME_FAILED", exception);
            }
        }
    }
        
}
