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
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import org.h2.server.web.DbStarter;
import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;
import static io.undertow.servlet.Servlets.listener;

public class Main {

    private static final String DB_URL = "jdbc:h2:./timesheet";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "sa";

    public static void main(String[] args) {
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
        Undertow server = Undertow.builder().addHttpListener(8000, "ec2-54-233-138-126.sa-east-1.compute.amazonaws.com").setHandler(servletHandler).build();
        server.start();
    }

    private static void initializeDatabase() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            ResultSet resultSet = conn.createStatement().executeQuery("show schemas");
            while(resultSet.next()) {
                int totalColumn = resultSet.getMetaData().getColumnCount();
                for (int i=1; i<=totalColumn; i++)
                    System.out.println("[" + i + "]: " + resultSet.getObject(i));
            }
            try {
                conn.createStatement().executeUpdate("create sequence ids");
            } catch (SQLException error) {
                if (!error.getMessage().contains("already exists"))
                    throw new RuntimeException(error);
            }
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

}