package org.apache.cxf.common.spi;

import java.util.Map;

public class GeneratedClassLoader implements ClassCreator {
    ClassLoader cl;
    GeneratedClassLoader (ClassLoader cl) {
        this.cl = cl;
    }
    public synchronized Class<?> createNamespaceWrapper(Class<?> mcls, Map<String, String> map) {
        String postFix = "";

        if (mcls.getName().contains("eclipse")) {
            try {
                return cl.loadClass("org.apache.cxf.jaxb.EclipseNamespaceMapper");
            } catch (ClassNotFoundException e) {
            }
        } else if (mcls.getName().contains(".internal")) {
            postFix = "Internal";
        } else if (mcls.getName().contains("com.sun")) {
            postFix = "RI";
        }
        try {
            return cl.loadClass("org.apache.cxf.jaxb.NamespaceMapper"+postFix);
        } catch (ClassNotFoundException e) {
        }
        return null;
    }
}
