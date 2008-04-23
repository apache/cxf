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

package org.apache.cxf.configuration.foo;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the org.apache.cxf.configuration.foo package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName POINT_QNAME = new QName("http://cxf.apache.org/configuration/foo",
                                                       "point");
    private static final QName ADDRESS_QNAME = new QName("http://cxf.apache.org/configuration/foo",
                                                         "address");
    private static final QName FOO_QNAME = new QName("http://cxf.apache.org/configuration/foo",
                                                     "foo");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived
     * classes for package: org.apache.cxf.configuration.foo
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Address }
     * 
     */
    public Address createAddress() {
        return new Address();
    }

    /**
     * Create an instance of {@link Foo }
     * 
     */
    public Foo createFoo() {
        return new Foo();
    }

    /**
     * Create an instance of {@link Point }
     * 
     */
    public Point createPoint() {
        return new Point();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Point }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://cxf.apache.org/configuration/foo", name = "point")
    public JAXBElement<Point> createPoint(Point value) {
        return new JAXBElement<Point>(POINT_QNAME, Point.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Address }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://cxf.apache.org/configuration/foo", name = "address")
    public JAXBElement<Address> createAddress(Address value) {
        return new JAXBElement<Address>(ADDRESS_QNAME, Address.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Foo }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://cxf.apache.org/configuration/foo", name = "foo")
    public JAXBElement<Foo> createFoo(Foo value) {
        return new JAXBElement<Foo>(FOO_QNAME, Foo.class, null, value);
    }

}
