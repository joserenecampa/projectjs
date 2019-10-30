package com.timesheet.importer;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import com.google.gson.Gson;


public class Main {

    private String mountDate(String date, String hour) {
        int year = Integer.parseInt(date.substring(0, 4));
        int month = Integer.parseInt(date.substring(4, 6))-1;
        int dat = Integer.parseInt(date.substring(6, 8));
        int hrs = Integer.parseInt(hour.substring(0, 2));
        int min = Integer.parseInt(hour.substring(2, 4));
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
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
            this.postData(date, item);
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
            this.postData(date, item);
            return item.getId();
        } catch (Throwable error) {
            System.out.println("Error on insert employee.");
            error.printStackTrace();
            return null;
        }
    }

    private String persistEmployee(String date, String id, String employeeName, String sector) {
        Item item = new Item();
        item.setId(id);
        item.setEmployeeName(employeeName);
        item.setOrderDb(0);
        item.setChecked(false);
        item.setClassName("p");
        item.setContent("<input class='form-check-input' onclick='checkme(this,\"" + item.getId() + "\");' type='checkbox' >" + item.getEmployeeName());
        item.setNestedInGroup(sector!=null?sector:"Wipedown");
        try {
            this.postData(date, item);
            return item.getId();
        } catch (Throwable error) {
            System.out.println("Error on insert employee.");
            error.printStackTrace();
            return null;
        }
    }

    private void postData(String date, Item item) throws Throwable {
        HttpPost post = new HttpPost("https://localhost/ts");
        Gson gson = new Gson();
        List<NameValuePair> urlParameters = new ArrayList<>();
        urlParameters.add(new BasicNameValuePair("date", date));
        urlParameters.add(new BasicNameValuePair("item", gson.toJson(item)));
        post.setEntity(new UrlEncodedFormEntity(urlParameters));
        try (CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
             CloseableHttpResponse response = httpClient.execute(post)) {
        }
    }

    public static void main(String[] args) throws Throwable {

        String id = "99454";
        String employeeName = "Jose Rene T.";
        String sector = "Wipedown";
        String date = "20191030";
        String start = "0800";
        String end = "1630";
        String pushStart = "0800";
        String pushEnd = "0900";

        Main main = new Main();
        id = main.persistEmployee(date, id, employeeName, sector);
        String schedulerId = main.persistScheduler(date, id, start, end);
        String pushId = main.persistPushback(date, id, pushStart, pushEnd);
        System.out.println(schedulerId);
        System.out.println(pushId);
    }


}