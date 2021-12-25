package lab6;

import java.util.ArrayList;

public class RefreshServerMessage {
    private final ArrayList<String> servers;

    public RefreshServerMessage(ArrayList<String> servers) {
        this.servers = servers;
    }

    public ArrayList<String> getServers() {
        return servers;
    }
}
