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
package org.apache.cxf.performance.client;

import edu.indiana.extreme.wsdl.benchmark1.Benchmark;
import edu.indiana.extreme.wsdl.benchmark1.Benchmark_Service;
import edu.indiana.extreme.wsdl.benchmark1.ReceiveMeshInterfaceObjectsResponse;
import edu.indiana.extreme.wsdl.benchmark1.SendMeshInterfaceObjectsResponse;
import edu.indiana.extreme.wsdl.benchmark1.SimpleEvent;
import edu.indiana.extreme.wsdl.benchmark1.MeshInterfaceObject;
import edu.indiana.extreme.wsdl.benchmark1.EchoSimpleEventsRequest;
import edu.indiana.extreme.wsdl.benchmark1.ArrayOfSimpleEvent;
import edu.indiana.extreme.wsdl.benchmark1.ReceiveSimpleEventsRequest;
import edu.indiana.extreme.wsdl.benchmark1.SendSimpleEventsRequest;
import edu.indiana.extreme.wsdl.benchmark1.EchoMeshInterfaceObjectsRequest;
import edu.indiana.extreme.wsdl.benchmark1.ReceiveMeshInterfaceObjectsRequest;
import edu.indiana.extreme.wsdl.benchmark1.ArrayOfMeshInterfaceObject;
import edu.indiana.extreme.wsdl.benchmark1.SendMeshInterfaceObjectsRequest;

import javax.xml.namespace.QName;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class BenchClient {
	private final static QName SERVICE_NAME = new QName("http://www.extreme.indiana.edu/wsdl/Benchmark1", "Benchmark");
    private final static boolean VERBOSE = true;
    private final static String SMOKE_TEST = "smoke_test";
    
    Benchmark_Service service;
    Benchmark port;

    public BenchClient(String location) throws Exception {
    	URL wsdlURL = null;
        File wsdlFile = new File(location);
        try {
            if (wsdlFile.exists()) {
                wsdlURL = wsdlFile.toURL();
            } else {
                wsdlURL = new URL(location);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }        
      
        service = new Benchmark_Service(wsdlURL, SERVICE_NAME);
        port = service.getBenchmark(); 
       
        
        /*options.setProperty(org.apache.axis2.transport.http.HTTPConstants.SO_TIMEOUT,new Integer(480000));
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.CONNECTION_TIMEOUT,new Integer(480000));*/
    }

    //URL or port of service
    //total number of elements to send (default 10K)
    //[rse] means receive, send, or echo (a == all)
    //[bdisva] means base64, double, int, string, void (only applies to echo), a == all methods;
    //arraySize (optional for void) - default to 10
    //java -Dmachine.name=... -Dserver.name=... Client URL total {rsea}{bdisva} [arraySize]
    public static void main(String[] args) throws Exception {
        long benchmarkStart = System.currentTimeMillis();
        final String BENCHMARK_DRIVER_VERSION = "$Date$";
        final String ID = "Benchmark1 Driver Version 1.0 (" + BENCHMARK_DRIVER_VERSION + ")";
        verbose("Starting " + ID + " at " + (new Date()));

        // allow multiple URLs (each must start with http"
        List<String> locationList = new ArrayList<String>();        
        int pos = 0;
        while (pos < args.length) {
            String s = args[pos];
            if (s.startsWith("http") || s.startsWith("file")) {
                locationList.add(s);                
            } else {
                break;
            }
            ++pos;
        }
        if (locationList.isEmpty()) {
            int port = 34321;
            locationList.add("http://localhost:" + port);
        }

        final int elementsToSend = args.length > pos ? Integer.parseInt(args[pos]) : 10000;

        String testType = "aa";
        if (args.length > (pos + 1)) {
            testType = args[(pos + 1)];
        }

        String arrSizeToSend = "10";

        if (args.length > (pos + 2)) {
            arrSizeToSend = args[(pos + 2)];
        }
        
        String[] locations = new String[locationList.size()];
        locationList.toArray(locations);

        for (int i = 0; i < locations.length; i++) {
            String location = locations[i];
            verbose("connecting to " + location);            
            runTestsForSize(location, elementsToSend, testType, arrSizeToSend);
        }
        long benchmarkEnd = System.currentTimeMillis();
        double seconds = ((benchmarkEnd - benchmarkStart) / 1000.0);
        System.out.println("Finished " + ID + " in " + seconds + " seconds at " + (new Date()));
    }

    private static void runTestsForSize(String location,
                                        final int elementsToSend,
                                        String testType,
                                        String arrSizeToSend)
            throws Exception {
        TestDescriptor td = new TestDescriptor(location, elementsToSend);
        int commaPos = -1;
        boolean finished = false;
        while (!finished) {
            td.setDirection(testType.charAt(0));
            td.setMethod(testType.charAt(1));
            int prevPos = commaPos;
            commaPos = arrSizeToSend.indexOf(",", prevPos + 1);
            String size;
            if (commaPos > 0) {
                size = arrSizeToSend.substring(prevPos + 1, commaPos);
            } else {
                size = arrSizeToSend.substring(prevPos + 1);
                finished = true;
            }
            td.arrSizeToSend = Integer.parseInt(size);
            //System.out.println("runnig test with size=" + size + " " + (new Date()));
            final char direction = td.getDirection();
            if (direction == 'a') {
                td.setDirection('e');
                runTestsForDirection(td);
                td.setDirection('r');
                runTestsForDirection(td);
                td.setDirection('s');
                runTestsForDirection(td);
                td.setDirection('a'); //restore
            } else {
                runTestsForDirection(td);
            }
        }
    }

    public static void runTestsForDirection(TestDescriptor td)
            throws Exception {
        final char direction = td.direction;
        final char method = td.method;
        if (method == 'a') {
            if (direction == 'e') {
                //test for the void 
                td.setMethod('v');
                runOneTest(td);
            }
            /*
            // test for Base64 
            td.setMethod('b');
            runOneTest(td);
            // test for Doubles
            td.setMethod('d');
            runOneTest(td);
            // test for Ints
            td.setMethod('i');
            runOneTest(td);
            // test for Strings
            td.setMethod('s');
            runOneTest(td);*/
            // test for MeshInterfaceObjects
            td.setMethod('m');
            runOneTest(td);
            // test for SimpleEvents
            td.setMethod('e');
            runOneTest(td);
            td.setMethod('a'); //restore
        } else {
            runOneTest(td);
        }
    }
        

    public static void runOneTest(TestDescriptor td)
            throws Exception {
        final char direction = td.direction;
        final char method = td.method;
        //int arrSize = method == 'v' ? 1 : td.arrSizeToSend;
        int arrSize = td.arrSizeToSend;
        int N = td.elementsToSend / arrSize; // + 1;
        if (N == 0) {
            N = 1;
        }
        final boolean smokeTest = System.getProperty(SMOKE_TEST) != null;
        if (smokeTest) N = 3;

        int totalInv = N * td.arrSizeToSend;

        byte[] barr = null;
        byte[] ba = null;
        if (method == 'b') {
            ba = new byte[td.arrSizeToSend];
            barr = new byte[totalInv];
            for (int i = 0; i < barr.length; i++) {
                barr[i] = (byte) i;
            }
        }

        Double[] darr = null;        
        if (method == 'd') {            
            darr = new Double[totalInv];
            for (int i = 0; i < darr.length; i++) {
                darr[i] = new Double(i);
            }
        }

        Integer[] iarr = null;           
        if (method == 'i') {            
            iarr = new Integer[totalInv];
            for (int i = 0; i < iarr.length ; i++) {
                iarr[i] = new Integer(i);
            }
        }

        String[] sarr = null;        
        if (method == 's') {            
            sarr = new String[totalInv];
            for (int i = 0; i < sarr.length; i++) {
                sarr[i] = "s" + i;
            }
        }

        MeshInterfaceObject[] marr = null;        
        if (method == 'm') {            
            marr = new MeshInterfaceObject[totalInv];
            for (int i = 0; i < totalInv; i++) {
            	marr[i] = new MeshInterfaceObject();
                marr[i].setX(i);
                marr[i].setY(i);
                marr[i].setValue(Math.sqrt(i));
            }
        }

        SimpleEvent[] earr = null;        
        if (method == 'e') {            
            earr = new SimpleEvent[totalInv];
            for (int i = 0; i < earr.length; i++) {
                earr[i] = new SimpleEvent();
                earr[i].setSequenceNumber(i);
                earr[i].setMessage("Message #"+i);
                earr[i].setTimestamp(Math.sqrt(i));
            }
        }

        BenchClient client = new BenchClient(td.serverLocation);

//        System.out.println("invoking " + N + (smokeTest ? " (SMOKE TEST)" : "")
//                + " times for test " + method + " arraysSize=" + td.arrSizeToSend
//                + " " + (new Date()));
        //boolean validate = true;
        long start = System.currentTimeMillis();
        for (int count = 0; count < N; count++) {
            int off = count * arrSize;
            //String arg = "echo"+i;
            if (method == 'v') {
                if (direction == 'e') {
                    client.echoVoid();
                } else if (direction == 'r' || direction == 's') {
                    throw new RuntimeException("usupported direction " + direction + " for void method");
                } else {
                    throw new RuntimeException("unrecongized direction " + direction);
                }
            } else if (method == 'b') {
            	System.arraycopy(barr, off, ba, 0, ba.length);
                byte[] uba = null;
                int ulen = -1;
                if (direction == 'e') {
                    uba = client.echoBase64(ba);
                } else if (direction == 'r') {
                    ulen = client.receiveBase64(ba);
                    if (ulen != ba.length) fail(method2s(direction, method) + " returned wrong size");
                } else if (direction == 's') {
                    uba = client.sendBase64(arrSize);
                } else {
                    throw new RuntimeException("unrecongized direction " + direction);
                }
                if ((count == 0 || count == N - 1) && (direction == 'e' || direction == 's')) {
                    // bruta force
                    if (direction == 's') off = 0;
                    if (uba == null) fail(method2s(direction, method) + " byte array response was null");
                    if (uba.length != ba.length) {
                        fail(method2s(direction, method) + " byte array had wrong size " + uba.length
                                + " (expected " + ba.length + ")");
                    }
                    for (int i = 0; i < ba.length; i++) {
                        if (uba[i] != barr[i + off]) {
                            fail("byte array response had wrong content");
                        }
                    }
                }
            } else if (method == 'd') {
            	ArrayList<Double> da = new ArrayList<Double>();            	 
            	new Util<Double>().copyList(darr, off, da, td.arrSizeToSend);            	
                List<Double> uda = null;
                int dlen = -1;
                if (direction == 'e') {
                    uda = client.echoDoubles(da);
                } else if (direction == 'r') {
                    dlen = client.receiveDoubles(da);
                    if (dlen != da.size()) fail("receive double array returned wrong size");
                } else if (direction == 's') {
                    uda = client.sendDoubles(arrSize);
                } else {
                    throw new RuntimeException("unrecongized direction " + direction);
                }
                if ((count == 0 || count == N - 1) && (direction == 'e' || direction == 's')) {
                    // bruta force verification
                    if (direction == 's') off = 0;
                    if (uda == null) fail(method2s(direction, method) + " double array response was null");
                    if (uda.size() != da.size()) {
                        fail(method2s(direction, method) + " double array had wrong size " + uda.size()
                                + " (expected " + da.size() + ")");
                    }
                    for (int i = 0; i < uda.size(); i++) {
                        if (uda.get(i) != darr[i + off]) {
                            fail(method2s(direction, method) + " double array response had wrong content");
                        }
                    }
                }
            } else if (method == 'i') {
            	ArrayList<Integer> ia = new ArrayList<Integer>();
            	new Util<Integer>().copyList(iarr, off, ia, td.arrSizeToSend);
                List<Integer> uia = null;
                int ulen = -1;
                if (direction == 'e') {
                    uia = client.echoInts(ia);
                } else if (direction == 'r') {
                    ulen = client.receiveInts(ia);
                    if (ulen != ia.size()) {
                        fail(method2s(direction, method) + " receive byte array returned wrong size");
                    }
                } else if (direction == 's') {
                    uia = client.sendInts(arrSize);
                } else {
                    throw new RuntimeException("unrecongized direction " + direction);
                }
                if ((count == 0 || count == N - 1) && (direction == 'e' || direction == 's')) {
                    // bruta force verification
                    if (direction == 's') off = 0;
                    if (uia == null) fail(method2s(direction, method) + " int array response was null");
                    if (uia.size() != ia.size()) {
                        fail(method2s(direction, method) + " int array had wrong size " + uia.size()
                                + " (expected " + ia.size() + ")");
                    }
                    for (int i = 0; i < uia.size(); i++) {
                        if (uia.get(i) != iarr[i + off]) {
                            fail(method2s(direction, method) + " int array response had wrong content");
                        }
                    }
                }
            } else if (method == 's') {
            	ArrayList<String> sa = new ArrayList<String>();
            	new Util<String>().copyList(sarr, off, sa, td.arrSizeToSend);
                List<String> usa = null;
                int slen = -1;
                if (direction == 'e') {
                    usa = client.echoStrings(sa);
                } else if (direction == 'r') {
                    slen = client.receiveStrings(sa);
                    if (slen != sa.size())
                        fail(method2s(direction, method) + " receive string array returned wrong size");
                } else if (direction == 's') {
                    usa = client.sendStrings(arrSize);
                } else {
                    throw new RuntimeException("unrecongized direction " + direction);
                }
                if (start > 0 && (count == 0 || count == N - 1) && (direction == 'e' || direction == 's')) {
                    // bruta force verification
                    if (direction == 's') off = 0;
                    if (usa == null) fail(method2s(direction, method) + " string array response was null");
                    if (usa.size() != sa.size()) {
                        fail(method2s(direction, method) + " string array had wrong size " + usa.size()
                                + " (expected " + sa.size() + ")");
                    }
                    for (int i = 0; i < usa.size(); i++) {
                        String s1 = usa.get(i);
                        String s2 = sarr[i + off];
                        if (!s1.equals(s2)) {
                            fail(method2s(direction, method) + " string array response"
                                    + " had wrong content (s1=" + s1 + " s2=" + s2 + " i=" + i + ")");
                        }
                    }
                }
            } else if (method == 'm') {
            	ArrayList<MeshInterfaceObject> ma = new ArrayList<MeshInterfaceObject>();
            	new Util<MeshInterfaceObject>().copyList(marr, off, ma, td.arrSizeToSend);
                List<MeshInterfaceObject> uma = null;
                int slen = -1;
                if (direction == 'e') {
                    uma = client.echoMeshInterfaceObjects(ma);
                } else if (direction == 'r') {
                    slen = client.receiveMeshInterfaceObjects(ma);
                    if (slen != ma.size())
                        fail(method2s(direction, method) + " receive MeshInterfaceObject array returned wrong size");
                } else if (direction == 's') {
                    uma = client.sendMeshInterfaceObjects(arrSize);
                } else {
                    throw new RuntimeException("unrecongized direction " + direction);
                }
                if (start > 0 && (count == 0 || count == N - 1) && (direction == 'e' || direction == 's')) {
                    // bruta force verification
                    if (direction == 's') off = 0;
                    if (uma == null) fail(method2s(direction, method) + " MeshInterfaceObject array response was null");
                    if (uma.size() != ma.size()) {
                        fail(method2s(direction, method) + " string MeshInterfaceObject had wrong size " + uma.size()
                                + " (expected " + ma.size() + ")");
                    }
                    for (int i = 0; i < uma.size(); i++) {
                        MeshInterfaceObject s1 = uma.get(i);
                        MeshInterfaceObject s2 = marr[i + off];
                        if (!toString(s1).equals(toString(s2))) {
                            fail(method2s(direction, method) + " MeshInterfaceObject array response"
                                    + " had wrong content (s1=" + s1 + " s2=" + s2 + " i=" + i + ")");
                        }
                    }
                }
            } else if (method == 'e') {
            	ArrayList<SimpleEvent> ea = new ArrayList<SimpleEvent>();
            	new Util<SimpleEvent>().copyList(earr, off, ea, td.arrSizeToSend);
                List<SimpleEvent> uea = null;
                int slen = -1;
                if (direction == 'e') {
                    uea = client.echoSimpleEvents(ea);
                } else if (direction == 'r') {
                    slen = client.receiveSimpleEvents(ea);
                    if (slen != ea.size())
                        fail(method2s(direction, method) + " receive SimpleEvent array returned wrong size");
                } else if (direction == 's') {
                    uea = client.sendSimpleEvents(arrSize);
                } else {
                    throw new RuntimeException("unrecongized direction " + direction);
                }
                if (start > 0 && (count == 0 || count == N - 1) && (direction == 'e' || direction == 's')) {
                    // bruta force verification
                    if (direction == 's') off = 0;
                    if (uea == null) fail(method2s(direction, method) + " SimpleEvent array response was null");
                    if (uea.size() != ea.size()) {
                        fail(method2s(direction, method) + " string SimpleEvent had wrong size " + uea.size()
                                + " (expected " + ea.size() + ")");
                    }
                    for (int i = 0; i < uea.size(); i++) {
                        SimpleEvent s1 = uea.get(i);
                        SimpleEvent s2 = earr[i + off];
                        if (!toString(s1).equals(toString(s2))) {
                            fail(method2s(direction, method) + " SimpleEvent array response"
                                    + " had wrong content (s1=" + s1 + " s2=" + s2 + " i=" + i + ")");
                        }
                    }
                }
            } else {
                throw new RuntimeException("unrecongized method " + method);
            }

            if (start > 0 && smokeTest) {
//                String resp = builder.serializeToString(handler.getLastResponse());
//                System.out.println(method2s(direction, method)+" response=\n"+resp+"---\n");
            }
        }
        if (start > 0) {
            long end = System.currentTimeMillis();
            long total = (end == start) ? 1 : (end - start);
            double seconds = (total / 1000.0);
            double invPerSecs = (double) N / seconds;
            double avgInvTimeInMs = (double) total / (double) N;
//            System.out.println("N=" + N + " avg invocation:" + avgInvTimeInMs + " [ms]" +
//                    "total:"+total+" [ms] "+
//                    " throughput:" + invPerSecs + " [invocations/second]" +
//                    " arraysSize=" + arrSize +
//                    " direction=" + direction +
//                    " method=" + method
//                    + " " + (new Date())
//            );
            td.printResult(avgInvTimeInMs / 1000.0, invPerSecs);
        }
    }

    private static String toString(SimpleEvent event){
        return "[" + event.getMessage() + ":" + event.getSequenceNumber() + ":" + event.getTimestamp() + "]";
    }

    private static String toString(MeshInterfaceObject object){
        return "[" + object.getValue() + ":" + object.getX() + ":" + object.getY() + "]";
    }

    private static void verbose(String msg) {
    	if(VERBOSE) {
    		System.out.println("B1> " + msg);
    	}	
    }

    private static void fail(String msg) {
        String s = "FATAL ERROR: service is not following benchmark requirement: " + msg;
        System.out.println(s);
        throw new RuntimeException(s);
        //System.exit(-1);
    }

    private static String method2s(char direction, char method) {
        StringBuilder sb = new StringBuilder(20);
        if (direction == 'e') {
            sb.append("echo");
        } else if (direction == 's') {
            sb.append("send");
        } else if (direction == 'r') {
            sb.append("receive");
        }
        if (method == 'v') {
            sb.append("Void");
        } else if (method == 'b') {
            sb.append("Base64");
        } else if (method == 'd') {
            sb.append("Doubles");
        } else if (method == 'i') {
            sb.append("Ints");
        } else if (method == 's') {
            sb.append("Strings");
        } else if (method == 'm') {
            sb.append("MeshInterfaceObjects");
        } else if (method == 'e') {
            sb.append("SimpleEvent");
        }
        return sb.toString();
    }

    private final static class TestDescriptor {
        private java.text.DecimalFormat df = new java.text.DecimalFormat("##0.000000000");
        private java.text.DecimalFormat df2 = new java.text.DecimalFormat("##0.0000");
        private String testSetup;
        private String clientName = "CXF";
        private String serverName = null;
        private String serverLocation;
        private int arrSizeToSend;
        private int elementsToSend;

        private char direction;
        private char method;

        TestDescriptor(//String serverName,
                       String location,
                       //final char direction,
                       //final char method,
                       int elementsToSend)
        //int arrSizeToSend)
        {
            this.testSetup = System.getProperty("test.setup");
            if (this.testSetup == null) {
                this.testSetup = System.getProperty("machine.name", "UDISCLOSED_SETUP");
            }
            final String SERVER_NAME = "server.name";
            this.serverName = System.getProperty(SERVER_NAME);
            if (serverName == null) {
                throw new RuntimeException(SERVER_NAME + " must be specified as system property");
            }
            this.serverName = serverName;
            this.serverLocation = location;
            //this.direction =  direction;
            //this.method = method;
            this.elementsToSend = elementsToSend;
            //this.arrSizeToSend = arrSizeToSend;
        }

        public void setDirection(char direction) {
            this.direction = direction;
        }

        public char getDirection() {
            return direction;
        }

        public void setMethod(char method) {
            this.method = method;
        }

        public char getMethod() {
            return method;
        }

        public void printResult(double timeSecs, double throughput) throws IOException {
            PrintWriter results = new PrintWriter(System.out, true);
            results.print(testSetup + '\t'
                    + clientName + '\t'
                    + serverName + '\t'
                    + method2s(direction, method) + ((method == 'm') ? "\t" : ((method=='e' && direction=='r') ?"\t\t":"\t\t\t"))
                    + arrSizeToSend + '\t'
                    + df.format(timeSecs) + '\t'
                    + df2.format(throughput)
                    + "\r\n");
            results.flush();
        }

    }

    public void echoVoid() throws java.lang.Exception {        
        port.echoVoid();
    }

    public List<String> echoStrings(List<String> input) throws java.lang.Exception {
        return port.echoStrings(input);
    }

    public int receiveBase64(byte[] input) throws java.lang.Exception {       
        return port.receiveBase64(input);
        
    }

    public int receiveDoubles(List<Double> input) throws java.lang.Exception {
        
        return port.receiveDoubles(input);
    }

    public List<Integer> sendInts(int input) throws java.lang.Exception {        
        return port.sendInts(input);
    }

    public byte[] echoBase64(byte[] input) throws java.lang.Exception {
        return port.echoBase64(input);        
    }

    public int receiveStrings(List<String> input) throws java.lang.Exception {        
        return port.receiveStrings(input);
    }

    public List<Integer> echoInts(List<Integer> input) throws java.lang.Exception {
        return port.echoInts(input);
    }

    public int receiveInts(List<Integer> input) throws java.lang.Exception {
        return port.receiveInts(input);
    }

    public List<Double> sendDoubles(int input) throws java.lang.Exception {
        return port.sendDoubles(input);
    }

    public byte[] sendBase64(int input) throws java.lang.Exception {
        
        return port.sendBase64(input);
    }

    public List<Double> echoDoubles(List<Double> input) throws java.lang.Exception {       
        return port.echoDoubles(input);
    }

    public List<String> sendStrings(int input) throws java.lang.Exception {        
        return port.sendStrings(input);
    }

    public List<SimpleEvent> echoSimpleEvents(List<SimpleEvent> input) throws java.lang.Exception {
        EchoSimpleEventsRequest request = new EchoSimpleEventsRequest();
        ArrayOfSimpleEvent array = new ArrayOfSimpleEvent();
        array.getItem().addAll(input);
        request.setInput(array);
        return port.echoSimpleEvents(request).getEchoSimpleEventsReturn().getItem();
    }

    public int receiveSimpleEvents(List<SimpleEvent> input) throws java.lang.Exception {
        ReceiveSimpleEventsRequest request = new ReceiveSimpleEventsRequest();
        ArrayOfSimpleEvent array = new ArrayOfSimpleEvent();
        array.getItem().addAll(input);
        request.setInput(array);
        return port.receiveSimpleEvents(request).getReceiveSimpleEventsReturn();
    }

    public List<SimpleEvent> sendSimpleEvents(int size) throws java.lang.Exception {
        SendSimpleEventsRequest request = new SendSimpleEventsRequest();
        request.setSize(size);
        return port.sendSimpleEvents(request).getSendSimpleEventsReturn().getItem();
    }

    public List<MeshInterfaceObject> echoMeshInterfaceObjects(List<MeshInterfaceObject> input) throws java.lang.Exception {
        EchoMeshInterfaceObjectsRequest request = new EchoMeshInterfaceObjectsRequest();
        request.getInput().addAll(input);
        return port.echoMeshInterfaceObjects(request).getEchoMeshInterfaceObjectReturn();
    }

    public int receiveMeshInterfaceObjects(List<MeshInterfaceObject> input) throws java.lang.Exception {
        ReceiveMeshInterfaceObjectsRequest request = new ReceiveMeshInterfaceObjectsRequest();
        ArrayOfMeshInterfaceObject array = new ArrayOfMeshInterfaceObject();
        array.getItem().addAll(input);
        request.setInput(array);
        ReceiveMeshInterfaceObjectsResponse response= port.receiveMeshInterfaceObjects(request);
        return response.getReceiveMeshInterfaceObjectsReturn();
    }

    public List<MeshInterfaceObject> sendMeshInterfaceObjects(int size) throws java.lang.Exception {
        SendMeshInterfaceObjectsRequest request = new SendMeshInterfaceObjectsRequest();
        request.setSize(size);
        SendMeshInterfaceObjectsResponse response = port.sendMeshInterfaceObjects(request);        
        return response.getSendMeshInterfaceObjectsReturn().getItem();
    }
    
    static class Util<E> {
    	public Util() {
    		
    	}
    	public void copyList(E[] src, int off, ArrayList<E>des, int size) {
    		des.clear();
    		for(int i = 0; i< size; i++) {
    			E o = src[i+off];
    			des.add(o);
    		}
    	}	
    }
}


