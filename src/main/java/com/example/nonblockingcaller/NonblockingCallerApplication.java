package com.example.nonblockingcaller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;

@SpringBootApplication
@Slf4j
@EnableAsync
public class NonblockingCallerApplication {

	@RestController
	public static class MyController {

		static final String URL1 = "http://localhost:8081/service1?req={req}";
		static final String URL2 = "http://localhost:8081/service2?req={req}";

		@Autowired
		MyService myService;

		WebClient client = WebClient.create();

		@GetMapping("/test")
		public String test() {
			return "Test";
		}

		@GetMapping("/rest")
		public Mono<String> rest(int idx) {

			Mono<String> stringMono = client.get().uri(URL1, idx)
					.exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class))
					.doOnNext(s -> log.info("--------------------------> URL1 결과 로그 {}",  s.toString()))
					.flatMap(result1 -> client.get().uri(URL2, result1).exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class)))
					.doOnNext(s -> log.info("--------------------------> URL2 결과 로그 {}",  s.toString()))
					.flatMap(result2 -> Mono.fromCompletionStage(this.myService.work(result2)))
					;

			return stringMono;
		}

	}

	@Service
	public static class MyService {
		@Async
		public CompletableFuture<String> work(String req) {
			return CompletableFuture.completedFuture(req + " / asyncWork");
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(NonblockingCallerApplication.class, args);
	}

}
