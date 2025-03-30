package com.adorsys.webank;


import com.adorsys.webank.service.OtpServiceApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
@EnableRepository
@EnablePrsServiceimpl
@SpringBootApplication
@ComponentScan(basePackages = "com.adorsys.webank" , basePackageClasses = OtpServiceApi.class)
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}