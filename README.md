# jettywebapp
It is HelloWorld example application of Jetty Http Server/Client using Opencensus to collect traces
and stats. The application exports traces to jaeger and stats to prometheus.
 
## Build
Clone the project and run following in root directory.
```
./gradlew installDist
```

## Run Jaeger 
```
docker run -d -p 5775:5775 -p 6831:6831 -p 6832:6832 -p 5778:5778 -p 16687:16686 -p 14268:14268 -p 9411:9411 jaegertracing/all-in-one:1.6
```

## Run HelloWorldServer
```
./build/install/jettywebapp/bin/HelloWorldServer
```


## Run HelloWorldClient
```
./build/install/jettywebapp/bin/HelloWorldClient
```

## Check Traces
Login on http://localhost:14268/ and search for traces.


## Check Stats
You can simply check metrics for clients and server.
- **Client**: http://localhost:9091/metrics
- **Server**: http://localhost:9090/metrics

Alternatively, you could run Prometheus collector and configure it to scrap stats from the client
the and server. Refere to [Prometheus](https://prometheus.io/) for setup and configuration.
