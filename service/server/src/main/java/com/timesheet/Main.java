package com.timesheet;

import static io.undertow.Handlers.path;
import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.listener;
import static io.undertow.servlet.Servlets.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletException;

import org.h2.server.web.DbStarter;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

public class Main {

    private static String DB_URL = null;
    private static String DB_USER = null;
    private static String DB_PASS = null;
    private static String SERVICE_PORT = null;
    private static String SERVICE_BIND = null;
    private static String ROOT_PATH = ".";
    private static String KEYSTORE_PATH = null;

    public static void main(String[] args) throws Throwable {
        initializeVariables();
        initializeDatabase();
        initializeServers();
    }

    private static void initializeServers() throws Throwable  {
        ServletInfo timeSheetServlet = servlet("TimeSheetServlet", TimeSheetServlet.class).addMapping("/ts");
        ListenerInfo databaseListener = listener(DbStarter.class);
        DeploymentInfo servletBuilder = deployment().setClassLoader(Main.class.getClassLoader()).setContextPath("/")
                .setDeploymentName("timesheet.war").setResourceManager(new PathResourceManager(Paths.get(Main.ROOT_PATH), 100))
                .addListener(databaseListener).addInitParameter("db.url", DB_URL).addInitParameter("db.user", DB_USER)
                .addInitParameter("db.password", DB_PASS).addWelcomePage("index.html")
                .addServlets(timeSheetServlet);
        DeploymentManager manager = defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        HttpHandler servletHandler = null;
        try {
            servletHandler = manager.start();
        } catch (ServletException e) {
        }
        int port = Integer.parseInt(SERVICE_PORT);
        String host = SERVICE_BIND;
        Undertow httpServer = Undertow.builder().addHttpsListener(port, host, Main.getSSLContext())
                .setHandler(servletHandler).build();
        httpServer.start();

        Undertow webSockerServer = Undertow.builder().addHttpsListener(Integer.parseInt(SERVICE_PORT) + 1, SERVICE_BIND, Main.getSSLContext())
                .setHandler(path().addPrefixPath("/",
                        new WebSocketProtocolHandshakeHandler(new WebSocketConnectionCallback() {
                            Set<WebSocketChannel> channels = new HashSet<WebSocketChannel>();
                            @Override
                            public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
                                channels.add(channel);
                                channel.getReceiveSetter().set(new AbstractReceiveListener() {

                                    @Override
                                    protected void onFullTextMessage(WebSocketChannel channel,
                                            BufferedTextMessage message) {
                                        String originalMessage = message.getData();
                                        for (WebSocketChannel c : channels) {
                                            if (!c.equals(channel)) {
                                                WebSockets.sendText(originalMessage, c, null);
                                            }
                                        }
                                    }
                                });
                                channel.resumeReceives();
                            }
                        })))
                .build();
        webSockerServer.start();

    }

    private static void initializeDatabase() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            try {
                conn.createStatement()
                        .executeUpdate("create table timesheet (date varchar(8) primary key, items clob, groups clob)");
            } catch (SQLException error) {
                if (!error.getMessage().contains("already exists"))
                    throw new RuntimeException(error);
            }
            try {
                conn.createStatement().executeUpdate(
                        "create table timesheet2 (date varchar(8), itemId varchar(36), orderDb int, item clob, type char, primary key (date, itemId))");
            } catch (SQLException error) {
                if (!error.getMessage().contains("already exists"))
                    throw new RuntimeException(error);
            }
            conn.close();
        } catch (Throwable error) {
            try {
                if (conn != null && !conn.isClosed())
                    conn.close();
            } catch (Throwable e) {
            }
            System.out.println("Erro ao inicializar o banco de dados.");
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static String initializeVariables(String variable) {
        String result = System.getenv(variable);
        if (result == null || result.isEmpty()) {
            result = System.getProperty(variable);
            if (result == null) {
                System.out.println(variable + ": NAO LOCALIZEI");
            } else {
                System.out.println("jvm -> " + variable + ": " + result);
            }
        } else {
            System.out.println("env -> " + variable + ": " + result);
        }
        return result;
    }

	private static SSLContext getSSLContext() throws Throwable {
		SSLContext sslContext = SSLContext.getDefault();
		sslContext = SSLContext.getInstance("TLSv1.2");
		String defaultAlgorithm = KeyManagerFactory.getDefaultAlgorithm();

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(defaultAlgorithm);
        
        KeyStore localKeyStore = KeyStore.getInstance("JKS");

        InputStream is = new FileInputStream(new File(Main.KEYSTORE_PATH));
        localKeyStore.load(is, "".toCharArray());

		keyManagerFactory.init(localKeyStore, "".toCharArray());

		KeyManager[] km = keyManagerFactory.getKeyManagers();

		TrustManager[] tm = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] c, String a) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] c, String a) throws CertificateException {
			}
		} };
		SecureRandom sr = new SecureRandom();
		sslContext.init(km, tm, sr);
		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
			public boolean verify(String h, SSLSession s) {
				return true;
			}
		});

		return sslContext;
	}


    private static void initializeVariables() {
        Main.DB_URL = initializeVariables("DB_URL");
        Main.DB_USER = initializeVariables("DB_USER");
        Main.DB_PASS = initializeVariables("DB_PASS");
        Main.SERVICE_PORT = initializeVariables("SERVICE_PORT");
        Main.SERVICE_BIND = initializeVariables("SERVICE_BIND");
        Main.ROOT_PATH = initializeVariables("ROOT_PATH");
        Main.KEYSTORE_PATH = initializeVariables("KEYSTORE_PATH");
    }

}