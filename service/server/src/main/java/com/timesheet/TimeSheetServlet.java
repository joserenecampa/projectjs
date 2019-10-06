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

import com.google.gson.Gson;

import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;

public class TimeSheetServlet extends HttpServlet {

    private static final long serialVersionUID = -7527127644015900442L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // loadComplete(req, resp);
        loadFromTimeseet2(req, resp);

    }

    private void loadFromTimeseet2(HttpServletRequest req, HttpServletResponse resp) {
        resp.setHeader("Content-Type", "application/json");
        Connection conn = (Connection) getServletContext().getAttribute("connection");
        String date = req.getParameter("date");
        try {
            StringBuilder items = new StringBuilder("[");
            PreparedStatement ps = conn.prepareStatement("select item from timesheet2 where date = ? and type = 'I' order by orderDb");
            ps.setString(1, date);
            ResultSet resultSet = ps.executeQuery();
            boolean inicio = true;
            while (resultSet.next()) {
                if (!inicio)
                    items.append(",");
                items.append(resultSet.getString(1));
                inicio = false;
            }
            items.append("]");
            StringBuilder groups = new StringBuilder("[");
            ps.close();
            ps = conn.prepareStatement("select item from timesheet2 where date = ? and type = 'G' order by orderDb");
            ps.setString(1, date);
            resultSet = ps.executeQuery();
            inicio = true;
            while (resultSet.next()) {
                if (!inicio)
                    groups.append(",");
                groups.append(resultSet.getString(1));
                inicio = false;
            }
            groups.append("]");
            resp.setStatus(200);
            resp.getWriter().print("{\"items\": " + items.toString() + ", \"groups\": " + groups.toString() + "}");
        } catch (Throwable error) {
            error.printStackTrace();
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-Type", "application/json");
        Connection conn = (Connection) getServletContext().getAttribute("connection");
        String itemId = req.getParameter("itemId");
        String date = req.getParameter("date");
        if (itemId != null && !itemId.isEmpty()) {
            deleteItem(conn, date, itemId, resp);
        } else {
            resp.setStatus(400);
            resp.getWriter().print("{\"error\": \"Falta informar o item\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Content-Type", "application/json");
        Connection conn = (Connection) getServletContext().getAttribute("connection");
        String item = req.getParameter("item");
        String date = req.getParameter("date");
        String items = req.getParameter("items");
        String groups = req.getParameter("groups");
        if (item != null && !item.isEmpty()) {

            saveOnlyOneItem(conn, date, item, resp);

        } else {
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
                        ps.close();
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
    }

    private void saveOnlyOneItem(Connection conn, String date, String item, HttpServletResponse resp) {
        this.saveOnlyOneItem(conn, date, item, resp, 1);
    }

    private void saveOnlyOneItem(Connection conn, String date, String item, HttpServletResponse resp, int tentativa) {
        Gson gson = new Gson();
        Item itemObject = gson.fromJson(item, Item.class);
        String itemId = itemObject.getId();
        String type = extractType(item);
        try {
            PreparedStatement ps = conn.prepareStatement("select item from timesheet2 where date = ? and itemId = ?");
            ps.setString(1, date);
            ps.setString(2, itemId);
            ResultSet resultSet = ps.executeQuery();
            if (resultSet.next()) {
                ps = conn.prepareStatement("update timesheet2 set item = ?, type = ? where date = ? and itemId = ?");
                ps.setCharacterStream(1, new InputStreamReader(new ByteArrayInputStream(item.getBytes())));
                ps.setString(2, type);
                ps.setString(3, date);
                ps.setString(4, itemId);
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
                ps.close();
                ps = conn.prepareStatement("insert into timesheet2 (date, itemId, orderDb, item, type) values (?,?,?,?,?)");
                ps.setString(1, date);
                ps.setString(2, itemId);
                ps.setInt(3, itemObject.getOrderDb());
                ps.setCharacterStream(4, new InputStreamReader(new ByteArrayInputStream(item.getBytes())));
                ps.setString(5, type);
                int total = ps.executeUpdate();
                if (total > 0) {
                    resp.setStatus(200);
                    resp.getWriter().print("{\"status\": \"data inserida com sucesso\"}");
                } else {
                    resp.setStatus(500);
                    resp.getWriter()
                            .print("{\"error\": \"nao foi possivel inserir os dados. nao retornou valor aceitavel\"}");
                }
            }
        } catch (JdbcSQLIntegrityConstraintViolationException error) {
            if (tentativa > 5) {
                throw new RuntimeException("Numero de tentativas foi excedido.", error);
            } else {
                this.saveOnlyOneItem(conn, date, item, resp, tentativa + 1);
            }
        } catch (Throwable error) {
            error.printStackTrace();
            resp.setStatus(500);
            try {
                resp.getWriter().print("{\"error\": \"" + error.getMessage() + "\"}");
            } catch (IOException e) {
            }
        }
    }

    private void deleteItem(Connection conn, String date, String itemId, HttpServletResponse resp) {
        // String itemId = extractItemId(item);
        try {
            PreparedStatement ps = conn.prepareStatement("delete from timesheet2 where date = ? and itemId = ?");
            ps.setString(1, date);
            ps.setString(2, itemId);
            int total = ps.executeUpdate();
            if (total == 1) {
                resp.setStatus(204);
            } else {
                resp.setStatus(500);
                resp.getWriter().print(
                        "{\"error\": \"nao foi possivel atualizar os dados pois nao retornou valor aceitavel\"}");
            }
        } catch (Throwable error) {
            error.printStackTrace();
            resp.setStatus(500);
            try {
                resp.getWriter().print("{\"error\": \"" + error.getMessage() + "\"}");
            } catch (IOException e) {
            }
        }
    }

    private String extractType(String item) {
        return item.contains("\"group\"") && (item.contains("\"typeOfWork\"") || item.contains("background")) ? "I"
                : "G";
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