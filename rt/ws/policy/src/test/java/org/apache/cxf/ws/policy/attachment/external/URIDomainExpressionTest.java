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

package org.apache.cxf.ws.policy.attachment.external;

import javax.xml.namespace.QName;

import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessageInfo.Type;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class URIDomainExpressionTest {

    private static final String TARGET_NAMESPACE = "http://org.apache.cxf/targetNamespace";
    private static final String SERVICE_NAME = "testService";
    private static final QName SERVICE_QNAME = new QName(TARGET_NAMESPACE, SERVICE_NAME);

    private static final String INTERFACE_NAME = "testPortType";
    private static final QName INTERFACE_QNAME = new QName(TARGET_NAMESPACE, INTERFACE_NAME);

    private static final String PORT_NAME = "testPort";
    private static final QName PORT_QNAME = new QName(TARGET_NAMESPACE, PORT_NAME);

    private static final String OPERATION_NAME = "testOperation";
    private static final QName OPERATION_QNAME = new QName(TARGET_NAMESPACE, OPERATION_NAME);

    private static final String BINDING_NAME = "testBinding";
    private static final QName BINDING_QNAME = new QName(TARGET_NAMESPACE, BINDING_NAME);

    private static final String MESSAGE_NAME = "testBinding";
    private static final QName MESSAGE_QNAME = new QName(TARGET_NAMESPACE, MESSAGE_NAME);

    private static final String FAULT_NAME = "testFault";
    private static final QName FAULT_QNAME = new QName(TARGET_NAMESPACE, FAULT_NAME);

    private IMocksControl control;

    private ServiceInfo si;
    private EndpointInfo ei;
    private BindingOperationInfo boi;
    private BindingMessageInfo bmi;
    private BindingFaultInfo bfi;
    private MessageInfo mi;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }

    @Test
    public void testServiceInfo() {
        mockInfoObjects();

        control.replay();

        String expression = TARGET_NAMESPACE + "#wsdl11.definitions()";
        URIDomainExpression ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(si));

        expression = TARGET_NAMESPACE + "#wsdl11.service(" + SERVICE_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(si));

        expression = TARGET_NAMESPACE + "#wsdl11.portType(" + INTERFACE_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(si));

        expression = TARGET_NAMESPACE + "#wsdl11.portType(" + INTERFACE_NAME + "wrong" + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertFalse("Expected false for expression: " + expression, ude.appliesTo(si));

        expression = TARGET_NAMESPACE + "wrong" + "#wsdl11.portType(" + INTERFACE_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertFalse("Expected false for expression: " + expression, ude.appliesTo(si));

        control.reset();
    }

    @Test
    public void testEndpointInfo() {
        mockInfoObjects();

        control.replay();

        String expression = TARGET_NAMESPACE + "#wsdl11.port(" + SERVICE_NAME + "/" + PORT_NAME + ")";
        URIDomainExpression ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(ei));

        expression = TARGET_NAMESPACE + "#wsdl11.port(" +  SERVICE_NAME + "/" + PORT_NAME + "wrong" + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertFalse("Expected false for expression: " + expression, ude.appliesTo(boi));

        control.reset();
    }

    @Test
    public void testBindingOperationInfo() {
        mockInfoObjects();

        control.replay();

        String expression = TARGET_NAMESPACE + "#wsdl11.binding(" + BINDING_NAME +  ")";
        URIDomainExpression ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(boi));

        expression = TARGET_NAMESPACE + "#wsdl11.bindingOperation(" + BINDING_NAME +  "/" + OPERATION_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(boi));

        expression = TARGET_NAMESPACE + "#wsdl11.portTypeOperation(" + INTERFACE_NAME +  "/" + OPERATION_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(boi));

        expression = TARGET_NAMESPACE + "#wsdl11.portTypeOperation(" + INTERFACE_NAME + "/"
                     + OPERATION_NAME + "wrong" + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertFalse("Expected false for expression: " + expression, ude.appliesTo(boi));

        control.reset();
    }

    @Test
    public void testBindingMessageInfo() {
        mockInfoObjects();

        control.replay();

        String expression = TARGET_NAMESPACE + "#wsdl11.message(" + MESSAGE_NAME +  ")";
        URIDomainExpression ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bmi));

        expression = TARGET_NAMESPACE + "#wsdl11.message(" + MESSAGE_NAME + "wrong" + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertFalse("Expected false for expression: " + expression, ude.appliesTo(bmi));

        control.reset();

        mockInfoObjects();
        EasyMock.expect(mi.getType()).andReturn(Type.INPUT).anyTimes();
        control.replay();

        expression = TARGET_NAMESPACE + "#wsdl11.bindingOperation.input(" + BINDING_NAME +  "/" + OPERATION_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bmi));

        expression = TARGET_NAMESPACE + "#wsdl11.portTypeOperation.input(" + INTERFACE_NAME + "/"
                     + OPERATION_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bmi));

        control.reset();

        mockInfoObjects();
        EasyMock.expect(mi.getType()).andReturn(Type.OUTPUT).anyTimes();
        control.replay();

        expression = TARGET_NAMESPACE + "#wsdl11.bindingOperation.output(" + BINDING_NAME +  "/" + OPERATION_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bmi));

        expression = TARGET_NAMESPACE + "#wsdl11.portTypeOperation.output(" + INTERFACE_NAME + "/"
                     + OPERATION_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bmi));

        control.reset();
    }

    @Test
    public void testBindingOperationFault() {
        mockInfoObjects();

        control.replay();

        String expression = TARGET_NAMESPACE + "#wsdl11.bindingOperation.fault(" + BINDING_NAME
                            + "/" + OPERATION_NAME + "/" + FAULT_NAME + ")";
        URIDomainExpression ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bfi));

        expression = TARGET_NAMESPACE + "#wsdl11.portTypeOperation.fault(" + INTERFACE_NAME
                     + "/" + OPERATION_NAME + "/" + FAULT_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bfi));

        expression = TARGET_NAMESPACE + "#wsdl11.portTypeOperation.fault(" + INTERFACE_NAME
                     + "/" + OPERATION_NAME + "/" + FAULT_NAME + "wrong" + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertFalse("Expected false for expression: " + expression, ude.appliesTo(bfi));

        control.reset();
    }

    private void mockInfoObjects() {
        si = control.createMock(ServiceInfo.class);
        ei = control.createMock(EndpointInfo.class);
        boi = control.createMock(BindingOperationInfo.class);
        bmi = control.createMock(BindingMessageInfo.class);
        bfi = control.createMock(BindingFaultInfo.class);

        InterfaceInfo ii = control.createMock(InterfaceInfo.class);

        EasyMock.expect(si.getTargetNamespace()).andReturn(TARGET_NAMESPACE).anyTimes();
        EasyMock.expect(si.getName()).andReturn(SERVICE_QNAME).anyTimes();
        EasyMock.expect(si.getInterface()).andReturn(ii).anyTimes();
        EasyMock.expect(ii.getName()).andReturn(INTERFACE_QNAME).anyTimes();

        EasyMock.expect(ei.getName()).andReturn(PORT_QNAME).anyTimes();
        EasyMock.expect(ei.getService()).andReturn(si).anyTimes();

        BindingInfo bi = control.createMock(BindingInfo.class);
        OperationInfo oi = control.createMock(OperationInfo.class);

        EasyMock.expect(boi.getName()).andReturn(OPERATION_QNAME).anyTimes();
        EasyMock.expect(boi.getBinding()).andReturn(bi).anyTimes();
        EasyMock.expect(bi.getName()).andReturn(BINDING_QNAME).anyTimes();
        EasyMock.expect(boi.getOperationInfo()).andReturn(oi).anyTimes();
        EasyMock.expect(oi.getInterface()).andReturn(ii).anyTimes();
        EasyMock.expect(oi.getName()).andReturn(OPERATION_QNAME).anyTimes();

        mi = control.createMock(MessageInfo.class);

        EasyMock.expect(bmi.getMessageInfo()).andReturn(mi).anyTimes();
        EasyMock.expect(mi.getName()).andReturn(MESSAGE_QNAME).anyTimes();
        EasyMock.expect(bmi.getBindingOperation()).andReturn(boi).anyTimes();

        FaultInfo fi = control.createMock(FaultInfo.class);

        bfi = control.createMock(BindingFaultInfo.class);
        EasyMock.expect(bfi.getBindingOperation()).andReturn(boi).anyTimes();
        EasyMock.expect(bfi.getFaultInfo()).andReturn(fi).anyTimes();
        EasyMock.expect(fi.getFaultName()).andReturn(FAULT_QNAME).anyTimes();

    }
}
