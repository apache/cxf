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

import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.jaxb.JAXBContextCache;
import org.apache.cxf.jaxb.JAXBContextCache.CachedContextAndSchemas;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.neethi.Assertion;
import org.apache.neethi.PolicyComponent;


/**
 * 
 */
public class JaxbAssertion<T> extends PrimitiveAssertion {
    private JAXBContext context;
    private Set<Class<?>> classes;
    
    private T data;

    public JaxbAssertion() {
    }

    public JaxbAssertion(QName qn, boolean optional) {
        super(qn, optional);
    }
    
    public JaxbAssertion(QName qn, boolean optional, boolean ignorable) {
        super(qn, optional, ignorable);
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

    protected Assertion clone(boolean optional) {
        JaxbAssertion<T> a = new JaxbAssertion<T>(getName(), optional, ignorable);
        a.setData(data);
        return a;        
    } 
    
    @SuppressWarnings("unchecked")
    public static <T> JaxbAssertion<T> cast(Assertion a) {
        return (JaxbAssertion<T>)a;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> JaxbAssertion<T> cast(Assertion a, Class<T> type) {
        return (JaxbAssertion<T>)a;
    }

    private synchronized JAXBContext getContext() throws JAXBException {
        if (context == null || classes == null) {
            CachedContextAndSchemas ccs 
                = JAXBContextCache.getCachedContextAndSchemas(data.getClass());
            classes = ccs.getClasses();
            context = ccs.getContext();
        }
        return context;
    }
    
    @Override
    public void serialize(XMLStreamWriter writer) throws XMLStreamException {
        try {
            JAXBContext ctx = getContext();
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty("jaxb.fragment", Boolean.TRUE);
            marshaller.marshal(data, writer);
        } catch (JAXBException ex) {
            throw new RuntimeException(ex);
        }
    }

}
