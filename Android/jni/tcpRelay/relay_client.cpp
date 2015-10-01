/*
 * relayclient.cpp
 *
 *  Created on: 2014/12/3
 *      Author: lintzuhsiu
 */
#include "relay_client.h"

RelayClient::RelayClient(const char *serverIP, const int serverPort) {
    memcpy(&(this->serverIP), &serverIP, strlen(serverIP)+1);
    this->serverPort = serverPort;
    this->receiveCallback = NULL;
    this->isStop = false;
}

RelayClient::~RelayClient() {
    this->close_socket();
}

bool RelayClient::connect_to_relay_server() {
	pthread_attr_t attrs;

	// create sockfd
    this->sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (this->sockfd == -1) {
        perror("Cloud not create socket");
    }

    // connect to tcp relay server
    server.sin_family = AF_INET;
    server.sin_addr.s_addr = inet_addr(this->serverIP);
    server.sin_port = htons(this->serverPort);
    if (connect(this->sockfd, (struct sockaddr *)&server, sizeof(server)) < 0) {
        perror("Could not connect to relay server");
        return false;
    }

    // create receive message thread
    pthread_attr_init(&attrs);
    pthread_attr_setdetachstate(&attrs, PTHREAD_CREATE_DETACHED);
    if (pthread_create(&(this->receiveMessageThreadID), &attrs, RelayClient::reveive_message_dispatcher, this) < 0) {
    	return false;
    }

    return true;
}

bool RelayClient::auth(bool isIPCam, const char *uuid, const char *auth) {
    RelayMessage_t sendMessage;
    char sendBuf[200];

    memset(&sendMessage, 0, sizeof(RelayMessage_t));
    memset(&sendBuf, 0, sizeof(sendBuf));

    sendMessage.command[0] = 0x31;
    sendMessage.command[1] = isIPCam == true ? 0x2d : 0x31;
    memcpy(&(sendMessage.uuid), uuid, strlen(uuid)+1);
    memcpy(&(sendMessage.auth), auth, strlen(auth)+1);

    memcpy(&sendBuf, &sendMessage, sizeof(RelayMessage_t));

    if (send(this->sockfd, sendBuf, strlen(sendBuf), 0) < 0) {
        perror("Send message fail");
        return false;
    }

    return true;
}

bool RelayClient::send_message(const char *buff, const int buffLen) {
	if (send(this->sockfd, buff, buffLen, 0) < 0) {
		perror("Send message fail");
		return false;
	}
	return true;
}

void *(RelayClient::receive_message)() {
	int status, bufLength;
	char receiveBuf[3000];

	while (!this->isStop) {
		memset(&receiveBuf, 0, sizeof(receiveBuf));

		bufLength = recv(this->sockfd, receiveBuf, sizeof(receiveBuf), 0);

		if (bufLength > 0) {
			if (this->receiveCallback != NULL) {
				this->receiveCallback(receiveBuf, bufLength);
			}
		}
		else {
			this->isStop = true;
		}
	}
}

void RelayClient::set_receive_callback(void (*callback)(char *, int)) {
	this->receiveCallback = callback;
}

void RelayClient::close_socket() {
	this->isStop = true;
    close(this->sockfd);
}

int* RelayClient::get_sockfd() {
	return &(this->sockfd);
}

