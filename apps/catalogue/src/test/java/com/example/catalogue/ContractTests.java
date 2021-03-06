package com.example.catalogue;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Objects;
import java.util.UUID;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@ActiveProfiles("pact")
@IgnoreNoPactsToVerify
@PactBroker
@Provider("catalogue")
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ContractTests {

    @LocalServerPort
    private int serverPort;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp(PactVerificationContext context) {
        if (Objects.nonNull(context)) { // this is needed as the context is null when there are no pacts to verify
            context.setTarget(new HttpTestTarget("localhost", serverPort));
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationSpringProvider.class)
    void verifyContracts(PactVerificationContext context) {
        if (Objects.nonNull(context)) { // this is needed as the context is null when there are no pacts to verify
            context.verifyInteraction();
        }
    }

    @State("a product with id 7f7b6b14-4034-429f-a286-e3946b135179 exists")
    void givenProductExist() {
        productRepository.save(new Product(UUID.fromString("7f7b6b14-4034-429f-a286-e3946b135179")));
    }

    @State("a product with id fc19f260-fcce-4808-b8b2-470d06b49987 does not exists")
    void givenProductDoesNotExist() {}
}


