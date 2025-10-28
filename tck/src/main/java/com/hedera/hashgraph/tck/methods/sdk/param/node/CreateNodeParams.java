package com.hedera.hashgraph.tck.methods.sdk.param.node;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

/**
 * CreateNodeParams for create node method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateNodeParams extends JSONRPC2Param {
    private Optional<String> accountId;
    private Optional<String> description;
    private Optional<List<EndpointParams>> gossipEndpoints;
    private Optional<List<EndpointParams>> serviceEndpoints;
    private Optional<String> gossipCaCertificate;
    private Optional<String> grpcCertificateHash;
    private Optional<String> adminKey;
    private Optional<Boolean> declineReward;
    private Optional<EndpointParams> grpcWebProxyEndpoint;
    private Optional<CommonTransactionParams> commonTransactionParams;

    @Override
    public CreateNodeParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedAccountId = Optional.ofNullable((String) jrpcParams.get("accountId"));
        var parsedDescription = Optional.ofNullable((String) jrpcParams.get("description"));
        var parsedGossipEndpoints = parseEndpointArray(jrpcParams, "gossipEndpoints");
        var parsedServiceEndpoints = parseEndpointArray(jrpcParams, "serviceEndpoints");
        var parsedGossipCaCertificate = Optional.ofNullable((String) jrpcParams.get("gossipCaCertificate"));
        var parsedGrpcCertificateHash = Optional.ofNullable((String) jrpcParams.get("grpcCertificateHash"));
        var parsedAdminKey = Optional.ofNullable((String) jrpcParams.get("adminKey"));
        var parsedDeclineReward = Optional.ofNullable((Boolean) jrpcParams.get("declineReward"));
        var parsedGrpcWebProxyEndpoint = parseEndpointObject(jrpcParams, "grpcWebProxyEndpoint");
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new CreateNodeParams(
            parsedAccountId,
            parsedDescription,
            parsedGossipEndpoints,
            parsedServiceEndpoints,
            parsedGossipCaCertificate,
            parsedGrpcCertificateHash,
            parsedAdminKey,
            parsedDeclineReward,
            parsedGrpcWebProxyEndpoint,
            parsedCommonTransactionParams);
    }

    private Optional<List<EndpointParams>> parseEndpointArray(Map<String, Object> params, String key) {
        if (!params.containsKey(key)) {
            return Optional.empty();
        }

        JSONArray jsonArray = (JSONArray) params.get(key);
        List<EndpointParams> endpoints = new ArrayList<>();
        for (Object element : jsonArray) {
            if (element instanceof JSONObject jsonObject) {
                endpoints.add(EndpointParams.parse(jsonObject));
            }
        }
        return Optional.of(endpoints);
    }

    private Optional<EndpointParams> parseEndpointObject(Map<String, Object> params, String key) {
        if (!params.containsKey(key)) {
            return Optional.empty();
        }

        JSONObject jsonObject = (JSONObject) params.get(key);
        return Optional.of(EndpointParams.parse(jsonObject));
    }
}
