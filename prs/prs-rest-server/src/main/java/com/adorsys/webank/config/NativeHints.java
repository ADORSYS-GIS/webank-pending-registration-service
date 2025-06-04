package com.adorsys.webank.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(NativeHints.LoggerHints.class)
public class NativeHints  {


    static class LoggerHints implements RuntimeHintsRegistrar {

        /**
         * Contribute hints to the given {@link RuntimeHints} instance.
         *
         * @param hints       the hints contributed so far for the deployment unit
         * @param classLoader the classloader, or {@code null} if even the system
         *                    ClassLoader is not accessible
         */
        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.reflection()
                    .registerType(org.hibernate.internal.log.ConnectionInfoLogger.class);
        }
    }
}
