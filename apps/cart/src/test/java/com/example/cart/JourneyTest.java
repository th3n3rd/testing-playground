package com.example.cart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.cart.Fixtures.Personas.bob;
import static com.example.cart.Fixtures.Products;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Import(ECommerceMessages.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class JourneyTest {

    @Autowired
    private ECommerceMessages eCommerceMessages;

    @MockBean
    private CatalogueService catalogueService;

    @Autowired
    private TestRestTemplate restClient;

    @BeforeEach
    void setUp() {
        eCommerceMessages.clear();
        givenProductsExist(
            Products.shirt,
            Products.trousers
        );
    }

    @Test
    void typicalJourney() {
        var cart = createCart();

        var shirt = selectItem(Products.shirt);
        addItemToCart(cart, shirt);

        assertItemsInCart(cart, List.of(shirt));

        var trousers = selectItem(Products.trousers);
        addItemToCart(cart, trousers);

        assertItemsInCart(cart, List.of(shirt, trousers));

        checkoutCart(cart, checkoutDetails(bob.getFirstName(), bob.getLastName(), bob.getPostalAddress()));

        assertCheckoutStarted(
            cart,
            bob.getFirstName(),
            bob.getLastName(),
            bob.getPostalAddress(),
            List.of(shirt, trousers)
        );
    }

    private void assertCheckoutStarted(
        CartResponse cart,
        String firstName,
        String lastName,
        PostalAddress postalAddress,
        List<AddItemToCartRequest> expectedItems
    ) {
        var message = eCommerceMessages.takeMessage(CheckoutStarted.class);

        var itemsCheckedOut = message
            .getItems()
            .stream()
            .map(CheckoutStarted.Item::getProductId)
            .collect(Collectors.toList());

        assertThat(message.getCartId()).isEqualTo(cart.getId());
        assertThat(message.getFirstName()).isEqualTo(firstName);
        assertThat(message.getLastName()).isEqualTo(lastName);
        assertThat(message.getPostalAddress()).isEqualTo(postalAddress);

        assertThat(itemsCheckedOut).containsAll(
            expectedItems
                .stream()
                .map(AddItemToCartRequest::getProductId)
                .collect(Collectors.toList())
        );
    }

    private CartResponse checkoutCart(CartResponse cart, CheckoutRequest checkout) {
        return restClient.postForObject(
            "/carts/{cartId}/checkout",
            checkout,
            CartResponse.class,
            cart.getId()
        );
    }

    private CartResponse createCart() {
        return restClient.postForObject("/carts", null, CartResponse.class);
    }

    private CartResponse getCart(UUID cartId) {
        return restClient.getForObject("/carts/{cartId}", CartResponse.class, cartId);
    }

    private CartResponse addItemToCart(CartResponse cart, AddItemToCartRequest itemToAdd) {
        return restClient.postForObject(
            "/carts/{cartId}/items",
            itemToAdd,
            CartResponse.class,
            cart.getId()
        );
    }

    private void assertItemsInCart(CartResponse cart, List<AddItemToCartRequest> expectedItems) {
        assertThat(getCart(cart.getId()).getItems())
            .containsAll(expectedItems
                .stream()
                .map(AddItemToCartRequest::getProductId)
                .collect(Collectors.toList())
            );
    }

    private AddItemToCartRequest selectItem(UUID productId) {
        return new AddItemToCartRequest(productId);
    }

    private void givenProductsExist(UUID... products) {
        for (UUID productId : products) {
            given(catalogueService.productExists(productId)).willReturn(true);
        }
    }

    private CheckoutRequest checkoutDetails(
        String firstName,
        String lastName,
        PostalAddress postalAddress
    ) {
        return new CheckoutRequest(
            firstName,
            lastName,
            postalAddress
        );
    }
}
