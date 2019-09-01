package com.timesheet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TimeSheetServlet extends HttpServlet {

    private static final long serialVersionUID = -7527127644015900442L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-Type", "application/json");
        Connection conn = (Connection) getServletContext().getAttribute("connection");
        String date = req.getParameter("date");
        try {
            PreparedStatement ps = conn.prepareStatement("select items, groups from timesheet where date = ?");
            ps.setString(1, date);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                resp.setStatus(200);
                String resultItems = resultSet.getString(1);
                String resultGroups = resultSet.getString(2);
                resp.setStatus(200);
                resp.getWriter().print("{\"items\": " + resultItems + ", \"groups\": " + resultGroups + "}");
        } else {
                resp.setStatus(404);
                resp.getWriter().print("{\"error\": \"Data nao encontrada\"}");
            }
        } catch (Throwable error) {
            error.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-Type", "application/json");
        Connection conn = (Connection) getServletContext().getAttribute("connection");
        String date = req.getParameter("date");
        String items = req.getParameter("items");
        String groups = req.getParameter("groups");
        if ((items == null || items.isEmpty()) || (groups == null || groups.isEmpty())) {
            resp.setStatus(400);
            resp.getWriter().print("{\"error\": \"Faltam informar os items ou os grupos\"}");
        } else {
            try {
                PreparedStatement ps = conn.prepareStatement("select items, groups from timesheet where date = ?");
                ps.setString(1, date);
                ResultSet resultSet = ps.executeQuery();
                if (resultSet.next()) {
                    ps = conn.prepareStatement("update timesheet set items = ?, groups = ? where date = ?");
                    ps.setCharacterStream(1, new InputStreamReader(new ByteArrayInputStream(items.getBytes())));
                    ps.setCharacterStream(2, new InputStreamReader(new ByteArrayInputStream(groups.getBytes())));
                    ps.setString(3, date);
                    int total = ps.executeUpdate();
                    if (total > 0) {
                        resp.setStatus(200);
                        resp.getWriter().print("{\"status\": \"data atualizada com sucesso\"}");
                    } else {
                        resp.setStatus(500);
                        resp.getWriter().print(
                                "{\"error\": \"nao foi possivel atualizar os dados pois nao retornou valor aceitavel\"}");
                    }
                } else {
                    ps = conn.prepareStatement("insert into timesheet (date, items, groups) values (?,?,?)");
                    ps.setString(1, date);
                    ps.setCharacterStream(2, new InputStreamReader(new ByteArrayInputStream(items.getBytes())));
                    ps.setCharacterStream(3, new InputStreamReader(new ByteArrayInputStream(groups.getBytes())));
                    int total = ps.executeUpdate();
                    if (total > 0) {
                        resp.setStatus(200);
                        resp.getWriter().print("{\"status\": \"data inserida com sucesso\"}");
                    } else {
                        resp.setStatus(500);
                        resp.getWriter().print(
                                "{\"error\": \"nao foi possivel inserir os dados. nao retornou valor aceitavel\"}");
                    }
                }
            } catch (Throwable error) {
                error.printStackTrace();
                resp.setStatus(500);
                resp.getWriter().print("{\"error\": \"" + error.getMessage() + "\"}");
            }
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Allow", "OPTIONS, GET, HEAD, POST, DELETE");
        resp.addHeader("Cache-Control", "max-age=604800");
        resp.addHeader("Access-Control-Request-Method", "OPTIONS, GET, HEAD, POST, DELETE");
        resp.addHeader("Access-Control-Allow-Origin", "*");
        resp.setStatus(204);
    }

}