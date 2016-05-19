# Falchion

Falchion is a JVM container for the fault tolerant server.
Falchion container requires JDK9 because it's using SO_REUSEPORT flag. JDK9 supports SO_REUSEPORT only on Linux.

When the container has started, it forks JVM processes until the given pool size.
If the container catch a HUP signal, it creates a new JVM processes and kill old processes.

## Options

- -cp  Classpath
- -p   The pool size of JVM processes. (default value is 1)
- --lifetime LIFETIME SECONDS  The lifetime of one jvm process. If you set the option to 3600, container will kill all old JVMs and create new JVMs gracefully.
- -m MONITORS  Adds monitors for monitoring a jvm process.
  Available monitors:
    - JSTAT_GCUTIL  GC statistics using jstat
    - METRICS_JMX   Metrics using JMX
- --auto-tuning  tuning JVM parameters automatically.
- --evaluator EVALUATOR 

   

## REST　API

![api](http://i.imgur.com/iIRC5Ix.png)

Falchion container provides HTTP APIs for getting the information of JVM processes or refreshing JVM processes.

### GET /jvms

```
% curl -i http://localhost:44010/jvms     
HTTP/1.1 200 OK
Date: Mon, 02 May 2016 10:11:24 GMT
Content-type: application/json
Content-length: 28

[{"id":"5jiic","pid":21914}]
```

### GET /jvm/{id}

```
% curl -i http://localhost:44010/jvm/5jiic
HTTP/1.1 200 OK
Date: Mon, 02 May 2016 10:12:23 GMT
Content-type: application/json
Content-length: 182

{"id":"5jiic","pid":21914,"uptime":258357,"stats":[{"type":"gcutil","S0":0.0,"S1":100.0,"E":16.88,"O":3.1,"M":94.86,"CCS":80.13,"YGC":1,"YGCT":0.004,"FGC":0,"FGCT":0.0,"GCT":0.004}]}                                                                                      
```

### POST /container/refresh

Kill all old JVMs and create new JVMs gracefully.

