package com.bluewave.apexbank;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;


@SpringBootApplication
@EnableRetry
public class ApexbankApplication {

	public static void main(String[] args) {

        Dotenv dotenv=Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(e->System.setProperty(e.getKey(),e.getValue()));


		SpringApplication.run(ApexbankApplication.class, args);
	}

}
