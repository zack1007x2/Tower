#include <string.h>
#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <stdlib.h>
#include <stdio.h>

/*-------------------------------- for UDT -----------------------------------------*/

#ifndef WIN32
#include <unistd.h>
#include <cstdlib>
#include <cstring>
#include <netdb.h>
#else
#include <winsock2.h>
   #include <ws2tcpip.h>
   #include <wspiapi.h>
#endif
#include <iostream>
#include <udt.h>
#include "cc.h"
#include "test_util.h"
#include "api.h"
using namespace std;
#include <netinet/in.h>
#include <arpa/inet.h>


/*-------------------------------- UDT END------------------------------------------*/
extern "C"{
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/opt.h>
#include <libavutil/audioconvert.h>
#include <libavutil/common.h>
#include <libavutil/imgutils.h>
#include <libavutil/mathematics.h>
#include <libavutil/samplefmt.h>
#include <libswscale/swscale.h>
#include <libavcodec/avcodec.h>
#include <libswresample/swresample.h>
#include <libavutil/mem.h>

#define LOG_TAG "UDTClient&RTPDecoder"
#define LOGI(...) {__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__);}
#define LOGE(...) {__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);}
#define LOGD(...) {__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__);}
AVCodecContext**  videoCodecCtx, *audioCodecCtx;
AVPacket* packet, audioPacket;
AVCodec* videoCodec, *audioCodec;
AVFrame **videoFrame, *audioFrame;

struct SwrContext *SwrCtx;
int *numBytes;
int *numBytesY, *numBytesU, *numBytesV;
int audioSize;
int audioSamprate;
int *frameOK;
uint8_t **videoData_Y;
uint8_t **videoData_U;
uint8_t **videoData_V;
uint8_t **audioData;
int *alreadySign;
/*-------------------------------- for UDT -----------------------------------------*/

// Automatically start up and clean up UDT module.
//UDTUpDown _udt_;

int createUDPSocket(int nPort)
{
	srand(time(NULL));
	int sockfd;
	struct sockaddr_in address;

	sockfd = socket(AF_INET, SOCK_DGRAM, 0);
	if(sockfd < 0) {
		LOGE("cant create socket");
//        perror("socket");
//        cout<<"error in line "<<__LINE__<<endl;
		return 0;
	}
	memset(&address, 0, sizeof(address));
	address.sin_family = AF_INET;
	address.sin_addr.s_addr = htonl(INADDR_ANY);
	address.sin_port = htons(nPort);
	if( bind(sockfd, (struct sockaddr *) &address, sizeof(address)) < 0) {
		perror("bind");
		LOGE("can't bind at port:%d",nPort);
		cout<<"error in line "<<__LINE__<<endl;
		return 0;
	}
	return sockfd;
}
int startVideoDecode(int);
uint8_t** videoBuffer;
int *videoOffset;
int* isVideoCodecOK;
int getVideoData(uint8_t*, int, int);
int isAudioCodecOK=0;

int getAudioData(uint8_t* tmpBuffer, int len, int decoderNumber){
//	LOGE("read AAC 1 len = %d",len);
	len = len -16 +7;
	int frameLen;
	uint8_t ADTS[]={0xFF, 0xF1, 0x00, 0x00, 0x00, 0x00, 0xFC};
//	int audioSamprate = 16000;
	int audioChannel = 1;
	int audioBit = 16;

	switch(audioSamprate)
	{
		case  16000:
			ADTS[2] = 0x60;
			break;
		case  32000:
			ADTS[2] = 0x54;
			break;
		case  44100:
			ADTS[2] = 0x50;
			break;
		case  48000:
			ADTS[2] = 0x4C;
			break;
		case  96000:
			ADTS[2] = 0x40;
			break;
		default:
			break;
	}
//	LOGE("audioSampleRate in C: %d",audioSamprate);
	ADTS[3] = (audioChannel==2)?0x80:0x40;
	frameLen = len << 5;//8bit * 2 - 11 = 5(headerSize 11bit)
	frameLen |= 0x1F;//5 bit    1
	ADTS[4] = frameLen>>8;
	ADTS[5] = frameLen & 0xFF;
	av_new_packet(&audioPacket,len);
	memcpy(audioPacket.data, ADTS, sizeof(ADTS));
	memcpy(audioPacket.data+7,tmpBuffer+16,len-7);
	int ret=0,frameFinished2=0;
	ret = avcodec_decode_audio4(audioCodecCtx, audioFrame, &frameFinished2, &audioPacket);
//	LOGE("audio ret %d",ret);
	if(ret > 0 && frameFinished2)
	{
		audioSize = av_samples_get_buffer_size(
				audioFrame->linesize,audioCodecCtx->channels,
				audioFrame->nb_samples,AV_SAMPLE_FMT_S16, 0);
//				LOGE("after getAudioBufferSize %d", audioSize);
		if(isAudioCodecOK==0){
			audioData[decoderNumber] = (uint8_t*)malloc(sizeof(uint8_t)*audioSize);

			int64_t destinationChannelLayout = AV_CH_LAYOUT_MONO;
			SwrCtx= swr_alloc_set_opts(SwrCtx,
									   destinationChannelLayout,
									   AV_SAMPLE_FMT_S16,
									   audioCodecCtx->sample_rate,//48000 16000
									   audioCodecCtx->channel_layout,
									   audioCodecCtx->sample_fmt,
//										AV_SAMPLE_FMT_FLTP,
									   audioCodecCtx->sample_rate,//48000 16000
									   0,
									   0);
//            LOGE("after allocate");
			if (swr_init(SwrCtx)<0) {
				LOGE("swr_init() for AV_SAMPLE_FMT_FLTP fail");
			}
			else{
				LOGE("swr_init() for AV_SAMPLE_FMT_FLTP success");
				isAudioCodecOK=1;
			}
		}
		else{
//		    LOGE("codec ok ");
		}
//        LOGE("before convert");
//        LOGE("audioData[%d] = %d!!!", decoderNumber, audioData[decoderNumber]);
		int outCount = swr_convert(SwrCtx,
								   (uint8_t **)(&audioData[decoderNumber]),
								   audioFrame->nb_samples,
								   (const uint8_t **)audioFrame->extended_data,
								   audioFrame->nb_samples);
//        LOGE("after convert");
		if (outCount < 0) {
			LOGE("swr_convert failed####");
		}
		av_free_packet(&audioPacket);
		return 1;
	}
	else{
		LOGE("audio not framefinished");
		return 0;
	}


}
int SEQ=0,lastSEQ=0;
int getRtpData(uint8_t* tmpBuffer,int len, int decoderNumber){

	if(len<20)
	LOGE("len = %d!!!",len);

	if(tmpBuffer[12]!=0x00){
		SEQ = ((tmpBuffer[2]&0xff)<<8) | ((tmpBuffer[3]&0xff));
		lastSEQ = SEQ;
		if(frameOK[decoderNumber] == 0){
			if((((tmpBuffer[13] & 0x80) == 0x80) && ((tmpBuffer[13] & 0x1f)==5))
			   || (((tmpBuffer[12] & 0x1f) == 24) && ((tmpBuffer[15] & 0x1f)==7))
			   || (((tmpBuffer[12] & 0x1f) == 24) && ((tmpBuffer[15] & 0x1f)==8))
			   || (((tmpBuffer[12] & 0x1f) <= 23) && ((tmpBuffer[12] & 0x1f)>=1))){
//                LOGE("frame OK!");
				frameOK[decoderNumber] = 1;
			}
			else{
				return 0;
			}
		}
	}

	//is RtpPacket
	if(tmpBuffer[0]==0x80){
		//AAC format
		if((tmpBuffer[12]== 0x00)){
//			LOGE("audio len = %d",len);
//            LOGE("getAudioData %d",decoderNumber);
			if(decoderNumber == 0){
				getAudioData(tmpBuffer,len, decoderNumber);
			}
			return 2;
		}
			//H264 format
		else{
//			LOGE("video len = %d",len);
			return getVideoData(tmpBuffer,len, decoderNumber);
		}
	}
	else
	LOGE("unknown typeQQ %d",tmpBuffer[0]);

}

JNIEXPORT jint Java_com_moremote_moapp_IPCam_getRtpData(JNIEnv *env, jobject obj, jbyteArray buf, jint len, jint decoderNumber){

uint8_t* tmpBuffer = (uint8_t*)env->GetByteArrayElements(buf, 0);

int type = 0;
if(len>12){
type = getRtpData(tmpBuffer, len, decoderNumber);
}

env->ReleaseByteArrayElements(buf, (jbyte*)tmpBuffer, 0);
return type;
}
int getVideoData(uint8_t* tmpBuffer, int len, int decoderNumber){

	int tmpOffset1=0, tmpOffset2=0;
	int nal_unit_type =(tmpBuffer[12] & 0x1f);
//    LOGE("read video from socket nalu type = %d",nal_unit_type);
	if((tmpBuffer[12] & 0x1f) == 28){
		//start of type 28
		if((tmpBuffer[13] & 0x80) == 0x80){
			nal_unit_type = ((tmpBuffer[12] & 0xe0) | (tmpBuffer[13] & 0x1f)) & 0x1f;
//			LOGE("before videoOffset %d",decoderNumber);
			videoOffset[decoderNumber] = 0;
//			LOGE("before videoBuffer %d",decoderNumber);
//			LOGE("videoOffset %d",videoOffset[decoderNumber]);
			//crash here
			memset(videoBuffer[decoderNumber] + videoOffset[decoderNumber], 0, 3);
//			LOGE("after videoBuffer %d",decoderNumber);
			memset(videoBuffer[decoderNumber] + videoOffset[decoderNumber]+3, 0x01, 1);
			memset(videoBuffer[decoderNumber] + videoOffset[decoderNumber]+4,(tmpBuffer[12] & 0xe0) | (tmpBuffer[13] & 0x1f),1);
			videoOffset[decoderNumber] += 5;

			memcpy(videoBuffer[decoderNumber] + videoOffset[decoderNumber], tmpBuffer+14, len-14);
			videoOffset[decoderNumber] += (len-14);
			return 0;
		}
		else if(videoOffset[decoderNumber] == 0){
			return 0;
		}
		else{
			memcpy(videoBuffer[decoderNumber] + videoOffset[decoderNumber] , tmpBuffer+14, len-14);
			videoOffset[decoderNumber] += (len-14);
		}
		//if not ending
		if((tmpBuffer[13] & 0x40) != 0x40){
			return 0;
		}
	}
	else if (((tmpBuffer[12] & 0x1f) == 24)){
		videoOffset[0] = 0;
//	    videoOffset[decoderNumber] = 0;
		nal_unit_type = (tmpBuffer[12] & 0x1f);
//		LOGE("nalu type = %d +++++++++++++++++++++++++++++++++++++",nal_unit_type);
		int i=0;
		for(i=0;i<len;i++){
//            LOGE("@@ data[%d] = %d", i, tmpBuffer[i]);
		}
		tmpOffset1 = ((tmpBuffer[13] & 0xff)<<8) | (tmpBuffer[14] & 0xff);
		tmpOffset2 = ((tmpBuffer[15+tmpOffset1] & 0xff)<<8) | (tmpBuffer[16+tmpOffset1] & 0xff);
//        LOGE("after tmpBuffer %d",decoderNumber);
//        LOGE("videoBuffer[%d] = %d",decoderNumber, videoBuffer[decoderNumber]);
//        LOGE("videoOffset[%d] = %d",decoderNumber, videoOffset[decoderNumber]);
		memset(videoBuffer[decoderNumber] + videoOffset[decoderNumber], 0, 3);
//		LOGE("afer videoBuffer %d",decoderNumber);
		memset(videoBuffer[decoderNumber] + videoOffset[decoderNumber]+3, 0x01, 1);
		memcpy(videoBuffer[decoderNumber] + videoOffset[decoderNumber]+4, tmpBuffer+15, tmpOffset1);
		videoOffset[decoderNumber] += 4+tmpOffset1;

		memset(videoBuffer[decoderNumber] + videoOffset[decoderNumber], 0, 3);
		memset(videoBuffer[decoderNumber] + videoOffset[decoderNumber]+3, 0x01, 1);
		memcpy(videoBuffer[decoderNumber] + videoOffset[decoderNumber]+4, tmpBuffer+17+tmpOffset1, tmpOffset2);
		videoOffset[decoderNumber] += 4+tmpOffset2;

	}
	else if(((tmpBuffer[12] & 0x1f) == 1) || ((tmpBuffer[12] & 0x1f) == 7) || ((tmpBuffer[12] & 0x1f) == 8)){
		videoOffset[decoderNumber] = 0;
		nal_unit_type = (tmpBuffer[12] & 0x1f);
		videoOffset[decoderNumber] = (len-12);
		memset(videoBuffer[decoderNumber], 0, 3);
		memset(videoBuffer[decoderNumber]+3, 0x01, 1);
		memcpy(videoBuffer[decoderNumber]+4, tmpBuffer+12, videoOffset[decoderNumber]);
		videoOffset[decoderNumber] +=4;
	}
	else{
		videoOffset[decoderNumber] = 0;
		nal_unit_type = (tmpBuffer[12] & 0x1f);
//		LOGE("!!!nalu type = %d",nal_unit_type);
		videoOffset[decoderNumber] = (len-12);
		memset(videoBuffer[decoderNumber], 0, 3);
		memset(videoBuffer[decoderNumber]+3, 0x01, 1);
		memcpy(videoBuffer[decoderNumber]+4, tmpBuffer+12, videoOffset[decoderNumber]);
		videoOffset[decoderNumber] += 4;
	}
//    LOGE("go startDecode!!");
	return startVideoDecode(decoderNumber);
}
int maxlen=0;
int lock =0;
int use = -1;
int startVideoDecode(int decoderNumber){

//    LOGE("start decode %d",videoOffset[decoderNumber]);
	int ret, frameFinished;
	av_new_packet(&packet[decoderNumber], videoOffset[decoderNumber]);
	memcpy(packet[decoderNumber].data, videoBuffer[decoderNumber], videoOffset[decoderNumber]);

	if(videoOffset[decoderNumber]>100000){
		if(videoOffset[decoderNumber] > maxlen){
			maxlen = videoOffset[decoderNumber];
		}
//	       LOGE("......send to decode len: %d",videoOffset);
	}
//    LOGE("max len[%d] = %d", decoderNumber, maxlen);
	ret = avcodec_decode_video2(videoCodecCtx[decoderNumber], videoFrame[decoderNumber], &frameFinished, &packet[decoderNumber]);
//    LOGE("video ret = %d",ret);
//    LOGE("video width = %d",videoCodecCtx->width);

	if(isVideoCodecOK[decoderNumber]==0 && (videoCodecCtx[decoderNumber]->width > 0)){
		isVideoCodecOK[decoderNumber]=1;
		int videoWidth = videoCodecCtx[decoderNumber]->width;
		int videoHeight = videoCodecCtx[decoderNumber]->height;
		numBytesY[decoderNumber] = videoWidth * videoHeight;
		numBytesU[decoderNumber] = videoWidth * videoHeight / 4;
		numBytesV[decoderNumber] = videoWidth * videoHeight / 4;
		numBytes[decoderNumber] = numBytesY[decoderNumber] + numBytesU[decoderNumber] + numBytesV[decoderNumber];
	}
//YUV420P
	av_free_packet(&packet[decoderNumber]);
//    LOGE("after free packet");
	if(ret != videoOffset[decoderNumber]){
		LOGE("decode error: %d, %d",ret, videoOffset[decoderNumber]);
		videoOffset[decoderNumber] = 0;
		return 0;
	}
	videoOffset[decoderNumber] = 0;


	if(frameFinished && isVideoCodecOK[decoderNumber]==1) {

//        memcpy(videoData_Y[decoderNumber], videoFrame[decoderNumber]->data[0], numBytesY);
//        memcpy(videoData_U[decoderNumber], videoFrame[decoderNumber]->data[1], numBytesU);
//        memcpy(videoData_V[decoderNumber], videoFrame[decoderNumber]->data[2], numBytesV);

		return 1;
	}
	return 0;
}

JNIEXPORT void Java_com_moremote_moapp_IPCam_startupUDT(JNIEnv *env, jobject obj)
{
	UDT::startup();
}

JNIEXPORT void Java_com_moremote_moapp_IPCam_cleanupUDT(JNIEnv *env, jobject obj)
{
	UDT::cleanup();
}

JNIEXPORT void Java_com_moremote_moapp_IPCam_closeUDTSocket(JNIEnv *env, jobject obj, jint UDTFD)
{
	UDT::close( (UDTSOCKET)UDTFD );
}

JNIEXPORT jint Java_com_moremote_moapp_IPCam_createUDTClient(JNIEnv *env, jobject obj, jint port, jstring serverip, jstring serverport){

srand(time(NULL));


const char* destIP = env->GetStringUTFChars(serverip,0);
const char* destPort = env->GetStringUTFChars(serverport, 0);
LOGD("createUDTClient %d %s %s", port, destIP, destPort);
LOGE("@@createUDTClient %d %s %s", port, destIP, destPort);
int socketfd = createUDPSocket(port);
LOGE("@@ get SockedFD %d",socketfd);
struct addrinfo hints, *local, *peer;

memset(&hints, 0, sizeof(struct addrinfo));

hints.ai_flags = AI_PASSIVE;
hints.ai_family = AF_INET;
hints.ai_socktype = SOCK_STREAM;

if (0 != getaddrinfo(NULL, "9000", &hints, &local))
{
LOGE("createUDTClient incorrect network address.");
return -1;
}

UDTSOCKET client = UDT::socket(local->ai_family, local->ai_socktype, local->ai_protocol);
if (UDT::ERROR == UDT::bind2(client,socketfd))
{
LOGE("createUDTClient bind2 error: %s",UDT::getlasterror().getErrorMessage());
return -1;
}
freeaddrinfo(local);

if (0 != getaddrinfo(destIP, destPort, &hints, &peer))
{
LOGE("incorrect server/peer address. %s : %s", destIP, destPort);
return -1;
}
int i = 0;
LOGE("UDT client connecting...");
while( ++i){
if (UDT::ERROR == UDT::connect(client, peer->ai_addr, peer->ai_addrlen))
{
LOGE("%d connect fail: %s", i, UDT::getlasterror().getErrorMessage());

if(i>10) return -1;

usleep(100*1000);
}
else break;
}
freeaddrinfo(peer);
env->ReleaseStringUTFChars(serverip, destIP);
env->ReleaseStringUTFChars(serverport, destPort);

LOGD("create UDT Client success sockfd: %d, UDTSOCKET: %d", socketfd, client);
LOGE("@@create UDT Client success sockfd: %d, UDTSOCKET: %d", socketfd, client);

return (int)client;
}

int* UDTSize = 0;
char** UDTData;
JNIEXPORT void Java_com_moremote_moapp_IPCam_initUDTPacket(JNIEnv *env, jobject obj, jint getUDTSize, jint decoderNumber)
{

	UDTSize[decoderNumber] = getUDTSize;
//	if(!UDTData[decoderNumber])
	UDTData[decoderNumber] = new char[getUDTSize];
//	LOGE("set UDTSize[%d]: %d", decoderNumber,UDTSize[decoderNumber]);
}

int getUDTLen(uint8_t* packet){
//    LOGE("in getLen: %x %x %x %x",packet[0], packet[1], packet[2], packet[3]);
	int len = ((packet[0]&0xff))    |
			  ((packet[1]&0xff)<<8) |
			  ((packet[2]&0xff)<<16)|
			  ((packet[3]&0xff)<<24);
	return len;
}

JNIEXPORT jint Java_com_moremote_moapp_IPCam_receiveUDTPacket(JNIEnv *env, jobject obj, jint UDTFD, jint decoderNumber) {
UDTSOCKET UDTClientFD = (UDTSOCKET)UDTFD;

//	int nSize = sizeof(struct packet_info);
//	int nSize = UDTSize + 4;
//	char* data;
//	data = new char[UDTSize];
int rs, len, rsize;

while (true)
{
rsize = 0;
while (rsize < UDTSize[decoderNumber])
{
//		    LOGE("in while UDTSize[%d] = %d",decoderNumber, UDTSize[decoderNumber]);
if (UDT::ERROR == (rs = UDT::recv(UDTClientFD, UDTData[decoderNumber] + rsize, UDTSize[decoderNumber] - rsize, 0)))
{
LOGE("ReceiveUDTPacket receive error %d, %s", UDTFD, UDT::getlasterror().getErrorMessage());
return 0;
break;
}
//			LOGE("rsize: %d, rs:%d",rsize, rs);
rsize += rs;
}
if (rsize < UDTSize[decoderNumber])
return 0;
//        LOGE("before get len");
len = getUDTLen((uint8_t*)UDTData[decoderNumber]);
//        LOGE("after get len %d",len);
uint8_t* tmpBuffer = (uint8_t*)malloc(sizeof(uint8_t)*len);
memcpy(tmpBuffer, UDTData[decoderNumber]+4, len);
//		LOGD("UDT get rtp len = %d", len);

//        LOGE("decoderNumber: %d",decoderNumber);
int type = getRtpData(tmpBuffer, len, decoderNumber);
free(tmpBuffer);
if(type != 0)
return type;
}
}

bool sendUDTPacket(UDTSOCKET UDTClientFD, char* data, int size)
{
	int ssize = 0;
	int ss;
	while (ssize < size)
	{
		if (UDT::ERROR == (ss = UDT::send(UDTClientFD, data + ssize, size - ssize, 0)))
		{
			LOGE("send: %s",UDT::getlasterror().getErrorMessage());
			break;
		}
		ssize += ss;
	}
	return (ssize<size);
}

JNIEXPORT jboolean Java_com_moremote_moapp_IPCam_sendByUDT( JNIEnv* env, jobject obj, jint UDTFD, jbyteArray buff, jint buffLen)
{
signed char* pBuff=env->GetByteArrayElements(buff,NULL);
bool ret = sendUDTPacket( (UDTSOCKET)UDTFD, (char*)pBuff, buffLen );
env->ReleaseByteArrayElements(buff, pBuff, 0);
return ret;
}

/*-------------------------------- UDT END------------------------------------------*/

JNIEXPORT void Java_com_moremote_moapp_IPCam_getVideoFromC(JNIEnv *env, jobject obj, jint decoderNumber, jobject buffer_Y, jobject buffer_U, jobject buffer_V ){
	uint8_t *tmpBuffer;

	uint8_t *buf_Y = (uint8_t*)env->GetDirectBufferAddress(buffer_Y);
	memcpy(buf_Y, videoFrame[decoderNumber]->data[0], numBytesY[decoderNumber]);

	uint8_t *buf_U = (uint8_t*)env->GetDirectBufferAddress(buffer_U);
	memcpy(buf_U, videoFrame[decoderNumber]->data[1], numBytesU[decoderNumber]);

	uint8_t *buf_V = (uint8_t*)env->GetDirectBufferAddress(buffer_V);
	memcpy(buf_V, videoFrame[decoderNumber]->data[2], numBytesV[decoderNumber]);
}
JNIEXPORT jint Java_com_moremote_moapp_IPCam_getVideoWidth( JNIEnv* env, jobject obj, jint decoderNumber)
{
return videoCodecCtx[decoderNumber]->width;
}
JNIEXPORT jint Java_com_moremote_moapp_IPCam_getVideoHeight( JNIEnv* env, jobject obj, jint decoderNumber)
{
return videoCodecCtx[decoderNumber]->height;
}
JNIEXPORT jint Java_com_moremote_moapp_IPCam_getAudioSize( JNIEnv* env)
{
return audioSize;
}
JNIEXPORT void Java_com_moremote_moapp_IPCam_getAudioFromC( JNIEnv* env, jobject obj, jobject audioBuffer, jint decoderNumber)
{
	uint8_t *audio = (uint8_t*)env->GetDirectBufferAddress(audioBuffer);
	memcpy(audio, audioData[decoderNumber], audioSize);
}
JNIEXPORT void Java_com_moremote_moapp_IPCam_setAudioSampleRate(JNIEnv *env, jobject obj, jint audioPlayerSamplerate){
	audioSamprate = audioPlayerSamplerate;
}
JNIEXPORT jint Java_com_moremote_moapp_IPCam_signDecoder(JNIEnv *env, jobject obj, jint decoderNumber){

LOGE("alreadySign[%d]=%d",decoderNumber,alreadySign[decoderNumber]);
if(alreadySign[decoderNumber] == 1){
if(videoFrame[decoderNumber]) av_frame_free(&videoFrame[decoderNumber]);
}
else if(alreadySign[decoderNumber] == 0){

videoBuffer[decoderNumber] = (uint8_t*)malloc(sizeof(uint8_t)*300000);

videoCodec = avcodec_find_decoder( AV_CODEC_ID_H264 );
videoCodecCtx[decoderNumber] = avcodec_alloc_context3( videoCodec );
videoCodecCtx[decoderNumber]->codec_id = AV_CODEC_ID_H264;
if(avcodec_open2(videoCodecCtx[decoderNumber], videoCodec, NULL) < 0) {
LOGE("Cannot open video decoder !@");
}
else{
LOGE("open codec success!@");
}
}

isVideoCodecOK[decoderNumber] = 0;
videoOffset[decoderNumber] = 0;
frameOK[decoderNumber]=0;
videoFrame[decoderNumber] = av_frame_alloc();

alreadySign[decoderNumber] = 1;
LOGE("codec ready %d !@",decoderNumber);
}

JNIEXPORT void Java_com_moremote_moapp_activity_RemoteControlActivity_DecoderInitial(JNIEnv *env)
{
	audioData = (uint8_t**)malloc(sizeof(uint8_t*)*10);

	// initial ffmpeg
//		videoBuffer = (uint8_t*)malloc(sizeof(uint8_t)*300000);
	videoBuffer = (uint8_t**)malloc(sizeof(uint8_t*)*10);
	videoOffset = (int *)malloc(sizeof(int)*10);
	packet = (AVPacket*)malloc(sizeof(AVPacket)*10);
	videoFrame = (AVFrame**)malloc(sizeof(AVFrame*)*10);
	videoCodecCtx = (AVCodecContext**)malloc(sizeof(AVCodecContext*)*10);
	UDTData = new char*[10];
	frameOK = (int*)malloc(sizeof(int)*10);
	alreadySign = (int*)malloc(sizeof(int)*10);
	UDTSize = (int*)malloc(sizeof(int)*10);
	int i;
	for(i=0;i<10;i++){
		alreadySign[i] = 0;
	}
	isVideoCodecOK = new int[10];
	numBytes = new int[10];
	numBytesY = new int[10];
	numBytesU = new int[10];
	numBytesV = new int[10];

	av_register_all();
	avcodec_register_all();
	avformat_network_init();

	/*********************************  FOR AUDIO  *********************************************/

	av_init_packet(&audioPacket);
	audioFrame = av_frame_alloc();
	isAudioCodecOK = 0;

	audioCodec = avcodec_find_decoder( AV_CODEC_ID_AAC );
	audioCodecCtx = avcodec_alloc_context3( audioCodec );
	audioCodecCtx->codec_id = AV_CODEC_ID_AAC;
	LOGE("audio codec finished !@");

	// Open audio codec
	if(avcodec_open2(audioCodecCtx, audioCodec, NULL) < 0)
	{
		LOGE("Cannot open audio decoder\n @!");
	}
	else
	LOGE("open audio decoder success@!");
	/********************************* AUDIO END *********************************************/
}//DecodeInitial
}//extern c