package com.campus.courier;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campus.courier.mapper")
public class CourierApplication {
    public static void main(String[] args) {
        SpringApplication.run(CourierApplication.class, args);
    }
}
