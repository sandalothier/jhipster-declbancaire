package com.fisc.declbancaire.web.rest;

import com.fisc.declbancaire.service.DeclbancaireKafkaProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/declbancaire-kafka")
public class DeclbancaireKafkaResource {

    private final Logger log = LoggerFactory.getLogger(DeclbancaireKafkaResource.class);

    private DeclbancaireKafkaProducer kafkaProducer;

    public DeclbancaireKafkaResource(DeclbancaireKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("/publish")
    public void sendMessageToKafkaTopic(@RequestParam("message") String message) {
        log.debug("REST request to send to Kafka topic the message : {}", message);
        this.kafkaProducer.send(message);
    }
}
