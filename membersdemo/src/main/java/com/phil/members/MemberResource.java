package com.phil.members;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.validation.ConstraintViolation;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirementsSet;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.hibernate.exception.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.phil.members.model.Member;
import com.phil.members.service.MemberRegistration;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.Query;

@Path("/membersweb/rest/members")
@Tag(name = "Customer Maintenance", description = "Customer Management")
@SecuritySchemes(
    value = {
        @SecurityScheme(
            securitySchemeName = "apikey",
            type = SecuritySchemeType.APIKEY,
            description = "Authentication required to use the customer API",
            in = SecuritySchemeIn.HEADER,
            apiKeyName = "user-key"
        )
    }
)
public class MemberResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/hello")
    @Operation(
        summary = "Say Hello!",
        operationId = "hello"
    )
    @Tag(
        name = "test",
        description = "Test the API"
        )
    @SecurityRequirement(name = "apikey")
    public String hello() {
        return "Hello RESTEasy";
    }

   private static final Logger log = Logger.getLogger(MemberResource.class);

   @Inject
   MemberRegistration memberRegistration;
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @Operation(
       summary = "list all members",
       operationId = "listAllMembers"
   )
   @Tag(
       name = "list",
       description = "Query Members"
   )
   @SecurityRequirement(name = "apikey")
   public Response listAllMembers() {
       //return memberRegistration.findAllOrderedByName();
       return Response.ok(memberRegistration.findAllOrderedByName()).build();
   }

   @GET
   @Path("/{id:[0-9][0-9]*}")
   @Produces(MediaType.APPLICATION_JSON)
   @Operation(
       summary = "List Member by Member ID",
       operationId = "lookupMemberById"
   )
   @Tag(
    name = "list",
    description = "Query Members"
    )
   @SecurityRequirement(name = "apikey")
   public Member lookupMemberById(@PathParam("id") long id) {
       Member member = memberRegistration.findById(id);
       if (member == null) {
           throw new WebApplicationException(Response.Status.NOT_FOUND);
       }
       
       return member;
   }

   /**
    * Creates a new member from the values provided. Performs validation, and will return a JAX-RS response with either 200 ok,
    * or with a map of fields, and related errors.
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Operation(
       summary = "Create new member",
       operationId = "createMember"
   )
   @SecurityRequirement(name = "apikey")
   public Response createMember(@Valid Member member) {
    
        Response.ResponseBuilder builder = null;

        try {
        memberRegistration.register(member);
        builder = Response.ok(member);


        } catch (ConstraintViolationException ce) {
            // Handle bean validation issues
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("email", "Email taken");
            //builder = createViolationResponse(ce.getConstraintViolations());
            builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
        } catch (ValidationException e) {
            // Handle the unique constrain violation
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("email", "Email taken");
            builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
        } catch (Exception e) {
            // Handle generic exceptions
            Map<String, String> responseObj = new HashMap<>();
            responseObj.put("error", e.getMessage());
            responseObj.put("email", "Email taken");
            builder = Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
        }
    
    
        return builder.build();
       
   }
   
   /**
    * Deletes a new member from the values provided. Performs validation, and will return a JAX-RS response with either 200 ok,
    * or with a member not found error.
    */
   @DELETE
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @Path("/{email}")
   @Operation(
       summary = "Delete a member",
       operationId = "deleteMember"
   )
   @SecurityRequirement(name = "apikey")
   public Response deleteMember(@PathParam("email") String emailAddress) {

       Response.ResponseBuilder builder = null;

       try {
          

        // Check to see if email address exists
           Member member = emailExists(emailAddress);
           if (member == null) {
               throw new ValidationException("Email doesn't exist");
           }
           //
           // delete member from the database
           //
           memberRegistration.unregister(member);
           // Create an "ok" response
           
           Map<String, String> succesfulResponseObject = new HashMap<>();
           succesfulResponseObject.put("Member", "Member Successfully deleted");
           builder = Response.status(Response.Status.OK).entity(succesfulResponseObject);
       } catch (ValidationException e) {
           // Handle the unique constrain violation
           Map<String, String> responseObj = new HashMap<>();
           responseObj.put("email", "Email Address doesn't exist");
           builder = Response.status(Response.Status.CONFLICT).entity(responseObj);
       } catch (Exception e) {
           // Handle generic exceptions
           Map<String, String> responseObj = new HashMap<>();
           responseObj.put("error", e.getMessage());
           builder = Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
       }

       return builder.build();
   }
   
   
   
   
   


   /**
    * Creates a JAX-RS "Bad Request" response including a map of all violation fields, and their message. This can then be used
    * by clients to show violations.
    * 
    * @param violations A set of violations that needs to be reported
    * @return JAX-RS response containing all violations
    */
   private Response.ResponseBuilder createViolationResponse(Set<ConstraintViolation<?>> violations) {
       log.trace("Validation completed. violations found: " + violations.size());

       Map<String, String> responseObj = new HashMap<>();

       for (ConstraintViolation<?> violation : violations) {
           responseObj.put(violation.getPropertyPath().toString(), violation.getMessage());
       }

       return Response.status(Response.Status.BAD_REQUEST).entity(responseObj);
   }

   /**
    * Checks if a member with the same email address is already registered. This is the only way to easily capture the
    * "@UniqueConstraint(columnNames = "email")" constraint from the Member class.
    * 
    * @param email The email to check
    * @return True if the email already exists, and false otherwise
    */
   public boolean emailAlreadyExists(String email) {
       Member member = null;
       try {
           member = memberRegistration.findByEmail(email);
       } catch (NoResultException e) {
           // ignore
       }
       return member != null;
   }
   public Member emailExists(String email) {
       Member member = null;
       try {
           member = memberRegistration.findByEmail(email);
       } catch (NoResultException e) {
           // ignore
       }
       
       // return member for deleting or updating
       return member;
   }
}