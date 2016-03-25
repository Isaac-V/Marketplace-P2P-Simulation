# Marketplace-P2P-Simulation
Socket-based P2P network simulating a marketplace scenario.

The provided GaulBazaarRegistry and GaulBazaarPeer directories can be readily imported into Eclipse as an existing project. Once imported various configuration parameters can be hardcoded into the main methods of the two programs. 

The variables N, neighborRadius, and portStart can be configured in the GaulBazaarRegistry main method. Most importantly, the regServer Socket variable in the GaulBazaarPeer main method must be set to the IP address of the machine that will be used to run GaulBazaarRegistry along with the same port number that was assigned to portStart in GaulBazaarRegistry. 

Once these parameters are properly configured, the programs can be readily exported as executable jar files or simply ran in Eclipse. The peers will produce output files in whatever directory they are executed within, these output files don’t conflict so multiple peers can be run from the same directory. Unfortunately these programs never terminate because their listening server threads continue to run indefinitely, thus running them in Eclipse or from a command line makes them easier to manage. You will know they are finished executing when every buyer peer has either printed “Done” to the console or when you notice that every buyer has produced its AvgResTime output file. The output files show the activity of each peer as well as average response times for each request and overall average response times for each buyer.
