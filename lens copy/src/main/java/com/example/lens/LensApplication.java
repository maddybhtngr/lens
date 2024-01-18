package com.example.lens;

import nu.pattern.OpenCV;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LensApplication {

    public static void main(String[] args) {
        OpenCV.loadShared();
        SpringApplication.run(LensApplication.class, args);
    }

}
