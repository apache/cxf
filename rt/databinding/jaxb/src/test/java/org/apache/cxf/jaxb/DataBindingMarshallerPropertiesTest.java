package org.apache.cxf.jaxb;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DataBindingMarshallerPropertiesTest extends TestBase {
    @Test 
    public void testInitializeUnmarshallerProperties() throws Exception {
        JAXBDataBinding db = new JAXBDataBinding();
        Map<String, Object> unmarshallerProperties = new HashMap<String, Object>();
        unmarshallerProperties.put("someproperty", "somevalue");
    	db.setUnmarshallerProperties(unmarshallerProperties);
        
      	db.initialize(service);
        
		assertTrue("somevalue".equals(db.getUnmarshallerProperties().get("someproperty")));
    }
}
