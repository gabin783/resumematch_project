package resumematch_demo.resumematch_demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("http://localhost:*") // 포트 번호가 바뀌어도 유연하게 대응
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}