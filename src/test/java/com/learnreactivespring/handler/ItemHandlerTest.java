package com.learnreactivespring.handler;

import com.learnreactivespring.constants.ItemConstants;
import com.learnreactivespring.document.Item;
import com.learnreactivespring.repository.ItemReactiveRepository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertNotNull;

@SpringBootTest
@RunWith(SpringRunner.class)
@DirtiesContext
@AutoConfigureWebTestClient
@ActiveProfiles("test")
public class ItemHandlerTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ItemReactiveRepository itemReactiveRepository;

    private List<Item> data() {
        return Arrays.asList(new Item(null, "Samsung TV", 399.99),
                new Item(null, "LG TV", 329.99),
                new Item(null, "Apple Watch", 349.99),
                new Item("ABC", "Beats HeadPhones", 19.99));
    }

    @Before
    public void setUp() {
        itemReactiveRepository.deleteAll()
                .thenMany(Flux.fromIterable(data()))
                .flatMap(itemReactiveRepository::save)
                .doOnNext(item -> System.out.println("Inserted item is: " + item))
                .blockLast();
    }

    @Test
    public void getAllItems() {
        webTestClient.get().uri(ItemConstants.ITEM_FUNCTIONAL_ENDPOINT_V1)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBodyList(Item.class)
                .hasSize(4);
    }

    @Test
    public void getAllItems_approach2() {
        webTestClient.get().uri(ItemConstants.ITEM_FUNCTIONAL_ENDPOINT_V1)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .expectBodyList(Item.class)
                .hasSize(4)
                .consumeWith(response -> {
                    List<Item> items = response.getResponseBody();
                    items.forEach(item -> assertNotNull(item.getId()));
                });
    }

    @Test
    public void getAllItems_approach3() {
        Flux<Item> itemFlux = webTestClient.get().uri(ItemConstants.ITEM_FUNCTIONAL_ENDPOINT_V1)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON_UTF8)
                .returnResult(Item.class)
                .getResponseBody();

        StepVerifier.create(itemFlux)
                .expectSubscription()
                .expectNextCount(4L)
                .verifyComplete();
    }

    @Test
    public void getOneItem() {
        webTestClient.get().uri(ItemConstants.ITEM_FUNCTIONAL_ENDPOINT_V1.concat("/{id}"), "ABC")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.price", 149.99);

    }

    @Test
    public void getOneItem_notFound() {
        webTestClient.get().uri(ItemConstants.ITEM_FUNCTIONAL_ENDPOINT_V1.concat("/{id}"), "DEF")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.price", 149.99);
    }

    @Test
    public void createItem() {
        Item item = Item.builder()
                .id(null)
                .description("Bose Speaker")
                .price(159.99)
                .build();

        webTestClient.post().uri(ItemConstants.ITEM_FUNCTIONAL_ENDPOINT_V1)
                .body(Mono.just(item), Item.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.description").isEqualTo(item.getDescription())
                .jsonPath("$.price").isEqualTo(item.getPrice());
    }

    @Test
    public void deleteItem() {
        String idToDelete = "ABC";

        webTestClient.delete().uri(ItemConstants.ITEM_FUNCTIONAL_ENDPOINT_V1.concat("/{id}"), idToDelete)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Void.class);

    }

    @Test
    public void updateItem() {
        double newPrice = 10.99;
        Item updatedItem = Item.builder()
                .id(null)
                .description("Beats HeadPhones")
                .price(newPrice)
                .build();

        webTestClient.put().uri(ItemConstants.ITEM_FUNCTIONAL_ENDPOINT_V1.concat("/{id}"), "ABC")
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updatedItem), Item.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo("ABC")
                .jsonPath("$.description").isEqualTo(updatedItem.getDescription())
                .jsonPath("$.price").isEqualTo(updatedItem.getPrice());
    }

    @Test
    public void updateItem_notFound() {
        double newPrice = 10.99;
        Item updatedItem = Item.builder()
                .id(null)
                .description("Beats HeadPhones")
                .price(newPrice)
                .build();

        webTestClient.put().uri(ItemConstants.ITEM_FUNCTIONAL_ENDPOINT_V1.concat("/{id}"), "DEF")
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(updatedItem), Item.class)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    public void runtimeException() {
        webTestClient.get().uri("/fun/runtimeException")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.message", "RuntimeException Occurred");
    }
}
