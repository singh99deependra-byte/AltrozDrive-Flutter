package com.altrozdrive.connectnext.hts.remotelinkmgr

enum class ReaderState { IDLE, READING, STOPPED, ERROR }
enum class LinkMonitorState { DOWN, CONNECTING, UP, CLOSING }
enum class HTMMessageType { NETWORK_ID, HTS_CONTROL, VEHICLE_SERVICE, ACK, UNKNOWN }
enum class MessageType { CONNECT, DISCONNECT, DATA, ERROR }
