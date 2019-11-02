package com.timesheet.importer;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import com.google.gson.Gson;

public class Main {

    private static String URL = null;
    private static String DIR = null;
    private static String ZONE = null;
    private static ArrayList<Item> ITEMS = new ArrayList<Item>();

    private String mountDate(String date, String hour) {
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(4, 6)) - 1;
        int dat = Integer.parseInt(date.substring(6, 8));
        int hrs = Integer.parseInt(hour.substring(0, 2));
        int min = Integer.parseInt(hour.substring(2, 4));
        Calendar cal = Calendar.getInstance();
        String zone = null;
        try {
            String[] zones = TimeZone.getAvailableIDs(Integer.parseInt(Main.ZONE)*60*60*1000);
            if (zones != null && zones.length > 0) {
                zone = zones[0];
            } else {
                zone = "GMT";
            }
        } catch (Throwable error) {
            zone= "GMT";
        }
        cal.setTimeZone(TimeZone.getTimeZone(zone));
        cal.set(year, month, dat, hrs, min);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.toInstant().toString();
    }

    private String persistPushback(String date, String employeeId, String start, String end) {
        Item item = new Item();
        item.setGroup(employeeId);
        item.setStart(mountDate(date, start));
        item.setEnd(mountDate(date, end));
        item.setOrderDb(0);
        item.setOpen(false);
        item.setClassName("aquamarine");
        item.setTypeOfWork("Push");
        item.setId(item.getGroup() + "-" + item.getTypeOfWork().toLowerCase() + "-" + item.getOrderDb());
        try {
            this.postData(date, item, "PERSIST", "I", false);
            return item.getId();
        } catch (Throwable error) {
            System.out.println("Error on insert employee.");
            error.printStackTrace();
            return null;
        }
    }

    private String persistScheduler(String date, String employeeId, String start, String end) {
        Item item = new Item();
        item.setGroup(employeeId);
        item.setStart(mountDate(date, start));
        item.setEnd(mountDate(date, end));
        item.setOrderDb(0);
        item.setType("background");
        item.setId(item.getGroup() + "-" + item.getType() + "-" + item.getOrderDb());
        try {
            this.postData(date, item, "PERSIST", "I", false);
            return item.getId();
        } catch (Throwable error) {
            System.out.println("Error on insert employee.");
            error.printStackTrace();
            return null;
        }
    }

    private String persistEmployee(String date, String id, String employeeName, String sector) {
        if ("Cash_Sales".equals(sector)) {
            sector = "Cash and Sale";
        } else if ("Management".equals(sector)) {
            sector = "Wipedown";
        }
        Item item = new Item();
        item.setId(id);
        item.setEmployeeName(employeeName);
        item.setOrderDb(0);
        item.setChecked(false);
        item.setClassName("p");
        item.setContent("<input class='form-check-input' onclick='checkme(this,\"" + item.getId()
                + "\");' type='checkbox' >" + item.getEmployeeName());
        item.setNestedInGroup(sector != null ? sector : "Wipedown");
        try {
            this.postData(date, item, "PERSIST", "G", true);
            return item.getId();
        } catch (Throwable error) {
            System.out.println("Error on insert employee.");
            error.printStackTrace();
            return null;
        }
    }

    private void postData(String date, Item item, String action, String itemType, boolean importer) throws Throwable {
        HttpPost post = new HttpPost(Main.URL);
        Gson gson = new Gson();
        String json = gson.toJson(item);
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("date", date));
        if (importer) {
            urlParameters.add(new BasicNameValuePair("importer", "true"));
        }
        urlParameters.add(new BasicNameValuePair("item", json));
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        try (CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build(); CloseableHttpResponse response = httpClient.execute(post)) {
            int httpResponseCode = response.getStatusLine().getStatusCode();
            if (httpResponseCode > 399) {
                System.err.println("Error on Post [HTTP " + httpResponseCode + "] Data: Date " + date + " - Item: "
                        + json + ". Response: " + response.getStatusLine().getReasonPhrase());
            } else {
                ByteArrayOutputStream os = new ByteArrayOutputStream((int) response.getEntity().getContentLength());
                response.getEntity().writeTo(os);
                String statusString = new String(os.toByteArray());
                Status status = gson.fromJson(statusString, Status.class);
                System.out.println("Post [HTTP " + httpResponseCode + "] Success. Data: Date " + date + " - Item: "
                        + json + ". Response: " + status.getStatus());
                WebSocketMessage message = new WebSocketMessage();
                message.setItem(item);
                message.setAction(action);
                message.setType(itemType);
                this.sendBroadcastMessage(message);
            }
        }
    }

    private void sendBroadcastMessage(WebSocketMessage message) throws Throwable {
        WebSocketClient client = new WebSocketClient(new URI("wss://localhost/ws")) {
            public void onOpen(ServerHandshake handshakedata) {
            }

            public void onMessage(String message) {
            }

            public void onClose(int code, String reason, boolean remote) {
            }

            public void onError(Exception ex) {
            }
        };
        if (client.connectBlocking()) {
            Gson gson = new Gson();
            message.setFrom("importer");
            client.send(gson.toJson(message));
            client.close();
        }
    }

    public void watchFiles() throws Throwable {
        WatchService watcher = FileSystems.getDefault().newWatchService();
        Path daySheetFile = Paths.get(Main.DIR);
        try {
            daySheetFile.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
        } catch (IOException x) {
            System.err.println(x);
        }
        for (;;) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == OVERFLOW) {
                    continue;
                }
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();
                Path absolute = Paths.get(Main.DIR, filename.toFile().getName());
                if (filename.toFile().getName().toLowerCase().contains("todayactivities")) {
                    loadDaySheetFromFile(absolute);
                    importerDayShift();
                } else if (filename.toFile().getName().toLowerCase().contains("employee")) {
                    loadEmployeesFromFile(absolute);
                }
            }
            boolean valid = key.reset();
            if (!valid) {
                break;
            }
        }
    }

    private void importerDayShift() {
        if (Main.ITEMS == null || Main.ITEMS.size() < 1) {
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        String now = cal.toInstant().toString();
        String date = now.substring(0, 4) + now.substring(5, 7) + now.substring(8, 10);
        System.out.println("Starting importer date " + date + ".");
        for (Item item : Main.ITEMS) {
            if (item.getStart() != null && !item.getStart().isEmpty()) {
                String id = persistEmployee(date, item.getId(), item.getEmployeeName(), item.getGroup());
                String schedulerId = persistScheduler(date, item.getId(), item.getStart(), item.getEnd());    
            }
        }
    }

    private boolean loadDaySheetFromFile(Path file) {
        if (Main.ITEMS != null && Main.ITEMS.size() > 0) {
            try {
                List<String> allLines = Files.readAllLines(file);
                for (String line : allLines) {
                    String[] objects = line.split("\\|");
                    String itemId = objects[1];
                    String start = objects[3];
                    String end = objects[5];
                    String pushBack = objects[2];
                    String sectorDay = objects[11];
                    String canceled = objects[10];
                    Item itemToRemove = null;
                    for (Item item : Main.ITEMS) {
                        if (item.getId().equals(itemId)) {
                            if ("C".equals(canceled) || "S".equals(canceled)) {
                                itemToRemove = item;
                            } else {
                                item.setStart(start);
                                item.setEnd(end);
                                if ("P".equals(sectorDay)) {
                                    item.setGroup("Prep");
                                }
                            }
                            break;
                        }
                    }
                }
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean loadEmployeesFromFile(Path file) {
        try {
            List<String> allLines = Files.readAllLines(file);
            Main.ITEMS = new ArrayList<Item>(allLines.size());
            for (String line : allLines) {
                String[] objects = line.split("\\|");
                if ((objects != null && objects.length > 2) && (objects[0] != "" || objects[2] != null)) {
                    Item item = new Item();
                    item.setId(objects[0]);
                    item.setEmployeeName(objects[2]);
                    String sector = objects[12];
                    if (sector == null || sector.isEmpty()) {
                        sector = "Wipedown";
                    } else if (sector.equals("Cash_Sales")) {
                        sector = "Cash and Sale";
                    } else if (sector.equals("Management")) {
                        sector = "Wipedown";
                    }
                    item.setGroup(sector);
                    Main.ITEMS.add(item);
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String getVar(String variable) {
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
        Main.URL = getVar("URL");
        Main.DIR = getVar("DIR");
        Main.ZONE = getVar("ZONE");
        if (Main.URL == null) {
            Main.URL = "https://localhost/ts";
        }
        if (Main.DIR == null) {
            Main.DIR = "d:\\dev\\junior\\github\\projectjs\\arquivos\\";
        }
        if (Main.ZONE == null) {
            Main.ZONE = "-3";
        }
    }

    public static void main(String[] args) throws Throwable {
        initializeVariables();
        Main main = new Main();
        main.watchFiles();
    }

}