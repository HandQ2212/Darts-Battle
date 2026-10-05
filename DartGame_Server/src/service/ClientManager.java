/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package service;

import java.util.ArrayList;

/**
 *
 * @author kaita
 */
public class ClientManager {
    ArrayList<Client> clients;

    public ClientManager() {
        clients = new ArrayList<>();
    }

    public boolean add(Client c) {
        if (!clients.contains(c)) {
            clients.add(c);
            return true;
        }
        return true;
    }

    public boolean remove(Client c) {
        if (clients.contains(c)) {
            clients.remove(c);
            return true;
        }
        return false;
    }
    public int getSize() {
        return clients.size();
    }
    public Client find(String username) {
        for (Client c : clients) {
            if (c.getLoginUser()!= null && c.getLoginUser().equals(username)) {
                return c;
            }
        }
        return null;
    }

    public void broadcast(String msg) {
        System.out.println("List Usee online: ");
        for (Client c : clients) {
            System.out.println(c.loginUser + " ");
        }
        clients.forEach((c) -> {
            c.sendData(msg);
        });
        System.out.println("===========================");
    }

    public void sendToAClient (String username, String msg) {
        clients.forEach((c) -> {
            if (c.getLoginUser().equals(username)) {
                c.sendData(msg);
            }
        });
    }
    public String getListUseOnline () {
        String result = "success;" + String.valueOf(clients.size()) + ";";
        for(int i = 0; i < clients.size(); i++) {
            result += clients.get(i).getLoginUser() + ";";
        }
        System.out.println("ReSult: "+ result);
        return result;
    }
}
