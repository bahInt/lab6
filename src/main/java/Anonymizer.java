import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.stream.ActorMaterializer;

public class Anonymizer {

    public static void main(String[] args) {
        ActorSystem system = ActorSystem.create("routes");
        Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
    }
}
