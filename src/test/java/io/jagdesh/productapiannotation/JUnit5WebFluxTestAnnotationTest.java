package io.jagdesh.productapiannotation;

import io.jagdesh.productapiannotation.controller.ProductController;
import io.jagdesh.productapiannotation.model.Product;
import io.jagdesh.productapiannotation.model.ProductEvent;
import io.jagdesh.productapiannotation.repository.ProductRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@WebFluxTest(ProductController.class)
public class JUnit5WebFluxTestAnnotationTest {

    @Autowired
    private WebTestClient client;
    private List<Product> expectedList;

    @MockBean
    private ProductRepository repo;

    @MockBean
    private CommandLineRunner clr;

    @BeforeEach
    void beforeEach() {
        this.expectedList = Arrays.asList(
                new Product("1", "Big Latte", 2.99)
        );
    }

    @Test
    void testGetAllProducts() {
        when(repo.findAll())
                .thenReturn(Flux.fromIterable(this.expectedList));
        client.get()
                .uri("/products")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(Product.class)
                .isEqualTo(expectedList);
    }

    @Test
    void testProductInvalidIdNotFound() {
        String id = "aaa";
        when(repo.findById(id))
                .thenReturn(Mono.empty());
        client.get()
                .uri("/products/{id}", id)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void testProductIdFound() {
        Product expectedProduct = expectedList.get(0);
        when(repo.findById(expectedProduct.getId()))
                .thenReturn(Mono.just(expectedProduct));
        client.get()
                .uri("/products/{id}", expectedProduct.getId())
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
                .uri("/products/events")
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
