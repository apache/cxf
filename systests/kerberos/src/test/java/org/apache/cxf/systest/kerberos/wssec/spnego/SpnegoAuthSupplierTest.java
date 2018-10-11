package org.apache.cxf.systest.kerberos.wssec.spnego;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.auth.SpnegoAuthSupplier;
import org.mockito.Mockito;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class SpnegoAuthSupplierTest {
    @org.junit.Test
    public void testSpnegoOverSymmetric() throws Exception {
      SpnegoAuthSupplier spnegoAuthSupplier = new SpnegoAuthSupplier();

      Map<String, String> loginConfig = new HashMap<>();
      loginConfig.put("useKeyTab", "false");
      loginConfig.put("storeKey", "true");
      loginConfig.put("refreshKrb5Config", "true");
      loginConfig.put("principal", "myuser@my.domain.com");
      loginConfig.put("useTicketCache", "true");
      loginConfig.put("debug", String.valueOf(true));

      spnegoAuthSupplier.setLoginConfig(new Configuration() {
        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
          return new AppConfigurationEntry[] {
            new AppConfigurationEntry("com.sun.security.auth.module.Krb5LoginModule",
              AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
              loginConfig)};
        }
      });

      URI uri = new URI("http://some-test-domain-doesnt-exist.com/");

      AuthorizationPolicy authorizationPolicy = Mockito.mock(AuthorizationPolicy.class);
      Message message = Mockito.mock(Message.class);
      spnegoAuthSupplier.getAuthorization(authorizationPolicy, uri, message, "ignored anyway");
    }
}
