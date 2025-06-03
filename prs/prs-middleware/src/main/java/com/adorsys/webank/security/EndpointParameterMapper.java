package com.adorsys.webank.security;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@RequiredArgsConstructor
public class EndpointParameterMapper {
    private final Map<String, List<String>> endpointParameters ;

    public List<String> getRequiredParameters(String path) {
        return endpointParameters.getOrDefault(path, Collections.emptyList());
    }
}
