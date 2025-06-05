package com.adorsys.webank.security;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@RequiredArgsConstructor
public class EndpointParameterMapper {
    private final Map<String, List<String>> endpointParameters ;

    public List<String> getRequiredParameters(String path) {
        // First try the exact match
        List<String> params = endpointParameters.get(path);
        if (params != null) {
            return params;
        }

        // Try pattern matching with dynamic placeholders
        for (Map.Entry<String, List<String>> entry : endpointParameters.entrySet()) {
            String pattern = entry.getKey();

            if (pattern.contains("{")) {
                // Replace all {xxx} with regex groups
                String regex = pattern.replaceAll("\\{[^/]+?}", "([^/]+)");

                // Match an incoming path against generated regex
                if (path.matches(regex)) {
                    return entry.getValue();
                }
            }
        }
        return Collections.emptyList();
    }

    // getter
    public Map<String, List<String>> getEndpointParameters() {
        return Collections.unmodifiableMap(endpointParameters);
    }

}
