package com.hedera.hashgraph.tck.methods.sdk.param.node;

import com.hedera.hashgraph.tck.methods.JSONRPC2Param;
import com.hedera.hashgraph.tck.methods.sdk.param.CommonTransactionParams;
import com.hedera.hashgraph.tck.util.JSONRPCParamParser;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DeleteNodeParams for delete node method
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DeleteNodeParams extends JSONRPC2Param {
    private Optional<String> nodeId;
    private Optional<CommonTransactionParams> commonTransactionParams;

    @Override
    public DeleteNodeParams parse(Map<String, Object> jrpcParams) throws Exception {
        var parsedNodeId = Optional.ofNullable((String) jrpcParams.get("nodeId"));
        var parsedCommonTransactionParams = JSONRPCParamParser.parseCommonTransactionParams(jrpcParams);

        return new DeleteNodeParams(parsedNodeId, parsedCommonTransactionParams);
    }
}
