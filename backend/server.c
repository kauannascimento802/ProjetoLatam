/*
 * Micro-servidor HTTP estático para demonstração acadêmica.
 * Compilar: cc server.c -o server
 * Executar na raiz do projeto: ./backend/server
 */
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

static const char *mime(const char *path) {
    if (strstr(path, ".html")) return "text/html; charset=utf-8";
    if (strstr(path, ".css")) return "text/css; charset=utf-8";
    if (strstr(path, ".js")) return "application/javascript; charset=utf-8";
    return "application/octet-stream";
}

int main(void) {
    int server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (server_fd < 0) return 1;

    int opt = 1;
    setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    struct sockaddr_in addr = {0};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    addr.sin_port = htons(8081);

    if (bind(server_fd, (struct sockaddr*)&addr, sizeof(addr)) < 0) return 1;
    if (listen(server_fd, 16) < 0) return 1;

    printf("LATAM demo em http://127.0.0.1:8081\n");

    for (;;) {
        int client = accept(server_fd, NULL, NULL);
        if (client < 0) continue;

        char request[4096] = {0};
        read(client, request, sizeof(request) - 1);

        char method[16] = {0}, path[1024] = {0};
        sscanf(request, "%15s %1023s", method, path);

        if (strcmp(method, "GET") != 0) {
            close(client);
            continue;
        }

        if (strcmp(path, "/") == 0) strcpy(path, "/index.html");

        char full[2048];
        snprintf(full, sizeof(full), "./%s", path + 1);

        FILE *fp = fopen(full, "rb");
        if (!fp) {
            const char *msg = "HTTP/1.1 404 Not Found\r\nContent-Length: 18\r\n\r\nArquivo nao existe";
            send(client, msg, strlen(msg), 0);
            close(client);
            continue;
        }

        fseek(fp, 0, SEEK_END);
        long size = ftell(fp);
        rewind(fp);

        char header[256];
        int hlen = snprintf(header, sizeof(header),
            "HTTP/1.1 200 OK\r\nContent-Type: %s\r\nContent-Length: %ld\r\nConnection: close\r\n\r\n",
            mime(full), size);

        send(client, header, hlen, 0);

        char buffer[8192];
        size_t n;
        while ((n = fread(buffer, 1, sizeof(buffer), fp)) > 0) {
            send(client, buffer, n, 0);
        }

        fclose(fp);
        close(client);
    }

    close(server_fd);
    return 0;
}
