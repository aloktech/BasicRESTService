
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.client.ClientConfig;
import org.json.JSONObject;
import org.junit.Test;

/**
 *
 * @author Alok
 */
public class BasicServiceTest {

    @Test
    public void testingGET() {
        ClientConfig config = new ClientConfig();

        Client client = ClientBuilder.newClient(config);

        WebTarget target = client.target(getBaseURI());

        Response response = target.path("basic").
                path("user").
                path("name").
                request().
                accept(MediaType.APPLICATION_JSON).
                get(Response.class);
        
        System.out.println((String) response.readEntity(String.class));
    }

    @Test
    public void testingPOST() {
        ClientConfig config = new ClientConfig();

        Client client = ClientBuilder.newClient(config);

        WebTarget target = client.target(getBaseURI());

        Response response = target.path("basic").
                path("user").
                path("name").
                path("1").
                request(MediaType.APPLICATION_JSON).
                accept(MediaType.APPLICATION_JSON).
                post(Entity.entity(new JSONObject().toString(), MediaType.APPLICATION_JSON));
        
        System.out.println((String) response.readEntity(String.class));
    }

    private static URI getBaseURI() {
        return UriBuilder.fromUri("http://localhost:8088/BasicRESTService/").build();
    }
}
