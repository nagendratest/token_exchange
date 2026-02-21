package com.example.tokencache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TokenCacheApplication {

  public static void main(String[] args) {
    SpringApplication.run(TokenCacheApplication.class, args);
  }
}
