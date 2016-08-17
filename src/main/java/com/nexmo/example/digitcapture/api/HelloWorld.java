package com.nexmo.example.digitcapture.api;


import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/hello")
public class HelloWorld {
    @GET
    public Response helloWorld(@QueryParam("name") String name) {
        String message = "Hello";

        if (name == null)
            message += " World";
        else
            message += " " + name;
        return Response.ok(message, MediaType.TEXT_PLAIN_TYPE).build();
    }
}
