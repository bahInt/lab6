import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CompletionStage;

public class Anonimizer {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final int CLIENT_PORT = 8080;
    private static final int TIMEOUT = 3000;
    private static final Object LOG_SOURCE = System.out;
    private static ZooKeeper keeper;

    public static void main(String[] args) throws IOException {
        ActorSystem system = ActorSystem.create("routes");
        LoggingAdapter l = Logging.getLogger(system, LOG_SOURCE);
        ActorRef cachingActor = system.actorOf(Props.create(ConfigStorageActor.class));
        initZooKeeper();
        Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createFlow(http, system, materializer, actor);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost(HOST, PORT),
                materializer
        );
        l.info("Server online at http://{}:{}/\n", HOST, PORT);
        System.in.read();
        binding.thenCompose(ServerBinding::unbind).thenAccept(unbound -> system.terminate());
    }

    public static Route createRoute(){}

    public static void initZooKeeper() throws IOException {
        keeper = new ZooKeeper(HOST + ":" + CLIENT_PORT, TIMEOUT, watcher);
    }

    public static Watcher watcher = watchedEvent -> {
        ArrayList<String> newServers = new ArrayList<>();

    }
}
