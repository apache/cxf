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

package org.apache.cxf.performance.server;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.indiana.extreme.wsdl.benchmark1.ArrayOfMeshInterfaceObject;
import edu.indiana.extreme.wsdl.benchmark1.ArrayOfSimpleEvent;
import edu.indiana.extreme.wsdl.benchmark1.Benchmark;
import edu.indiana.extreme.wsdl.benchmark1.EchoMeshInterfaceObjectsResponse;
import edu.indiana.extreme.wsdl.benchmark1.EchoSimpleEventsResponse;
import edu.indiana.extreme.wsdl.benchmark1.MeshInterfaceObject;
import edu.indiana.extreme.wsdl.benchmark1.ReceiveMeshInterfaceObjectsResponse;
import edu.indiana.extreme.wsdl.benchmark1.ReceiveSimpleEventsResponse;
import edu.indiana.extreme.wsdl.benchmark1.SendMeshInterfaceObjectsResponse;
import edu.indiana.extreme.wsdl.benchmark1.SendSimpleEventsResponse;
import edu.indiana.extreme.wsdl.benchmark1.SimpleEvent;



@javax.jws.WebService(name = "Benchmark", serviceName = "Benchmark",
                      portName = "Benchmark",
                      targetNamespace = "http://www.extreme.indiana.edu/wsdl/Benchmark1", 
                      endpointInterface = "edu.indiana.extreme.wsdl.benchmark1.Benchmark")
                      
public class BenchmarkImpl implements Benchmark {

    private static final Logger LOG = 
        Logger.getLogger(BenchmarkImpl.class.getPackage().getName());
    
    public BenchmarkImpl() {
    	LOG.setLevel(Level.INFO);
    }
    	
    

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#echoVoid(*
     */
    public void echoVoid() { 
        LOG.info("Executing operation echoVoid");
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#echoBase64(byte[]  input )*
     */
    public byte[] echoBase64 (byte[] input) { 
        LOG.info("Executing operation echoBase64");        
        return input;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#echoStrings(java.util.List<java.lang.String>  input )*
     */
    public java.util.List<java.lang.String> echoStrings(
        java.util.List<java.lang.String> input
    )
    { 
       LOG.info("Executing operation echoStrings");
        return input;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#echoInts(java.util.List<java.lang.Integer>  input )*
     */
    public java.util.List<java.lang.Integer> echoInts(
        java.util.List<java.lang.Integer> input
    )
    { 
        LOG.info("Executing operation echoInts");
        return input;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#echoDoubles(java.util.List<java.lang.Double>  input )*
     */
    public java.util.List<java.lang.Double> echoDoubles(
        java.util.List<java.lang.Double> input
    )
    { 
        LOG.info("Executing operation echoDoubles");
        return input;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#echoSimpleEvents(edu.indiana.extreme.wsdl.benchmark1.EchoSimpleEventsRequest  input )*
     */
    public edu.indiana.extreme.wsdl.benchmark1.EchoSimpleEventsResponse echoSimpleEvents(
        edu.indiana.extreme.wsdl.benchmark1.EchoSimpleEventsRequest input
    )
    { 
        LOG.info("Executing operation echoSimpleEvents");
        EchoSimpleEventsResponse ret = new EchoSimpleEventsResponse();
        ret.setEchoSimpleEventsReturn(input.getInput());
        return ret;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#echoMeshInterfaceObjects(edu.indiana.extreme.wsdl.benchmark1.EchoMeshInterfaceObjectsRequest  input )*
     */
    public edu.indiana.extreme.wsdl.benchmark1.EchoMeshInterfaceObjectsResponse echoMeshInterfaceObjects(
        edu.indiana.extreme.wsdl.benchmark1.EchoMeshInterfaceObjectsRequest input
    )
    { 
        LOG.info("Executing operation echoMeshInterfaceObjects");
        EchoMeshInterfaceObjectsResponse ret = new EchoMeshInterfaceObjectsResponse();
        List<MeshInterfaceObject> output = ret.getEchoMeshInterfaceObjectReturn();
        for(MeshInterfaceObject mio :input.getInput()) {
        	output.add(mio);
        }
        return ret;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#receiveBase64(byte[]  input )*
     */
    public int receiveBase64(
        byte[] input
    )
    { 
        LOG.info("Executing operation receiveBase64");
        System.out.println("Executing operation receiveBase64 " + input.length);
        return input.length;        
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#receiveStrings(java.util.List<java.lang.String>  input )*
     */
    public int receiveStrings(
        java.util.List<java.lang.String> input
    )
    { 
        LOG.info("Executing operation receiveStrings");
        return input.size();        
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#receiveInts(java.util.List<java.lang.Integer>  input )*
     */
    public int receiveInts(
        java.util.List<java.lang.Integer> input
    )
    { 
        LOG.info("Executing operation receiveInts");
        return input.size();
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#receiveDoubles(java.util.List<java.lang.Double>  input )*
     */
    public int receiveDoubles(
        java.util.List<java.lang.Double> input
    )
    { 
        LOG.info("Executing operation receiveDoubles");
        return input.size();
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#receiveSimpleEvents(edu.indiana.extreme.wsdl.benchmark1.ReceiveSimpleEventsRequest  input )*
     */
    public edu.indiana.extreme.wsdl.benchmark1.ReceiveSimpleEventsResponse receiveSimpleEvents(
        edu.indiana.extreme.wsdl.benchmark1.ReceiveSimpleEventsRequest input
    )
    { 
        LOG.info("Executing operation receiveSimpleEvents");
        ReceiveSimpleEventsResponse ret = new ReceiveSimpleEventsResponse();
        ret.setReceiveSimpleEventsReturn(input.getInput().getItem().size());
        return ret;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#receiveMeshInterfaceObjects(edu.indiana.extreme.wsdl.benchmark1.ReceiveMeshInterfaceObjectsRequest  input )*
     */
    public edu.indiana.extreme.wsdl.benchmark1.ReceiveMeshInterfaceObjectsResponse receiveMeshInterfaceObjects(
        edu.indiana.extreme.wsdl.benchmark1.ReceiveMeshInterfaceObjectsRequest input
    )
    { 
        LOG.info("Executing operation receiveMeshInterfaceObjects");
        ReceiveMeshInterfaceObjectsResponse ret = new ReceiveMeshInterfaceObjectsResponse();
        ret.setReceiveMeshInterfaceObjectsReturn(input.getInput().getItem().size());
        return ret;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#sendBase64(int  size )*
     */
    public byte[] sendBase64(
        int size
    )
    { 
        LOG.info("Executing operation sendBase64");        
        return new byte[size];
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#sendStrings(int  size )*
     */
    public java.util.List<java.lang.String> sendStrings(
        int size
    )
    { 
       LOG.info("Executing operation sendStrings");       
       List<String> ret = new ArrayList<String>(0);
       for (int i = 0; i < size; i++) {
           String temp = "s" + i;
           ret.add(temp);
       }
       return ret;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#sendInts(int  size )*
     */
    public java.util.List<java.lang.Integer> sendInts(
        int size
    )
    { 
        LOG.info("Executing operation sendInts");
        List<Integer> ret = new LinkedList<Integer>();
        for (int i = 0; i < size; i++) {
        	Integer temp = i;
        	ret.add(temp);
        }
        return ret;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#sendDoubles(int  size )*
     */
    public java.util.List<java.lang.Double> sendDoubles(
        int size
    )
    { 
        LOG.info("Executing operation sendDoubles");
        List<Double> ret = new LinkedList<Double>();
        for (int i = 0; i < size; i++) {
        	double temp = i ;
        	ret.add(temp);
        }
        return ret;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#sendSimpleEvents(edu.indiana.extreme.wsdl.benchmark1.SendSimpleEventsRequest  size )*
     */
    public edu.indiana.extreme.wsdl.benchmark1.SendSimpleEventsResponse sendSimpleEvents(
        edu.indiana.extreme.wsdl.benchmark1.SendSimpleEventsRequest size
    )
    { 
        LOG.info("Executing operation sendSimpleEvents");
        SendSimpleEventsResponse ret = new SendSimpleEventsResponse();
        ArrayOfSimpleEvent value = new ArrayOfSimpleEvent();
        List<SimpleEvent> item = value.getItem();
        for(int i = 0 ; i < size.getSize() ; i++) {        	
        	SimpleEvent object = new SimpleEvent();
            object.setSequenceNumber(i);
            object.setMessage("Message #" + i);
            object.setTimestamp(Math.sqrt(i));
            item.add(object);
        }
        ret.setSendSimpleEventsReturn(value);
        return ret;
    }

    /* (non-Javadoc)
     * @see edu.indiana.extreme.wsdl.benchmark1.Benchmark#sendMeshInterfaceObjects(edu.indiana.extreme.wsdl.benchmark1.SendMeshInterfaceObjectsRequest  size )*
     */
    public edu.indiana.extreme.wsdl.benchmark1.SendMeshInterfaceObjectsResponse sendMeshInterfaceObjects(
        edu.indiana.extreme.wsdl.benchmark1.SendMeshInterfaceObjectsRequest size
    )
    { 
        LOG.info("Executing operation sendMeshInterfaceObjects");
        SendMeshInterfaceObjectsResponse ret = new SendMeshInterfaceObjectsResponse();
        ArrayOfMeshInterfaceObject value = new ArrayOfMeshInterfaceObject();
        List<MeshInterfaceObject> item = value.getItem();
        for(int i = 0 ; i < size.getSize() ; i++) {
        	MeshInterfaceObject object = new MeshInterfaceObject();
        	object.setX(i);
            object.setY(i);
            object.setValue(Math.sqrt(i));
            item.add(object);
        }
        ret.setSendMeshInterfaceObjectsReturn(value);
        return ret;
    }

}