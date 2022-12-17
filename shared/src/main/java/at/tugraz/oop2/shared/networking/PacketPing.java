package at.tugraz.oop2.shared.networking;

import java.io.Serializable;

public class PacketPing implements Serializable {
    public long time = System.currentTimeMillis();
}
