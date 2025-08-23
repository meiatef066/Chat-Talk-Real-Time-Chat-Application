package com.system.chattalk_serverside.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dvghbsyda",
                "api_key", "647485999596941",
                "api_secret", "kJ-f_CbgOmNvA8yhh35GiI27beU",
                "secure", true
        ));
    }
}
