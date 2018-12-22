package com.glmapper.spring.demo.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * HelloTwoAutoconfigure
 * 如果 com.glmapper.helloTwo 配置存在, 而且值为2时,该配置类生效,
 * 如果该配置不存在 ,该配置类生效
 *
 * @author J.Queue
 * @date 2018-11-27
 */
@Configuration
@ConditionalOnProperty(value = "com.glmapper.hello.two", havingValue = "2", matchIfMissing = true)
public class HelloTwoAutoconfigure {

}
