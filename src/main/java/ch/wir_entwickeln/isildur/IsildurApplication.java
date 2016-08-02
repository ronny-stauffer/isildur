package ch.wir_entwickeln.isildur;

import com.google.common.base.Strings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class IsildurApplication {
	public static void main(String[] args) {
		SpringApplication.run(IsildurApplication.class, args);
	}

	@Bean
	public MessageChannel mqttInputChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageProducer inbound() {
		MqttPahoMessageDrivenChannelAdapter adapter =
				new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:11883", "isildur", "openhab/m2s/update/#");
		adapter.setCompletionTimeout(5000);
		adapter.setConverter(new DefaultPahoMessageConverter());
		adapter.setQos(1);
		adapter.setOutputChannel(mqttInputChannel());
		return adapter;
	}

	@Bean
	@ServiceActivator(inputChannel = "mqttInputChannel")
	public MessageHandler handler() {
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
			}
		};
	}
}
