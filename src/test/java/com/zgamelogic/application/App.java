package com.zgamelogic.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.zgamelogic.controllers"
})
public class App {
    public static void main(String[] args){
        SpringApplication.run(App.class, args);
    }
}
