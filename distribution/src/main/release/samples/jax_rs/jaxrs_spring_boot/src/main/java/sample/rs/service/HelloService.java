package sample.rs.service;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
 
@Path("/sayHello")
public class HelloService {
 
    @GET
    @Path("/{a}")
    @Produces(MediaType.TEXT_PLAIN)
    public String sayHello(@PathParam("a") String a) {
        return "Hello " + a + ", Welcome to CXF RS Spring Boot World!!!";
    }
    
}