#include <jni.h>
#include <android/log.h>
#include <string.h>

#include "moremote.p2p_ITCPRelayLibrary.h"
#include "relay_client.h"

#define LOG_TAG "relay"
#define LOGD(...)__android_log_print( ANDROID_LOG_DEBUG,LOG_TAG, __VA_ARGS__ )

static JavaVM *androidJVM = NULL;
static jclass interfaceClass = NULL;
static jobject caller = NULL;
static JNINativeMethod gMethods[] = {
	"connectToRelayServer", "(Ljava/lang/String;I)Z", (void *)Java_moremote.p2p_ITCPRelayLibrary_connectToRelayServer,
	"close", "()V", (void *)Java_moremote.p2p_ITCPRelayLibrary_close,
	"auth", "(ZLjava/lang/String;Ljava/lang/String;)Z", (void *)Java_moremote.p2p_ITCPRelayLibrary_auth,
	"sendMessage", "([BI)Z", (void *)Java_moremote.p2p_ITCPRelayLibrary_sendMessage
};

RelayClient *relayClient;

void receiveMessageCallback(char *message,int len)
{
	JNIEnv *env;
	bool isAttached = false;

	if (androidJVM->GetEnv((void **)&env, JNI_VERSION_1_4) < 0) {
		if (androidJVM->AttachCurrentThread(&env, NULL) < 0) {
			LOGD("fail to attach");
			return;
		}

		LOGD("attached to main thread");
		isAttached = true;
	}

	jmethodID method = env->GetMethodID(env->GetObjectClass(caller), "receiveMessage", "([B)V");
	if (!method) {
		LOGD("could not get method");
		if (isAttached) {
			androidJVM->DetachCurrentThread();
		}
		return;
	}

	LOGD("receive message, %d", len);

    int sockfd;
    sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if(sockfd < 0) {
		LOGD("socket");
		return;
    }

    struct sockaddr_in target;
	memset((char *)&target, 0, sizeof(struct sockaddr_in));
    target.sin_family = AF_INET;
    target.sin_port = htons(5006);
    target.sin_addr.s_addr = inet_addr("192.168.0.180");
    int addr_len = sizeof(struct sockaddr_in);

    sendto((int)sockfd, message+66, len-66, 0, (struct sockaddr*)&target, addr_len) ;


	/*jbyte *messageBuf = (jbyte *) message;
	jbyteArray jarray = env->NewByteArray(strlen(message)+1);
	env->SetByteArrayRegion(jarray, 0, strlen(message)+1, messageBuf);

	env->CallVoidMethod(caller, method, jarray);*/

	if (isAttached) {
		androidJVM->DetachCurrentThread();
	}
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
	jclass clz;
	int methodLength;
	JNIEnv *env;

	// to get jni environment
	if((vm->GetEnv((void **)&env, JNI_VERSION_1_4)) != JNI_OK) {
		return JNI_ERR;
	}

	// find class
	clz = env->FindClass("moremote.p2p/ITCPRelayLibrary");

	// register methods
	methodLength = (int)(sizeof(gMethods) / sizeof(gMethods[0]));
	if (env->RegisterNatives(clz, gMethods, methodLength) < 0) {
		return JNI_ERR;
	}

	// set global variable
	androidJVM = vm;
	interfaceClass = clz;

	return JNI_VERSION_1_4;
}

JNIEXPORT jboolean JNICALL Java_moremote.p2p_ITCPRelayLibrary_connectToRelayServer
  (JNIEnv *env, jobject thiz, jstring jserverip, jint jserverport)
{
	const char *serverIP = env->GetStringUTFChars(jserverip, 0);
	int serverPort = (int) jserverport;

	caller = env->NewGlobalRef(thiz);

	relayClient = new RelayClient(serverIP, serverPort);
    if (!relayClient->connect_to_relay_server()) {
    	LOGD("ERROR");
    }

	relayClient->set_receive_callback(&receiveMessageCallback);
}

JNIEXPORT jboolean JNICALL Java_moremote.p2p_ITCPRelayLibrary_auth
  (JNIEnv *env, jobject thiz, jboolean jisIPCam, jstring juuid, jstring jauth)
{
	const char *uuid = env->GetStringUTFChars(juuid, 0);
	const char *auth = env->GetStringUTFChars(jauth, 0);

	relayClient->auth((bool)jisIPCam, uuid, auth);
}

JNIEXPORT jboolean JNICALL Java_moremote.p2p_ITCPRelayLibrary_sendMessage
  (JNIEnv *env, jobject thiz, jbyteArray jbuff, jint jbuffLen)
{
	signed char *buff = env->GetByteArrayElements(jbuff, 0);
	bool result = relayClient->send_message((const char *)buff, jbuffLen);
	env->ReleaseByteArrayElements(jbuff,buff,0);
	return result;
}

JNIEXPORT void JNICALL Java_moremote.p2p_ITCPRelayLibrary_close
  (JNIEnv *env, jobject thiz)
{
	relayClient->close_socket();
}
