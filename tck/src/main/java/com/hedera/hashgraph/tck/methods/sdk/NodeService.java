package com.hedera.hashgraph.tck.methods.sdk;

import com.hedera.hashgraph.sdk.NodeDeleteTransaction;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.node.DeleteNodeParams;
import com.hedera.hashgraph.tck.methods.sdk.response.NodeResponse;
import java.time.Duration;

@JSONRPC2Service
public class NodeService extends AbstractJSONRPC2Service {
    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(3L);
    private final SdkService sdkService;

    public NodeService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("deleteNode")
    public NodeResponse deleteNode(final DeleteNodeParams params) throws Exception{
        NodeDeleteTransaction transaction = new NodeDeleteTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);
        params.getNodeId().ifPresent(nodeIdStr -> transaction.setNodeId(parseNodeId(nodeIdStr)));
        params.getCommonTransactionParams()
            .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));
        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());
        String nodeId = params.getNodeId().orElse("");
        if(receipt.status == Status.SUCCESS && receipt.nodeId != 0){
            nodeId = Long.toString(receipt.nodeId);
        }
        return new NodeResponse(nodeId, receipt.status);
    }
    private long parseNodeId(String nodeIdStr) {
        try {
            long nodeId = Long.parseLong(nodeIdStr);
            if (nodeId < 0) {
                throw new IllegalArgumentException("Node ID cannot be negative: " + nodeIdStr);
            }
            return nodeId;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid node ID: " + nodeIdStr, ex);
        }
    }
}
