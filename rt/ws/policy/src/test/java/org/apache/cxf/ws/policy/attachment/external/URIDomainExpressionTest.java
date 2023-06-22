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

import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    private ServiceInfo si;
    private EndpointInfo ei;
    private BindingOperationInfo boi;
    private BindingMessageInfo bmi;
    private BindingFaultInfo bfi;
    private MessageInfo mi;

    @Test
    public void testServiceInfo() {
        mockInfoObjects();

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
    }

    @Test
    public void testEndpointInfo() {
        mockInfoObjects();

        String expression = TARGET_NAMESPACE + "#wsdl11.port(" + SERVICE_NAME + "/" + PORT_NAME + ")";
        URIDomainExpression ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(ei));

        expression = TARGET_NAMESPACE + "#wsdl11.port(" +  SERVICE_NAME + "/" + PORT_NAME + "wrong" + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertFalse("Expected false for expression: " + expression, ude.appliesTo(boi));
    }

    @Test
    public void testBindingOperationInfo() {
        mockInfoObjects();

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
    }

    @Test
    public void testBindingMessageInfo() {
        mockInfoObjects();

        String expression = TARGET_NAMESPACE + "#wsdl11.message(" + MESSAGE_NAME +  ")";
        URIDomainExpression ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bmi));

        expression = TARGET_NAMESPACE + "#wsdl11.message(" + MESSAGE_NAME + "wrong" + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertFalse("Expected false for expression: " + expression, ude.appliesTo(bmi));

        mockInfoObjects();
        when(mi.getType()).thenReturn(Type.INPUT);

        expression = TARGET_NAMESPACE + "#wsdl11.bindingOperation.input(" + BINDING_NAME +  "/" + OPERATION_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bmi));

        expression = TARGET_NAMESPACE + "#wsdl11.portTypeOperation.input(" + INTERFACE_NAME + "/"
                     + OPERATION_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bmi));

        mockInfoObjects();
        when(mi.getType()).thenReturn(Type.OUTPUT);

        expression = TARGET_NAMESPACE + "#wsdl11.bindingOperation.output(" + BINDING_NAME +  "/" + OPERATION_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bmi));

        expression = TARGET_NAMESPACE + "#wsdl11.portTypeOperation.output(" + INTERFACE_NAME + "/"
                     + OPERATION_NAME + ")";
        ude = new URIDomainExpression(expression);
        Assert.assertTrue("Expected true for expression: " + expression, ude.appliesTo(bmi));
    }

    @Test
    public void testBindingOperationFault() {
        mockInfoObjects();

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
    }

    private void mockInfoObjects() {
        si = mock(ServiceInfo.class);
        ei = mock(EndpointInfo.class);
        boi = mock(BindingOperationInfo.class);
        bmi = mock(BindingMessageInfo.class);
        bfi = mock(BindingFaultInfo.class);

        InterfaceInfo ii = mock(InterfaceInfo.class);

        when(si.getTargetNamespace()).thenReturn(TARGET_NAMESPACE);
        when(si.getName()).thenReturn(SERVICE_QNAME);
        when(si.getInterface()).thenReturn(ii);
        when(ii.getName()).thenReturn(INTERFACE_QNAME);

        when(ei.getName()).thenReturn(PORT_QNAME);
        when(ei.getService()).thenReturn(si);

        BindingInfo bi = mock(BindingInfo.class);
        OperationInfo oi = mock(OperationInfo.class);

        when(boi.getName()).thenReturn(OPERATION_QNAME);
        when(boi.getBinding()).thenReturn(bi);
        when(bi.getName()).thenReturn(BINDING_QNAME);
        when(boi.getOperationInfo()).thenReturn(oi);
        when(oi.getInterface()).thenReturn(ii);
        when(oi.getName()).thenReturn(OPERATION_QNAME);

        mi = mock(MessageInfo.class);

        when(bmi.getMessageInfo()).thenReturn(mi);
        when(mi.getName()).thenReturn(MESSAGE_QNAME);
        when(bmi.getBindingOperation()).thenReturn(boi);

        FaultInfo fi = mock(FaultInfo.class);

        bfi = mock(BindingFaultInfo.class);
        when(bfi.getBindingOperation()).thenReturn(boi);
        when(bfi.getFaultInfo()).thenReturn(fi);
        when(fi.getFaultName()).thenReturn(FAULT_QNAME);

    }
}
