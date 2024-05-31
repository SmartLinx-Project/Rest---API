package rest;

import java.io.IOException;
import java.net.URI;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import schedule.Schedule;

/**
TODO:
 <li><s> sistemare la parte del setUser e delUser per ottenere la mail dal token e non dal body </s></li>
 <li><s> controllare che quando si aggiunge un membro famiglia esso esista già nella tabella user con una subquery </s></li>
 <li><s> modificare il database aggiungendo un hubID nella tabella home, impostando il campo come UNIQUE in modo che lo
      stesso hub possa essere associato a una sola casa, e controllando che quando si aggiunge l'hub esso sia connesso</s></li>
 <li> creare il thread per gestire la schedulazione </li>
*/

public class Main {
    static final String IP = "example ip";
    static final int PORT = 5055;

    public static void main(String[] args) throws IOException {
        URI serverAddress = URI.create("https://" + IP + ":" + PORT + "/");

        // Configura il contesto SSL
        SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();
        sslContextConfig.setKeyStoreFile("resources/keystore.jks"); // Percorso del tuo keystore
        sslContextConfig.setKeyStorePass("example-pass"); // Password del tuo keystore

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(
                serverAddress,
                new ResourceConfig(RestResources.class),
                true, // Abilita HTTPS
                new SSLEngineConfigurator(sslContextConfig).setClientMode(false).setNeedClientAuth(false)
        );
        server.start();
        System.out.println("Server avviato su " + serverAddress);

        Schedule.polling();
    }
}