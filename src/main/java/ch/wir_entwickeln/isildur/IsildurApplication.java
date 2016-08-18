package ch.wir_entwickeln.isildur;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

@SpringBootApplication
@IntegrationComponentScan
public class IsildurApplication {
//	@Autowired
//	private ApplicationContext applicationContext;

	@Autowired
	private VardaHubotMQTTBridgeOutboundGateway vardaHubotGateway;

	public static void main(String[] args) {
		ConfigurableApplicationContext applicationContext = SpringApplication.run(IsildurApplication.class, args);

//		System.out.println("After run()");

//		VardaHubotMQTTBridgeOutboundGateway vardaHubotGateway = applicationContext.getBean(VardaHubotMQTTBridgeOutboundGateway.class);
//		vardaHubotGateway.send("Hello world!");

//		System.out.println("After send()");
	}

	@Bean
	public MessageProducer yavannaInbound() {
		MqttPahoMessageDrivenChannelAdapter yavannaEventBusAdapter =
				new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:11883", "isildur", "openhab/m2s/update/#"); //TODO Rename topic to "yavanna/status"?
		yavannaEventBusAdapter.setCompletionTimeout(5000);
		yavannaEventBusAdapter.setConverter(new DefaultPahoMessageConverter());
		yavannaEventBusAdapter.setQos(1);
		yavannaEventBusAdapter.setOutputChannel(yavannaInboundChannel());
		return yavannaEventBusAdapter;
	}

	@Bean
	public MessageChannel yavannaInboundChannel() {
		return new DirectChannel();
	}

	@Bean
	public MqttPahoClientFactory vardaHubotMQTTBridgeClientFactory() {
		DefaultMqttPahoClientFactory vardaHubotMQTTBridgeClientFactory = new DefaultMqttPahoClientFactory();
		vardaHubotMQTTBridgeClientFactory.setServerURIs("tcp://m21.cloudmqtt.com:17429", "tcp://host2:1883");
		vardaHubotMQTTBridgeClientFactory.setUserName("isildur");
		vardaHubotMQTTBridgeClientFactory.setPassword("isildur");
		return vardaHubotMQTTBridgeClientFactory;
	}

	@Bean
	@ServiceActivator(inputChannel = "vardaHubotMQTTBridgeOutboundChannel")
	public MessageHandler vardaHubotMQTTBridgeOutboundHandler() {
		MqttPahoMessageHandler messageHandler =
				new MqttPahoMessageHandler("isildur", vardaHubotMQTTBridgeClientFactory());
		messageHandler.setAsync(true);
		messageHandler.setDefaultTopic("hubot-outbound");
		return messageHandler;
	}

	@Bean
	public MessageChannel vardaHubotMQTTBridgeOutboundChannel() {
		return new DirectChannel();
	}







	@Bean
	@ServiceActivator(inputChannel = "yavannaInboundChannel")
	public MessageHandler yavannaInboundHandler() {
		return new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				String topic = (String)message.getHeaders().get("mqtt_topic");
				if (Strings.isNullOrEmpty(topic)) {
					return;
				}
				String itemName = topic.substring(topic.lastIndexOf("/") + 1);
				String updatedValue = (String)message.getPayload();

				System.out.println(String.format("Item: %s, Value: %s", itemName, updatedValue));

				if ("helligkeitWGWest".equals(itemName)) {
					vardaHubotGateway.send(String.format("Die Helligkeit beträgt jetzt %s.", updatedValue));
				}
			}
		};
	}

	@MessagingGateway(defaultRequestChannel = "vardaHubotMQTTBridgeOutboundChannel")
	public interface VardaHubotMQTTBridgeOutboundGateway {
		void send(String data);
	}
}
