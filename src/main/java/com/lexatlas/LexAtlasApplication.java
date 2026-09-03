package com.lexatlas;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@MapperScan("com.lexatlas.mapper")
@EnableAsync
public class LexAtlasApplication {

    public static void main(String[] args) {
        SpringApplication.run(LexAtlasApplication.class, args);
    }
}
