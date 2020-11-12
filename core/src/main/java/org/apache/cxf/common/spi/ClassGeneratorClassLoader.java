package org.apache.cxf.common.spi;

import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.WeakIdentityHashMap;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClassGeneratorClassLoader implements ClassCreator {
    protected static final Map<ClassLoader, WeakReference<TypeHelperClassLoader>> LOADER_MAP
            = new WeakIdentityHashMap<>();
    protected static final Map<Class<?>, WeakReference<TypeHelperClassLoader>> CLASS_MAP
            = new WeakIdentityHashMap<>();
    private ClassGenerator gen;
    public ClassGeneratorClassLoader(Bus bus) {
        gen = new ClassGenerator(bus);
    }
    public synchronized Class<?> createNamespaceWrapper(Class<?> mcls, Map<String, String> map) {
        String postFix = "";

        if (mcls.getName().contains("eclipse")) {
            return createEclipseNamespaceMapper(mcls, map);
        } else if (mcls.getName().contains(".internal")) {
            postFix = "Internal";
        } else if (mcls.getName().contains("com.sun")) {
            postFix = "RI";
        }

        String className = "org.apache.cxf.jaxb.NamespaceMapper";
        className += postFix;
        Class<?> cls = findClass(className, ClassGeneratorClassLoader.class);
        if (cls == null) {
            cls = findClass(className, mcls);
        }
        Throwable t = null;
        if (cls == null) {
            try {
                byte[] bts = gen.createNamespaceWrapperInternal(postFix);
                className = "org.apache.cxf.jaxb.NamespaceMapper" + postFix;
                return loadClass(className, mcls, bts);
            } catch (RuntimeException ex) {
                // continue
                t = ex;
            }
        }
        if (cls == null
                && (!mcls.getName().contains(".internal.") && mcls.getName().contains("com.sun"))) {
            try {
                cls = ClassLoaderUtils.loadClass("org.apache.cxf.common.jaxb.NamespaceMapper",
                        ClassGenerator.class);
            } catch (Throwable ex2) {
                // ignore
                t = ex2;
            }
        }
        return cls;
    }

    private Class<?> createEclipseNamespaceMapper(Class<?> mcls, Map<String, String> map) {
        String className = "org.apache.cxf.jaxb.EclipseNamespaceMapper";
        Class<?> cls = findClass(className, ClassGeneratorClassLoader.class);
        if (cls != null)
            return cls;
        byte[] bts = gen.createEclipseNamespaceMapper(mcls);
        return loadClass(className, mcls, bts);
    }

    private Class<?> loadClass(String className, Class<?> clz, byte[] bytes) {
        TypeHelperClassLoader loader = getTypeHelperClassLoader(clz);
        synchronized (loader) {
            Class<?> cls = loader.lookupDefinedClass(className);
            if (cls == null) {
                return loader.defineClass(className, bytes);
            }
            return cls;
        }
    }
    private Class<?> loadClass(String className, ClassLoader l, byte[] bytes) {
        TypeHelperClassLoader loader = getTypeHelperClassLoader(l);
        synchronized (loader) {
            Class<?> cls = loader.lookupDefinedClass(className);
            if (cls == null) {
                return loader.defineClass(className, bytes);
            }
            return cls;
        }
    }
    private Class<?> findClass(String className, Class<?> clz) {
        TypeHelperClassLoader loader = getTypeHelperClassLoader(clz);
        return loader.lookupDefinedClass(className);
    }
    private Class<?> findClass(String className, ClassLoader l) {
        TypeHelperClassLoader loader = getTypeHelperClassLoader(l);
        return loader.lookupDefinedClass(className);
    }

    private synchronized TypeHelperClassLoader getTypeHelperClassLoader(ClassLoader l) {
        WeakReference<TypeHelperClassLoader> ref = LOADER_MAP.get(l);
        TypeHelperClassLoader ret;
        if (ref == null || ref.get() == null) {
            ret = new TypeHelperClassLoader(l);
            LOADER_MAP.put(l, new WeakReference<TypeHelperClassLoader>(ret));
        } else {
            ret = ref.get();
        }
        return ret;
    }
    private synchronized TypeHelperClassLoader getTypeHelperClassLoader(Class<?> cls) {
        WeakReference<TypeHelperClassLoader> ref = CLASS_MAP.get(cls);
        TypeHelperClassLoader ret;
        if (ref == null || ref.get() == null) {
            ret = new TypeHelperClassLoader(cls.getClassLoader());
            CLASS_MAP.put(cls, new WeakReference<TypeHelperClassLoader>(ret));
        } else {
            ret = ref.get();
        }
        return ret;
    }


    public static class TypeHelperClassLoader extends ClassLoader {
        ConcurrentHashMap<String, Class<?>> defined = new ConcurrentHashMap<>();

        TypeHelperClassLoader(ClassLoader parent) {
            super(parent);
        }
        public Class<?> lookupDefinedClass(String name) {
            return defined.get(name.replace('/', '.'));
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.endsWith("package-info")) {
                return getParent().loadClass(name);
            }
            return super.findClass(name);
        }
        public void addExternalClass(String name, Class<?> cls) {
            if (name == null) {
                return;
            }
            defined.putIfAbsent(name.replace('/', '.'), cls);
        }
        public Class<?> defineClass(String name, byte[] bytes) {
            Class<?> ret = defined.get(name.replace('/', '.'));
            if (ret != null) {
                return ret;
            }
            if (name.endsWith("package-info")) {
                Package p = super.getPackage(name.substring(0, name.length() - 13));
                if (p == null) {
                    definePackage(name.substring(0, name.length() - 13).replace('/', '.'),
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null);
                }
            }

            ret = super.defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
            Class<?> tmpRet = defined.putIfAbsent(name.replace('/', '.'), ret);
            if (tmpRet != null) {
                ret = tmpRet;
            }
            return ret;
        }
    }
}
