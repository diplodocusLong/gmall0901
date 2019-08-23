package com.lianglong.gmall.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
@Configuration
public class WebMvcConfiguration extends WebMvcConfigurerAdapter {
    @Autowired
    AuthInterceptor authInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
          registry.addInterceptor(authInterceptor).addPathPatterns("/**");

          super.addInterceptors(registry);

    }
    //定制嵌入式servelet容器配置
    public EmbeddedServletContainerCustomizer emb(){

      return new EmbeddedServletContainerCustomizer() {
          @Override
          public void customize(ConfigurableEmbeddedServletContainer container) {
             // container.setPort(9898);
          }
      };
    }



}
