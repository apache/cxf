package org.apache.cxf.jaxb;

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.ClassGeneratorClassLoader;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.ReflectionUtil;

import java.lang.reflect.Constructor;

public class FactoryClassGenerator extends ClassGeneratorClassLoader implements FactoryClassCreator {
    private ASMHelper helper;
    FactoryClassGenerator (Bus bus) {
        helper = bus.getExtension(ASMHelper.class);
    }
    @SuppressWarnings("unused")
    public Class<?> createFactory(Class<?> cls) {
        String newClassName = cls.getName() + "Factory";
        Class<?> factoryClass = findClass(newClassName, cls);
        if (factoryClass != null) {
            return factoryClass;
        }
        Constructor<?> contructor = ReflectionUtil.getDeclaredConstructors(cls)[0];
        ASMHelper.OpcodesProxy Opcodes = helper.getOpCodes();
        ASMHelper.ClassWriter cw = helper.createClassWriter();
        ASMHelper.MethodVisitor mv;

        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                helper.periodToSlashes(newClassName), null, "java/lang/Object", null);

        cw.visitSource(cls.getSimpleName() + "Factory" + ".java", null);

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "create" + cls.getSimpleName(),
                "()L" + helper.periodToSlashes(cls.getName()) + ";", null, null);
        mv.visitCode();
        String name = cls.getName().replace(".", "/");
        mv.visitTypeInsn(Opcodes.NEW, name);
        mv.visitInsn(Opcodes.DUP);
        StringBuilder paraString = new StringBuilder(32).append("(");

        for (Class<?> paraClass : contructor.getParameterTypes()) {
            mv.visitInsn(Opcodes.ACONST_NULL);
            paraString.append("Ljava/lang/Object;");
        }
        paraString.append(")V");

        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, name, "<init>", paraString.toString(), false);

        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cw.visitEnd();
        return loadClass(newClassName, cls, cw.toByteArray());
    }
}
