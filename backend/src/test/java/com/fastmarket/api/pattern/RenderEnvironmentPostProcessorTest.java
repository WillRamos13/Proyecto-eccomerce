package com.fastmarket.api.pattern;

import com.fastmarket.api.config.RenderEnvironmentPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderEnvironmentPostProcessorTest {

    @Test
    void derivaUrlsHttpsDelHostnameDeRender() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("RENDER_EXTERNAL_HOSTNAME", "fastmarket-demo.onrender.com");

        new RenderEnvironmentPostProcessor().postProcessEnvironment(
                environment,
                new SpringApplication(Object.class)
        );

        assertEquals("https://fastmarket-demo.onrender.com", environment.getProperty("app.frontend.base-url"));
        assertEquals("https://fastmarket-demo.onrender.com", environment.getProperty("app.backend.public-url"));
        assertEquals("https://fastmarket-demo.onrender.com", environment.getProperty("app.cors.allowed-origin-patterns"));
    }

    @Test
    void respetaSobrescriturasExplicitas() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("RENDER_EXTERNAL_HOSTNAME", "fastmarket-demo.onrender.com")
                .withProperty("FRONTEND_BASE_URL", "https://tienda.ejemplo.com");

        new RenderEnvironmentPostProcessor().postProcessEnvironment(
                environment,
                new SpringApplication(Object.class)
        );

        assertEquals("https://tienda.ejemplo.com", environment.getProperty("FRONTEND_BASE_URL"));
        assertEquals("https://fastmarket-demo.onrender.com", environment.getProperty("app.backend.public-url"));
    }
}
