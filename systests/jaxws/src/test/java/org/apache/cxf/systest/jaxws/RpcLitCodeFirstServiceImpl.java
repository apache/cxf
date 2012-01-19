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
import java.util.List;
import java.util.Vector;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.cxf.systest.jaxws.DocLitWrappedCodeFirstService.Foo;

@WebService(endpointInterface = "org.apache.cxf.systest.jaxws.RpcLitCodeFirstService",
            serviceName = "RpcLitCodeFirstService",
            portName = "RpcLitCodeFirstServicePort",
            targetNamespace = "http://cxf.apache.org/systest/jaxws/RpcLitCodeFirstService")
public class RpcLitCodeFirstServiceImpl implements RpcLitCodeFirstService {
    public static final String DATA[] = new String[] {"string1", "string2", "string3"};
    
    public int thisShouldNotBeInTheWSDL(int i) {
        return i;
    }
    
    public String[] arrayOutput() {
        return DATA;
    }

    public Vector<String> listOutput() {
        return new Vector<String>(Arrays.asList(DATA));
    }

    public String arrayInput(String[] inputs) {
        StringBuffer buf = new StringBuffer();
        for (String s : inputs) {
            buf.append(s);
        }
        return buf.toString();
    }

    public String listInput(List<String> inputs) {
        StringBuffer buf = new StringBuffer();
        if (inputs != null) {
            for (String s : inputs) {
                buf.append(s);
            }
        }
        return buf.toString();
    }
    
    public String multiListInput(List<String> inputs1, List<String> inputs2, String x, int y) {
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
    public String multiHeaderInOut(Holder<String> a, Holder<String> b,
                                   Holder<String> c,  Holder<String> d,
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

    public String[] convertToString(int[] numbers) {
        String ret[] = new String[numbers.length];
        for (int x = 0; x < numbers.length; x++) {
            ret[x] = Integer.toString(numbers[x]);
        }
        return ret;
    }
}
