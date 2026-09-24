package com.checkout.backend.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.HandlerTypePredicate;

/**
 * Antepone /api/v1 a la ruta de cada controller del proyecto.
 *
 * La version va en el prefijo y no en cada @RequestMapping por una razon
 * concreta: cuando llegue la v2, un controller nuevo solo tiene que declarar su
 * ruta propia y la version la sigue decidiendo este archivo. Repetir "/api/v1"
 * en veinte anotaciones garantiza que alguna se quede sin actualizar, y el error
 * no se nota hasta que un cliente pega contra la ruta vieja.
 *
 * El predicado limita el prefijo a los @RestController de com.checkout.backend.
 * Sin esa restriccion tambien se prefijarian los controllers que traen las
 * dependencias — los de springdoc, por ejemplo — y /v3/api-docs dejaria de
 * responder donde Swagger UI lo busca.
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    /** Prefijo comun de la API. Publico porque los tests construyen URLs con el. */
    public static final String API_V1 = "/api/v1";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                API_V1,
                HandlerTypePredicate.forBasePackage("com.checkout.backend")
                        .and(HandlerTypePredicate.forAnnotation(RestController.class)));
    }

}
