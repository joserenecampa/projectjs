package com.timesheet;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.ServletInfo;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.servlet.ServletException;
import org.h2.server.web.DbStarter;
import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;
import static io.undertow.servlet.Servlets.listener;

public class Main {

    private static String DB_URL = null;
    private static String DB_USER = null;
    private static String DB_PASS = null;
    private static String SERVICE_PORT = null;
    private static String SERVICE_BIND = null;

    public static void main(String[] args) {
        initializeVariables();
        initializeDatabase();
        initializeServerHTTP();
    }

    private static void initializeServerHTTP() {
        ServletInfo timeSheetServlet = servlet("TimeSheetServlet", TimeSheetServlet.class).addMapping("/ts");
        ListenerInfo databaseListener = listener(DbStarter.class);
        DeploymentInfo servletBuilder = deployment().setClassLoader(Main.class.getClassLoader()).setContextPath("/")
                .setDeploymentName("timesheet.war").setResourceManager(new PathResourceManager(Paths.get("."), 100))
                .addListener(databaseListener).addInitParameter("db.url", DB_URL)
                .addInitParameter("db.user", DB_USER).addInitParameter("db.password", DB_PASS).addServlets(timeSheetServlet);
        DeploymentManager manager = defaultContainer().addDeployment(servletBuilder);
        manager.deploy();
        HttpHandler servletHandler = null;
        try {
            servletHandler = manager.start();
        } catch (ServletException e) {
        }
        Undertow server = Undertow.builder().addHttpListener(Integer.parseInt(SERVICE_PORT), SERVICE_BIND).setHandler(servletHandler).build();
        server.start();
    }

    private static void initializeDatabase() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            try {
                conn.createStatement().executeUpdate("create table timesheet (date varchar(8) primary key, items clob, groups clob)");
            } catch (SQLException error) {
                if (!error.getMessage().contains("already exists"))
                    throw new RuntimeException(error);
            }
            conn.close();
        } catch (Throwable error) {
            try { if (conn != null && !conn.isClosed()) conn.close(); } catch (Throwable e) {}
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

    private static void initializeVariables() {
        Main.DB_URL = initializeVariables("DB_URL");
        Main.DB_USER = initializeVariables("DB_USER");
        Main.DB_PASS = initializeVariables("DB_PASS");
        Main.SERVICE_PORT = initializeVariables("SERVICE_PORT");
        Main.SERVICE_BIND = initializeVariables("SERVICE_BIND");
    }

}