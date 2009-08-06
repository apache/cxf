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
package org.apache.cxf.systest.jaxws;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.annotations.WSDLDocumentation;
import org.apache.cxf.feature.Features;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.systest.jaxws.DocLitWrappedCodeFirstService.Foo;
import org.apache.cxf.systest.jaxws.types.Bar;
import org.apache.cxf.systest.jaxws.types.BarImpl;

@WebService(endpointInterface = "org.apache.cxf.systest.jaxws.DocLitWrappedCodeFirstService",
            serviceName = "DocLitWrappedCodeFirstService",
            portName = "DocLitWrappedCodeFirstServicePort",
            targetNamespace = "http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService")
//@Features(features = { "org.apache.cxf.feature.FastInfosetFeature" })
@Features(features = { "org.apache.cxf.transport.http.gzip.GZIPFeature", 
                       "org.apache.cxf.feature.FastInfosetFeature" })
@WSDLDocumentation("DocLitWrappedCodeFirstService impl")                       
public class DocLitWrappedCodeFirstServiceImpl implements DocLitWrappedCodeFirstService {
    public static final String DATA[] = new String[] {"string1", "string2", "string3"};
    
    @Resource
    WebServiceContext context;
    
    public int thisShouldNotBeInTheWSDL(int i) {
        return i;
    }
    
    public String[] arrayOutput() {
        if (context == null) {
            throw new RuntimeException("No CONTEXT!!!");
        }
        return DATA;
    }

    public Vector<String> listOutput() {
        if (context == null) {
            throw new RuntimeException("No CONTEXT!!!");
        }
        return new Vector<String>(Arrays.asList(DATA));
    }

    public String arrayInput(String[] inputs) {
        if (context == null) {
            throw new RuntimeException("No CONTEXT!!!");
        }
        StringBuffer buf = new StringBuffer();
        for (String s : inputs) {
            buf.append(s);
        }
        return buf.toString();
    }
    
    public int[] echoIntArray(int[] ar, Exchange ex) {
        if (context == null) {
            throw new RuntimeException("No CONTEXT!!!");
        }
        return ar;
    }


    public String listInput(List<String> inputs) {
        if (context == null) {
            throw new RuntimeException("No CONTEXT!!!");
        }
        StringBuffer buf = new StringBuffer();
        if (inputs != null) {
            for (String s : inputs) {
                buf.append(s);
            }
        }
        return buf.toString();
    }
    
    public String multiListInput(List<String> inputs1, List<String> inputs2, String x, int y) {
        if (context == null) {
            throw new RuntimeException("No CONTEXT!!!");
        }
        StringBuffer buf = new StringBuffer();
        for (String s : inputs1) {
            buf.append(s);
        }
        for (String s : inputs2) {
            buf.append(s);
        }
        if (x == null) {
            buf.append("<null>");
        } else {
            buf.append(x);
        }
        buf.append(Integer.toString(y));
        return buf.toString();
    }
    
    public String multiInOut(Holder<String> a, Holder<String> b, Holder<String> c, Holder<String> d,
                             Holder<String> e, Holder<String> f, Holder<String> g) {
        String ret = b.value + d.value + e.value; 
        a.value = "a";
        b.value = "b";
        c.value = "c";
        d.value = "d";
        e.value = "e";
        f.value = "f";
        g.value = "g";
        return ret;
    }

    public List<Foo> listObjectOutput() {
        Foo a = new Foo();
        a.setName("a");
        Foo b = new Foo();
        b.setName("b");
        return Arrays.asList(a, b);
    }
    
    public Set<Foo> getFooSet() {
        return new LinkedHashSet<Foo>(listObjectOutput());
    }

    public List<Foo[]> listObjectArrayOutput() {
        Foo a = new Foo();
        a.setName("a");
        Foo b = new Foo();
        b.setName("b");
        Foo c = new Foo();
        c.setName("c");
        Foo d = new Foo();
        d.setName("d");
        
        return Arrays.asList(new Foo[] {a, b}, new Foo[] {c, d});
    }
   
    public int throwException(int i) 
        throws ServiceTestFault, CustomException, ComplexException {
        switch (i) {
        case -1:
            throw new ServiceTestFault("Hello!");
        case -2: {
            CustomException cex = new CustomException("CE: " + i);
            cex.setA("A Value");
            cex.setB("B Value");
            throw cex;
        }
        case -3: {
            ComplexException ex = new ComplexException("Throw user fault -3");
            ex.setReason("Test");
            ComplexException.MyBean bean = new ComplexException.MyBean();
            bean.setName("Marco");
            ex.setBeans(new ComplexException.MyBean[] {bean});
            ex.setInts(new int[] {1, 2, 3});
            throw ex;
        }
        default:
            throw new ServiceTestFault(new ServiceTestFault.ServiceTestDetails(i));
        }
    }
    
    public String echo(String msg) {
        return msg;
    }

    public int echoIntDifferentWrapperName(int i) {
        return i;
    }

    public Bar createBar(String val) {
        return new BarImpl(val);
    }

    public boolean listObjectIn(Holder<List<Foo[]>> foos) {
        return false;
    }

    public void doOneWay() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public String echoStringNotReallyAsync(String s) {
        return s;
    }

    public String doFooList(List<Foo> fooList) {
        return "size: " + fooList.size();
    }
    
}
