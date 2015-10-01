/*
 * stunner.h
 *
 *  Created on: 2014/12/5
 *      Author: lintzuhsiu
 */

#ifndef STUNNER_H_
#define STUNNER_H_

class Stunner {
public:
	Stunner(char *friendName, char *serverIP, int serverPort);
	bool init_socket();
	bool get_public_and_port();
	void try_to_stun();
	void set_public_ip_port_callback(void (*callback)(char *friendName, char *ip, char *port));
	~Stunner();
private:
	char *friendName;
	char *serverIP;
	int *serverPort;

};

#endif /* STUNNER_H_ */
