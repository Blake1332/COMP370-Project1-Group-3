package raft_demo;

import java.net.*;
import java.util.*;
import java.io.*;


 //The network server for a Raft node.
 //Handles communication, serialization, and the main loop.
public class RaftServer {
    private DatagramSocket socket;
    private RaftNode raftNode;
    private int port;

    private Logger logger;

    
     // Initializes the server with its ID, port, and cluster members.

    public RaftServer(int id, int port, Map<Integer, Integer> clusterMembers) throws Exception {
        this.port = port;
        this.socket = new DatagramSocket(port);
        this.raftNode = new RaftNode(id, clusterMembers);
        this.logger = new Logger("Node-" + id, "logs/node_" + id + ".log");
        this.raftNode.setLogger(this.logger);
    }

     // Starts the server's background threads and main logic loop.

    public void start() {
        logger.log("Server " + raftNode.id + " started on port " + port);

        //Network Receiver Thread 
        // Continuously listens for incoming UDP packets and dispatches them to handlePacket
        new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (true) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    logger.log("Received packet from " + packet.getAddress() + ":" + packet.getPort() + " sent to handlePacket");
                    handlePacket(packet);
                } catch (Exception e) {
                    logger.log("Error handling packet: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();

        //Main Logic Loop
        // Periodically checks for election timeouts or sends heartbeats if leader
        while (true) {
            try {
                Thread.sleep(500); // Tick rate of 500ms (Slower for visibility)
                synchronized (raftNode) {
                    if (raftNode.role == RaftNode.Role.LEADER) {
                        sendHeartbeats();
                    } else if (raftNode.isElectionTimeout()) {
                        startElection();
                    }
                    
                    // State Machine Application
                    // Apply committed entries that haven't been applied yet
                    while (raftNode.lastApplied < raftNode.commitIndex) {
                        raftNode.lastApplied++;
                        if (raftNode.lastApplied < raftNode.log.size()) {
                            RaftRPC.LogEntry entry = raftNode.log.get(raftNode.lastApplied);
                            logger.log("Applied to State Machine: " + entry.command + " at index " + raftNode.lastApplied);
                        }
                    }
                }
            } catch (Exception e) {
                logger.log("Error in main loop: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    
    // Deserializes incoming packets and routes them to the  handler.
     
    private void handlePacket(DatagramPacket packet) throws Exception {
        Object obj = deserialize(packet.getData());
        synchronized (raftNode) {

            if (obj instanceof RaftRPC.RequestVoteArgs) {
                // Handle a vote request from another node
                logger.log("Handling RequestVoteArgs from " + packet.getAddress() + ":" + packet.getPort());
                RaftRPC.RequestVoteResults res = raftNode.handleRequestVote((RaftRPC.RequestVoteArgs) obj);
                sendResponse(res, packet.getAddress(), packet.getPort());

            } else if (obj instanceof RaftRPC.AppendEntriesArgs) {
                // Handle a heartbeat or log replication request from a leader
                logger.log("Handling AppendEntriesArgs from " + packet.getAddress() + ":" + packet.getPort());
                RaftRPC.AppendEntriesResults res = raftNode.handleAppendEntries((RaftRPC.AppendEntriesArgs) obj);
                sendResponse(res, packet.getAddress(), packet.getPort());

            } else if (obj instanceof RaftRPC.RequestVoteResults) {
                // Handle a vote response we received after starting an election
                logger.log("Handling RequestVoteResults from " + packet.getAddress() + ":" + packet.getPort());
                handleVoteResult((RaftRPC.RequestVoteResults) obj);

            } else if (obj instanceof RaftRPC.AppendEntriesResults) {
                // Handle a response to a log replication request we sent
                logger.log("Handling AppendEntriesResults from " + packet.getAddress() + ":" + packet.getPort());
                int fromId = -1;
                for (Map.Entry<Integer, Integer> entry : raftNode.clusterMembers.entrySet()) {
                    if (entry.getValue() == packet.getPort()) {
                        fromId = entry.getKey();
                        break;
                    }
                }
                handleAppendEntriesResult((RaftRPC.AppendEntriesResults) obj, fromId);
            }
        }
    }

    private int votesReceived = 0;

    
    // Initiates an election by requesting votes from all other cluster members.
     
    private void startElection() throws Exception {
        logger.log("Election timeout! Starting election for term " + (raftNode.currentTerm + 1));
        raftNode.startElection();
        votesReceived = 1; // Vote for self ofc

        logger.log("Requesting votes from other nodes...");
        RaftRPC.RequestVoteArgs args = new RaftRPC.RequestVoteArgs(
            raftNode.currentTerm, raftNode.id, 
            raftNode.log.size() - 1, 
            raftNode.log.isEmpty() ? 0 : raftNode.log.get(raftNode.log.size() - 1).term
        );
        

        for (Map.Entry<Integer, Integer> member : raftNode.clusterMembers.entrySet()) {
            if (member.getKey() != raftNode.id) {
                logger.log("Requesting vote from node " + member.getKey() + " at port " + member.getValue());
                sendRequest(args, "localhost", member.getValue());
            }
        }
    }

    
    //Processes a vote response and transitions to leader if a majority is reached.
    private void handleVoteResult(RaftRPC.RequestVoteResults res) {
        if (res.term > raftNode.currentTerm) {
            logger.log("Received higher term (" + res.term + "), stepping down");
            raftNode.stepDown(res.term);
            return;
        }
        if (raftNode.role == RaftNode.Role.CANDIDATE && res.voteGranted) {
            votesReceived++;
            logger.log("Vote received! Total votes: " + votesReceived + "/" + raftNode.clusterMembers.size());
            if (votesReceived > raftNode.clusterMembers.size() / 2) {
                logger.log("Majority votes received! Becoming leader for term " + raftNode.currentTerm);
                raftNode.becomeLeader();
            }
        }
    }

    
    //Sends heartbeats to all followers.
    //heartbeats are just empty AppendEntries Requests
    private void sendHeartbeats() throws Exception {
        if (System.currentTimeMillis() - raftNode.lastHeartbeat < 1500) {
            logger.log("Less than 1500ms since last heartbeat, skipping heartbeat send");
            return;
        }
        raftNode.lastHeartbeat = System.currentTimeMillis();
        logger.log("Sending heartbeats to followers...");
        for (Map.Entry<Integer, Integer> member : raftNode.clusterMembers.entrySet()) {
            if (member.getKey() != raftNode.id) {
                logger.log("Sending heartbeat to node " + member.getKey() + " at port " + member.getValue());
                RaftRPC.AppendEntriesArgs args = new RaftRPC.AppendEntriesArgs(
                    raftNode.currentTerm, raftNode.id, 
                    raftNode.log.size() - 1,
                    raftNode.log.isEmpty() ? 0 : raftNode.log.get(raftNode.log.size() - 1).term,
                    null, raftNode.commitIndex
                );
                sendRequest(args, "localhost", member.getValue());
            }
        }
    }

    
     // Processes log replication responses and updates replication progress.

    private void handleAppendEntriesResult(RaftRPC.AppendEntriesResults res, int fromId) {
        if (res.term > raftNode.currentTerm) {
            logger.log("Received higher term (" + res.term + "), stepping down");
            raftNode.stepDown(res.term);
            return;
        }
        
        if (raftNode.role == RaftNode.Role.LEADER) {
            if (res.success) {
                // Update the match index for this follower
                logger.log("AppendEntries successful from node " + fromId + ", updating matchIndex");
                raftNode.matchIndex.put(fromId, raftNode.log.size() - 1);
                updateCommitIndex();
            }
        }
    }

    
    // Checks if a majority of nodes have replicated an entry and updates commitIndex.
    private void updateCommitIndex() {
        int count = 1; // self
        int lastIndex = raftNode.log.size() - 1;
        for (int match : raftNode.matchIndex.values()) {
            if (match >= lastIndex && match >= 0) count++;  // Only count if match index >= 0 (has replicated)
        }
        
        logger.log("Checking commit index: " + count + " nodes have replicated index " + lastIndex);
        if (count > raftNode.clusterMembers.size() / 2 && lastIndex > raftNode.commitIndex) {
            raftNode.commitIndex = lastIndex;
            logger.log("Majority reached! Committed up to index " + raftNode.commitIndex);
        }
    }

    // Networking Helper stuff

    private void sendRequest(Object obj, String host, int port) throws Exception {
        byte[] data = serialize(obj);
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(host), port);
        socket.send(packet);
    }

    private void sendResponse(Object obj, InetAddress address, int port) throws Exception {
        byte[] data = serialize(obj);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    private byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    private Object deserialize(byte[] data) throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    
     // Main entry point to start a node.
     
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: RaftServer <id>");
            return;
        }
        int id = Integer.parseInt(args[0]);
        
        // Define ports for the nodes
        Map<Integer, Integer> members = new HashMap<>();
        members.put(1, 9102);
        members.put(2, 9103);
        members.put(3, 9104);

        if (!members.containsKey(id)) {
            System.out.println("Invalid Node ID: " + id);
            return;
        }

        int port = members.get(id);
        RaftServer server = new RaftServer(id, port, members);
        server.start();
    }
}
