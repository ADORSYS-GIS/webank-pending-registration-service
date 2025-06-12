package com.adorsys.webank.config;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.web.bind.annotation.RestController;

public class NativeReflectionHintScanner {

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

        System.out.println("🔍 Classes that may need @RegisterReflectionForBinding:");
        candidates.stream()
                .sorted(Comparator.comparing(Class::getName))
                .forEach(clazz -> System.out.println("→ " + clazz.getName()));

        System.out.println("\n📌 Suggested @RegisterReflectionForBinding usage:\n");
        System.out.println("@RegisterReflectionForBinding({");
        System.out.println(candidates.stream()
                .map(c -> c.getName() + ".class")
                .collect(Collectors.joining(",\n")));
        System.out.println("})");
    }
}

