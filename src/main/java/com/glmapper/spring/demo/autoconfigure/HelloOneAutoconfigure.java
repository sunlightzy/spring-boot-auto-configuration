package com.glmapper.spring.demo.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * HelloOneAutoconfigure
 * com.glmapper.hello.one: true     //正常
 * com.glmapper.hello.one:          //正常，空字符时
 * com.glmapper.hello.one: false    //失败
 * com.glmapper.hello.one: null     //正常
 * com.glmapper.hello.one: 30       //正常
 * 只要com.glmapper.hello.one 配置项存在,且不为false , 配置类都生效
 *
 * @author zhenyu.szy
 * @date 2018-11-27
 */
@Configuration
@ConditionalOnProperty(value = "com.glmapper.hello.one")
public class HelloOneAutoconfigure {

}
