package com.example.orders;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;

import java.util.List;
import java.util.function.Consumer;

import static au.com.dius.pact.consumer.dsl.LambdaDsl.newJsonBody;
import static com.example.orders.Fixtures.*;
import static org.mockito.BDDMockito.then;

@PactTestFor(pactVersion = PactSpecVersion.V3, providerType = ProviderType.ASYNCH)
@ExtendWith(PactConsumerTestExt.class)
@Import(TestChannelBinderConfiguration.class)
@SpringBootTest
class CheckoutStartedHandlerTests {

    @Autowired
    private InputDestination input;

    @Autowired
    private OutputDestination output;

    @MockBean
    private Consumer<CheckoutStarted> checkoutStartedHandler;

    @Pact(consumer = "orders", provider = "cart")
    MessagePact checkoutStarted(MessagePactBuilder builder) {
        return builder
            .given("a non-empty cart with id 6e61fced-2bbd-431c-8d92-9e9052ffe8ff is checked out")
            .expectsToReceive("a checkout started event")
            .withContent(
                newJsonBody(body -> {
                    body.eachLike("items", item -> {
                       item.uuid("productId", Products.shirt);
                    });
                }).build()
            )
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "checkoutStarted")
    void receiveCheckoutStarted(List<Message> messages) {
        for (var message : messages) {
            input.send(MessageBuilder
                .withPayload(message.getContents().valueAsString())
                .build()
            );
            then(checkoutStartedHandler).should().accept(
                new CheckoutStarted(
                    List.of(new CheckoutStarted.Item(Products.shirt))
                )
            );
        }
    }
}