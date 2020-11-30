package org.apache.cxf.common.spi;

import java.util.Map;

public interface ClassGeneratorCapture {
    void save(String className, byte[] bytes);

    Map<String, byte[]> restore();
}
