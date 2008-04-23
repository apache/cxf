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
package org.apache.cxf.aegis.type.encoded;

import java.util.Map;
import java.util.TreeMap;
import javax.xml.namespace.QName;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.type.basic.BeanTypeInfo;
import org.apache.cxf.aegis.xml.jdom.JDOMWriter;
import org.apache.cxf.aegis.xml.stax.ElementReader;
import org.apache.cxf.helpers.CastUtils;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.junit.Test;

public class StructTypeTest extends AbstractEncodedTest {
    private StructType addressType;
    private StructType purchaseOrderType;
    
    private Context getLocalContext() {
        AegisContext aegisContext = new AegisContext();
        return new Context(aegisContext);
    }

    public void setUp() throws Exception {
        super.setUp();

        // address type
        BeanTypeInfo addressInfo = new BeanTypeInfo(Address.class, "urn:Bean");
        addressInfo.setTypeMapping(mapping);

        addressType = new StructType(addressInfo);
        addressType.setTypeClass(Address.class);
        addressType.setSchemaType(new QName("urn:Bean", "address"));
        mapping.register(addressType);

        // purchase order type
        BeanTypeInfo poInfo = new BeanTypeInfo(PurchaseOrder.class, "urn:Bean");
        poInfo.setTypeMapping(mapping);

        purchaseOrderType = new StructType(poInfo);
        purchaseOrderType.setTypeClass(PurchaseOrder.class);
        purchaseOrderType.setTypeMapping(mapping);
        purchaseOrderType.setSchemaType(new QName("urn:Bean", "po"));
        mapping.register(purchaseOrderType);
    }

    @Test
    public void testSimpleStruct() throws Exception {
        // Test reading
        ElementReader reader = new ElementReader(getClass().getResourceAsStream("struct1.xml"));
        Address address = (Address) addressType.readObject(reader, getLocalContext());
        validateShippingAddress(address);
        reader.getXMLStreamReader().close();

        // Test reading - no namespace on nested elements
        reader = new ElementReader(getClass().getResourceAsStream("struct2.xml"));
        address = (Address) addressType.readObject(reader, getLocalContext());
        validateShippingAddress(address);
        reader.getXMLStreamReader().close();

        // Test writing
        Element element = new Element("root", "b", "urn:Bean");
        new Document(element);
        addressType.writeObject(address, new JDOMWriter(element), getLocalContext());
        validateShippingAddress(element);
    }

    @Test
    public void testComplexStruct() throws Exception {
        // Test reading
        ElementReader reader = new ElementReader(getClass().getResourceAsStream("struct3.xml"));
        PurchaseOrder po = (PurchaseOrder) purchaseOrderType.readObject(reader, getLocalContext());
        validatePurchaseOrder(po);
        reader.getXMLStreamReader().close();

        // Test reading - no namespace on nested elements
        reader = new ElementReader(getClass().getResourceAsStream("struct4.xml"));
        po = (PurchaseOrder) purchaseOrderType.readObject(reader, getLocalContext());
        validatePurchaseOrder(po);
        reader.getXMLStreamReader().close();

        // Test writing
        Element element = writeRef(po);
        validatePurchaseOrder(element);
    }

    @Test
    public void testStructRef() throws Exception {
        PurchaseOrder purchaseOrder;

        // Simple nested ref
        purchaseOrder = (PurchaseOrder) readRef("ref1.xml");
        validatePurchaseOrder(purchaseOrder);

        // Strings referenced
        purchaseOrder = (PurchaseOrder) readRef("ref2.xml");
        validatePurchaseOrder(purchaseOrder);

        // completely unrolled
        purchaseOrder = (PurchaseOrder) readRef("ref3.xml");
        validatePurchaseOrder(purchaseOrder);

        // Test writing
        Element element = writeRef(purchaseOrder);

        validatePurchaseOrder(element);
    }

    public static void validateShippingAddress(Element shipping) {
        assertNotNull("shipping is null", shipping);
        assertChildEquals("1234 Riverside Drive", shipping, "street");
        assertChildEquals("Gainesville", shipping, "city");
        assertChildEquals("FL", shipping, "state");
        assertChildEquals("30506", shipping, "zip");
    }

    public static void validateBillingAddress(Element billing) {
        assertNotNull("billing is null", billing);
        assertChildEquals("1234 Fake Street", billing, "street");
        assertChildEquals("Las Vegas", billing, "city");
        assertChildEquals("NV", billing, "state");
        assertChildEquals("89102", billing, "zip");
    }

    private void validatePurchaseOrder(PurchaseOrder purchaseOrder) {
        assertNotNull(purchaseOrder);
        assertNotNull(purchaseOrder.getShipping());
        assertEquals("1234 Riverside Drive", purchaseOrder.getShipping().getStreet());
        assertEquals("Gainesville", purchaseOrder.getShipping().getCity());
        assertEquals("FL", purchaseOrder.getShipping().getState());
        assertEquals("30506", purchaseOrder.getShipping().getZip());
        assertNotNull(purchaseOrder.getBilling());
        assertEquals("1234 Fake Street", purchaseOrder.getBilling().getStreet());
        assertEquals("Las Vegas", purchaseOrder.getBilling().getCity());
        assertEquals("NV", purchaseOrder.getBilling().getState());
        assertEquals("89102", purchaseOrder.getBilling().getZip());
    }

    private void validatePurchaseOrder(Element element) throws Exception {
        Element poRefElement = null;
        Map<String, Element> blocks = new TreeMap<String, Element>();
        for (Content content : CastUtils.<Content>cast(element.getContent())) {
            if (content instanceof Element) {
                Element child = (Element) content;
                if (poRefElement == null) {
                    poRefElement = child;
                } else {
                    String id = getId("Trailing block ", child);
                    blocks.put(id, child);
                }
            }
        }

        Element po = getReferencedElement("poRef", poRefElement, blocks);

        Element shippingRef = po.getChild("shipping");
        Element shipping = getReferencedElement("shipping", shippingRef, blocks);
        validateShippingAddress(shipping);

        Element billingRef = po.getChild("billing");
        Element billing = getReferencedElement("billing", billingRef, blocks);
        validateBillingAddress(billing);
    }

    private Element getReferencedElement(String childName,
            Element element,
            Map<String, Element> blocks) {
        assertNotNull(childName + " is null", element);
        assertNotNull("element is null", element);
        String refId = getRef(childName, element);
        Element refElement = blocks.get(refId);
        assertNotNull(childName + " referenced non-existant element " + refId, refElement);
        return refElement;
    }

    private static void assertChildEquals(String expected, Element element, String childName) {
        assertEquals(expected, element.getChild(childName).getText());
    }

    private String getId(String childName, Element child) {
        assertNotNull(childName + " is null", child);
        Attribute idAttribute = child.getAttribute("id");
        XMLOutputter xmlOutputter = new XMLOutputter();
        assertNotNull(childName + " id is null \n" + xmlOutputter.outputString(child), idAttribute);
        String id = idAttribute.getValue();
        assertNotNull(childName + " id is null \n" + xmlOutputter.outputString(child), id);
        return id;
    }

    private String getRef(String childName, Element child) {
        assertNotNull(childName + " is null", child);
        Attribute hrefAttribute = child.getAttribute("href");
        XMLOutputter xmlOutputter = new XMLOutputter();
        assertNotNull(childName + " href is null \n" + xmlOutputter.outputString(child), hrefAttribute);
        String href = hrefAttribute.getValue();
        assertNotNull(childName + " href is null \n" + xmlOutputter.outputString(child), href);
        return href;
    }
}
