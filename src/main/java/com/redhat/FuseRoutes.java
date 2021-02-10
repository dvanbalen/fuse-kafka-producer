package com.redhat;

import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class FuseRoutes extends RouteBuilder {
	
	@Override
	public void configure() throws Exception {
		restConfiguration().component("servlet").bindingMode(RestBindingMode.json);
		
		// "passthrough" kafka producer. Takes messages from static XML app 
		// and forwards them directly to kafka, using the topic indicated by the resourceType
		
		from("timer://fuse-kafka-producer?period={{message.interval}}").to("{{xmlAppUrl}}").unmarshal().jacksonxml().process(new Processor() {
			@Override
			public void process(Exchange exchange) throws Exception {
				Message message = exchange.getIn();
				
				message.setBody(message.getBody(Map.class).get("result"));
				message.setHeader(KafkaConstants.PARTITION_KEY, 0);
				message.setHeader(KafkaConstants.KEY, "Camel");
				
				log.info("###DVB EDIT to logs!!! Sending the following message to Kafka topic: {}", message.getBody(String.class));
			}
		}).recipientList(simple("kafka:${sysenv.KAFKA_BACKEND_TOPIC}?sslTruststoreLocation={{spring.kafka.properties.ssl.truststore.location}}&" 
	            + "sslTruststorePassword={{spring.kafka.properties.ssl.truststore.password}}&"
				+ "securityProtocol={{spring.kafka.properties.security.protocol}}&"
				+ "brokers=${sysenv.SPRING_KAFKA_BOOTSTRAP_SERVERS}")).setBody(constant("Message sent successfully."));
	}
}
