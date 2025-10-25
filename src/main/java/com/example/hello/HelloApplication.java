package com.example.hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// JSP + (tomcat(WAS) + Servlet)
// Spring Model(Data) - view(template(Thymeleaf, mustache, Jinja...)) - control -
// 통합된 구조 ---> 소규모 프로젝트, 간단한 웹 어플리케이션 제작시에 사용, 1인 개발
// python : django, flask, fastapi

// Back-End
// Full-stack : BE + FE(react, android mobile)

@SpringBootApplication
public class HelloApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelloApplication.class, args);
	}

}
