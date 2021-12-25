import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

import java.util.ArrayList;

public class ConfigStorageActor extends AbstractActor {
    ArrayList<String> servers = new ArrayList<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(RefreshServerMessage.class, msg -> {
                    servers = msg.getServers();
                })
                .match(GetNextServer.class, msg -> {
                    
                })
                .build();
    }
}
