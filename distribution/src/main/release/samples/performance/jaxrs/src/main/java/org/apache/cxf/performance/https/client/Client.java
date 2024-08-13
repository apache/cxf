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

package org.apache.cxf.performance.https.client;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.pat.internal.TestCaseBase;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.pat.internal.TestResult;
import org.apache.cxf.performance.https.common.Customer;
import org.apache.cxf.performance.https.server.Server;
import org.apache.http.util.Asserts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public final class Client extends TestCaseBase<WebClient> {

    private static final String CLIENT_CONFIG_FILE = "ClientConfig.xml";
    private static final String BASE_SERVICE_URL = ":9000/customerservice/customers";

    private int opid;

    private String inputString = new String();
    private String host = "localhost";
    private String protocol = "https";
    private final int asciiCount = 1024;

    public Client(String[] args, boolean warmup) {
        super("JAX-RS PERF", args, warmup);
        wsdlPath = "";
        serviceName = "";
        portName = "";
        operationName = "get";
        wsdlNameSpace = "";
        amount = 30;
        packetSize = 1;
        usingTime = true;
        numberOfThreads = 4;
        for (int x = 0; x < args.length; x++) {
            if ("-host".equals(args[x])) {
                host = args[x + 1];
                x++;
            } else if ("-protocol".equals(args[x])) {
                protocol = args[x + 1];
                x++;
            }
        }
    }

    @Override
    public void initTestData() {
        String temp = "abcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+?><[]/0123456789";
        StringBuilder builder = new StringBuilder(packetSize * 1024);
        builder.append(inputString);
        for (int i = 0; i < asciiCount / temp.length() * packetSize; i++) {
            builder.append(temp);
        }
        inputString = builder.toString();
    }

    @Override
    public void doJob(WebClient webClient) {
        try {
            switch (opid) {
                case 0:
                    //GET
                    try (Response respGet = webClient.get()) {
                        Asserts.check(respGet.getStatus() == 200, "Get should have been OK");
                    }
                    break;
                case 1:
                    //POST
                    Customer customerPost = new Customer();
                    customerPost.setName("Jack");
                    try (Response respPost = webClient.post(customerPost)) {
                        Asserts.check(respPost.getStatus() == 200, "Post should have been OK");
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                    break;
                case 2:
                    //PUT
                    Customer customerPut = new Customer();
                    customerPut.setId(123);
                    customerPut.setName("Mary");
                    try (Response respPut = webClient.put(customerPut)) {
                        Asserts.check(respPut.getStatus() == 200, "Put should have been OK");
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                    break;
                case 3:
                    //DELETE
                    try (Response respDel = webClient.delete()) {
                        Asserts.check(respDel.getStatus() == 200 || respDel.getStatus() == 304, "Delete should have been OK");
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                    break;
                default:
                    //Do Nothing
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized WebClient getPort() {
        if (opid == 0 || opid == 3) {
            return WebClient.create(protocol + "://" + host + BASE_SERVICE_URL + "/123", CLIENT_CONFIG_FILE);
        } else {
            return WebClient.create(protocol + "://" + host + BASE_SERVICE_URL, CLIENT_CONFIG_FILE);
        }
    }

    @Override
    public void printUsage() {
        System.out.println("Syntax is: Client [-operation verb] [-PacketSize packetnumber] ");
    }


    public void processArgs() {
        super.processArgs();
        if ("get".equalsIgnoreCase(getOperationName())) {
            opid = 0;
        } else if ("post".equalsIgnoreCase(getOperationName())) {
            opid = 1;
        } else if ("put".equalsIgnoreCase(getOperationName())) {
            opid = 2;
        } else if ("delete".equalsIgnoreCase(getOperationName())) {
            opid = 3;
        } else {
            System.out.println("Invalid operation: " + getOperationName());
        }
    }

    public static void main(String[] args) throws Exception {

        int threadIdx = -1;
        int servIdx = -1;
        for (int x = 0; x < args.length; x++) {
            if ("-Threads".equals(args[x])) {
                threadIdx = x + 1;
            } else if ("-Server".equals(args[x])) {
                servIdx = x;
                break;
            }
        }
        if (servIdx != -1) {
            String[] tmp = new String[args.length - servIdx];
            System.arraycopy(args, servIdx, tmp, 0, args.length - servIdx);
            Server.main(tmp);

            tmp = new String[servIdx];
            System.arraycopy(args, 0, tmp, 0, servIdx);
            args = tmp;
        }
        List<String> threadList = new ArrayList<>();
        if (threadIdx != -1) {
            String[] threads = args[threadIdx].split(",");
            for (String s : threads) {
                if (s.indexOf('-') != -1) {
                    String s1 = s.substring(0, s.indexOf('-'));
                    String s2 = s.substring(s.indexOf('-') + 1);
                    int i1 = Integer.parseInt(s1);
                    int i2 = Integer.parseInt(s2);
                    for (int x = i1; x <= i2; x++) {
                        threadList.add(Integer.toString(x));
                    }
                } else {
                    threadList.add(s);
                }
            }
        } else {
            threadList.add("1");
        }
        System.out.println(threadList);
        boolean first = true;
        for (String numThreads: threadList) {
            if (threadIdx != -1) {
                args[threadIdx] = numThreads;
            }
            System.out.println(Arrays.asList(args));
            Client client = new Client(args, first);
            first = false;
            client.initialize();

            client.run();

            List results = client.getTestResults();
            TestResult testResult = null;

            double rt = 0.0;
            double tp = 0.0;
            for (Iterator iter = results.iterator(); iter.hasNext();) {
                testResult = (TestResult)iter.next();
                System.out.println("Throughput " + testResult.getThroughput());
                System.out.println("AVG Response Time " + testResult.getAvgResponseTime());
                rt += testResult.getAvgResponseTime();
                tp += testResult.getThroughput();
            }
            rt *= 1000;
            rt /= (double)results.size();

            System.out.println("Total(" + numThreads + "):  " + tp + " tps     " + rt + " ms");

            System.out.println();
            System.out.println();
            System.out.println();
        }

        System.out.println("cxf client is going to shutdown!");
        System.exit(0);
    }
}
