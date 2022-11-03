package io.jagdesh.productapiannotation;

import io.jagdesh.productapiannotation.controller.ProductController;
import io.jagdesh.productapiannotation.model.Product;
import io.jagdesh.productapiannotation.model.ProductEvent;
import io.jagdesh.productapiannotation.repository.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.util.List;

@SpringBootTest
public class JUnit5ControllerTest {

    private WebTestClient client;
    private List<Product> expectedList;

    @Autowired
    private ProductRepository repo;

    @BeforeEach
    void beforeEach() {
        this.client = WebTestClient
                .bindToController(new ProductController(repo))
                .configureClient()
                .baseUrl("/products")
                .build();
        this.expectedList = repo.findAll()
                .collectList()
                .block();
    }

    @Test
    void testGetAllProducts() {
        client.get()
                .uri("/")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Product.class)
                .isEqualTo(expectedList);
    }

    @Test
    void testProductInvalidIdNotFound() {
        client.get()
                .uri("/aaa")
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void testProductIdFound() {
        Product expectedProduct = expectedList.get(0);
        client.get()
                .uri("/{id}", expectedProduct.getId())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(Product.class)
                .isEqualTo(expectedProduct);
    }

    @Test
    void testProductEvents() {
        ProductEvent expectedEvent = new ProductEvent(0L, "Product Event");
        FluxExchangeResult<ProductEvent> result = client.get()
                .uri("/events")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(ProductEvent.class);

        StepVerifier.create(result.getResponseBody())
                .expectNext(expectedEvent)
                .expectNextCount(2)
                .consumeNextWith(event ->
                        Assertions.assertEquals(Long.valueOf(3), event.getEventId())
                )
                .thenCancel()
                .verify();
    }

}
