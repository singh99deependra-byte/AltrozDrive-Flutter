package com.altrozdrive.connectnext.apptalk.client.file.async
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
class FileTransferExecutor { val executor:ExecutorService=Executors.newSingleThreadExecutor() }
class FileReceiverExecutor { val executor:ExecutorService=Executors.newSingleThreadExecutor() }
class FileTransferTask(val bytes:ByteArray,val callback:((ByteArray)->Unit)?=null):Runnable{override fun run(){callback?.invoke(bytes.copyOf())}}
class FileReceiverTask(val bytes:ByteArray,val callback:((ByteArray)->Unit)?=null):Runnable{override fun run(){callback?.invoke(bytes.copyOf())}}
