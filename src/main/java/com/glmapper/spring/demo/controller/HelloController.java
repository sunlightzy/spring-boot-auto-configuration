package com.glmapper.spring.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HelloController
 *
 * @author zhenyu.szy
 * @date 2018-11-27
 */
@RestController
@RequestMapping("/api/hello")
public class HelloController {

    @GetMapping("/say-hello")
    public String sayHello(String name) {
        return "hello, " + name;
    }
}
