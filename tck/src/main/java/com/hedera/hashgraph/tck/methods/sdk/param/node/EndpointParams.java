package com.hedera.hashgraph.tck.methods.sdk.param.node;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minidev.json.JSONObject;

/**
 * Endpoint parameters used by node transactions.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EndpointParams {
    private Optional<String> ipAddressV4;
    private Optional<Integer> port;
    private Optional<String> domainName;

    public static EndpointParams parse(JSONObject jsonObject) {
        var parsedIpAddress = Optional.ofNullable((String) jsonObject.get("ipAddressV4"));

        Optional<Integer> parsedPort = Optional.empty();
        Object portValue = jsonObject.get("port");
        if (portValue instanceof Number portNumber) {
            parsedPort = Optional.of(portNumber.intValue());
        }

        var parsedDomainName = Optional.ofNullable((String) jsonObject.get("domainName"));

        return new EndpointParams(parsedIpAddress, parsedPort, parsedDomainName);
    }
}
