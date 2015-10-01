/*
 * relayclient.h
 *
 *  Created on: 2014/12/3
 *      Author: lintzuhsiu
 */

#ifndef RELAYCLIENT_H
#define RELAYCLIENT_H

#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <pthread.h>

typedef struct RelayMessage_st {
	unsigned char command[2];
	unsigned char uuid[32];
	unsigned char auth[32];
} RelayMessage_t;

class RelayClient {
public:
	static void *reveive_message_dispatcher(void* obj) {
		return ((RelayClient *)obj)->receive_message();
	}

	RelayClient(const char* serverIP, const int serverPort);
	bool connect_to_relay_server();
	bool auth(bool isIPCam, const char *uuid, const char *auth);
	bool send_message(const char *msg, const int length);
	void *receive_message();
	void set_receive_callback(void (*callback)(char *, int));
	int* get_sockfd();
	void close_socket();
	~RelayClient();
private:
	int sockfd;
	struct sockaddr_in server;
	char *serverIP;
	int serverPort;
	void (*receiveCallback)(char *, int);
	bool isStop;
	pthread_t receiveMessageThreadID;
};

#endif /* RELAYCLIENT_H_ */
