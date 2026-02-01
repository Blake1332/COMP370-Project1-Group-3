package raft_demo;

import java.io.Serializable; //the main bad boy for the network stuff
import java.util.List;


// Defines the raft structures used in the protocol.
public class RaftRPC {

    
    //Arguments for the RequestVote RPC. 
    public static class RequestVoteArgs implements Serializable {
        // Candidate's current term 
        public int term;
        // Candidate requesting vote 
        public int candidateId;
        //Index of candidate's last log entry 
        public int lastLogIndex;
        // Term of candidate's last log entry
        public int lastLogTerm;

        public RequestVoteArgs(int term, int candidateId, int lastLogIndex, int lastLogTerm) {
            this.term = term;
            this.candidateId = candidateId;
            this.lastLogIndex = lastLogIndex;
            this.lastLogTerm = lastLogTerm;
        }
    }

    // Results for the RequestVote RPC.
    public static class RequestVoteResults implements Serializable {
        // currentTerm, for candidate to update itself 
        public int term;
        // true means candidate received vote 
        public boolean voteGranted;

        public RequestVoteResults(int term, boolean voteGranted) {
            this.term = term;
            this.voteGranted = voteGranted;
        }
    }

    /**
      Arguments for the AppendEntries
      Sent by leaders to followers as heartbeats.
      sent by followers to leaders to get updates
     **/
    public static class AppendEntriesArgs implements Serializable {
        // Leader's term 
        public int term;

        // Leader's ID so followers can redirect clients 
        public int leaderId;

        // Index of log entry immediately preceding new ones 
        public int prevLogIndex;

        // Term of prevLogIndex entry 
        public int prevLogTerm;

        // Log entries to store (empty for heartbeat) 
        public List<LogEntry> entries;

        // Leader's commitIndex 
        public int leaderCommit;

        public AppendEntriesArgs(int term, int leaderId, int prevLogIndex, int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
            this.term = term;
            this.leaderId = leaderId;
            this.prevLogIndex = prevLogIndex;
            this.prevLogTerm = prevLogTerm;
            this.entries = entries;
            this.leaderCommit = leaderCommit;
        }
    }

    
    // Results for the AppendEntries.
    public static class AppendEntriesResults implements Serializable {
        // currentTerm, leader updates itself  
        public int term;

        // true if follower contained entry matching prevLogIndex and prevLogTerm 
        public boolean success;

        public AppendEntriesResults(int term, boolean success) {
            this.term = term;
            this.success = success;
        }
    }

    
    // A single entry in the Raft log.
     
    public static class LogEntry implements Serializable {
        // Term when entry was received by leader 
        public int term;
        public String command;

        public LogEntry(int term, String command) {
            this.term = term;
            this.command = command;
        }
    }
}
