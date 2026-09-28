/*
 * Micro-servidor HTTP estático para demonstração acadêmica.
 * Compilar: g++ -std=c++17 server.cpp -o server_cpp
 * Executar na raiz do projeto: ./backend/server_cpp
 */
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <sstream>
#include <string>

namespace fs = std::filesystem;

bool endsWith(const std::string& value, const std::string& suffix) {
    return value.size() >= suffix.size() &&
           value.compare(value.size() - suffix.size(), suffix.size(), suffix) == 0;
}

std::string mime(const std::string& path) {
    if (endsWith(path, ".html")) return "text/html; charset=utf-8";
    if (endsWith(path, ".css")) return "text/css; charset=utf-8";
    if (endsWith(path, ".js")) return "application/javascript; charset=utf-8";
    return "application/octet-stream";
}

int main() {
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0) return 1;

    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    addr.sin_port = htons(8082);

    if (bind(server_fd, reinterpret_cast<sockaddr*>(&addr), sizeof(addr)) < 0) return 1;
    if (listen(server_fd, 16) < 0) return 1;

    std::cout << "LATAM demo em http://127.0.0.1:8082\n";

    while (true) {
        int client = accept(server_fd, nullptr, nullptr);
        if (client < 0) continue;

        char buffer[4096]{};
        read(client, buffer, sizeof(buffer) - 1);

        std::istringstream req(buffer);
        std::string method, path, protocol;
        req >> method >> path >> protocol;

        if (method != "GET") {
            close(client);
            continue;
        }

        if (path == "/") path = "/index.html";

        fs::path file = fs::path(".") / path.substr(1);
        if (!fs::exists(file) || fs::is_directory(file)) {
            std::string msg = "HTTP/1.1 404 Not Found\r\nContent-Length: 18\r\n\r\nArquivo nao existe";
            send(client, msg.c_str(), msg.size(), 0);
            close(client);
            continue;
        }

        std::ifstream in(file, std::ios::binary);
        std::string body((std::istreambuf_iterator<char>(in)), std::istreambuf_iterator<char>());

        std::ostringstream header;
        header << "HTTP/1.1 200 OK\r\n"
               << "Content-Type: " << mime(file.string()) << "\r\n"
               << "Content-Length: " << body.size() << "\r\n"
               << "Connection: close\r\n\r\n";

        auto h = header.str();
        send(client, h.c_str(), h.size(), 0);
        send(client, body.data(), body.size(), 0);
        close(client);
    }

    close(server_fd);
    return 0;
}
