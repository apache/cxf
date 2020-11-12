package org.apache.cxf.jaxws.spi;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxws.WrapperClassGenerator;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class GeneratedWrapperClassLoader implements WrapperClassCreator {
    private ClassLoader cl;
    private Set<Class<?>> wrapperBeans = new LinkedHashSet<>();
    private InterfaceInfo interfaceInfo;
    private boolean qualified;
    private JaxWsServiceFactoryBean factory;

    public GeneratedWrapperClassLoader(ClassLoader cl) {
        this.cl = cl;
    }

    @Override
    public Set<Class<?>> generate(Bus bus, JaxWsServiceFactoryBean fact, InterfaceInfo interfaceInfo, boolean q) {
        factory = fact;
        this.interfaceInfo = interfaceInfo;
        qualified = q;
        for (OperationInfo opInfo : interfaceInfo.getOperations()) {
            if (opInfo.isUnwrappedCapable()) {
                Method method = (Method)opInfo.getProperty(ReflectionServiceFactoryBean.METHOD);
                if (method == null) {
                    continue;
                }
                MessagePartInfo inf = opInfo.getInput().getFirstMessagePart();
                if (inf.getTypeClass() == null) {
                    MessageInfo messageInfo = opInfo.getUnwrappedOperation().getInput();
                    createWrapperClass(inf,
                            messageInfo,
                            opInfo,
                            method,
                            true);
                }
                MessageInfo messageInfo = opInfo.getUnwrappedOperation().getOutput();
                if (messageInfo != null) {
                    inf = opInfo.getOutput().getFirstMessagePart();
                    if (inf.getTypeClass() == null) {
                        createWrapperClass(inf,
                                messageInfo,
                                opInfo,
                                method,
                                false);
                    }
                }
            }
        }
        return wrapperBeans;
    }

    private void createWrapperClass(MessagePartInfo wrapperPart,
                                    MessageInfo messageInfo,
                                    OperationInfo op,
                                    Method method,
                                    boolean isRequest) {
        QName wrapperElement = messageInfo.getName();
        boolean anonymous = factory.getAnonymousWrapperTypes();

        String pkg = getPackageName(method) + ".jaxws_asm" + (anonymous ? "_an" : "");
        String className = pkg + "."
                + StringUtils.capitalize(op.getName().getLocalPart());
        if (!isRequest) {
            className = className + "Response";
        }

        Class<?> clz = null;
        try {
            clz = cl.loadClass(className);
        } catch (ClassNotFoundException e) {
        }
        wrapperPart.setTypeClass(clz);
        wrapperBeans.add(clz);
    }
    private String getPackageName(Method method) {
        String pkg = PackageUtils.getPackageName(method.getDeclaringClass());
        return pkg.length() == 0 ? WrapperClassGenerator.DEFAULT_PACKAGE_NAME : pkg;
    }
}
