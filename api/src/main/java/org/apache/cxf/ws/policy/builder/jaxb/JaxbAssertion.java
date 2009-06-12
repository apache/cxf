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

package org.apache.cxf.ws.policy.builder.jaxb;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.neethi.PolicyComponent;


/**
 * 
 */
public class JaxbAssertion<T> extends PrimitiveAssertion {
    
    private T data;

    public JaxbAssertion() {
    }

    public JaxbAssertion(QName qn, boolean optional) {
        super(qn, optional);
    }
      
    @Override
    @SuppressWarnings("unchecked")
    public boolean equal(PolicyComponent policyComponent) {
        if (!super.equal(policyComponent)) {
            return false;
        }
        JaxbAssertion<T> a = (JaxbAssertion<T>)policyComponent;
        return data.equals(a.getData());
    }

    public void setData(T d) {
        data = d;
    }

    public T getData() {
        return data;
    }

    protected PolicyAssertion cloneMandatory() {
        JaxbAssertion<T> a = new JaxbAssertion<T>(getName(), false);
        a.setData(data);
        return a;        
    } 
    
    @SuppressWarnings("unchecked")
    public static <T> JaxbAssertion<T> cast(PolicyAssertion a) {
        return (JaxbAssertion<T>)a;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> JaxbAssertion<T> cast(PolicyAssertion a, Class<T> type) {
        return (JaxbAssertion<T>)a;
    }

    @Override
    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        try {
            JAXBContext context = JAXBContext.newInstance(PackageUtils.getPackageName(data.getClass()),
                                                          data.getClass().getClassLoader());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty("jaxb.fragment", Boolean.TRUE);
            marshaller.marshal(data, writer);
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }

}
