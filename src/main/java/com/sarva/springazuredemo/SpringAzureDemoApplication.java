package com.sarva.springazuredemo;

import com.microsoft.azure.spring.data.cosmosdb.core.mapping.Document;
import com.microsoft.azure.spring.data.cosmosdb.repository.DocumentDbRepository;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.data.annotation.Id;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@SpringBootApplication
public class SpringAzureDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringAzureDemoApplication.class, args);
	}
}

@RestController
class greetings {
	@GetMapping("/hi/{name}")
	public String sayHi(@PathVariable String name) {
		return "Hi " + name;
	}
}

@Log4j2
@Component
class SqlServerDemo {

	private final JdbcTemplate jdbcTemplate;

	SqlServerDemo(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void sqlServerDemo() {

		List<CarInventory> customerList = this.jdbcTemplate
			.query("select top 10 * from CarInventory",
				(rs, rowNum) -> new CarInventory(rs.getLong("id"), rs.getString("make"), rs.getString("model"), rs.getInt("availableStock")));

		customerList.forEach(log::info);
	}
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class CarInventory {
	private Long id;
	private String make, model;
	private int availableStock;
}

/*
@Log4j2
@Component
class ServiceBusDemo {

	private final ITopicClient topicClient;
	private final ISubscriptionClient subscriptionClient;

	ServiceBusDemo(ITopicClient tc, ISubscriptionClient sc) {
		this.topicClient = tc;
		this.subscriptionClient = sc;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void serviceBusDemo() throws Exception {

		this.topicClient
		.send(new Message("Hey @ " + Instant.now().toString()));

		this.subscriptionClient.registerMessageHandler(new IMessageHandler() {

			@Override
			public CompletableFuture<Void> onMessageAsync(IMessage message) {

				log.info(String.format("new message having body '%s' and id '%s'",
					new String(message.getBody()), message.getMessageId()));

				return CompletableFuture.completedFuture(null);
			}

			@Override
			public void notifyException(Throwable exception, ExceptionPhase phase) {
				log.error("error: ", exception);
			}
		});

	}
}
*/

@Component
@Log4j2
class ObjectStorageServiceDemo {

	private final CloudStorageAccount cloudStorageAccount;
	private final Resource resource;
	private final CloudBlobContainer files;

	ObjectStorageServiceDemo(
		CloudStorageAccount cloudStorageAccount,
		@Value("classpath:/cloud.jpeg") Resource resource) throws URISyntaxException, StorageException {
		this.cloudStorageAccount = cloudStorageAccount;
		this.resource = resource;
		this.files = this.cloudStorageAccount
			.createCloudBlobClient()
			.getContainerReference("files");
	}

	// @EventListener(ApplicationReadyEvent.class)
	public void objectStorageServiceDemo() throws Exception {
		CloudBlockBlob cbb = this.files.getBlockBlobReference("cloud-" + UUID.randomUUID().toString() + ".jpeg");
		cbb.upload(this.resource.getInputStream(), this.resource.contentLength());
		log.info("uploaded the resource to " + cbb.getStorageUri());
	}
}

@Component
@Log4j2
class CarCosmosDbDemo {

	private final CarRepository carRepository;

	CarCosmosDbDemo(CarRepository carRepository) {
		this.carRepository = carRepository;
	}

	// @EventListener(ApplicationReadyEvent.class)
	public void carCosmosDbDemo() throws Exception {
		this.carRepository.deleteAll();

		Stream.of("Honda", "Audi", "Toyota")
			.map(name -> new Car(null, name))
			.map(this.carRepository::save)
			.forEach(log::info);
	}
}

interface CarRepository extends DocumentDbRepository<Car, String> {
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "cars")
class Car {

	@Id
	private String id;
	private String make;
}
