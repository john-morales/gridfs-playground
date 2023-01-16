Java GridFS Client for easily generating load or storage usage on a MongoDB replica set or sharded cluster.

## Building 
mvn clean compile assembly:single  
 
## Running
java -jar target/gridfs-playground-1.0-SNAPSHOT-jar-with-dependencies.jar  
  
## Usage / Options
```
Usage: -Dgridfs.infiniteModeEnabled=false -Dgridfs.num.threads=8 -Dgridfs.database=gridfs -Dgridfs.bucket=bucket -Dgridfs.chunksSizeBytes=358400 -Dgridfs.status.logIntervalMS=10000 -Dgridfs.maxBytesPerSecond=9223372036854775807 -Dgridfs.sharding.enabled=false -Dgridfs.sharding.presplit.enabled=false -Dgridfs.sharding.presplit.files.chunks=32 -Dgridfs.sharding.presplit.chunks.chunks=32 com.jmo.mongo.javadriver.gridfs.GridFS [mongoUri] [file1] [file2] ...  
```

Example:
```
$ java -jar target/gridfs-playground-1.0-SNAPSHOT-jar-with-dependencies.jar "mongodb://username:password@localhost:26000/admin?retryWrites=false&w=1" ~/opt/mongodb60-ent/bin/*  
2023-01-16T12:21:21.862-0500 [main] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:gridfsIngest:100] - Connecting to client at 'mongodb://localhost:26000/admin?retryWrites=false&w=1' with 8 thread(s) with collection 'gridfs.bucket.*' @ max 9.223372036854776E18 B/s  
2023-01-16T12:21:21.876-0500 [StatusThread] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:run:333] - Starting status logging interval: 10000ms  
2023-01-16T12:21:22.030-0500 [main] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:gridfsIngest:164] - Awaiting copy to complete for 8 files  
2023-01-16T12:21:22.031-0500 [pool-1-thread-7] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:127] - Saving file 7/8: '/Users/johnmorales/opt/mongodb60-ent/bin/mongoldap', 49,365,624 bytes  
2023-01-16T12:21:22.031-0500 [pool-1-thread-4] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:127] - Saving file 4/8: '/Users/johnmorales/opt/mongodb60-ent/bin/mongod', 129,875,624 bytes  
2023-01-16T12:21:22.031-0500 [pool-1-thread-3] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:127] - Saving file 3/8: '/Users/johnmorales/opt/mongodb60-ent/bin/mongocryptd', 86,311,552 bytes  
2023-01-16T12:21:22.031-0500 [pool-1-thread-1] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:127] - Saving file 1/8: '/Users/johnmorales/opt/mongodb60-ent/bin/install_compass', 15,205 bytes  
2023-01-16T12:21:22.031-0500 [pool-1-thread-8] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:127] - Saving file 8/8: '/Users/johnmorales/opt/mongodb60-ent/bin/mongos', 93,550,392 bytes  
2023-01-16T12:21:22.031-0500 [pool-1-thread-6] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:127] - Saving file 6/8: '/Users/johnmorales/opt/mongodb60-ent/bin/mongokerberos', 19,721,520 bytes  
2023-01-16T12:21:22.031-0500 [pool-1-thread-5] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:127] - Saving file 5/8: '/Users/johnmorales/opt/mongodb60-ent/bin/mongodecrypt', 19,850,728 bytes  
2023-01-16T12:21:22.031-0500 [pool-1-thread-2] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:127] - Saving file 2/8: '/Users/johnmorales/opt/mongodb60-ent/bin/mongo', 87,380,416 bytes  
2023-01-16T12:21:23.693-0500 [pool-1-thread-1] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:150] - Saved filename install_compass fileId 6c821921-e432-4098-80cf-9db2e7b47665 size 15205, after PT1.665S @ 0.0 MB/s  
2023-01-16T12:21:23.694-0500 [pool-1-thread-1] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:remove:293] - Removing status entry 6c821921-e432-4098-80cf-9db2e7b47665  
2023-01-16T12:21:23.694-0500 [pool-1-thread-1] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:log:321] - Status: 6c821921-e432-4098-80cf-9db2e7b47665 install_compass Last: 0.00 MB/s, Cumulative: 0.0 MB/s, Elapsed: 1.7 s, Completed: 15,205/15,205 bytes (100.0%)  
2023-01-16T12:21:24.073-0500 [pool-1-thread-5] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:150] - Saved filename mongodecrypt fileId efb421c0-4b0e-4188-91c0-aab62f004ae8 size 19850728, after PT2.044S @ 9.7 MB/s  
2023-01-16T12:21:24.074-0500 [pool-1-thread-5] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:remove:293] - Removing status entry efb421c0-4b0e-4188-91c0-aab62f004ae8  
2023-01-16T12:21:24.074-0500 [pool-1-thread-5] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:log:321] - Status: efb421c0-4b0e-4188-91c0-aab62f004ae8 mongodecrypt Last: 1.99 MB/s, Cumulative: 9.7 MB/s, Elapsed: 2.0 s, Completed: 19,850,728/19,850,728 bytes (100.0%)  
2023-01-16T12:21:24.091-0500 [pool-1-thread-6] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:150] - Saved filename mongokerberos fileId da6b5dce-27a1-4ecb-9984-5abca9867f4f size 19721520, after PT2.06S @ 9.6 MB/s  
2023-01-16T12:21:24.091-0500 [pool-1-thread-6] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:remove:293] - Removing status entry da6b5dce-27a1-4ecb-9984-5abca9867f4f  
2023-01-16T12:21:24.092-0500 [pool-1-thread-6] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:log:321] - Status: da6b5dce-27a1-4ecb-9984-5abca9867f4f mongokerberos Last: 1.97 MB/s, Cumulative: 9.6 MB/s, Elapsed: 2.1 s, Completed: 19,721,520/19,721,520 bytes (100.0%)  
2023-01-16T12:21:24.442-0500 [pool-1-thread-7] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:150] - Saved filename mongoldap fileId 832f024a-8f0b-4fb9-b5ad-de76fd14426d size 49365624, after PT2.412S @ 20.5 MB/s  
2023-01-16T12:21:24.443-0500 [pool-1-thread-7] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:remove:293] - Removing status entry 832f024a-8f0b-4fb9-b5ad-de76fd14426d  
2023-01-16T12:21:24.444-0500 [pool-1-thread-7] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:log:321] - Status: 832f024a-8f0b-4fb9-b5ad-de76fd14426d mongoldap Last: 4.94 MB/s, Cumulative: 20.5 MB/s, Elapsed: 2.4 s, Completed: 49,365,624/49,365,624 bytes (100.0%)  
2023-01-16T12:21:24.724-0500 [pool-1-thread-3] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:150] - Saved filename mongocryptd fileId 0ad5d27c-9b47-4ed8-8bdf-4d5d8b54fa36 size 86311552, after PT2.695S @ 32.0 MB/s  
2023-01-16T12:21:24.724-0500 [pool-1-thread-3] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:remove:293] - Removing status entry 0ad5d27c-9b47-4ed8-8bdf-4d5d8b54fa36  
2023-01-16T12:21:24.724-0500 [pool-1-thread-3] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:log:321] - Status: 0ad5d27c-9b47-4ed8-8bdf-4d5d8b54fa36 mongocryptd Last: 8.63 MB/s, Cumulative: 32.1 MB/s, Elapsed: 2.7 s, Completed: 86,311,552/86,311,552 bytes (100.0%)  
2023-01-16T12:21:24.733-0500 [pool-1-thread-2] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:150] - Saved filename mongo fileId 275c638e-8051-437c-b205-d21f92fd8676 size 87380416, after PT2.704S @ 32.3 MB/s  
2023-01-16T12:21:24.733-0500 [pool-1-thread-2] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:remove:293] - Removing status entry 275c638e-8051-437c-b205-d21f92fd8676  
2023-01-16T12:21:24.733-0500 [pool-1-thread-2] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:log:321] - Status: 275c638e-8051-437c-b205-d21f92fd8676 mongo Last: 8.74 MB/s, Cumulative: 32.4 MB/s, Elapsed: 2.7 s, Completed: 87,380,416/87,380,416 bytes (100.0%)  
2023-01-16T12:21:24.790-0500 [pool-1-thread-8] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:150] - Saved filename mongos fileId e18406ac-14cc-4780-a608-48061ec23983 size 93550392, after PT2.759S @ 33.9 MB/s  
2023-01-16T12:21:24.790-0500 [pool-1-thread-8] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:remove:293] - Removing status entry e18406ac-14cc-4780-a608-48061ec23983  
2023-01-16T12:21:24.790-0500 [pool-1-thread-8] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:log:321] - Status: e18406ac-14cc-4780-a608-48061ec23983 mongos Last: 9.36 MB/s, Cumulative: 33.9 MB/s, Elapsed: 2.8 s, Completed: 93,550,392/93,550,392 bytes (100.0%)  
2023-01-16T12:21:25.064-0500 [pool-1-thread-4] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:lambda$gridfsIngest$0:150] - Saved filename mongod fileId 1feecf07-b4f1-4675-a97a-270fc32b705a size 129875624, after PT3.035S @ 42.8 MB/s  
2023-01-16T12:21:25.065-0500 [pool-1-thread-4] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:remove:293] - Removing status entry 1feecf07-b4f1-4675-a97a-270fc32b705a  
2023-01-16T12:21:25.065-0500 [pool-1-thread-4] INFO  com.jmo.mongo.javadriver.gridfs.GridFS$StatusThread [GridFS.java:log:321] - Status: 1feecf07-b4f1-4675-a97a-270fc32b705a mongod Last: 12.99 MB/s, Cumulative: 42.8 MB/s, Elapsed: 3.0 s, Completed: 129,875,624/129,875,624 bytes (100.0%)  
2023-01-16T12:21:25.075-0500 [main] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:gridfsIngest:172] - Overall performance 151.9 MB/s, 486,071,061 bytes read, over 0min 3sec  
2023-01-16T12:21:25.076-0500 [main] INFO  com.jmo.mongo.javadriver.gridfs.GridFS [GridFS.java:gridfsIngest:177] - Exiting
```
