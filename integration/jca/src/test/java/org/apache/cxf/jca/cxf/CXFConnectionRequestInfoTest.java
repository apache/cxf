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
package org.apache.cxf.jca.cxf;

import java.net.URL;

import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.Test;


public class CXFConnectionRequestInfoTest extends Assert {

    
    @Test
    public void testCXFConnectionRequestInfoEquals() throws Exception {

        CXFConnectionRequestInfo cr1 = new CXFConnectionRequestInfo(Foo.class,
                                                                          new URL("file:/tmp/foo"),
                                                                          new QName("service"),
                                                                          new QName("fooPort"));
        CXFConnectionRequestInfo cr2 = new CXFConnectionRequestInfo(Foo.class,
                                                                          new URL("file:/tmp/foo"),
                                                                          new QName("service"),
                                                                          new QName("fooPort"));

        assertTrue("Checking equals ", cr1.equals(cr2));

        assertTrue("Checking hashcodes ", cr1.hashCode() == cr2.hashCode());

        cr1 = new CXFConnectionRequestInfo(Foo.class, null, new QName("service"), null);

        cr2 = new CXFConnectionRequestInfo(Foo.class, null, new QName("service"), null);

        assertTrue("Checking equals with null parameters ", cr1.equals(cr2));

        assertTrue("Checking hashcodes  with null parameters ", cr1.hashCode() == cr2.hashCode());

        cr1 = new CXFConnectionRequestInfo(Foo.class, new URL("file:/tmp/foo"), new QName("service"),
                                              new QName("fooPort"));
        cr2 = new CXFConnectionRequestInfo(String.class, new URL("file:/tmp/foo"), new QName("service"),
                                              new QName("fooPort"));

        assertTrue("Checking that objects are not equals ", !cr1.equals(cr2));

        cr1 = new CXFConnectionRequestInfo(Foo.class, new URL("file:/tmp/foox"), new QName("service"),
                                              new QName("fooPort"));
        cr2 = new CXFConnectionRequestInfo(Foo.class, new URL("file:/tmp/foo"), new QName("service"),
                                              new QName("fooPort"));

        assertTrue("Checking that objects are not equal ", !cr1.equals(cr2));

        cr1 = new CXFConnectionRequestInfo(Foo.class, new URL("file:/tmp/foo"), new QName("service"),
                                              new QName("fooPort"));
        cr2 = new CXFConnectionRequestInfo(Foo.class, new URL("file:/tmp/foo"), new QName("servicex"),
                                              new QName("fooPort"));

        assertTrue("Checking that objects are not equal ", !cr1.equals(cr2));

        cr1 = new CXFConnectionRequestInfo(Foo.class, new URL("file:/tmp/foo"), new QName("service"),
                                              new QName("fooPort"));
        cr2 = new CXFConnectionRequestInfo(Foo.class, new URL("file:/tmp/foo"), new QName("service"),
                                              new QName("fooPortx"));

        assertTrue("Checking that objects are not equal ", !cr1.equals(cr2));

        cr1 = new CXFConnectionRequestInfo(Foo.class, new URL("file:/tmp/foo"), new QName("service"),
                                              new QName("fooPort"));
        cr2 = new CXFConnectionRequestInfo(Foo.class, null, new QName("service"), new QName("fooPort"));

        assertTrue("Checking that objects are not equal ", !cr1.equals(cr2));

    }

    
}
