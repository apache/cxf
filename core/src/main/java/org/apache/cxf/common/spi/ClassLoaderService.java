package org.apache.cxf.common.spi;

import java.util.Map;

public interface ClassLoaderService {
    Object createNamespaceWrapper(Class<?> mcls, Map<String, String> map);
}
