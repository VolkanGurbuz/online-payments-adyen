package dev.volkangurbuz.onlinepaymentsadyen.config

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect
import org.springframework.context.annotation.Bean

class Config {

    @Bean
     fun layoutDialect() : LayoutDialect =  LayoutDialect()

}