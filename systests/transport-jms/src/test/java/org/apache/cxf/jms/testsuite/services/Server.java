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
package org.apache.cxf.jms.testsuite.services;

import javax.xml.ws.Endpoint;

import org.apache.cxf.jms.testsuite.util.JMSTestUtil;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {

    protected void run() {
        Test0001Impl t0001 = new Test0001Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0001").getAddress().trim(), t0001);
        
        Test0101Impl t0101 = new Test0101Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0101").getAddress().trim(), t0101);

        Test0003Impl t0003 = new Test0003Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0003").getAddress().trim(), t0003);
        
        Test0005Impl t0005 = new Test0005Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0005").getAddress().trim(), t0005);
        
        Test0006Impl t0006 = new Test0006Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0006").getAddress().trim(), t0006);
        
        Test0008Impl t0008 = new Test0008Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0008").getAddress().trim(), t0008);
        
        Test0009Impl t0009 = new Test0009Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0009").getAddress().trim(), t0009);
        
        Test0010Impl t0010 = new Test0010Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0010").getAddress().trim(), t0010);
        
        Test0011Impl t0011 = new Test0011Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0011").getAddress().trim(), t0011);
        
        Test0012Impl t0012 = new Test0012Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0012").getAddress().trim(), t0012);
        
        Test0013Impl t0013 = new Test0013Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0013").getAddress().trim(), t0013);
        
        Test0014Impl t0014 = new Test0014Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test0014").getAddress().trim(), t0014);
        
        Test1001Impl t1001 = new Test1001Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1001").getAddress().trim(), t1001);
        
        Test1002Impl t1002 = new Test1002Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1002").getAddress().trim(), t1002);
        
        Test1003Impl t1003 = new Test1003Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1003").getAddress().trim(), t1003);
        
        Test1004Impl t1004 = new Test1004Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1004").getAddress().trim(), t1004);
        
        Test1006Impl t1006 = new Test1006Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1006").getAddress().trim(), t1006);
        
        Test1007Impl t1007 = new Test1007Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1007").getAddress().trim(), t1007);
        
        Test1008Impl t1008 = new Test1008Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1008").getAddress().trim(), t1008);
        
        Test1101Impl t1101 = new Test1101Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1101").getAddress().trim(), t1101);
        
        Test1102Impl t1102 = new Test1102Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1102").getAddress().trim(), t1102);
        
        Test1103Impl t1103 = new Test1103Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1103").getAddress().trim(), t1103);
        
        Test1104Impl t1104 = new Test1104Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1104").getAddress().trim(), t1104);
        
        Test1105Impl t1105 = new Test1105Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1105").getAddress().trim(), t1105);
        
        Test1106Impl t1106 = new Test1106Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1106").getAddress().trim(), t1106);
        
        Test1107Impl t1107 = new Test1107Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1107").getAddress().trim(), t1107);
        
        Test1108Impl t1108 = new Test1108Impl();
        Endpoint.publish(JMSTestUtil.getTestCase("test1108").getAddress().trim(), t1108);
    }

    public static void main(String[] args) {
        try {
            Server s = new Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
