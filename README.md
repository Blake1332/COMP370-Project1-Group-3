# COMP370-Project1-Group-3

##TODO
- write linux bash script to launch (run.sh)   - DONE
- logger - DONE


- implement client  
- persistence (maybe)
- test cases (junit would be nice!)
- dynamic adding and removeing of members (currently hard coded into raftserver.java)
- simulation of failures for both network and node crashes to see if the cluster remains

## Observed:

Saw this entry, even though one server was killed and only 2 were online: "Checking commit index: 3 nodes have replicated index -1"

Put in a check here, will test it after client implementation - src/raft_demo/RaftServer.java(215-219)