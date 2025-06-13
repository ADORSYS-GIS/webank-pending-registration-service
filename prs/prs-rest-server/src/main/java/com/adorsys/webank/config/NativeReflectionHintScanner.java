package com.adorsys.webank.config;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeReflectionHintScanner {

    private static final Logger log = LoggerFactory.getLogger(NativeReflectionHintScanner.class);

    private static final List<Class<? extends Annotation>> TARGET_ANNOTATIONS = List.of(
            Entity.class,
            JsonProperty.class
    );

    public static void main(String[] args) throws IOException {
        String basePackage = "com.adorsys.webank";
        scanForReflectionNeeds(basePackage);
    }

    private static void scanForReflectionNeeds(String basePackage) {
        Reflections reflections = new Reflections(basePackage, Scanners.TypesAnnotated);

        Set<Class<?>> candidates = new HashSet<>();

        for (Class<? extends Annotation> annotation : TARGET_ANNOTATIONS) {
            candidates.addAll(reflections.getTypesAnnotatedWith(annotation));
        }

        log.info("Classes that may need @RegisterReflectionForBinding:");
        candidates.stream()
                .sorted(Comparator.comparing(Class::getName))
                .forEach(clazz -> log.info("→ {}", clazz.getName()));

        log.info("\n Suggested @RegisterReflectionForBinding usage:\n");
        log.info("@RegisterReflectionForBinding({");
        log.info(candidates.stream()
                .map(c -> c.getName() + ".class")
                .collect(Collectors.joining(",\n")));
        log.info("})");
    }
}
