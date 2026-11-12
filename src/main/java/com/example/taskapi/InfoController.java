package com.example.taskapi;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {

    @Value("${spring.application.name}")
    private String name;

    @Value("${app.version}")
    private String version;

    /** Name, version and the pod/host serving the request - handy to see load balancing on GKE. */
    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of("name", name, "version", version, "host", hostname());
    }

    private static String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
