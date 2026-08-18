package org.apache.cxf.endpoint;


import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cxf.BusFactory;
import org.apache.cxf.message.Message;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class ClientImplTest {

    private static class TestClientImpl extends ClientImpl {

        public TestClientImpl() {
            super(BusFactory.newInstance().createBus(), null);
        }

        void filter(Map<String, Object> context) {
            filterResponseContextProperties(context);
        }
    }

    private final TestClientImpl testClientImpl = new TestClientImpl();

    private static Set<String> getExcludedProperties() throws Exception {
        Field field = ClientImpl.class
                .getDeclaredField("RESPONSE_CONTEXT_EXCLUDED_IN_PROPERTIES");

        field.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<String> properties = (Set<String>) field.get(null);

        return properties;
    }

    private static Set<String> defaultExcludedProperties;

    @BeforeClass
    public static void initDefaults() throws Exception {
        defaultExcludedProperties =
                new HashSet<>(getExcludedProperties());
    }

    @Before
    public void setUp() throws Exception {
        Set<String> properties = getExcludedProperties();

        properties.clear();
        properties.addAll(defaultExcludedProperties);
    }

    @Test
    public void shouldFilterDefaultExcludedProperty() {

        Map<String, Object> context = new HashMap<>();
        context.put(Message.INVOCATION_CONTEXT, "invocation-context");
        context.put("property.to.keep", "value");

        testClientImpl.filter(context);

        assertFalse(context.containsKey(Message.INVOCATION_CONTEXT));
        assertEquals("value", context.get("property.to.keep"));
    }

    @Test
    public void shouldFilterPropertyAddedWithAdd() {
        String property = "my.custom.property";

        ClientImpl.addResponseContextExcludedInProperty(property);

        Map<String, Object> context = new HashMap<>();
        context.put(property, "custom-value");
        context.put("property.to.keep", "value");

        testClientImpl.filter(context);

        assertFalse(context.containsKey(property));
        assertEquals("value", context.get("property.to.keep"));
    }

    @Test
    public void shouldFilterPropertiesAddedWithAddAll() {
        Set<String> properties = Set.of(
                "custom.property.1",
                "custom.property.2"
        );

        ClientImpl.addAllResponseContextExcludedInProperties(properties);

        Map<String, Object> context = new HashMap<>();
        context.put("custom.property.1", "value1");
        context.put("custom.property.2", "value2");
        context.put("property.to.keep", "keep");

        testClientImpl.filter(context);

        assertFalse(context.containsKey("custom.property.1"));
        assertFalse(context.containsKey("custom.property.2"));
        assertEquals("keep", context.get("property.to.keep"));
    }

    @Test
    public void shouldKeepPropertiesNotExcluded() {

        Map<String, Object> context = new HashMap<>();
        context.put("property.1", "value1");
        context.put("property.2", "value2");

        testClientImpl.filter(context);

        assertEquals(2, context.size());
        assertEquals("value1", context.get("property.1"));
        assertEquals("value2", context.get("property.2"));
    }
}