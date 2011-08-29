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

package org.apache.cxf.alarm_retrieval.client;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.ws.Response;

import v1.tmf854.fault.ActiveAlarmFilterT;
import v1.tmf854.fault.AlarmT;
import v1.tmf854.fault.CommunicationPatternT;
import v1.tmf854.fault.CommunicationStyleT;
import v1.tmf854.fault.EventInformationT;
import v1.tmf854.fault.GetActiveAlarmsCountResponseT;
import v1.tmf854.fault.GetActiveAlarmsCountT;
import v1.tmf854.fault.GetActiveAlarmsT;
import v1.tmf854.fault.HeaderT;
import v1.tmf854.fault.MsgTypeT;
import v1.tmf854.fault.ProbableCauseT;
import ws.v1.tmf854.fault.http.AlarmRetrieval;
import ws.v1.tmf854.fault.http.AlarmRetrieval_Service;

/**
 * org.apache.cxf.alarm_retrieval.client.Client
 */
public final class Client {

    private static final QName SERVICE_NAME = new QName("tmf854.v1.ws", "AlarmRetrieval");

    private Client() {
    }

    public static void main(String args[]) throws Exception {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        File wsdl = new File(args[0]);

        AlarmRetrieval_Service ss = new AlarmRetrieval_Service(wsdl.toURL(), SERVICE_NAME);
        AlarmRetrieval port = ss.getAlarmRetrieval();

        boolean foundOp = false;

        if ("all".equals(args[1])) {
            foundOp = true;
            getActiveAlarmsCount(port);
            getActiveAlarms(port);
        } else if ("getActiveAlarmsCount".equals(args[1])) {
            foundOp = true;
            getActiveAlarmsCount(port);
        } else if ("getActiveAlarms".equals(args[1])) {
            foundOp = true;
            getActiveAlarms(port);
        } else {
            printUsage();
            System.exit(1);
        }

        System.exit(0);
    }

    private static void getActiveAlarmsCount(AlarmRetrieval port) {

        HeaderT header = new HeaderT();
        header.setActivityName("getActiveAlarmsCountAsync");
        header.setMsgName("getActiveAlarmsCountAsync");
        header.setMsgType(MsgTypeT.REQUEST);
        header.setSenderURI("http://mtosi.iona.com/fault/sender");
        header.setDestinationURI("http://mtosi.iona.com/fault/destination");
        header.setCommunicationPattern(CommunicationPatternT.SIMPLE_RESPONSE);
        header.setCommunicationStyle(CommunicationStyleT.MSG);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss.SSSZ");
        header.setTimestamp(formatter.format(new Date()));
        Holder<HeaderT> mtosiHeader = new Holder<HeaderT>(header);

        GetActiveAlarmsCountT mtosiBody = new GetActiveAlarmsCountT();
        mtosiBody.setFilter(new ActiveAlarmFilterT());

        // use polling method to obtain response
        
        System.out.println("Invoking getActiveAlarmsCountAsync using polling.");
        Response<GetActiveAlarmsCountResponseT> response =
            port.getActiveAlarmsCountAsync(mtosiHeader, mtosiBody);

        try {
            while (!response.isDone()) {
                System.out.println("waiting for operation response...");
                Thread.sleep(100);
            }
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException while sleeping.");
        }

        try {
            if (response.get() != null) {
                System.out.println("Active Alarms Count: " + response.get().getActiveAlarmCount());
            } else {
                System.err.println("Error - null response.");
            }
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException getting asynchronous response.");
        } catch (java.util.concurrent.ExecutionException ex) {
            System.out.println("Operation received ExecutionException.");
        }

        System.out.println();
    }

    private static void getActiveAlarms(AlarmRetrieval port) {

        HeaderT header = new HeaderT();
        header.setActivityName("getActiveAlarmsAsync");
        header.setMsgName("getActiveAlarmsAsync");
        header.setMsgType(MsgTypeT.REQUEST);
        header.setSenderURI("http://mtosi.iona.com/fault/sender");
        header.setDestinationURI("http://mtosi.iona.com/fault/destination");
        header.setCommunicationPattern(CommunicationPatternT.SIMPLE_RESPONSE);
        header.setCommunicationStyle(CommunicationStyleT.MSG);
        header.setRequestedBatchSize(new Long(2));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss.SSSZ");
        header.setTimestamp(formatter.format(new Date()));
        Holder<HeaderT> mtosiHeader = new Holder<HeaderT>(header);

        GetActiveAlarmsT mtosiBody = new GetActiveAlarmsT();
        mtosiBody.setFilter(new ActiveAlarmFilterT());

        // use callback method to obtain response
        
        System.out.println("Invoking getActiveAlarmsAsync using callback.");
        AsyncAlarmHandler handler = new AsyncAlarmHandler();
        Future<?> response = port.getActiveAlarmsAsync(mtosiHeader, mtosiBody, handler);

        try {
            while (!response.isDone()) {
                System.out.println("waiting for handler to receive response...");
                Thread.sleep(100);
            }
            System.out.println("getActiveAlarmsAsync operation completed.");
        } catch (InterruptedException ex) {
            System.out.println("InterruptedException while sleeping.");
        }
        
        if (handler.getResponse() == null) {
            System.err.println("Error - null response.");
            return;
        }
        List<AlarmT> alarms = handler.getResponse().getActiveAlarm();

        int alarmCount = alarms.size();
        if (alarmCount == 0) {
            System.out.println("No alarm.");
        } else {
            System.out.println("Displaying details for " + alarmCount
                + " alarm" + (alarmCount == 1 ? ":" : "s:"));
            System.out.println();
        }

        int i = 0;
        for (AlarmT alarm : alarms) {
            System.out.println("Alarm #" + i++ + ":");
            EventInformationT eventInfo = alarm.getEventInfo();
            System.out.println("- Notification ID: " + eventInfo.getNotificationId());
            System.out.println("- Object type: " + eventInfo.getObjectType().value());
            System.out.println("- OS time: " + eventInfo.getOsTime());

            if (eventInfo.getNeTime() != null) {
                System.out.println("- NE time: " + eventInfo.getNeTime());
            }

            String layerRate = alarm.getLayerRate();
            if (layerRate != null) {
                System.out.println("- Layer/Rate: " + layerRate);
            }

            ProbableCauseT probableCause = alarm.getProbableCause();
            String type = probableCause.getType();
            if (type != null) {
                System.out.println("- Probable cause type: " + type);
            }

            String perceivedSeverity = alarm.getPerceivedSeverity();
            if (perceivedSeverity != null) {
                System.out.println("- Perceived severity: " + perceivedSeverity);
            }

            System.out.println("- Service affecting: " + alarm.getServiceAffecting().value());
            System.out.println("- Root Cause Alarm Indication: "
                + ((alarm.isRcaiIndicator()) ? "YES" : "NO"));
            System.out.println();
        }
    }

    public static void printUsage() {
        System.out.println("Syntax is: Client <wsdl> <operation>");
        System.out.println("   operation is one of: ");
        System.out.println("      getActiveAlarmsCount");
        System.out.println("      getActiveAlarms");
    }
}
