package org.apache.cxf.jaxb;

public class FactoryClassLoader implements FactoryClassCreator{
    ClassLoader cl;
    public FactoryClassLoader(ClassLoader cl) {
        this.cl = cl;
    }
    @Override
    public Class<?> createFactory(Class<?> cls) {
        String newClassName = cls.getName() + "Factory";
        try {
            return cl.loadClass(newClassName);
        } catch (ClassNotFoundException e) {
        }
        return null;
    }
}
