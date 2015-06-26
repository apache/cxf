package demo.jaxrs.tracing.client;

import java.util.Arrays;
import java.util.HashMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.tracing.htrace.jaxrs.HTraceClientProvider;
import org.apache.htrace.HTraceConfiguration;
import org.apache.htrace.Trace;
import org.apache.htrace.impl.AlwaysSampler;
import org.apache.htrace.impl.StandardOutSpanReceiver;

public class Client {
    public static void main( final String[] args ) throws Exception {
        final HashMap<String, String> properties = new HashMap<String, String>();
        final HTraceConfiguration conf = HTraceConfiguration.fromMap(properties);
        Trace.addReceiver(new StandardOutSpanReceiver(conf));
        
        final HTraceClientProvider provider = new HTraceClientProvider(new AlwaysSampler(conf));
        final Response response = WebClient
            .create("http://localhost:9000/catalog", Arrays.asList(provider))
            .accept(MediaType.APPLICATION_JSON)
            .get();
        
        System.out.println(response.readEntity(String.class));
        response.close();
    }
}
