package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter(){

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 允许跨域访问的域名。*：代表所有域名，使用*不能携带cookie
        corsConfiguration.addAllowedOrigin("http://manager.gmall.com");
        corsConfiguration.addAllowedOrigin("http://gmall.com");
        corsConfiguration.addAllowedOrigin("http://www.gmall.com");
        // 是否允许携带cookie。如果允许携带cookie：origin不能为*，并且该参数必须是true
        corsConfiguration.setAllowCredentials(true);
        // 允许携带所有的头信息
        corsConfiguration.addAllowedHeader("*");
        // 允许所有请求方式跨域访问
        corsConfiguration.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(configurationSource);
    }
}
