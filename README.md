HFFSM-
======

A algorithm HFFSM based on FFSM is presented to solve 5 problems of the classic frequent subgraph mining algorithm FFSM when it’s applied to rule mining.

- First equally convert directed multigraphs to undirected simple graphs, 
- and then construct suboptimal CAM tree and mine frequent subgraphs with the top-down depth-first method. 

The empirical study shows that HFFSM can deal with directed multigraphs well, reduce false alarms when the algorithm is applied, and is a little better than FFSM in efficiency, but more useful than FSSM
