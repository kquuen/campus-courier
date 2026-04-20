package com.campus.courier;

import com.campus.courier.config.CampusIdentityProperties;
import com.campus.courier.config.UploadProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.campus.courier.mapper")
@EnableConfigurationProperties({CampusIdentityProperties.class, UploadProperties.class})
public class CourierApplication {
    public static void main(String[] args) {
        SpringApplication.run(CourierApplication.class, args);
    }
}
