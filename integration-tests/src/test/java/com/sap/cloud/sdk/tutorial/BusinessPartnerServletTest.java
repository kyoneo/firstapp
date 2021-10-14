package com.sap.cloud.sdk.tutorial;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.vavr.control.Try;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.net.URL;

import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.testutil.MockDestination;
import com.sap.cloud.sdk.testutil.MockUtil;

import static io.restassured.RestAssured.when;

@RunWith(Arquillian.class)
public class BusinessPartnerServletTest {
    private static final MockUtil mockUtil = new MockUtil();
    private static final JsonSchemaValidator jsonValidator_List = JsonSchemaValidator
            .matchesJsonSchemaInClasspath("businesspartners-schema.json");

    private static final String DESTINATION_NAME = "MyErpSystem";
    private static final Destination dummyDestination = DefaultDestination.builder().property("name", DESTINATION_NAME).property("URL", "foo").build();

    @ArquillianResource
    private URL baseUrl;

    @Deployment
    public static WebArchive createDeployment() {
        return TestUtil.createDeployment(BusinessPartnerServlet.class);
    }

    @BeforeClass
    public static void beforeClass() {
        mockUtil.mockDefaults();
    }

    @Before
    public void before() {
        RestAssured.baseURI = baseUrl.toExternalForm();
    }

    @Test
    public void testService() {
        // TODO: insert your service URL down below
        mockUtil.mockDestination(MockDestination.builder(DESTINATION_NAME, URI.create("https://odata-mock-server-terrific-gelada-yr.cfapps.us10.hana.ondemand.com/sap/opu/odata/sap/API_BUSINESS_PARTNER/A_BusinessPartner")).build());
        // HTTP GET response OK, JSON header and valid schema
        when()
                .get("/businesspartners")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(jsonValidator_List);
    }

    @Test
    public void testWithFallback() {
        // Simulate a failed VDM call with non-existent destination
        DestinationAccessor.setLoader((n, o) -> Try.success(dummyDestination));
    
        // Assure an empty list is returned as fallback
        when()
                .get("/businesspartners")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("", Matchers.hasSize(0));
    }
}