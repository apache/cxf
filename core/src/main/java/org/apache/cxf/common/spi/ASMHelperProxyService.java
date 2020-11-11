package org.apache.cxf.common.spi;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.ASMHelperImpl;

public class ASMHelperProxyService implements ASMHelperService {
    private final ASMHelper loader;

    public ASMHelperProxyService(Bus bus)
    {
        this.loader = bus.getExtension(ASMHelper.class);
    }

    public ASMHelperProxyService(final ProxiesASMHelper loader)
    {
        this.loader = loader;
    }

    @Override
    public ASMHelper getProxyASMHelper()
    {
        return loader;
    }

    public static class ProxiesASMHelper extends ASMHelperImpl {
        //TODO
    }
}
