package com.github.wenweihu86.raft.service.impl;

import com.github.wenweihu86.raft.Peer;
import com.github.wenweihu86.raft.RaftNode;
import com.github.wenweihu86.raft.proto.Raft;
import com.github.wenweihu86.raft.service.RaftClientService;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by wenweihu86 on 2017/5/14.
 */
public class RaftClientServiceImpl implements RaftClientService {
    private static final Logger LOG = LoggerFactory.getLogger(RaftClientServiceImpl.class);

    private RaftNode raftNode;

    public RaftClientServiceImpl(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    @Override
    public Raft.GetLeaderResponse getLeader(Raft.GetLeaderRequest request) {
        LOG.info("receive getLeader request");
        Raft.GetLeaderResponse.Builder responseBuilder = Raft.GetLeaderResponse.newBuilder();
        responseBuilder.setSuccess(true);
        Raft.EndPoint.Builder endPointBuilder = Raft.EndPoint.newBuilder();
        raftNode.getLock().lock();
        try {
            int leaderId = raftNode.getLeaderId();
            if (leaderId == 0) {
                responseBuilder.setSuccess(false);
            } else if (leaderId == raftNode.getLocalServer().getServerId()) {
                endPointBuilder.setHost(raftNode.getLocalServer().getHost());
                endPointBuilder.setPort(raftNode.getLocalServer().getPort());
            } else {
                List<Peer> peers = raftNode.getPeers();
                for (Peer peer : peers) {
                    if (peer.getServerAddress().getServerId() == leaderId) {
                        endPointBuilder.setHost(peer.getServerAddress().getHost());
                        endPointBuilder.setPort(peer.getServerAddress().getPort());
                        break;
                    }
                }
            }
        } finally {
            raftNode.getLock().unlock();
        }
        responseBuilder.setLeader(endPointBuilder.build());
        Raft.GetLeaderResponse response = responseBuilder.build();
        final JsonFormat.Printer printer = JsonFormat.printer().omittingInsignificantWhitespace();
        try {
            LOG.info("getLeader response={}", printer.print(response));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return responseBuilder.build();
    }

}