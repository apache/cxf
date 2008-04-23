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

package org.apache.cxf.alarm_retrieval.server;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Holder;
import javax.xml.ws.Response;

import v1.tmf854.fault.AlarmT;
import v1.tmf854.fault.EventInformationT;
import v1.tmf854.fault.GetActiveAlarmsCountResponseT;
import v1.tmf854.fault.GetActiveAlarmsCountT;
import v1.tmf854.fault.GetActiveAlarmsResponseT;
import v1.tmf854.fault.GetActiveAlarmsT;
import v1.tmf854.fault.HeaderT;
import v1.tmf854.fault.MsgTypeT;
import v1.tmf854.fault.NamingAttributesT;
import v1.tmf854.fault.ObjectTypeT;
import v1.tmf854.fault.ProbableCauseT;
import v1.tmf854.fault.ServiceAffectingT;
import ws.v1.tmf854.fault.http.AlarmRetrieval;
import ws.v1.tmf854.fault.http.ProcessingFailureException;

@WebService(
            serviceName = "AlarmRetrieval",
            portName = "AlarmRetrieval",
            targetNamespace = "tmf854.v1.ws",
            endpointInterface = "ws.v1.tmf854.fault.http.AlarmRetrieval"
            )
public class AlarmRetrievalImpl implements AlarmRetrieval {

    private static final Logger LOG = 
        Logger.getLogger(AlarmRetrievalImpl.class.getPackage().getName());

    private List<AlarmT> alarms = new Vector<AlarmT>();

    public void addAlarm(int alarmID) {

        AlarmT alarm = new AlarmT();

        EventInformationT eventInfo = new EventInformationT();
        ProbableCauseT probableCause = new ProbableCauseT();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss.SSSZ");

        switch (alarmID % 2) {
        case 1:
            eventInfo.setNotificationId("0001239");
            eventInfo.setObjectType(ObjectTypeT.OT_EQUIPMENT);
            eventInfo.setObjectName(new NamingAttributesT());
            eventInfo.setOsTime(formatter.format(new Date()));
            eventInfo.setNeTime(formatter.format(new Date()));
            eventInfo.setEdgePointRelated(Boolean.FALSE);
            probableCause.setType("PROP_odd_probable_cause_type");
            alarm.setEventInfo(eventInfo);
            alarm.setIsClearable(false);
            alarm.setLayerRate("PROP_layer_rate_odd");
            alarm.setProbableCause(probableCause);
            alarm.setPerceivedSeverity("PROP_odd_perceived_severity");
            alarm.setServiceAffecting(ServiceAffectingT.SA_UNKNOWN);
            alarm.setRcaiIndicator(false);
            break;

        default:
            eventInfo.setNotificationId("9876543");
            eventInfo.setObjectType(ObjectTypeT.OT_OS);
            eventInfo.setObjectName(new NamingAttributesT());
            eventInfo.setOsTime(formatter.format(new Date()));
            eventInfo.setEdgePointRelated(Boolean.FALSE);
            probableCause.setType("PROP_even_probable_cause_type");
            alarm.setEventInfo(eventInfo);
            alarm.setIsClearable(true);
            alarm.setLayerRate("PROP_layer_rate_even");
            alarm.setProbableCause(probableCause);
            alarm.setPerceivedSeverity("PROP_even_perceived_severity");
            alarm.setServiceAffecting(ServiceAffectingT.SA_SERVICE_AFFECTING);
            alarm.setRcaiIndicator(false);
            break;

        }

        alarms.add(alarm);
    }

    public Future<?> getActiveAlarmsCountAsync(
        Holder<HeaderT> mtosiHeader,
        GetActiveAlarmsCountT mtosiBody,
        AsyncHandler<GetActiveAlarmsCountResponseT> asyncHandler) { 
        LOG.info("Executing operation Future<?> getActiveAlarmsCountAsync");
        System.out.println("Executing operation Future<?> getActiveAlarmsCountAsync");
        return null;
    }

    public Response<GetActiveAlarmsCountResponseT> getActiveAlarmsCountAsync(
        Holder<HeaderT> mtosiHeader,
        GetActiveAlarmsCountT mtosiBody) {
        LOG.info("Executing operation Response<?> getActiveAlarmsCountAsync");
        System.out.println("Executing operation Response<?> getActiveAlarmsCountAsync");
        return null;
    }

    public GetActiveAlarmsCountResponseT getActiveAlarmsCount(
        Holder<HeaderT> mtosiHeader,
        GetActiveAlarmsCountT mtosiBody) throws ProcessingFailureException {
        LOG.info("Executing operation getActiveAlarmsCount");
        System.out.println("getActiveAlarmsCount() called.");

        mtosiHeader.value.setMsgName("getActiveAlarmsCountResponse");
        mtosiHeader.value.setMsgType(MsgTypeT.RESPONSE);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss.SSSZ");
        mtosiHeader.value.setTimestamp(formatter.format(new Date()));

        GetActiveAlarmsCountResponseT response = new GetActiveAlarmsCountResponseT();
        response.setActiveAlarmCount(alarms.size()); 

        return response;
    }

    public Future<?> getActiveAlarmsAsync(
        Holder<HeaderT> mtosiHeader,
        GetActiveAlarmsT mtosiBody,
        AsyncHandler<GetActiveAlarmsResponseT> asyncHandler) { 
        LOG.info("Executing operation Future<?> getActiveAlarmsAsync");
        System.out.println("Executing operation Future<?> getActiveAlarmsAsync");
        return null;
    }

    public Response<GetActiveAlarmsResponseT> getActiveAlarmsAsync(
        Holder<HeaderT> mtosiHeader, 
        GetActiveAlarmsT mtosiBody) {
        LOG.info("Executing operation Response<?> getActiveAlarmsAsync");
        System.out.println("Executing operation Response<?> getActiveAlarmsAsync");
        return null;
    }

    public GetActiveAlarmsResponseT getActiveAlarms(
        Holder<HeaderT> mtosiHeader,
        GetActiveAlarmsT mtosiBody) throws ProcessingFailureException {
        LOG.info("Executing operation getActiveAlarms");
        System.out.println("getActiveAlarms() called.");

        mtosiHeader.value.setMsgName("getActiveAlarmsResponse");
        mtosiHeader.value.setMsgType(MsgTypeT.RESPONSE);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss.SSSZ");
        mtosiHeader.value.setTimestamp(formatter.format(new Date()));
        mtosiHeader.value.setBatchSequenceEndOfReply(Boolean.TRUE);

        GetActiveAlarmsResponseT.ActiveAlarmList alarmList = new GetActiveAlarmsResponseT.ActiveAlarmList();
        
        Long requestedCount = mtosiHeader.value.getRequestedBatchSize();
        if (requestedCount != null) {
            for (int i = 0; i < requestedCount && alarms.size() > 0; i++) {
                AlarmT alarm = alarms.remove(0);
                alarmList.getActiveAlarm().add(alarm);
            }
            // Indicate to the caller if there are more alarms remaining. 
            if (alarms.size() > 0) {
                mtosiHeader.value.setBatchSequenceEndOfReply(Boolean.FALSE);
            }
        } else {
            alarmList.getActiveAlarm().addAll(alarms);
        }
        GetActiveAlarmsResponseT response = new GetActiveAlarmsResponseT();
        response.setActiveAlarmList(alarmList);

        return response;
    }

}
