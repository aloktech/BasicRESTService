package com.mycompany.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONObject;

/**
 *
 * @author Alok
 */
@Path("user")
public class RESTBasicService {
    
    @GET
    @Path("name")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getName() {
        JSONObject status = new JSONObject();
        status.put("status", "OK");
        
        System.out.println("GET : "+status.toString());
        
        return Response.status(Response.Status.OK).entity(status.toString()).build();
    }
    
    @POST
    @Path("name/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setName(@PathParam("id") Long id, String data) {
        JSONObject status = new JSONObject(data);
        status.put("status", "OK");
        
        System.out.println("POST : "+status.toString());
        
        return Response.status(Response.Status.CREATED).entity(status.toString()).build();
    }
}
