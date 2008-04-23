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
package org.apache.cxf.performance.basic_type.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.pat.internal.TestCaseBase;
import org.apache.cxf.pat.internal.TestResult;
import org.apache.cxf.performance.basic_type.BasicPortType;
import org.apache.cxf.performance.basic_type.BasicService;
import org.apache.cxf.performance.basic_type.server.Server;
 
public final class Client extends TestCaseBase<BasicPortType> {
    
    private static final QName SERVICE_NAME 
        = new QName("http://cxf.apache.org/performance/basic_type", "BasicService");

    private static int opid;
    
    private  byte[] inputBase64;
    private  String inputString = new String();

    private final int asciiCount = 1 * 1024;
    
    private BasicService ss;

    public Client(String[] args) {
        super("Basic Type TestCase", args);
        serviceName = "BasicService";
        portName = "BasicPortType";
        operationName = "echoString";
        amount = 30;
        wsdlNameSpace = "http://cxf.apache.org/performance/basic_type";
    }

    public void initTestData() {
        String temp = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+?><[]/0123456789";
        inputBase64 = new byte[1024 * packetSize];
        for (int idx = 0; idx < 4 * packetSize; idx++) {
            for (int jdx = 0; jdx < 256; jdx++) {
                inputBase64[idx * 256 + jdx] = (byte)(jdx - 128);
            }
        }
        for (int i = 0; i < asciiCount / temp.length() * packetSize; i++) {
            inputString = inputString + temp;
        }
    }

    public void printUsage() {
        System.out.println("Syntax is: Client [-WSDL wsdllocation] operation [-operation args...]");
        System.out.println("   operation is one of: ");
        System.out.println("      echoBase64");
        System.out.println("      echoString");
    }

    public static void main(String args[]) throws Exception {
        Client client = new Client(args);
        client.initialize();
        if (client.getOperationName().equals("echoString")) {
            opid = 0;
        } else {
            opid = 1;
        }
        client.run();
        List results = client.getTestResults();
        TestResult testResult = null;
        for (Iterator iter = results.iterator(); iter.hasNext();) {
            testResult = (TestResult)iter.next();
            System.out.println("Throughput " + testResult.getThroughput());
            System.out.println("AVG Response Time " + testResult.getAvgResponseTime());
        }
        System.out.println("cxf client is going to shutdown!");
        System.exit(0);
    }

    public void doJob(BasicPortType port) {
        try {
            switch(opid) {
            case 0:
                port.echoString(inputString);
                break;
            case 1:
                port.echoBase64(inputBase64);
                break;
            default:
                port.echoString(inputString);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BasicPortType getPort() {
       
        try {
            URL wsdl = null;
            if ((wsdlPath.startsWith("file://")) || (wsdlPath.startsWith("http://"))) {
                 wsdl = new URL(wsdlPath);
            } else {
                 wsdl = new URL("file://" + wsdlPath);
            }
            ss = new BasicService(wsdl, SERVICE_NAME);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return ss.getSoapHttpPort();
    }
} 
 


