package lab.jpa.concert.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lab.jpa.concert.common.Config;
import lab.jpa.concert.domain.Concert;

/**
 * Simple JUnit test to test the behaviour of the Concert Web service.
 * <p>
 * The test is implemented using the JAX-RS client API.
 */
public class ConcertResourceJsonIT {

    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResourceJsonIT.class);

    private static String WEB_SERVICE_URI = "http://localhost:10000/services/concerts";

    private static Client client;

    // List of Concerts.
    private static List<Concert> concerts = new ArrayList<>();

    // List of Concert URIs generated by the Web service. The Concert at
    // position i in concerts has the URI at position i in concertUris.
    private static List<String> concertUris = new ArrayList<>();

    // Set of cookie values returned by the Web service.
    private static Set<String> cookieValues = new HashSet<>();

    @BeforeClass
    public static void createClient() {
        // Use ClientBuilder to create a new client that can be used to create
        // connections to the Web service.
        client = ClientBuilder.newClient();

        // Create some Concerts.
        concerts.add(new Concert(
                "One Night of Queen",
                LocalDateTime.of(2017, 8, 4, 20, 0)));
        concerts.add(new Concert(
                "The Selecter and the Beat",
                LocalDateTime.of(2018, 1, 25, 20, 0)));
        concerts.add(new Concert(
                "Spend the Night with Alice Cooper",
                LocalDateTime.of(2017, 10, 27, 19, 0)));
    }

    @AfterClass
    public static void closeConnection() {
        // After all tests have run, close the client.
        client.close();

        // After running all tests, only one cookie value should have been
        // generated by the Web service, since all requests have been made by
        // the same client.
        assertEquals(1, cookieValues.size());
    }

    @Before
    public void clearAndPopulate() {
        // Delete all Concerts in the Web service.
        Builder builder = client.target(WEB_SERVICE_URI).request();
        addCookieToInvocation(builder);
        Response response = builder.delete();
        processCookieFromResponse(response);
        response.close();

        // Clear Parolee Uris
        concertUris.clear();

        // Populate the Web service with Concerts.
        for (Concert concert : concerts) {
            builder = client.target(WEB_SERVICE_URI).request();
            addCookieToInvocation(builder);
            response = builder.post(Entity.json(concert));
            processCookieFromResponse(response);
            String concertUri = response.getLocation().toString();
            concertUris.add(concertUri);
            response.close();
        }
    }

    @Test
    public void testCreate() {
        Response response = null;

        // Create a new Concert.
        Concert concert = new Concert(
                "Blondie",
                LocalDateTime.of(2017, 4, 26, 20, 0));

        try {
            // Prepare an invocation on the Concert service
            Builder builder = client.target(WEB_SERVICE_URI).request();

            // Add any cookie that's previously been returned by the Web service.
            addCookieToInvocation(builder);

            // Make the service invocation via a HTTP POST message, and wait
            // for the response.
            response = builder.post(Entity.json(concert));

            // Check that the HTTP response code is 201 Created.
            int responseCode = response.getStatus();
            assertEquals(201, responseCode);

            // Check that the Location header has been set.
            URI concertUri = response.getLocation();
            assertNotNull(concertUri);

            // Store any cookie returned in the HTTP response message.
            processCookieFromResponse(response);
        } finally {
            // Close the Response object.
            response.close();
        }
    }

    @Test
    public void testRetrieve() {
        Response response = null;

        try {
            String concertUri = concertUris.get(concertUris.size() - 1);

            // Make an invocation on a Concert URI and specify Java-
            // serialization as the required data format.
            Builder builder = client.target(concertUri).request().accept(MediaType.APPLICATION_JSON);

            // Add any cookie that's previously been returned by the Web service.
            addCookieToInvocation(builder);

            // Make the service invocation via a HTTP GET message, and wait for
            // the response.
            response = builder.get();

            // Check that the HTTP response code is 200 OK.
            int responseCode = response.getStatus();
            assertEquals(200, responseCode);

            // Check that the expected Concert is returned.
            Concert concert = response.readEntity(Concert.class);
            assertEquals(concerts.get(concerts.size() - 1).getTitle(), concert.getTitle());

            // Store any cookie returned in the HTTP response message.
            processCookieFromResponse(response);
        } finally {
            // Close the Response object.
            response.close();
        }
    }

    @Test
    public void testRetrieveWithRange() {
        Response response = null;

        try {
            // Prepare an invocation on a Concert URI and specify Java-
            // serialization as the required data format. Specify values for
            // query parameters start (2) and size (10).
            Builder builder = client
                    .target(WEB_SERVICE_URI + "?start=2&size=10")
                    .request()
                    .accept(MediaType.APPLICATION_JSON);

            // Add any cookie that's previously been returned by the Web
            // service.
            addCookieToInvocation(builder);

            // Make the service invocation via a HTTP GET message, and wait for
            // the response.
            response = builder.get();

            // Check that 2 Concerts were returned.
            ArrayList<Concert> concerts = response.readEntity(new GenericType<ArrayList<Concert>>() {
            });
            assertEquals(2, concerts.size());

            // Store any cookie returned in the HTTP response message.
            processCookieFromResponse(response);
        } finally {
            // Close the Response object.
            response.close();
        }
    }

    @Test
    public void testDelete() {
        Response response = null;

        try {
            // Prepare an invocation on the Concert Web service.
            Builder builder = client.target(WEB_SERVICE_URI).request();

            // Add any cookie that's previously been returned by the Web
            // service.
            addCookieToInvocation(builder);

            // Make the service invocation via a HTTP DELETE message, and wait
            // for the response.
            response = builder.delete();

            // Check that the HTTP response code is 204 No content.
            int status = response.getStatus();
            assertEquals(204, status);

            // Store any cookie returned in the HTTP response message.
            processCookieFromResponse(response);
        } finally {
            // Close the Response object.
            response.close();
        }
    }

    // Method to add any cookie previously returned from the Web service to an
    // Invocation.Builder instance.
    private void addCookieToInvocation(Builder builder) {
        if (!cookieValues.isEmpty()) {
            builder.cookie(Config.CLIENT_COOKIE, cookieValues.iterator().next());
        }
    }

    // Method to extract any cookie from a Response object received from the
    // Web service. If there is a cookie named clientId (Config.CLIENT_COOKIE)
    // it is added to the cookieValues set, which stores all cookie values for
    // clientId received by the Web service.
    private void processCookieFromResponse(Response response) {
        Map<String, NewCookie> cookies = response.getCookies();

        if (cookies.containsKey(Config.CLIENT_COOKIE)) {
            String cookieValue = cookies.get(Config.CLIENT_COOKIE).getValue();
            cookieValues.add(cookieValue);
        }
    }
}