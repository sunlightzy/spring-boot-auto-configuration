package com.glmapper.spring.demo;

import com.glmapper.spring.demo.autoconfigure.HelloFiveSixAutoconfigure;
import com.glmapper.spring.demo.autoconfigure.HelloOneAutoconfigure;
import com.glmapper.spring.demo.autoconfigure.HelloThreeFourAutoconfigure;
import com.glmapper.spring.demo.autoconfigure.HelloTwoAutoconfigure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * HelloConfiguration
 *
 * @author zhenyu.szy
 * @date 2018-11-27
 */
@Configuration
public class HelloConfiguration {
    @Autowired(required = false)
    HelloOneAutoconfigure helloOneAutoconfigure;
    @Autowired(required = false)
    HelloTwoAutoconfigure helloTwoAutoconfigure;
    @Autowired(required = false)
    HelloThreeFourAutoconfigure helloThreeFourAutoconfigure;
    @Autowired(required = false)
    HelloFiveSixAutoconfigure helloFiveSixAutoconfigure;

    @PostConstruct
    public void print() {
        String yes = "生效";
        String no = "失效";
        System.out.println("helloOneAutoconfigure " + (Objects.isNull(helloOneAutoconfigure) ? no : yes));
        System.out.println("helloTwoAutoconfigure " + (Objects.isNull(helloTwoAutoconfigure) ? no : yes));
        System.out.println("helloThreeFourAutoconfigure " + (Objects.isNull(helloThreeFourAutoconfigure) ? no : yes));
        System.out.println("helloFiveSixAutoconfigure " + (Objects.isNull(helloFiveSixAutoconfigure) ? no : yes));
    }
}
