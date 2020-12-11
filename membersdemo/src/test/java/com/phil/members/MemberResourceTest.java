package com.phil.members;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class MemberResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/membersweb/rest/members/hello")
          .then()
             .statusCode(200)
             .body(is("Hello RESTEasy"));
    }

}