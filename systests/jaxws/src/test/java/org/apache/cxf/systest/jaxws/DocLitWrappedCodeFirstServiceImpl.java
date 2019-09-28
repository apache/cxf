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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.annotations.FastInfoset;
import org.apache.cxf.annotations.GZIP;
import org.apache.cxf.annotations.WSDLDocumentation;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.systest.jaxws.types.Bar;
import org.apache.cxf.systest.jaxws.types.BarImpl;

@WebService(endpointInterface = "org.apache.cxf.systest.jaxws.DocLitWrappedCodeFirstService",
            serviceName = "DocLitWrappedCodeFirstService",
            portName = "DocLitWrappedCodeFirstServicePort",
            targetNamespace = "http://cxf.apache.org/systest/jaxws/DocLitWrappedCodeFirstService")
@WSDLDocumentation("DocLitWrappedCodeFirstService impl")
@GZIP(threshold = 10)
@FastInfoset(force = true)
public class DocLitWrappedCodeFirstServiceImpl implements DocLitWrappedCodeFirstService {
    public static final String[] DATA = new String[] {"string1", "string2", "string3"};

    @Resource
    WebServiceContext context;

    public DocLitWrappedCodeFirstServiceImpl() {

    }

    public int thisShouldNotBeInTheWSDL(int i) {
        return i;
    }

    public String[] arrayOutput() {
        if (context == null) {
            throw new RuntimeException("No CONTEXT!!!");
        }
        return DATA;
    }

    public List<String> listOutput() {
        if (context == null) {
            throw new RuntimeException("No CONTEXT!!!");
        }
        return new ArrayList<String>(Arrays.asList(DATA));
    }

    public String arrayInput(String[] inputs) {
        if (context == null) {
            throw new RuntimeException("No CONTEXT!!!");
        }
        StringBuilder buf = new StringBuilder();
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
        StringBuilder buf = new StringBuilder();
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
        StringBuilder buf = new StringBuilder();
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

    public void singleInOut(Holder<Boolean> created) {
        if (created.value == null) {
            created.value = false;
        } else {
            created.value = true;
        }

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
        case -4: {
            throw new RuntimeException("RuntimeException!!");
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
            e.printStackTrace();
        }
    }

    public String echoStringNotReallyAsync(String s) {
        return s;
    }

    public String doFooList(List<Foo> fooList) {
        return "size: " + fooList.size();
    }

    public String outOnly(Holder<String> out1, Holder<String> out2) {
        out1.value = "out1";
        out2.value = "out2";
        return "hello";
    }

    public Foo modifyFoo(Foo f) {
        if ("DoNoName".equals(f.getName())) {
            f.setNameIgnore("NoName");
        }
        return f;
    }

    public CXF2411Result<CXF2411SubClass> doCXF2411() {
        CXF2411Result<CXF2411SubClass> ret = new CXF2411Result<>();
        CXF2411SubClass[] content = new CXF2411SubClass[1];
        content[0] = new CXF2411SubClass();
        ret.setContent(content);
        return ret;
    }

    public String doBug2692(String name) {
        return name;
    }

}
