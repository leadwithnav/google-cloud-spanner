package com.example.spannerbank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry // BEST PRACTICE: Enables Spring Retry for Cloud Spanner lock contention AbortedExceptions
public class SpannerBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpannerBankApplication.class, args);
    }
}
