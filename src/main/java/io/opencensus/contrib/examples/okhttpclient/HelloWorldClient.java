package io.opencensus.contrib.examples.okhttpclient;

import io.opencensus.contrib.http.util.HttpViews;
import io.opencensus.exporter.stats.prometheus.PrometheusStatsCollector;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.exporter.trace.jaeger.JaegerTraceExporter;
import io.opencensus.exporter.trace.logging.LoggingTraceExporter;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration;
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter;
import io.opencensus.trace.Tracing;
import io.opencensus.trace.config.TraceConfig;
import io.opencensus.trace.samplers.Samplers;
import io.prometheus.client.exporter.HTTPServer;
import java.io.IOException;
import java.util.Arrays;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class HelloWorldClient {
  private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

  class OcOkHttpClientRequest extends Request.Builder {

    OcOkHttpClientRequest(Builder builder) {

    }
  }

  private static void initTracing() {
    TraceConfig traceConfig = Tracing.getTraceConfig();
    Logger.getRootLogger().setLevel(Level.INFO);
    traceConfig.updateActiveTraceParams(
        traceConfig.getActiveTraceParams().toBuilder().setSampler(Samplers.alwaysSample()).build());

    LoggingTraceExporter.register();
    // Register Jaeger Tracing.
    JaegerTraceExporter.createAndRegister("http://localhost:14268/api/traces", "helloworld-okhttp-client");
  }

  private static void initStatsExporter() throws IOException {
    HttpViews.registerAllClientViews();
    try {
      StackdriverStatsExporter.createAndRegister();
      StackdriverTraceExporter.createAndRegister(StackdriverTraceConfiguration.builder().build());
    } catch (Exception e) {
      logger.info("Failed to register Stackdriver exporter " + e.toString());
    }
    // Register Prometheus exporters and export metrics to a Prometheus HTTPServer.
    PrometheusStatsCollector.createAndRegister();
    HTTPServer prometheusServer = new HTTPServer(9091, true);
  }

  public static void main(String[] args) throws Exception {
    BasicConfigurator.configure();

    initTracing();
    initStatsExporter();

    Arrays.hashCode(new Long[] {0L, 1L});

    OkHttpClient client = new OkHttpClient();
    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    RequestBody body = RequestBody.create(JSON, "{'hello':'world'}");



    do {
      Request request = new Request.Builder()
          .url("http://localhost:8080")
          .build();
      Request asyncRequest = new Request.Builder()
          .url("http://localhost:8080/async")
          .build();
      Request postRequest = new Request.Builder()
          .url("http://localhost:8080")
          .post(body)
          .build();

      if (request == null) {
        logger.info("Request is null");
        break;
      }

      Response response = client.newCall(request).execute();
      response = client.newCall(asyncRequest).execute();
      response = client.newCall(postRequest).execute();

      try {
        Thread.sleep(15000);
      } catch (Exception e) {

      }
    } while (true);
  }
}
