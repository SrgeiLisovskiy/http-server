package ru.netology;

public class Main {
    private final static int PORT = 9999;
    private final static int SIZE_THREADS = 64;

    public static void main(String[] args) {
        Server server = new Server(PORT, SIZE_THREADS);
        server.start();
    }
}