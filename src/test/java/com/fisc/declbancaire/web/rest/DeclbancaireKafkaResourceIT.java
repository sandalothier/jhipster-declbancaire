package com.fisc.declbancaire.web.rest;

import com.fisc.declbancaire.DeclbancaireApp;
import com.fisc.declbancaire.service.DeclbancaireKafkaProducer;
import com.fisc.declbancaire.service.DeclbancaireKafkaConsumer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.testcontainers.containers.KafkaContainer;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DeclbancaireApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class DeclbancaireKafkaResourceIT {

    private MockMvc restMockMvc;

    private static boolean started = false;

    private static KafkaContainer kafkaContainer;

    @Autowired
    private DeclbancaireKafkaProducer producer;

    @Autowired
    private DeclbancaireKafkaConsumer consumer;

    private static final int MAX_ATTEMPT = 5;

    @BeforeAll
    public static void startServer() {
        if (!started) {
            startTestcontainer();
            started = true;
        }
    }

    private static void startTestcontainer() {
        kafkaContainer = new KafkaContainer("5.3.1");
        kafkaContainer.start();
        System.setProperty("kafkaBootstrapServers", kafkaContainer.getBootstrapServers());
    }

    @BeforeEach
    public void setup() {
        DeclbancaireKafkaResource kafkaResource = new DeclbancaireKafkaResource(producer);

        this.restMockMvc = MockMvcBuilders.standaloneSetup(kafkaResource)
            .build();

        producer.init();
        consumer.start();
    }

    @Test
    public void producedMessageHasBeenConsumed() throws Exception {
        restMockMvc.perform(post("/api/declbancaire-kafka/publish?message=test"))
            .andExpect(status().isOk());

        Map<MetricName, ? extends Metric> metrics = consumer.getKafkaConsumer().metrics();

        Metric recordsConsumedTotalMetric = metrics.entrySet().stream()
            .filter(entry -> "records-consumed-total".equals(entry.getKey().name()))
            .findFirst()
            .get()
            .getValue();

        Double expectedTotalConsumedMessage = 1.0;
        Double totalConsumedMessage;
        int attempt = 0;
        do {
            totalConsumedMessage = (Double) recordsConsumedTotalMetric.metricValue();
            Thread.sleep(200);
        } while (!totalConsumedMessage.equals(expectedTotalConsumedMessage) && attempt++ < MAX_ATTEMPT);

        Assertions.assertThat(attempt).isLessThan(MAX_ATTEMPT);
        Assertions.assertThat(totalConsumedMessage).isEqualTo(expectedTotalConsumedMessage);
    }

}

