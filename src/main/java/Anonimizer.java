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
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;

public class Anonimizer {
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final int CLIENT_PORT = 8080;
    private static final int TIMEOUT = 3000;
    private static final Object LOG_SOURCE = System.out;
    private static final Duration timeout = Duration.ofSeconds(5);
    private static ZooKeeper keeper;
    private static ActorRef configStorageActor;
    private static Http http;
    private static LoggingAdapter l;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        ActorSystem system = ActorSystem.create("routes");
        l = Logging.getLogger(system, LOG_SOURCE);
        configStorageActor = system.actorOf(Props.create(ConfigStorageActor.class));
        initZooKeeper();
        http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost(HOST, PORT),
                materializer
        );
        l.info("Server online at http://{}:{}/\n", HOST, PORT);
        System.in.read();
        binding.thenCompose(ServerBinding::unbind).thenAccept(unbound -> system.terminate());
    }

    public static Route createRoute(){
        return route(get(() ->
            parameter("url", url ->
                    parameter("count", count -> {
                        if(Integer.parseInt(count) <= 0) return completeWithFuture(fetch(url));
                        return completeWithFuture(Patterns.ask(configStorageActor, new GetNextServer(), timeout)
                                .thenApply(nextPort -> (String)nextPort)
                                .thenCompose(nextPort -> fetch(String.format("http://localhost:%s?url=%s&count=%d", nextPort, url, Integer.parseInt(count) - 1))));
                        }))
        ));
    }

    private static CompletionStage<HttpResponse> fetch(String url) {
        return http.singleRequest(HttpRequest.create(url));
    }
    public static void initZooKeeper() throws IOException, InterruptedException, KeeperException {
        keeper = new ZooKeeper(HOST + ":" + CLIENT_PORT, TIMEOUT, watcher);
        l.info("Creating servers on port {}", PORT);
        keeper.create("/servers/" + PORT, (PORT + "").getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        WatchedEvent event = new WatchedEvent(Watcher.Event.EventType.NodeCreated, Watcher.Event.KeeperState.SyncConnected, "");
        watcher.process(event);
    }

    public static Watcher watcher = watchedEvent -> {
        ArrayList<String> newServers = new ArrayList<>();

        try {
            for(String s: keeper.getChildren("/servers", false, null)) {
                byte[] port = keeper.getData("servers/" + s, false, null);
                newServers.add(new String(port));
            }
            configStorageActor.tell(new RefreshServerMessage(newServers), ActorRef.noSender());
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    };
}
