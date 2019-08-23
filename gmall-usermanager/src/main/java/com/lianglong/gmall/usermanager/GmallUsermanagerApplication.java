package com.lianglong.gmall.usermanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;
@MapperScan("com.lianglong.gmall.usermanager.mapper")
@ComponentScan("com.lianglong.gmall")
@SpringBootApplication
public class GmallUsermanagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GmallUsermanagerApplication.class, args);
	}

}

