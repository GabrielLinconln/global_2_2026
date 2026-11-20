package br.com.fiap.plantao;

import br.com.fiap.plantao.dao.ConnectionFactory;
import br.com.fiap.plantao.http.Servidor;
import br.com.fiap.plantao.service.InmetService;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Inicia o Plantao Clima. Rodar na pasta do projeto (onde estao sql/ e web/).
public class App {
    public static void main(String[] args) throws Exception {
        int porta = Integer.parseInt(System.getProperty("porta", "8080"));
        ConnectionFactory.inicializar();
        new Servidor().iniciar(porta);
        System.out.println("Plantao Clima rodando em http://localhost:" + porta);
        // avisos oficiais do Inmet: consulta ao iniciar e a cada 30 minutos; sem internet, o sistema segue com aviso manual
        InmetService inmet = new InmetService();
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(() -> {
            try {
                Map<String, Object> r = inmet.importar();
                System.out.println("Inmet " + r.get("consultadoEm") + ": " + r.get("mensagem"));
            } catch (RuntimeException e) {
                System.out.println("Inmet: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.MINUTES);
        // endereco para abrir no celular conectado ao mesmo Wi-Fi
        for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                    System.out.println("No celular (mesmo Wi-Fi): http://" + a.getHostAddress() + ":" + porta);
                }
            }
        }
    }
}
