package com.hedera.hashgraph.tck.methods.sdk;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Endpoint;
import com.hedera.hashgraph.sdk.NodeCreateTransaction;
import com.hedera.hashgraph.sdk.NodeDeleteTransaction;
import com.hedera.hashgraph.sdk.Status;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Method;
import com.hedera.hashgraph.tck.annotation.JSONRPC2Service;
import com.hedera.hashgraph.tck.methods.AbstractJSONRPC2Service;
import com.hedera.hashgraph.tck.methods.sdk.param.node.CreateNodeParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.DeleteNodeParams;
import com.hedera.hashgraph.tck.methods.sdk.param.node.EndpointParams;
import com.hedera.hashgraph.tck.methods.sdk.response.NodeResponse;
import com.hedera.hashgraph.tck.util.KeyUtils;
import java.time.Duration;

@JSONRPC2Service
public class NodeService extends AbstractJSONRPC2Service {
    private static final Duration DEFAULT_GRPC_DEADLINE = Duration.ofSeconds(3L);
    private final SdkService sdkService;

    public NodeService(SdkService sdkService) {
        this.sdkService = sdkService;
    }

    @JSONRPC2Method("createNode")
    public NodeResponse createNode(final CreateNodeParams params) throws Exception {
        NodeCreateTransaction transaction = new NodeCreateTransaction().setGrpcDeadline(DEFAULT_GRPC_DEADLINE);

        params.getAccountId().ifPresent(accountId -> transaction.setAccountId(AccountId.fromString(accountId)));
        params.getDescription().ifPresent(transaction::setDescription);

        params.getGossipEndpoints().ifPresent(endpoints -> {
            endpoints.forEach(endpoint -> transaction.addGossipEndpoint(convertEndpoint(endpoint)));
        });

        params.getServiceEndpoints().ifPresent(endpoints -> {
            endpoints.forEach(endpoint -> transaction.addServiceEndpoint(convertEndpoint(endpoint)));
        });

        params.getGossipCaCertificate()
            .ifPresent(cert -> transaction.setGossipCaCertificate(decodeHex(cert, "gossipCaCertificate")));

        params.getGrpcCertificateHash()
            .ifPresent(hash -> transaction.setGrpcCertificateHash(decodeHex(hash, "grpcCertificateHash")));

        params.getAdminKey().ifPresent(key -> {
            try {
                transaction.setAdminKey(KeyUtils.getKeyFromString(key));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid admin key format", e);
            }
        });

        params.getDeclineReward().ifPresent(transaction::setDeclineReward);

        params.getGrpcWebProxyEndpoint()
            .map(this::convertEndpoint)
            .ifPresent(transaction::setGrpcWebProxyEndpoint);

        params.getCommonTransactionParams()
            .ifPresent(commonParams -> commonParams.fillOutTransaction(transaction, sdkService.getClient()));

        TransactionResponse txResponse = transaction.execute(sdkService.getClient());
        TransactionReceipt receipt = txResponse.getReceipt(sdkService.getClient());

        String nodeId = "";
        if (receipt.status == Status.SUCCESS && receipt.nodeId != 0) {
            nodeId = Long.toString(receipt.nodeId);
        }

        return new NodeResponse(nodeId, receipt.status);
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

    private Endpoint convertEndpoint(EndpointParams params) {
        Endpoint endpoint = new Endpoint();
        params.getIpAddressV4().ifPresent(address -> endpoint.setAddress(decodeHex(address, "ipAddressV4")));
        params.getPort().ifPresent(endpoint::setPort);
        params.getDomainName().ifPresent(endpoint::setDomainName);
        return endpoint;
    }

    private byte[] decodeHex(String value, String fieldName) {
        try {
            return ByteString.fromHex(value).toByteArray();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid " + fieldName + " hex string", ex);
        }
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
