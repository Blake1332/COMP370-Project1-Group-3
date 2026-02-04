# COMP370-Project1-Group-3

##TODO
- write linux bash script to launch (run.sh)   - DONE
- logger - DONE
- implement client   - DONE

- persistence (maybe)
- test cases (junit would be nice!)
- dynamic adding and removeing of members (currently hard coded into raftserver.java)
- simulation of failures for both network and node crashes to see if the cluster remains

## Changes made:

### RaftNode (RaftNode.java)

Added Logger integration (logger field and setLogger()), replaced System.out with logger.log() throughout.
Added leader tracking (currentLeaderId) and updates in handleRequestVote() / handleAppendEntries() / becomeLeader().
Added leader replication state (nextIndex) and helper appendAsLeader() for log appends with matchIndex updates.
More detailed logging around vote handling, AppendEntries processing, conflicts, and commit updates.

### RaftServer (RaftServer.java)

Added logger initialization and wired it into RaftNode (setLogger).
Added TCP client listener and request handling (ServerSocket, client thread, handleClientConnection()).
Heartbeats now send actual log entries using nextIndex and track last-sent index per follower for accurate matchIndex updates.
Commit index advancement updated to require majority replication (current term).
Added shutdown hook cleanup for sockets and logger.
Expanded logging around elections, packet handling, replication, and state machine application.

### Logger (Logger.java)

Writes timestamped messages to both console and a log file.
Thread‑safe log(); close() flushes/ends the file writer.

### Client (Client.java)

Discovers leader by sending GET_LEADER to each node’s client port (TCP).
Sends PROCESS_JOB to leader; if response says NOT_LEADER, it re‑discovers and retries.
Interactive CLI loop; closes reader and logger on quit.
