package org.apache.cxf.common.spi;

import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;

public class GeneratedClassClassLoader {
    private static final Logger LOG = LogUtils.getL7dLogger(ClassLoaderProxyService.class);
    protected final Bus bus;
    public GeneratedClassClassLoader(Bus bus) {
        this.bus = bus;
    }
    protected Class<?> loadClass(String className, Class<?> callingClass) {
        ClassLoader cl = bus.getExtension(ClassLoader.class);
        if (cl != null) {
            try {
                return cl.loadClass(className);
            } catch (ClassNotFoundException e) {
                //ignore and try with other class loader
            }
        }
        try {
            return ClassLoaderUtils.loadClass(className, callingClass);
        } catch (ClassNotFoundException e) {
            LOG.fine("Failed to load class :" + e.toString());
        }
        return null;
    }

}
