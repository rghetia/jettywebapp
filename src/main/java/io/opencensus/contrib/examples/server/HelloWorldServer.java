package io.opencensus.contrib.examples.server;

import io.opencensus.contrib.http.servlet.OcHttpServletFilter;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;

public class HelloWorldServer extends AbstractHandler {
  public static class HelloServlet extends HttpServlet {
    private static String HEAVY_RESOURCE =
        "<h1>Hello .......................... ............................ SimpleServlet</h1>";

    private static final long serialVersionUID = 1L;

    private void nonAsyncGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

      String str = HEAVY_RESOURCE.concat("<h3>non-async</h3>");
      ByteBuffer content = ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8));

      PrintWriter pout = response.getWriter();

      pout.print("<html><body>");
      pout.print(str);
      pout.print("</body></html>");
      return;
    }

    private void asyncGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
      String str = HEAVY_RESOURCE.concat("<h3>async</h3>");
      ByteBuffer content = ByteBuffer.wrap(str.getBytes(StandardCharsets.UTF_8));

      AsyncContext async = request.startAsync();
      response.setContentType("text/html");
      try {
        Thread.sleep(100);
      } catch (Exception e) {
        //          logger.info("Error sleeping");
      }
      ServletOutputStream out = response.getOutputStream();
      out.setWriteListener(
          new WriteListener() {
            @Override
            public void onWritePossible() throws IOException {
              while (out.isReady()) {
                if (!content.hasRemaining()) {
                  response.setStatus(200);
                  async.complete();
                  return;
                }
                out.write(content.get());
//                out.write("async".getBytes());

              }
            }

            @Override
            public void onError(Throwable t) {
              //            logger.info("Server onError callled");
              getServletContext().log("Async Error", t);
              async.complete();
            }
          });
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
      if (request.getPathInfo().contains("async")) {
        asyncGet(request, response);
      } else {
        nonAsyncGet(request, response);
      }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
      // Read from request
      StringBuilder buffer = new StringBuilder();
      BufferedReader reader = request.getReader();
      String line;
      while ((line = reader.readLine()) != null) {
        buffer.append(line);
      }
      String data = buffer.toString();

      PrintWriter pout = response.getWriter();

      pout.print("<html><body>");
      pout.print("<h3>Hello Servlet Post</h3>");
      pout.print("</body></html>");
      return;
    }
  }

  public void handle(
      String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    response.setContentType("text/html;charset=utf-8");
    response.setStatus(HttpServletResponse.SC_OK);
    baseRequest.setHandled(true);
    response.getWriter().println("<h1>Hello World. default handle</h1>");
  }

  private static void initStatsExporter() throws IOException {
    HttpViews.registerAllServerViews();
    try {
      StackdriverStatsExporter.createAndRegister();
      StackdriverTraceExporter.createAndRegister(StackdriverTraceConfiguration.builder().build());
    } catch (Exception e) {
      //        logger.info("Failed to register Stackdriver exporter " + e.toString());
    }
    // Register Prometheus exporters and export metrics to a Prometheus HTTPServer.
    PrometheusStatsCollector.createAndRegister();
    HTTPServer prometheusServer = new HTTPServer(9090, true);
  }

  private static void initTracing() {
    TraceConfig traceConfig = Tracing.getTraceConfig();
    traceConfig.updateActiveTraceParams(
        traceConfig.getActiveTraceParams().toBuilder().setSampler(Samplers.alwaysSample()).build());

    LoggingTraceExporter.register();
    // Register Jaeger Tracing.
    JaegerTraceExporter.createAndRegister("http://localhost:14268/api/traces", "helloworldserver");
  }

  public static void main(String[] args) throws Exception {
    initTracing();
    initStatsExporter();
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");

    Server server = new Server(8080);
    ServletHandler handler = new ServletHandler();
    server.setHandler(handler);

    handler.addFilterWithMapping(
        OcHttpServletFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
    handler.addServletWithMapping(HelloServlet.class, "/*");

    server.start();
    server.join();
  }
}
