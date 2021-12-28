package com.example.nonblockingcaller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LoadTest {

    static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {

        ExecutorService es = Executors.newFixedThreadPool(100);

        String loadTestUrl = "http://localhost:8080/rest?idx={idx}";

        StopWatch main = new StopWatch();
        main.start();

        WebClient client = WebClient.create();

        List<CompletableFuture<String>> completableFutures = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
                int idx = counter.addAndGet(1);
                log.info("---------------> Thread : {}", idx);
                StopWatch sw = new StopWatch();
                sw.start();
                String block = client.get()
                        .uri(loadTestUrl, idx).exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class))
                        .block();
                sw.stop();
                log.info("■■■■■■■■■■■■■■■■■■■■■■■■■■ 단위 Elased Time idx : {} - {} 초", idx, sw.getTotalTimeSeconds());
                return block;
            }, es);
            completableFutures.add(completableFuture);
        }

        for (CompletableFuture cf : completableFutures) {
            log.info("-------------------> 결과 : {}", cf.join() );
        }

        es.shutdown();
        es.awaitTermination(100, TimeUnit.SECONDS); // 지정된 시간까지 기다리고 지정된 시간이 넘어서면 종료
        main.stop();
        log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% Total : {}", main.getTotalTimeSeconds());

    }
}
