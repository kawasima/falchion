package net.unit8.falchion.loadtest;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class TodoSimulation extends Simulation {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:3000");

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(BASE_URL)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    Iterator<Map<String, Object>> titleFeeder = Stream.generate(
            (Supplier<Map<String, Object>>) () -> Map.of("title", "Todo-" + UUID.randomUUID())
    ).iterator();

    // Create a todo and save its id
    ChainBuilder create = feed(titleFeeder)
            .exec(
                    http("Create Todo")
                            .post("/todos")
                            .body(StringBody("{\"title\": \"#{title}\", \"completed\": false}"))
                            .check(status().is(201))
                            .check(jsonPath("$.id").saveAs("todoId"))
            );

    // List all todos
    ChainBuilder list = exec(
            http("List Todos")
                    .get("/todos")
                    .check(status().is(200))
    );

    // Get a single todo (404 is expected when SO_REUSEPORT routes to a different process)
    ChainBuilder get = exec(
            http("Get Todo")
                    .get("/todos/#{todoId}")
                    .check(status().in(200, 404))
    );

    // Update a todo (404 is expected when SO_REUSEPORT routes to a different process)
    ChainBuilder update = exec(
            http("Update Todo")
                    .put("/todos/#{todoId}")
                    .body(StringBody("{\"title\": \"#{title}-updated\", \"completed\": true}"))
                    .check(status().in(200, 404))
    );

    // Delete a todo (404 is expected when SO_REUSEPORT routes to a different process)
    ChainBuilder delete = exec(
            http("Delete Todo")
                    .delete("/todos/#{todoId}")
                    .check(status().in(204, 404))
    );

    // CRUD scenario: create -> get -> list -> update -> delete
    ScenarioBuilder crudScenario = scenario("Todo CRUD")
            .exec(create)
            .pause(1)
            .exec(get)
            .pause(1)
            .exec(list)
            .pause(1)
            .exec(update)
            .pause(1)
            .exec(delete);

    // Read-heavy scenario: create a few, then repeatedly list and get
    ScenarioBuilder readHeavyScenario = scenario("Read Heavy")
            .exec(create)
            .pause(1)
            .repeat(10).on(
                    exec(list)
                            .pause(1)
                            .exec(get)
                            .pause(1)
            );

    {
        setUp(
                crudScenario.injectOpen(
                        rampUsersPerSec(1).to(20).during(60),
                        constantUsersPerSec(20).during(120)
                ),
                readHeavyScenario.injectOpen(
                        rampUsersPerSec(1).to(10).during(60),
                        constantUsersPerSec(10).during(120)
                )
        ).protocols(httpProtocol);
    }
}
