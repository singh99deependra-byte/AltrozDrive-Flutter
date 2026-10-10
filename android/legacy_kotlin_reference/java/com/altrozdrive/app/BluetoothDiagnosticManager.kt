package com.altrozdrive.app

import com.altrozdrive.connectnext.session.ConnectNextSessionController
import com.altrozdrive.connectnext.vehicle.client.utils.VehiclePacketParser_X451
import com.altrozdrive.connectnext.vehicle.client.utils.X451VehicleWrapper

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.UUID
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.io.BufferedInputStream
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONObject

private const val MAX_HCI_CAPTURE_BYTES = 64L * 1024L * 1024L

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val type: String,
    val rssi: Int? = null,
    val paired: Boolean = false,
    val connected: Boolean = false,
    val serviceUuids: List<String> = emptyList()
)

data class BluetoothCharacteristicInfo(
    val serviceUuid: String,
    val uuid: String,
    val properties: String
)

/**
 * Authoritative live-session state. Bluetooth callbacks, RFCOMM readers and the
 * HTS parser run on different threads, so keeping LIVE state only inside the
 * mutable diagnostic snapshot can lose a validated update when another callback
 * publishes an older copy. This small atomic snapshot prevents that race without
 * changing the transport or protocol lifecycle.
 */
private data class LiveSessionState(
    val htsConnected: Boolean = false,
    val liveOdo: Int? = null,
    val liveFuelPercent: Int? = null,
    val liveRpm: Int? = null,
    val liveDteKm: Int? = null,
    val liveTripA: Float? = null,
    val liveTripB: Float? = null,
    val liveAvgFuelEcoTripA: Float? = null,
    val liveAvgFuelEcoTripB: Float? = null,
    val liveVisValidated: Boolean = false,
    val liveDataAt: String = "Never",
    val liveDataAtMs: Long = 0L
)

data class BluetoothDiagnosticState(
    val bluetoothSupported: Boolean = true,
    val bluetoothEnabled: Boolean = false,
    val scanning: Boolean = false,
    val transport: String = "Unknown",
    val status: String = "Idle",
    val selectedDevice: BluetoothDeviceInfo? = null,
    val devices: List<BluetoothDeviceInfo> = emptyList(),
    val characteristics: List<BluetoothCharacteristicInfo> = emptyList(),
    val gattDescriptors: List<String> = emptyList(),
    val rxPackets: Long = 0,
    val rxBytes: Long = 0,
    val lastRxAt: String = "Never",
    val txPackets: Long = 0,
    val txBytes: Long = 0,
    val lastTxAt: String = "Never",
    val readError: String = "None",
    val advertisementPackets: Long = 0,
    val advertisementBytes: Long = 0,
    val classicSdpStatus: String = "Not requested",
    val classicServiceUuids: List<String> = emptyList(),
    val lastRxTransport: String = "Never",
    val connectedAt: String = "Never",
    val channelAttempts: List<String> = emptyList(),
    val diagnosticEvents: List<String> = emptyList(),
    val htsConnected: Boolean = false,
    val htsVersion: String = "Unknown",
    val htsFrames: Long = 0,
    val htsJsonFrames: Long = 0,
    val htsBinaryFrames: Long = 0,
    val htsLastSequence: Long = -1,
    val htsLastMessageType: String = "None",
    val htsServices: List<String> = emptyList(),
    val htsVehicleBinaryFrames: Long = 0,
    val htsMessageTypes: List<String> = emptyList(),
    val htsLastPayloadPreview: String = "None",
    val cnProtocolStage: String = "DISCONNECTED",
    val cnAuthenticated: Boolean = false,
    val cnHandshakeSeen: Boolean = false,
    val cnHeartbeatCount: Long = 0,
    val cnDiscoverySeen: Boolean = false,
    val cnSubscribedSignals: List<String> = emptyList(),
    val cnServices: List<String> = emptyList(),
    val cnLastCommand: String = "None",
    val cnLastResponse: String = "None",
    val cnAuthDetail: String = "Not started",
    // Compatibility field retained for the newer UI; v5 Bluetooth recovery logic remains unchanged.
    val cnInternalRecovery: Boolean = false,
    val hciCaptureName: String = "None",
    val hciRecords: Long = 0,
    val hciAclPackets: Long = 0,
    val hciConnectNextPackets: Long = 0,
    val hciVendorFrames: Long = 0,
    val hciDlcis: List<String> = emptyList(),
    val hciServices: List<String> = emptyList(),
    val hciLastPayloadPreview: String = "None",
    val hciOdo: Int? = null,
    val hciFuelPercent: Int? = null,
    val hciDteKm: Int? = null,
    val hciVehicleTempC: Int? = null,
    val hciFanSpeed: Int? = null,
    val hciAcOn: Boolean? = null,
    val hciAutoMode: Boolean? = null,
    val hciEcoMode: Boolean? = null,
    val hciAirFlowPattern: Int? = null,
    val hciRecirculation: Boolean? = null,
    val hciMaxDefrost: Boolean? = null,
    val hciRearDefog: Boolean? = null,
    val hciHighFanSpeed: Int? = null,
    val hciLowFanSpeed: Int? = null,
    val hciHighTempC: Int? = null,
    val hciLowTempC: Int? = null,
    val liveOdo: Int? = null,
    val liveFuelPercent: Int? = null,
    val liveDteKm: Int? = null,
    val liveAcTempC: Int? = null,
    val liveFanSpeed: Int? = null,
    val liveAcOn: Boolean? = null,
    val liveSpeedKph: Int? = null,
    val liveRpm: Int? = null,
    val liveBatteryVoltage: Double? = null,
    val liveCoolantTempC: Int? = null,
    val liveIgnitionOn: Boolean? = null,
    val liveVehicleWarning: Int? = null,
    val liveTripA: Float? = null,
    val liveTripB: Float? = null,
    val liveAvgFuelEcoTripA: Float? = null,
    val liveAvgFuelEcoTripB: Float? = null,
    val liveVisValidated: Boolean = false,
    val cachedOdo: Int? = null,
    val cachedFuelPercent: Int? = null,
    val cachedDteKm: Int? = null,
    val cachedTripA: Float? = null,
    val cachedTripB: Float? = null,
    val cachedAvgFuelEcoTripA: Float? = null,
    val cachedAvgFuelEcoTripB: Float? = null,
    val cachedDataAt: Long? = null,
    val cachedDataSource: String? = null,
    val liveDataAt: String = "Never",
    /** Wall-clock time of the last validated VIS frame; the UI uses it to decide LIVE vs OFFLINE. */
    val liveDataAtMs: Long = 0L,
    val autoConnect: Boolean = true,
    val autoConnectDevice: String = "None",
    val hciAnalysisStatus: String = "Not imported"
)


/**
 * Disk-backed protocol logger stored in the user's normal Downloads folder.
 * Android 10+ uses MediaStore so the files are visible in File Manager without
 * MANAGE_EXTERNAL_STORAGE or access to the hidden Android/data directory.
 */
private class ProtocolFileLogger(private val context: Context) {
    private val lock = Any()
    private val maxBytes = 8L * 1024L * 1024L
    private val maxFiles = 20
    private var writer: BufferedWriter? = null
    private var uri: Uri? = null
    private var fileName: String? = null
    private var bytesWritten = 0L
    private var linesSinceFlush = 0

    private val relativePath = "Download/AltrozDrive/Logs/"

    private fun openLocked() {
        if (writer != null) return
        val name = "bluetooth_${SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())}.log"
        val values = android.content.ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
        }
        val newUri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("Unable to create log in Downloads/AltrozDrive/Logs")
        uri = newUri
        fileName = name
        val output = context.contentResolver.openOutputStream(newUri, "wa")
            ?: throw IllegalStateException("Unable to open log output stream")
        writer = BufferedWriter(output.writer(Charsets.UTF_8), 64 * 1024)
        bytesWritten = 0L
        writer?.write("# AltrozDrive Bluetooth debug log\n")
        writer?.flush()
        bytesWritten += 32
        linesSinceFlush = 0
        trimOldLogsLocked()
    }

    fun log(message: String) {
        synchronized(lock) {
            try {
                openLocked()
                val line = "${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())}  $message\n"
                val lineBytes = line.toByteArray(Charsets.UTF_8).size.toLong()
                if (bytesWritten + lineBytes > maxBytes) rotateLocked()
                writer?.write(line)
                bytesWritten += lineBytes
                linesSinceFlush++
                if (linesSinceFlush >= 50) {
                    writer?.flush()
                    linesSinceFlush = 0
                }
            } catch (_: Exception) { }
        }
    }

    private fun rotateLocked() {
        try { writer?.flush(); writer?.close() } catch (_: Exception) {}
        writer = null
        uri = null
        fileName = null
        bytesWritten = 0L
        openLocked()
    }

    private fun trimOldLogsLocked() {
        try {
            val names = mutableListOf<Pair<String, Uri>>()
            val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads._ID)
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Downloads.RELATIVE_PATH}=? AND ${MediaStore.Downloads.DISPLAY_NAME} LIKE ?",
                arrayOf(relativePath, "bluetooth_%.log"),
                "${MediaStore.Downloads.DATE_ADDED} DESC"
            )?.use { c ->
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val idCol = c.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                while (c.moveToNext()) {
                    val n = c.getString(nameCol)
                    val id = c.getLong(idCol)
                    names += n to Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString())
                }
            }
            names.drop(maxFiles).forEach { (_, oldUri) ->
                try { context.contentResolver.delete(oldUri, null, null) } catch (_: Exception) {}
            }
        } catch (_: Exception) { }
    }

    fun flushNow() = synchronized(lock) {
        try { writer?.flush(); linesSinceFlush = 0 } catch (_: Exception) { }
    }

    fun currentFileName(): String? = synchronized(lock) {
        try { writer?.flush() } catch (_: Exception) {}
        fileName
    }

    fun currentUri(): Uri? = synchronized(lock) {
        try { writer?.flush() } catch (_: Exception) {}
        uri
    }

    fun directoryDescription(): String = "Internal storage/Download/AltrozDrive/Logs"

    fun close() = synchronized(lock) {
        try { writer?.flush(); writer?.close() } catch (_: Exception) {}
        writer = null
    }
}

class BluetoothDiagnosticManager private constructor(private val context: Context) {
    companion object {
        @Volatile private var INSTANCE: BluetoothDiagnosticManager? = null
        fun getInstance(context: Context): BluetoothDiagnosticManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BluetoothDiagnosticManager(context.applicationContext).also { INSTANCE = it }
            }
    }
    private val protocolLogger = ProtocolFileLogger(context)
    // Must be initialized before init { ... } below. Kotlin initializes properties
    // in declaration order; accessing this field from init before its declaration
    // produced the observed JVM null reference and prevented last-known loading.
    private val vehicleSnapshotDb = VehicleSnapshotDb(context)
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var scanner: BluetoothLeScanner? = null
    private var gatt: BluetoothGatt? = null
    private var classicSocket: android.bluetooth.BluetoothSocket? = null
    private var htsSocket: android.bluetooth.BluetoothSocket? = null
    private var sppServerSocket: android.bluetooth.BluetoothServerSocket? = null
    private val sppServerSockets = java.util.Collections.synchronizedList(mutableListOf<android.bluetooth.BluetoothServerSocket>())
    private val incomingSppAccepted = AtomicBoolean(false)
    // ConnectNext application-session safety net. ACL_CONNECTED is not the same as a
    // live ConnectNext session, so an accepted-but-stalled SppLink must be recoverable
    // without requiring a manual Bluetooth reconnect.
    private val connectNextWatchdogHandler = Handler(Looper.getMainLooper())
    @Volatile private var connectNextLastProgressAt = 0L
    // Fixed, non-renewable anchor for "how long has this session been waiting on
    // Sl_Supported_App_Ack". connectNextLastProgressAt (and the old peer19-only
    // watch below) were both renewed by markConnectNextProgress(), which a bare
    // repeated HU NETWORK_ID resend also triggers. A 2026-10-10 field log shows
    // this HU sends a NETWORK_ID heartbeat every ~3s for an entire 26-minute
    // drive; that log happened to also succeed within 6s, so the renewable timer
    // never mattered there, but it logged "PEER19 WATCH armed" every 3 seconds
    // for the whole drive, which could not have recovered a session that
    // genuinely never advanced. connectNextNegotiationStartedAt is set once per
    // accepted session and never refreshed, so it can't be defeated by repeated
    // HU traffic the way the renewable timers could.
    @Volatile private var connectNextNegotiationStartedAt = 0L
    // Hard upper bound from accepted RFCOMM session, covering stalls before NETWORK_ID is decoded.
    @Volatile private var connectNextSessionAcceptedAt = 0L
    private val connectNextWatchdog = object : Runnable {
        override fun run() {
            if (intentionalDisconnect || !incomingSppAccepted.get() || htsSocket?.isConnected != true) return
            if (liveSession.get().liveVisValidated) return
            val now = System.currentTimeMillis()
            // A heartbeat is not proof that negotiation advanced. Enforce fixed, stage-aware
            // deadlines that cannot be renewed by NETWORK_ID or other repeated HU traffic.
            // Before a valid NETWORK_ID is decoded, allow up to 45s from accepted SppLink;
            // once it is decoded, require the SupportedApps ACK within 20s of that first frame.
            if (!evidenceSupportedAppsAckSeen) {
                val acceptedAt = connectNextSessionAcceptedAt
                val negotiationAt = connectNextNegotiationStartedAt
                val timedOutBeforeNetworkId = negotiationAt == 0L && acceptedAt > 0L && now - acceptedAt >= 45_000L
                val timedOutAfterNetworkId = negotiationAt > 0L && now - negotiationAt >= 20_000L
                if (timedOutBeforeNetworkId || timedOutAfterNetworkId) {
                    val deadline = if (timedOutAfterNetworkId) "20s after first NETWORK_ID" else "45s after SppLink accept before NETWORK_ID"
                    recordChannel("CONNECTNEXT NEGOTIATION DEADLINE — no Sl_Supported_App_Ack by $deadline, peer=${observedPeerDeviceId ?: -1}, appsSent=$evidenceAppsSent; recovering")
                    recoverStaleConnectNextSession(now, "negotiation-deadline")
                    return
                }
            }
            val stalledFor = if (connectNextLastProgressAt > 0L) now - connectNextLastProgressAt else 0L
            val threshold = connectNextRecoveryThresholdMs()
            if (stalledFor >= threshold) {
                recoverStaleConnectNextSession(now, "application-stall")
                return
            }
            connectNextWatchdogHandler.postDelayed(this, 5_000L)
        }
    }
    @Volatile private var observedHuOwnDeviceId: Int? = null
    @Volatile private var observedPeerDeviceId: Int? = null
    // Single-owner SppLink lifecycle. Only one local SDP/RFCOMM server may exist at a time.
    private val htsServerLock = Any()
    @Volatile private var htsServerListening = false
    @Volatile private var htsServerGeneration = 0L
    private val htsReassembler = ConnectNextHtsReassembler()
    private var htsSequence = 0
    private val requestedHtsServices = mutableSetOf<String>()
    private var htsNextTxSequence = 0
    private val htsAckActions = ConcurrentHashMap<Int, () -> Unit>()
    // One writer + one sequence allocator for the authoritative HTS socket.
    // This does not change the wire protocol; it prevents concurrent callbacks/timeouts
    // from interleaving bytes or allocating duplicate TX sequence numbers.
    private val htsTxLock = Any()
    // Evidence from the supplied HCI trace: the useful ConnectNext HTS session is
    // RFCOMM server channel 7 / DLCI 14. Channels 13 and 10 remain fallbacks because
    // other RFCOMM sessions are also present in the vehicle's Bluetooth stack.
    private val discovered = ConcurrentHashMap<String, BluetoothDeviceInfo>()
    private var packetListener: ((String, String) -> Unit)? = null
    private var stateListener: ((BluetoothDiagnosticState) -> Unit)? = null
    private var state = BluetoothDiagnosticState(bluetoothSupported = adapter != null)
    // The authoritative LIVE/HTS fields are published atomically because state is
    // also updated by ACL, RFCOMM-reader, parser and UI callbacks on different threads.
    private val liveSession = AtomicReference(LiveSessionState())
    private val packetTimeFormatter = ThreadLocal.withInitial { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    init {
        try { vehicleSnapshotDb.finalizeStaleActiveTrips() } catch (_: Throwable) { }
        loadLastKnownSnapshot()
    }

    private val connectedAddresses = ConcurrentHashMap.newKeySet<String>()
    private var receiverRegistered = false
    private val gattOperations = ArrayDeque<() -> Unit>()
    private var gattOperationRunning = false
    private var intentionalDisconnect = false
    private var sdpLatch: CountDownLatch? = null
    private var sdpAddress: String? = null
    private val cccdUuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    @SuppressLint("MissingPermission")
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            mainExecutor.execute {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    state = state.copy(status = "BLE connected", transport = "BLE/GATT")
                    publish()
                    try { g.discoverServices() } catch (_: Exception) {}
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    try { g.close() } catch (_: Exception) {}
                    if (gatt === g) gatt = null
                    state = state.copy(status = "BLE disconnected")
                    publish()
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            mainExecutor.execute {
                state = state.copy(
                    status = if (status == BluetoothGatt.GATT_SUCCESS) "BLE services discovered" else "BLE service discovery failed: $status"
                )
                publish()
            }
        }

        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val value = characteristic.value ?: return
            recordChannel("BLE RX ${characteristic.uuid} ${value.toHex()}")
            emitRx(value.toHex())
        }
    }
    private var bleScanGeneration = 0
    private var autoConnectAttemptedAddress: String? = null
    private var autoConnectInProgress = false
    // Hard gate: only one ConnectNext SppLink connection-attempt thread may exist.
    // This prevents Activity/service/manual refresh races from creating competing RFCOMM servers.
    private val connectNextAttemptActive = AtomicBoolean(false)
    private var autoConnectGeneration = 0
    private val channelRouter = LogicalChannelRouter()
    // Authoritative clean-room ConnectNext architecture facade. Android socket ownership remains here.
    private val connectNextMirrorSession = ConnectNextSessionController()
    // Read-only VIS decoder. It consumes only confirmed X451 VehicleInfo payloads.
    private val x451LiveDecoder = X451LiveSignalDecoder()
    private val autoConnectHandler = Handler(Looper.getMainLooper())
    private val highRatePublishScheduled = AtomicBoolean(false)
    private val highRatePublishAgain = AtomicBoolean(false)
    private val lastPacketCallbackAt = java.util.concurrent.atomic.AtomicLong(0L)
    private val lastDecoderEventAt = java.util.concurrent.atomic.AtomicLong(0L)
    private val lastDbSnapshotAt = java.util.concurrent.atomic.AtomicLong(0L)
    private val lastTelemetryPurgeAt = java.util.concurrent.atomic.AtomicLong(0L)
    private val lastDailyTelemetryDay = java.util.concurrent.atomic.AtomicLong(Long.MIN_VALUE)
    private var tripDetectionBaseline: X451LiveSignalDecoder.Values? = null
    private var lastTripProgressPersistAt: Long = 0L

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            try {
                when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val enabled = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_ON
                    recordChannel("BT STATE ${if (enabled) "ON" else "OFF"}")
                    if (!enabled) {
                        intentionalDisconnect = true
                        connectNextAttemptActive.set(false)
                        autoConnectInProgress = false
                        autoConnectGeneration++
                        autoConnectHandler.removeCallbacksAndMessages(null)
                        connectedAddresses.clear()
                        stopBleScan()
                        try { classicSocket?.close() } catch (_: Exception) {}
                        closeAllSppServers()
                        try { gatt?.close() } catch (_: Exception) {}
                        classicSocket = null
                        gatt = null
                    } else {
                        // A full Bluetooth OFF/ON cycle intentionally tears down the
                        // previous SppLink owner. Allow the fresh ON state to start a new
                        // single-owner server lifecycle.
                        intentionalDisconnect = false
                    }
                    refresh()
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    val device = getDeviceExtra(intent)
                    if (device != null) {
                        connectedAddresses += device.address
                        updateConnected(device.address, true)
                        recordChannel("ACL CONNECTED ${safeDeviceName(device)} ${safeDeviceAddress(device)}")
                        // ConnectNext is Classic/RFCOMM; stop the diagnostic BLE scan once the
                        // Classic ACL is up so this path matches the official app's quiet transport state.
                        if (safeDeviceName(device).equals("ConnectNext", ignoreCase = true)) stopBleScan()
                    }
                }
                BluetoothDevice.ACTION_UUID -> {
                val device = getDeviceExtra(intent)
                if (device != null) {
                    val uuids = try {
                        @Suppress("DEPRECATION") intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
                            ?.mapNotNull { (it as? android.os.ParcelUuid)?.uuid?.toString() } ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                    updateServiceUuids(safeDeviceAddress(device), uuids)
                    recordChannel("SDP UUID EVENT ${device.address}: ${if (uuids.isEmpty()) "none" else uuids.joinToString()}")
                    if (sdpAddress == safeDeviceAddress(device)) {
                        state = state.copy(classicSdpStatus = if (uuids.isEmpty()) "SDP completed — no UUIDs reported" else "SDP completed — ${uuids.size} UUID(s)", classicServiceUuids = uuids.distinct())
                        publish()
                        sdpLatch?.countDown()
                    }
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val device = getDeviceExtra(intent)
                    if (device != null) {
                        connectedAddresses -= device.address
                        updateConnected(device.address, false)
                        recordChannel("ACL DISCONNECTED ${safeDeviceName(device)} ${safeDeviceAddress(device)}")
                        if (!intentionalDisconnect) {
                            if (safeDeviceName(device).equals("ConnectNext", ignoreCase = true)) {
                                // IMPORTANT: an ACL disconnect is NOT proof that the phone-hosted
                                // SppLink RFCOMM server is dead. The HU can create short-lived ACL
                                // sessions while probing SDP/RFCOMM. Never destroy a healthy
                                // listening SppLink server here; only readHtsLoop() owns cleanup
                                // after an accepted application socket actually closes.
                                val serverStillOwned = synchronized(htsServerLock) {
                                    htsServerListening && sppServerSocket != null
                                }
                                val acceptedSocketOwned = incomingSppAccepted.get()
                                recordChannel("ACL DISCONNECT CONNECTNEXT — preserveSppServer=$serverStillOwned acceptedSocket=$acceptedSocketOwned")
                                if (acceptedSocketOwned) {
                                    // The accepted socket/reader is authoritative. Even if the
                                    // Android ACL callback arrives before BluetoothSocket.isConnected
                                    // flips, do not start a second lifecycle here.
                                    autoConnectInProgress = false
                                    connectNextAttemptActive.set(false)
                                } else if (!serverStillOwned) {
                                    incomingSppAccepted.set(false)
                                    connectNextAttemptActive.set(false)
                                }
                            }
                            autoConnectInProgress = false
                            val waitingServer = synchronized(htsServerLock) { htsServerListening && sppServerSocket != null }
                            if (!waitingServer && !incomingSppAccepted.get()) {
                                scheduleAutoConnect(1500L)
                            } else if (waitingServer) {
                                recordChannel("ACL DISCONNECT — existing SppLink listener retained; no replacement generation")
                            }
                        }
                    }
                }
                }
            } catch (e: Exception) {
                recordChannel("BT RECEIVER HANDLER FAILED: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getDeviceExtra(intent: Intent): BluetoothDevice? = try {
        if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        else intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    } catch (_: Exception) { null }

    private val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // VERIFIED from the official ConnectNext 6.40 HCI SDP exchange and the
    // official CBTConnectionManager implementation: ConnectNext's HTS RFCOMM
    // service is UUID(0L, 4L), i.e. 00000000-0000-0000-0000-000000000004.
    // The HU queries this UUID and the phone's SDP response advertises RFCOMM
    // server channel 7. The official service name is "SppLink".
    private val connectNextHtsUuid = UUID(0L, 4L)
    private val connectNextServiceName = "SppLink"
    // Verified from the successful 2026-09-13 capture: channel 7 / DLCI 14 is the
    // primary ConnectNext application path; 13 and 10 are diagnostic fallbacks.
    private val connectNextServerChannels = listOf(7, 13, 10)
    private val htsChannel = 7
    private val knownConnectNextUuids = listOf(connectNextHtsUuid)

    fun setListeners(
        onState: (BluetoothDiagnosticState) -> Unit,
        onPacket: (direction: String, hex: String) -> Unit = { _, _ -> }
    ) {
        stateListener = onState
        // Raw packets/events are persisted by ProtocolFileLogger. Do not retain or
        // stream them into the UI; this keeps capture memory bounded.
        packetListener = { _, _ -> }
        registerBluetoothReceiver()
        publish()
    }

    private fun registerBluetoothReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_UUID)
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.registerReceiver(context, bluetoothStateReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
            } else {
                @Suppress("DEPRECATION") context.registerReceiver(bluetoothStateReceiver, filter)
            }
            receiverRegistered = true
        } catch (_: Exception) { }
    }

    private fun updateConnected(address: String, connected: Boolean) {
        val updated = discovered[address]?.copy(connected = connected)
        if (updated != null) {
            discovered[address] = updated
            state = state.copy(
                devices = discovered.values.toList(),
                selectedDevice = if (state.selectedDevice?.address == address) updated else state.selectedDevice,
                status = if (connected) "${updated.name} connected" else if (state.selectedDevice?.address == address) "${updated.name} disconnected" else state.status
            )
            publish()
        } else {
            refresh()
        }
    }

    private fun selectedVehicleKey(): String? {
        val address = try {
            context.getSharedPreferences("altrozdrive", Context.MODE_PRIVATE)
                .getString("auto_vehicle_bt_address", null)
        } catch (_: Exception) { null }
        return address?.uppercase(Locale.US)?.takeIf { it.isNotBlank() }
    }

    /** Vehicle key used by background route recording. Kept read-only so transport ownership stays here. */
    fun currentRouteVehicleKey(): String? =
        (state.selectedDevice?.address ?: selectedVehicleKey())?.uppercase(Locale.US)?.takeIf { it.isNotBlank() }

    private fun loadLastKnownSnapshot() {
        try {
            // AltrozDrive's local SQLite snapshot is authoritative for offline UI.
            // Do not import the stale/mismatched ConnectNext VEHICLE_INFO row here.
            vehicleSnapshotDb.ensureSeeded()
            val key = selectedVehicleKey()
            val snapshot = if (key != null) {
                vehicleSnapshotDb.load(key) ?: vehicleSnapshotDb.loadMostRecent()
            } else {
                vehicleSnapshotDb.loadMostRecent()
            }
            if (snapshot == null) return
            state = state.copy(
                cachedOdo = snapshot.odo,
                cachedFuelPercent = snapshot.fuelPercent,
                cachedDteKm = snapshot.dteKm,
                cachedTripA = snapshot.tripA,
                cachedTripB = snapshot.tripB,
                cachedAvgFuelEcoTripA = snapshot.avgA,
                cachedAvgFuelEcoTripB = snapshot.avgB,
                cachedDataAt = snapshot.updatedAt,
                cachedDataSource = snapshot.source
            )
            recordChannel("LAST KNOWN LOAD source=LOCAL_DB updatedAt=${snapshot.updatedAt}")
        } catch (e: Exception) {
            recordChannel("LAST KNOWN LOAD FAILED ${e.message ?: e.javaClass.simpleName}")
        }
    }

    private fun persistValidatedVisSnapshot(v: X451LiveSignalDecoder.Values) {
        if (!v.validated) return
        if (v.odo == null && v.fuelPercent == null && v.dteKm == null &&
            v.tripA == null && v.tripB == null && v.avgFuelEcoTripA == null && v.avgFuelEcoTripB == null) return
        val key = (state.selectedDevice?.address ?: selectedVehicleKey())
            ?.uppercase(Locale.US)?.takeIf { it.isNotBlank() } ?: return
        try {
            val now = System.currentTimeMillis()
            // Keep the last-known row current without turning a 10-100 Hz VIS stream
            // into an SQLite write storm. A two-second checkpoint is enough for
            // offline startup and still preserves the real drive history below.
            if (now - lastDbSnapshotAt.get() < 2000L) return
            lastDbSnapshotAt.set(now)
            val snapshot = vehicleSnapshotDb.mergeAndSave(
                vehicleKey = key,
                uid = observedVehicleUid,
                odo = v.odo,
                fuelPercent = v.fuelPercent,
                dteKm = v.dteKm,
                tripA = v.tripA,
                tripB = v.tripB,
                avgA = v.avgFuelEcoTripA,
                avgB = v.avgFuelEcoTripB,
                source = "X451_LIVE"
            )
            val dayStart = java.util.Calendar.getInstance().apply {
                timeInMillis = now
                set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            if (lastDailyTelemetryDay.getAndSet(dayStart) != dayStart) {
                vehicleSnapshotDb.appendTelemetrySampleIfDayMissing(
                    VehicleSnapshotDb.TelemetrySample(
                        timestamp = now, vehicleKey = key, odo = v.odo,
                        fuelPercent = v.fuelPercent, dteKm = v.dteKm,
                        tripA = v.tripA, tripB = v.tripB,
                        avgA = v.avgFuelEcoTripA, avgB = v.avgFuelEcoTripB
                    )
                )
            }
            // Keep graph/history storage bounded while retaining a useful long-term window.
            if (now - lastTelemetryPurgeAt.get() > 24L * 60L * 60L * 1000L) {
                lastTelemetryPurgeAt.set(now)
                vehicleSnapshotDb.purgeHistoryOlderThanDays(180L)
            }
            state = state.copy(
                cachedOdo = snapshot.odo,
                cachedFuelPercent = snapshot.fuelPercent,
                cachedDteKm = snapshot.dteKm,
                cachedTripA = snapshot.tripA,
                cachedTripB = snapshot.tripB,
                cachedAvgFuelEcoTripA = snapshot.avgA,
                cachedAvgFuelEcoTripB = snapshot.avgB,
                cachedDataAt = snapshot.updatedAt,
                cachedDataSource = snapshot.source
            )
            recordChannel(
                "X451 LIVE DB UPDATE SUCCESS key=$key " +
                    "ODO=${snapshot.odo ?: "-"} FUEL=${snapshot.fuelPercent ?: "-"} " +
                    "DTE=${snapshot.dteKm ?: "-"} TRIPA=${snapshot.tripA ?: "-"} " +
                    "TRIPB=${snapshot.tripB ?: "-"} AVGA=${snapshot.avgA ?: "-"} AVGB=${snapshot.avgB ?: "-"}"
            )
        } catch (e: Exception) {
            recordChannel("LAST KNOWN SAVE FAILED ${e.message ?: e.javaClass.simpleName}")
        }
    }

    fun currentState(): BluetoothDiagnosticState = state


    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String = try { device.name ?: "Unnamed device" } catch (_: SecurityException) { "Permission required" } catch (_: Exception) { "Unnamed device" }

    private fun safeDeviceAddress(device: BluetoothDevice): String = try { device.address } catch (_: Exception) { "unknown" }

    private fun has(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= 31) arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        ) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasRequiredPermissions(): Boolean = requiredPermissions().all(::has)

    @SuppressLint("MissingPermission")
    fun refresh() {
        val enabled = try { adapter?.isEnabled == true } catch (_: SecurityException) { false }
        state = state.copy(
            bluetoothEnabled = enabled,
            status = when {
                adapter == null -> "Bluetooth not supported"
                !hasRequiredPermissions() -> "Bluetooth permission required"
                enabled -> "Ready"
                else -> "Bluetooth is OFF"
            }
        )
        discovered.clear()
        if (adapter != null && hasRequiredPermissions() && has(Manifest.permission.BLUETOOTH_CONNECT)) {
            try { adapter.bondedDevices?.forEach { addDevice(it, null, true) } } catch (_: SecurityException) { }
        }
        publish()
        // refresh() can be called by the Activity, permission callback, and foreground service.
        // It must never create/restart an already-live SppLink server.
        if (!htsServerListening && htsSocket?.isConnected != true && classicSocket?.isConnected != true) {
            scheduleAutoConnect(700L)
        }
    }

    @SuppressLint("MissingPermission")
    private fun isLikelyVehicleDevice(device: BluetoothDevice): Boolean {
        val name = safeDeviceName(device).lowercase(Locale.US)
        val saved = try { context.getSharedPreferences("altrozdrive", Context.MODE_PRIVATE).getString("auto_vehicle_bt_address", null) } catch (_: Exception) { null }
        return saved == safeDeviceAddress(device) ||
            listOf("connectnext", "tata", "tml", "infotainment", "head unit", "headunit").any { name.contains(it) }
    }

    @SuppressLint("MissingPermission")
    private fun scheduleAutoConnect(delayMs: Long = 1200L) {
        if (adapter == null || !hasRequiredPermissions() || adapter?.isEnabled != true || intentionalDisconnect) return
        if (autoConnectInProgress || htsServerListening || htsSocket?.isConnected == true || classicSocket?.isConnected == true) return
        val generation = ++autoConnectGeneration
        autoConnectHandler.removeCallbacksAndMessages(null)
        autoConnectHandler.postDelayed({
            if (generation != autoConnectGeneration || intentionalDisconnect || autoConnectInProgress || htsServerListening || htsSocket?.isConnected == true || classicSocket?.isConnected == true) return@postDelayed
            val bonded = try { adapter?.bondedDevices?.toList().orEmpty() } catch (_: Exception) { emptyList() }
            val candidate = bonded.firstOrNull { isLikelyVehicleDevice(it) }
                ?: bonded.singleOrNull { it.type == BluetoothDevice.DEVICE_TYPE_CLASSIC || it.type == BluetoothDevice.DEVICE_TYPE_DUAL }
            if (candidate != null) {
                autoConnectInProgress = true
                autoConnectAttemptedAddress = safeDeviceAddress(candidate)
                val info = BluetoothDeviceInfo(safeDeviceName(candidate), safeDeviceAddress(candidate), when(candidate.type) {
                    BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                    BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                    else -> "Unknown"
                }, paired = true, connected = connectedAddresses.contains(candidate.address))
                recordChannel("AUTO-CONNECT candidate=${safeDeviceName(candidate)}")
                state = state.copy(autoConnect = true, autoConnectDevice = safeDeviceName(candidate), status = "Automatically connecting to ${safeDeviceName(candidate)}…")
                publish()
                connect(info, fromAuto = true)
            } else {
                state = state.copy(autoConnect = true, autoConnectDevice = "Waiting for paired vehicle")
                publish()
            }
        }, delayMs)
    }

    private fun addDevice(device: BluetoothDevice, rssi: Int?, paired: Boolean) {
        val name = safeDeviceName(device)
        val type = when (device.type) {
            BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
            else -> "Unknown"
        }
        val previous = discovered[device.address]
        discovered[device.address] = BluetoothDeviceInfo(
            name, device.address, type, rssi ?: previous?.rssi, paired || (previous?.paired == true),
            connectedAddresses.contains(device.address), previous?.serviceUuids ?: emptyList()
        )
        state = state.copy(devices = discovered.values.toList())
        publish()
        if (previous == null) recordChannel("DEVICE ${name} | ${device.address} | ${type} | RSSI=${rssi ?: "n/a"} | paired=${paired}")
    }


    private fun updateServiceUuids(address: String, uuids: List<String>) {
        val old = discovered[address] ?: return
        discovered[address] = old.copy(serviceUuids = uuids.distinct())
        state = state.copy(devices = discovered.values.toList().takeLast(100), selectedDevice = if (state.selectedDevice?.address == address) discovered[address] else state.selectedDevice)
        publish()
    }

    private fun emitRx(hex: String) {
        // hex is generated directly from the received bytes, so its byte count is
        // deterministic and does not require regex/split allocations on every packet.
        val byteCount = if (hex.isEmpty()) 0L else (hex.length + 1L) / 3L
        val now = packetTimeFormatter.get().format(Date())
        state = state.copy(
            rxPackets = state.rxPackets + 1,
            rxBytes = state.rxBytes + byteCount,
            lastRxAt = now,
            lastRxTransport = state.transport,
            readError = "None",
            connectedAt = state.connectedAt
        )
        protocolLogger.log("RX transport=${state.transport} bytes=$byteCount HEX=$hex")
        // ProtocolFileLogger already flushes every 50 lines; do not force a disk
        // flush for every Bluetooth packet on the hot path.
        publishHighRate()
    }

    private fun recordChannel(message: String) {
        // Debug/protocol events are disk-only. Do not copy them into state and do
        // not publish a UI update for each event; this is a hot path during capture.
        protocolLogger.log("EVENT $message")
    }

    private fun emitTx(hex: String, label: String = "TX") {
        var byteCount = 0L
        var tokenChars = 0
        for (ch in hex) {
            if (ch.isWhitespace()) {
                if (tokenChars > 0) { byteCount++; tokenChars = 0 }
            } else tokenChars++
        }
        if (tokenChars > 0) byteCount++
        val now = packetTimeFormatter.get().format(Date())
        state = state.copy(txPackets = state.txPackets + 1, txBytes = state.txBytes + byteCount, lastTxAt = now)
        protocolLogger.log("$label transport=${state.transport} bytes=$byteCount HEX=$hex")
        // Keep logging buffered so filesystem latency cannot stall packet handling.
        publishHighRate()
    }

    private fun setReadError(message: String) {
        state = state.copy(readError = message, status = message)
        publish()
    }

    private fun emitAdvertisement(result: ScanResult) {
        val record = result.scanRecord?.bytes ?: return
        val now = packetTimeFormatter.get().format(Date())
        state = state.copy(
            advertisementPackets = state.advertisementPackets + 1,
            advertisementBytes = state.advertisementBytes + record.size
        )
        protocolLogger.log("ADV ${result.device.address} HEX=${record.toHex()}")
        publishHighRate()
    }

    @SuppressLint("MissingPermission")
    fun startBleScan() {
        if (adapter == null) return
        if (!hasRequiredPermissions()) {
            state = state.copy(status = "Bluetooth permission required")
            publish()
            return
        }
        if (!adapter.isEnabled) {
            state = state.copy(status = "Turn Bluetooth ON first")
            publish()
            return
        }
        stopBleScan()
        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            state = state.copy(status = "BLE scanner unavailable")
            publish()
            return
        }
        bleScanGeneration += 1
        val generation = bleScanGeneration
        state = state.copy(scanning = true, transport = "BLE", status = "Scanning for BLE devices…")
        publish()
        try {
            scanner?.startScan(scanCallback)
        } catch (e: SecurityException) {
            state = state.copy(scanning = false, status = "BLE scan permission was revoked")
            publish()
            recordChannel("BLE SCAN START SECURITY EXCEPTION: ${e.message ?: "permission"}")
            return
        } catch (e: IllegalStateException) {
            state = state.copy(scanning = false, status = "BLE scanner is temporarily unavailable")
            publish()
            recordChannel("BLE SCAN START FAILED: ${e.message ?: "illegal state"}")
            return
        }
        mainHandler.postDelayed({
            if (bleScanGeneration == generation && state.scanning) stopBleScan()
        }, 15000L)
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        try { scanner?.stopScan(scanCallback) } catch (_: SecurityException) { } catch (_: IllegalStateException) { }
        scanner = null
        if (state.scanning) {
            state = state.copy(scanning = false, status = "Scan stopped")
            publish()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                addDevice(result.device, result.rssi, false)
                emitAdvertisement(result)
            } catch (e: Exception) {
                recordChannel("BLE RESULT HANDLER FAILED: ${e.message ?: e.javaClass.simpleName}")
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            results.forEach { result ->
                try { addDevice(result.device, result.rssi, false); emitAdvertisement(result) } catch (e: Exception) { recordChannel("BLE BATCH RESULT FAILED: ${e.message ?: e.javaClass.simpleName}") }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            state = state.copy(scanning = false, status = "BLE scan failed: $errorCode")
            publish()
            recordChannel("BLE SCAN FAILED code=$errorCode")
        }
    }

    @SuppressLint("MissingPermission")
    fun startClassicDiscovery() {
        if (adapter == null || !hasRequiredPermissions()) {
            state = state.copy(status = "Bluetooth permission required")
            publish()
            return
        }
        if (!adapter.isEnabled) {
            state = state.copy(status = "Turn Bluetooth ON first")
            publish()
            return
        }
        try { context.unregisterReceiver(classicReceiver) } catch (_: Exception) { }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                ContextCompat.registerReceiver(
                    context,
                    classicReceiver,
                    IntentFilter(BluetoothDevice.ACTION_FOUND),
                    ContextCompat.RECEIVER_EXPORTED
                )
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(classicReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            }
        } catch (_: Exception) { }
        val started = try {
            adapter.cancelDiscovery()
            adapter.startDiscovery()
        } catch (e: SecurityException) {
            state = state.copy(status = "Bluetooth permission was revoked")
            publish()
            recordChannel("CLASSIC DISCOVERY SECURITY EXCEPTION: ${e.message ?: "permission"}")
            return
        } catch (e: IllegalStateException) {
            state = state.copy(status = "Classic discovery is temporarily unavailable")
            publish()
            recordChannel("CLASSIC DISCOVERY FAILED: ${e.message ?: "illegal state"}")
            return
        }
        state = state.copy(transport = "Classic", status = if (started) "Classic discovery running…" else "Classic discovery could not start")
        publish()
    }

    @SuppressLint("MissingPermission")
    fun stopClassicDiscovery() {
        try { adapter?.cancelDiscovery() } catch (_: SecurityException) { } catch (_: IllegalStateException) { }
        try { context.unregisterReceiver(classicReceiver) } catch (_: Exception) { }
    }

    private val classicReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (BluetoothDevice.ACTION_FOUND == intent.action) {
                val device = if (Build.VERSION.SDK_INT >= 33)
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    addDevice(device, rssi, false)
                    recordChannel("CLASSIC FOUND ${safeDeviceName(device)} | ${safeDeviceAddress(device)} | RSSI=${rssi}")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connect(deviceInfo: BluetoothDeviceInfo, fromAuto: Boolean = false) {
        // Invalidate any queued auto-connect callback before a manual connection starts.
        autoConnectGeneration++
        autoConnectHandler.removeCallbacksAndMessages(null)
        // Auto-connect is advisory; never tear down an already-listening/accepted SppLink session.
        if (fromAuto && (htsServerListening || incomingSppAccepted.get() || htsSocket?.isConnected == true)) {
            recordChannel("AUTO-CONNECT ignored — SppLink session/server already owned by ConnectNext lifecycle")
            return
        }
        requestedHtsServices.clear()
        resetEvidenceSession()
        synchronized(htsTxLock) {
            htsSequence = 0
            htsNextTxSequence = 0
        }
        htsReassembler.reset()
        x451LiveDecoder.reset()
        liveSession.set(LiveSessionState())
        incomingSppAccepted.set(false)
        connectNextWatchdogHandler.removeCallbacks(connectNextWatchdog)
        connectNextLastProgressAt = 0L
        connectNextNegotiationStartedAt = 0L
        connectNextSessionAcceptedAt = 0L
        if (adapter == null || !hasRequiredPermissions()) {
            state = state.copy(status = "Bluetooth permission required")
            publish()
            return
        }
        if (!fromAuto) disconnect()
        intentionalDisconnect = false
        autoConnectInProgress = true
        val device = try { adapter.getRemoteDevice(deviceInfo.address) } catch (_: Exception) { null }
        if (device == null) {
            autoConnectInProgress = false
            state = state.copy(status = "Device not found")
            publish()
            scheduleAutoConnect(3000L)
            return
        }

        try { context.getSharedPreferences("altrozdrive", Context.MODE_PRIVATE).edit().putString("auto_vehicle_bt_address", deviceInfo.address).apply() } catch (_: Exception) {}
        state = state.copy(
            selectedDevice = deviceInfo,
            transport = if (device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC || device.type == BluetoothDevice.DEVICE_TYPE_DUAL) "Classic / RFCOMM" else "BLE / GATT",
            status = "Connecting to ${deviceInfo.name}…",
            characteristics = emptyList(),
            rxPackets = 0,
            rxBytes = 0,
            lastRxAt = "Never",
            txPackets = 0,
            txBytes = 0,
            lastTxAt = "Never",
            readError = "None",
            liveVisValidated = false,
            liveDataAtMs = 0L,
            classicSdpStatus = "Not requested",
            classicServiceUuids = emptyList(),
            connectedAt = "Never",
            channelAttempts = emptyList(),
            diagnosticEvents = emptyList(),
            htsConnected = false,
            htsVersion = "Unknown",
            htsFrames = 0,
            htsJsonFrames = 0,
            htsBinaryFrames = 0,
            htsLastSequence = -1,
            htsLastMessageType = "None",
            htsServices = emptyList(),
            htsVehicleBinaryFrames = 0,
            htsMessageTypes = emptyList(),
            htsLastPayloadPreview = "None"
        )
        publish()
        // Run a short BLE advertisement scan in parallel with the connection attempt so a dual-mode
        // ConnectNext unit is not missed just because Android reports it as Classic.
        startBleScan()

        if (device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC || device.type == BluetoothDevice.DEVICE_TYPE_DUAL) {
            connectClassic(device)
        } else {
            try {
                gatt = device.connectGatt(context, false, gattCallback)
                if (gatt == null) {
                    state = state.copy(status = "BLE connection could not be created", readError = "connectGatt returned null")
                    publish()
                }
            } catch (e: SecurityException) {
                state = state.copy(status = "Bluetooth permission was revoked", readError = e.message ?: "SecurityException")
                publish()
            } catch (e: Exception) {
                state = state.copy(status = "BLE connection failed", readError = e.message ?: e.javaClass.simpleName)
                publish()
            }
        }
    }

    private fun applyProtocolSignal(name: String, value: Double) {
        var changed = false
        when (name) {
            "ODO_SIGNAL" -> if (value in 0.0..1_000_000.0) {
                val v = value.toInt()
                if (state.liveOdo != v) changed = true
                state = state.copy(liveOdo = v)
            }
            "DTE_SIGNAL" -> if (value in 0.0..2_000.0) {
                val v = value.toInt()
                if (state.liveDteKm != v) changed = true
                state = state.copy(liveDteKm = v)
            }
            "DASHBOARD_FUEL" -> if (value in 0.0..100.0) {
                val v = value.toInt()
                if (state.liveFuelPercent != v) changed = true
                state = state.copy(liveFuelPercent = v)
            }
            // These were already being decoded/emitted upstream but had no case here,
            // so trip A/B and the fuel-economy averages were silently discarded.
            "TRIPA" -> if (value in 0.0..100_000.0) {
                val v = value.toFloat()
                if (state.liveTripA != v) changed = true
                state = state.copy(liveTripA = v)
            }
            "TRIPB" -> if (value in 0.0..100_000.0) {
                val v = value.toFloat()
                if (state.liveTripB != v) changed = true
                state = state.copy(liveTripB = v)
            }
            "AVG_FUEL_ECO_TRIPA" -> if (value in 0.0..100.0) {
                val v = value.toFloat()
                if (state.liveAvgFuelEcoTripA != v) changed = true
                state = state.copy(liveAvgFuelEcoTripA = v)
            }
            "AVG_FUEL_ECO_TRIPB" -> if (value in 0.0..100.0) {
                val v = value.toFloat()
                if (state.liveAvgFuelEcoTripB != v) changed = true
                state = state.copy(liveAvgFuelEcoTripB = v)
            }
            "AC_TEMPERATURE" -> if (value in -20.0..150.0) {
                val v = value.toInt()
                if (state.liveAcTempC != v) changed = true
                state = state.copy(liveAcTempC = v)
            }
            "FAN_SPEED" -> if (value in 0.0..20.0) {
                val v = value.toInt()
                if (state.liveFanSpeed != v) changed = true
                state = state.copy(liveFanSpeed = v)
            }
            "AC_ON" -> {
                val v = value != 0.0
                if (state.liveAcOn != v) changed = true
                state = state.copy(liveAcOn = v)
            }
            "SPEED" -> if (value in 0.0..260.0) {
                val v = value.toInt()
                if (state.liveSpeedKph != v) changed = true
                state = state.copy(liveSpeedKph = v)
            }
            "RPM" -> if (value in 0.0..9_000.0) {
                val v = value.toInt()
                if (state.liveRpm != v) changed = true
                state = state.copy(liveRpm = v)
            }
            "BATTERY_VOLTAGE" -> if (value in 0.0..20.0) {
                if (state.liveBatteryVoltage != value) changed = true
                state = state.copy(liveBatteryVoltage = value)
            }
            "COOLANT_TEMPERATURE" -> if (value in -20.0..150.0) {
                val v = value.toInt()
                if (state.liveCoolantTempC != v) changed = true
                state = state.copy(liveCoolantTempC = v)
            }
            "IGNITION" -> {
                val v = value != 0.0
                if (state.liveIgnitionOn != v) changed = true
                state = state.copy(liveIgnitionOn = v)
            }
            "VEHICLE_WARNING" -> if (value in 0.0..1_000.0) {
                val v = value.toInt()
                if (state.liveVehicleWarning != v) changed = true
                state = state.copy(liveVehicleWarning = v)
            }
        }
        if (changed) {
            state = state.copy(liveDataAt = packetTimeFormatter.get().format(Date()))
            recordChannel("CN SIGNAL $name=$value")
            publish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectClassic(device: BluetoothDevice) {
        // Proven ConnectNext behavior: the phone advertises the SppLink RFCOMM
        // service and waits for the HU to connect. The successful session on
        // 2026-09-14 accepted the SppLink connection several minutes after the
        // listener was created. Do NOT run outbound RFCOMM probes in parallel;
        // they can disturb the HU's SPP negotiation and prevent the SppLink
        // listener from ever being accepted.
        // Transport invariant for 5.9.8.1: exactly one phone-hosted SppLink listener owns
        // the ConnectNext UUID until BluetoothServerSocket.accept() returns the selected HU.
        // ACL_CONNECTED/ACL_DISCONNECTED are only link-state hints and must never replace the
        // listener before accept() establishes the authoritative BluetoothSocket.
        if (!connectNextAttemptActive.compareAndSet(false, true)) {
            recordChannel("PRIMARY CONNECTNEXT IGNORED — existing SppLink lifecycle is already active")
            return
        }
        Thread {
            try {
                try { adapter?.cancelDiscovery() } catch (_: SecurityException) {} catch (_: IllegalStateException) {}
                recordChannel("PRIMARY: ConnectNext SppLink server only service=$connectNextServiceName uuid=$connectNextHtsUuid")
                startConnectNextHtsServer(device)

                val deadline = System.currentTimeMillis() + 120_000L
                while (!intentionalDisconnect && !incomingSppAccepted.get() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(100L)
                }

                if (intentionalDisconnect) return@Thread
                if (incomingSppAccepted.get() && htsSocket?.isConnected == true) {
                    autoConnectInProgress = false
                    connectNextAttemptActive.set(false)
                    recordChannel("CONNECTNEXT HTS SERVER SESSION ACCEPTED — primary SppLink path won")
                    return@Thread
                }

                // If no SppLink socket was accepted during the bounded wait, treat the
                // current Android RFCOMM listener as a stale advertisement and re-arm it.
                // A manual reconnect was previously required to get the HU to discover a
                // fresh SppLink server after an ACL disconnect. Re-arming here reproduces
                // that recovery automatically without changing the ConnectNext protocol.
                state = state.copy(
                    status = "Refreshing ConnectNext connection…",
                    readError = "No incoming SppLink RFCOMM session yet — refreshing listener"
                )
                publish()
                recordChannel("PRIMARY HTS SERVER WAIT EXPIRED — re-arming SppLink listener; no outgoing RFCOMM probing")

                var closedOwnedServer = false
                synchronized(htsServerLock) {
                    val ownedServer = sppServerSocket
                    val ownedGeneration = htsServerGeneration
                    if (ownedServer != null && ownedGeneration == htsServerGeneration && !incomingSppAccepted.get()) {
                        // Invalidate the accept thread before closing the socket so its
                        // IOException cannot schedule a second recovery for the same server.
                        htsServerGeneration++
                        sppServerSocket = null
                        sppServerSockets.remove(ownedServer)
                        htsServerListening = false
                        try { ownedServer.close() } catch (_: Exception) {}
                        closedOwnedServer = true
                        recordChannel("HTS SERVER RE-ARM CLOSED stale listener generation=$ownedGeneration")
                    }
                }

                if (!intentionalDisconnect && closedOwnedServer) {
                    autoConnectInProgress = false
                    connectNextAttemptActive.set(false)
                    scheduleAutoConnect(1500L)
                }
            } catch (e: InterruptedException) {
                if (!intentionalDisconnect) recordChannel("CONNECTNEXT SERVER WAIT INTERRUPTED")
            } catch (e: Exception) {
                if (!intentionalDisconnect) recordChannel("CONNECTNEXT SERVER ATTEMPT FAILED: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                // Release the attempt gate after the 120-second passive wait. If the listener
                // is re-armed later, the new generation owns the server lifecycle.
                if (!intentionalDisconnect) {
                    autoConnectInProgress = false
                    connectNextAttemptActive.set(false)
                    val owned = synchronized(htsServerLock) { htsServerListening && sppServerSocket != null }
                    if (owned) {
                        recordChannel("CONNECTNEXT SPP LINK OWNER RETAINED — no fallback probes or replacement server")
                    }
                }
            }
        }.also { it.name = "AltrozDrive-ConnectNext-connect"; it.isDaemon = true; it.start() }
    }

    @SuppressLint("MissingPermission")
    private fun runConnectNextDiagnosticFallbacks(device: BluetoothDevice) {
        if (intentionalDisconnect || incomingSppAccepted.get()) return

        val advertised = requestSdpAndWait(device)
        if (intentionalDisconnect || incomingSppAccepted.get()) return

        // First reproduce the exact physical RFCOMM channels seen in the successful HCI trace.
        for (serverChannel in connectNextServerChannels) {
            if (intentionalDisconnect || incomingSppAccepted.get()) return
            if (connectDirectConnectNextChannel(device, serverChannel)) {
                connectNextAttemptActive.set(false)
                autoConnectInProgress = false
                return
            }
            // The phone-hosted SppLink server may have accepted while the direct probe was running.
            if (incomingSppAccepted.get() && htsSocket?.isConnected == true) {
                connectNextAttemptActive.set(false)
                autoConnectInProgress = false
                return
            }
        }

        // Then preserve the old diagnostic UUID probing behavior. This is only a probe:
        // no guessed application bytes are written to an arbitrary RFCOMM service.
        val candidates = (listOf(connectNextHtsUuid) + advertised.filter { uuid ->
            uuid != UUID.fromString("00001105-0000-1000-8000-00805F9B34FB")
        }).distinct()
        for (uuid in candidates) {
            if (intentionalDisconnect || incomingSppAccepted.get()) return
            for (insecure in listOf(false, true)) {
                if (intentionalDisconnect || incomingSppAccepted.get()) return
                val security = if (insecure) "insecure" else "secure"
                recordChannel("TRY $uuid / $security")
                val socket = try {
                    if (insecure) device.createInsecureRfcommSocketToServiceRecord(uuid)
                    else device.createRfcommSocketToServiceRecord(uuid)
                } catch (e: Exception) {
                    recordChannel("FAILED $uuid / $security: create ${e.message ?: e.javaClass.simpleName}")
                    continue
                }
                classicSocket = socket
                val timedOut = AtomicBoolean(false)
                val watchdog = Thread {
                    try {
                        Thread.sleep(8_000L)
                        if (!socket.isConnected && !intentionalDisconnect) {
                            timedOut.set(true)
                            recordChannel("CONNECT-TIMEOUT $uuid / $security after 8s")
                            try { socket.close() } catch (_: Exception) {}
                        }
                    } catch (_: InterruptedException) {}
                }
                watchdog.isDaemon = true
                watchdog.start()
                try {
                    state = state.copy(status = "Trying RFCOMM $uuid ($security)…")
                    publish()
                    try { socket.connect() } finally { watchdog.interrupt() }
                    if (timedOut.get() || !socket.isConnected) throw java.io.IOException("RFCOMM connection timeout")
                    connectedAddresses += device.address
                    updateConnected(device.address, true)
                    state = state.copy(
                        status = "RFCOMM connected — waiting for application data",
                        transport = "Classic / RFCOMM $uuid",
                        connectedAt = packetTimeFormatter.get().format(Date()),
                        readError = "None"
                    )
                    publish()
                    recordChannel("CONNECTED $uuid / $security")

                    if (uuid == connectNextHtsUuid) {
                        htsSocket = socket
                        state = state.copy(
                            htsConnected = false,
                            status = "SPP connected — waiting for ConnectNext HTS handshake",
                            transport = "Classic / RFCOMM ConnectNext HTS UUID4"
                        )
                        publish()
                        // The HTS parser/reassembler owns this path; never synthesize a bootstrap frame.
                        val validHts = AtomicBoolean(false)
                        val reader = Thread {
                            val input = try { socket.inputStream } catch (_: Exception) { return@Thread }
                            val buffer = ByteArray(4096)
                            try {
                                while (socket.isConnected && !intentionalDisconnect && !incomingSppAccepted.get()) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    if (count == 0) continue
                                    val bytes = buffer.copyOf(count)
                                    emitRx(bytes.toHex())
                                    htsReassembler.append(bytes) { frame ->
                                        validHts.set(true)
                                        handleMirroredHtsFrame(frame, device)
                                    }
                                }
                            } catch (e: Exception) {
                                if (!intentionalDisconnect && !validHts.get()) {
                                    recordChannel("RFCOMM $uuid reader failed: ${e.message ?: e.javaClass.simpleName}")
                                }
                            }
                        }
                        reader.name = "AltrozDrive-ConnectNext-reader"
                        reader.isDaemon = true
                        reader.start()
                        val deadline = System.currentTimeMillis() + 15_000L
                        while (!intentionalDisconnect && !incomingSppAccepted.get() && !validHts.get() && System.currentTimeMillis() < deadline) {
                            Thread.sleep(100L)
                        }
                        if (validHts.get()) {
                            state = state.copy(htsConnected = true, status = "ConnectNext HTS active — application data received")
                            publish()
                            recordChannel("CN UUID DATA CHANNEL CONFIRMED $uuid / $security")
                            connectNextAttemptActive.set(false)
                            autoConnectInProgress = false
                            return
                        }
                    } else {
                        // Standard profile probe: collect raw application bytes for diagnostics,
                        // but do not label them as ConnectNext data until an HTS frame is parsed.
                        val received = AtomicBoolean(false)
                        val reader = Thread {
                            val input = try { socket.inputStream } catch (_: Exception) { return@Thread }
                            val buffer = ByteArray(4096)
                            try {
                                while (socket.isConnected && !intentionalDisconnect) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    if (count == 0) continue
                                    received.set(true)
                                    emitRx(buffer.copyOf(count).toHex())
                                }
                            } catch (_: Exception) {}
                        }
                        reader.name = "AltrozDrive-RFCOMM-probe-reader"
                        reader.isDaemon = true
                        reader.start()
                        val deadline = System.currentTimeMillis() + 15_000L
                        while (!intentionalDisconnect && !incomingSppAccepted.get() && !received.get() && System.currentTimeMillis() < deadline) {
                            Thread.sleep(100L)
                        }
                        if (received.get()) {
                            recordChannel("RFCOMM PROBE RECEIVED DATA uuid=$uuid / $security — raw bytes preserved")
                        }
                    }
                } catch (e: Exception) {
                    try { socket.close() } catch (_: Exception) {}
                    recordChannel("FAILED $uuid / $security: ${e.message ?: e.javaClass.simpleName}")
                }
                if (classicSocket === socket && htsSocket !== socket) classicSocket = null
                if (htsSocket === socket && !socket.isConnected) htsSocket = null
                try { socket.close() } catch (_: Exception) {}
                if (incomingSppAccepted.get() && htsSocket?.isConnected == true) return
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startConnectNextHtsServer(selectedDevice: BluetoothDevice) {
        if (intentionalDisconnect) return
        synchronized(htsServerLock) {
            // SppLink is a long-lived phone-hosted RFCOMM endpoint. Exactly one
            // BluetoothServerSocket owns the SDP record until the HU actually connects.
            // Never close/recreate a live or still-owned listener just because another
            // lifecycle callback fires.
            if (htsServerListening && sppServerSocket != null) {
                recordChannel("HTS SERVER START IGNORED — SppLink listener already active generation=$htsServerGeneration")
                return
            }
            if (sppServerSocket != null || sppServerSockets.isNotEmpty()) {
                recordChannel("HTS SERVER START IGNORED — stale SppLink socket exists; waiting for its owner cleanup")
                return
            }
            htsServerGeneration++
            incomingSppAccepted.set(false)
            startSppServerForLocked(selectedDevice, connectNextHtsUuid, connectNextServiceName, htsServerGeneration)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSppServerFor(selectedDevice: BluetoothDevice, uuid: UUID, serviceName: String) {
        synchronized(htsServerLock) {
            if (htsServerListening && sppServerSocket != null) return
            htsServerGeneration++
            startSppServerForLocked(selectedDevice, uuid, serviceName, htsServerGeneration)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startSppServerForLocked(selectedDevice: BluetoothDevice, uuid: UUID, serviceName: String, generation: Long) {
        val server = try {
            // Official ConnectNext 6.40 uses the secure Android RFCOMM server API for SppLink.
            adapter?.listenUsingRfcommWithServiceRecord(serviceName, uuid)
        } catch (e: Exception) {
            recordChannel("HTS SERVER CREATE FAILED uuid=$uuid: ${e.message ?: e.javaClass.simpleName}")
            return
        }
        if (server == null) {
            recordChannel("HTS SERVER CREATE FAILED uuid=$uuid: null server")
            return
        }
        sppServerSockets.add(server)
        sppServerSocket = server
        htsServerListening = true
        recordChannel("HTS SERVER LISTENING service=$serviceName uuid=$uuid generation=$generation")
        state = state.copy(
            transport = "Classic / RFCOMM SPP server ($serviceName)",
            status = "Waiting for ConnectNext HU connection…",
            readError = "None"
        )
        publish()

        val acceptThread = Thread {
            try {
                recordChannel("HTS SERVER ACCEPT waiting service=$serviceName uuid=$uuid generation=$generation")
                // accept() is the authoritative SppLink event. Do not infer an application
                // connection from ACL_CONNECTED; ConnectNext may briefly raise/lower ACL
                // while discovering the RFCOMM service. Keep the SAME server owner alive
                // until accept() actually returns a socket.
                var socket: android.bluetooth.BluetoothSocket? = null
                while (!intentionalDisconnect) {
                    val candidate = try { server.accept() } catch (e: java.io.IOException) {
                        throw e
                    }
                    val stillOwner = synchronized(htsServerLock) {
                        sppServerSocket === server && htsServerGeneration == generation
                    }
                    if (!stillOwner) {
                        recordChannel("SPP SERVER ACCEPT STALE generation=$generation — closing unowned socket")
                        try { candidate.close() } catch (_: Exception) {}
                        return@Thread
                    }

                    val remote = try { candidate.remoteDevice } catch (_: Exception) { null }
                    val remoteAddress = try { remote?.address } catch (_: Exception) { null }
                    recordChannel("HTS SERVER ACCEPT RETURNED service=$serviceName uuid=$uuid remote=${remoteAddress ?: "unknown"} connected=${candidate.isConnected} generation=$generation")
                    if (remoteAddress != null && !remoteAddress.equals(selectedDevice.address, ignoreCase = true)) {
                        recordChannel("SPP SERVER REJECT unrelated remote=$remoteAddress — listener remains active")
                        try { candidate.close() } catch (_: Exception) {}
                        continue
                    }
                    if (!candidate.isConnected) {
                        recordChannel("SPP SERVER ACCEPT RETURNED DISCONNECTED SOCKET — listener remains active")
                        try { candidate.close() } catch (_: Exception) {}
                        continue
                    }
                    socket = candidate
                    break
                }
                val acceptedSocket = socket ?: return@Thread

                // Atomically transfer ownership: from this point the accepted socket, not
                // the server listener, is the authoritative ConnectNext transport. No ACL
                // callback is allowed to tear down a waiting server before this point.
                val ownershipTransferred = synchronized(htsServerLock) {
                    val stillOwner = sppServerSocket === server && htsServerGeneration == generation
                    if (!stillOwner) {
                        recordChannel("SPP SERVER ACCEPT LOST OWNERSHIP generation=$generation — closing accepted socket")
                        try { acceptedSocket.close() } catch (_: Exception) {}
                        false
                    } else if (!incomingSppAccepted.compareAndSet(false, true)) {
                        recordChannel("SPP SERVER ACCEPT DUPLICATE generation=$generation — closing accepted socket")
                        try { acceptedSocket.close() } catch (_: Exception) {}
                        false
                    } else {
                        // Transfer ownership under one lock before publishing any connected
                        // state. The listener is now detached; no later ACL callback can make
                        // us start a replacement server for this accepted session.
                        sppServerSocket = null
                        sppServerSockets.remove(server)
                        htsServerListening = false
                        classicSocket = acceptedSocket
                        htsSocket = acceptedSocket
                        connectNextSessionAcceptedAt = System.currentTimeMillis()
                        connectNextNegotiationStartedAt = 0L
                        liveSession.set(LiveSessionState())
                        true
                    }
                }
                if (!ownershipTransferred) return@Thread
                if (intentionalDisconnect) {
                    try { acceptedSocket.close() } catch (_: Exception) {}
                    return@Thread
                }
                val remote = try { acceptedSocket.remoteDevice } catch (_: Exception) { null }
                val remoteAddress = try { remote?.address } catch (_: Exception) { null }
                recordChannel("HTS SERVER SOCKET OWNED service=$serviceName uuid=$uuid remote=${remoteAddress ?: "unknown"} connected=${acceptedSocket.isConnected} generation=$generation")
                connectNextAttemptActive.set(false)
                autoConnectInProgress = false
                markConnectNextProgress()
                connectNextWatchdogHandler.removeCallbacks(connectNextWatchdog)
                connectNextWatchdogHandler.postDelayed(connectNextWatchdog, 5_000L)
                connectedAddresses += selectedDevice.address
                updateConnected(selectedDevice.address, true)
                state = state.copy(
                    transport = "Classic / RFCOMM SPP server (incoming, $serviceName)",
                    status = "ConnectNext RFCOMM session accepted — waiting for application data",
                    connectedAt = packetTimeFormatter.get().format(Date()),
                    readError = "None",
                    htsConnected = false
                )
                publish()
                val reader = Thread { readHtsLoop(acceptedSocket, selectedDevice) }
                reader.name = "AltrozDrive-ConnectNext-server-reader"
                reader.isDaemon = true
                reader.start()
                recordChannel("HTS SERVER APPLICATION READER STARTED service=$serviceName uuid=$uuid generation=$generation")
            } catch (e: java.io.IOException) {
                val currentOwner = synchronized(htsServerLock) {
                    sppServerSocket === server && htsServerGeneration == generation
                }
                if (currentOwner) {
                    synchronized(htsServerLock) {
                        if (sppServerSocket === server && htsServerGeneration == generation) {
                            sppServerSocket = null
                            sppServerSockets.remove(server)
                            htsServerListening = false
                        }
                    }
                }
                if (!intentionalDisconnect) {
                    recordChannel("SPP SERVER ACCEPT FAILED uuid=$uuid generation=$generation currentOwner=$currentOwner: ${e.message ?: e.javaClass.simpleName}")
                    // A closed/stale accept thread must never restart the SppLink lifecycle.
                    // Only the thread that still owns the current server may release the gate
                    // and schedule one recovery attempt.
                    if (currentOwner) {
                        autoConnectInProgress = false
                        connectNextAttemptActive.set(false)
                        scheduleAutoConnect(2000L)
                    } else {
                        recordChannel("SPP SERVER STALE ACCEPTER — no auto-connect scheduled")
                    }
                }
            } catch (e: SecurityException) {
                val currentOwner = synchronized(htsServerLock) {
                    sppServerSocket === server && htsServerGeneration == generation
                }
                if (currentOwner) {
                    synchronized(htsServerLock) {
                        if (sppServerSocket === server && htsServerGeneration == generation) {
                            sppServerSocket = null
                            sppServerSockets.remove(server)
                            htsServerListening = false
                        }
                    }
                }
                if (!intentionalDisconnect) {
                    recordChannel("SPP SERVER SECURITY FAILED uuid=$uuid generation=$generation currentOwner=$currentOwner: ${e.message ?: e.javaClass.simpleName}")
                    if (currentOwner) connectNextAttemptActive.set(false)
                    setReadError("Bluetooth permission: ${e.message ?: "denied"}")
                }
            } catch (e: Exception) {
                val currentOwner = synchronized(htsServerLock) {
                    sppServerSocket === server && htsServerGeneration == generation
                }
                if (currentOwner) {
                    synchronized(htsServerLock) {
                        if (sppServerSocket === server && htsServerGeneration == generation) {
                            sppServerSocket = null
                            sppServerSockets.remove(server)
                            htsServerListening = false
                        }
                    }
                }
                if (!intentionalDisconnect) {
                    recordChannel("SPP SERVER FAILED uuid=$uuid generation=$generation currentOwner=$currentOwner: ${e.message ?: e.javaClass.simpleName}")
                    if (currentOwner) {
                        autoConnectInProgress = false
                        connectNextAttemptActive.set(false)
                        scheduleAutoConnect(2000L)
                    } else {
                        recordChannel("SPP SERVER STALE FAILURE — no auto-connect scheduled")
                    }
                }
            }
        }
        acceptThread.name = "AltrozDrive-ConnectNext-SppServer-$serviceName"
        acceptThread.isDaemon = true
        acceptThread.start()
    }

    private fun closeAllSppServers() {
        synchronized(htsServerLock) {
            htsServerGeneration++
            sppServerSockets.forEach { server ->
                try { server.close() } catch (_: Exception) {}
            }
            sppServerSockets.clear()
            sppServerSocket = null
            htsServerListening = false
            incomingSppAccepted.set(false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectDirectConnectNextChannel(device: BluetoothDevice, serverChannel: Int): Boolean {
        if (intentionalDisconnect || incomingSppAccepted.get()) return false
        val socket = try {
            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            method.invoke(device, serverChannel) as? android.bluetooth.BluetoothSocket
        } catch (e: Exception) {
            recordChannel("CN DIRECT ch$serverChannel CREATE FAILED: ${e.message ?: e.javaClass.simpleName}")
            return false
        } ?: return false

        return try {
            // This probe is deliberately independent of the phone-hosted SppLink server.
            // It never closes or replaces that server and it writes nothing until a valid
            // HTS frame is actually observed.
            classicSocket = socket
            state = state.copy(
                transport = "Classic / RFCOMM ConnectNext ch$serverChannel",
                status = "Trying ConnectNext RFCOMM channel $serverChannel…",
                readError = "None"
            )
            publish()
            recordChannel("CN DIRECT TRY serverChannel=$serverChannel (observed DLCI=${serverChannel shl 1})")

            val timedOut = AtomicBoolean(false)
            val watchdog = Thread {
                try {
                    Thread.sleep(8_000L)
                    if (!socket.isConnected && !intentionalDisconnect) {
                        timedOut.set(true)
                        recordChannel("CN DIRECT TIMEOUT serverChannel=$serverChannel after 8s")
                        try { socket.close() } catch (_: Exception) {}
                    }
                } catch (_: InterruptedException) {}
            }
            watchdog.isDaemon = true
            watchdog.start()
            try { socket.connect() } finally { watchdog.interrupt() }
            if (timedOut.get() || !socket.isConnected) throw java.io.IOException("RFCOMM channel $serverChannel did not connect")

            connectedAddresses += device.address
            updateConnected(device.address, true)
            state = state.copy(
                transport = "Classic / RFCOMM ConnectNext ch$serverChannel",
                status = "ConnectNext RFCOMM channel $serverChannel connected — waiting for HTS",
                connectedAt = packetTimeFormatter.get().format(Date()),
                readError = "None",
                htsConnected = false
            )
            publish()
            recordChannel("CN DIRECT CONNECTED ch$serverChannel / DLCI ${serverChannel shl 1}")

            htsSocket = socket
            liveSession.set(LiveSessionState())
            val validHts = AtomicBoolean(false)
            val reader = Thread {
                val input = try { socket.inputStream } catch (_: Exception) { return@Thread }
                val buffer = ByteArray(4096)
                try {
                    while (socket.isConnected && !intentionalDisconnect && !incomingSppAccepted.get()) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        if (count == 0) continue
                        val bytes = buffer.copyOf(count)
                        emitRx(bytes.toHex())
                        htsReassembler.append(bytes) { frame ->
                            validHts.set(true)
                            handleMirroredHtsFrame(frame, device)
                        }
                    }
                } catch (e: Exception) {
                    if (!intentionalDisconnect && !validHts.get()) {
                        recordChannel("CN DIRECT ch$serverChannel READER FAILED: ${e.message ?: e.javaClass.simpleName}")
                    }
                }
            }
            reader.name = "AltrozDrive-ConnectNext-reader-ch$serverChannel"
            reader.isDaemon = true
            reader.start()

            val deadline = System.currentTimeMillis() + 15_000L
            while (!intentionalDisconnect && !incomingSppAccepted.get() && !validHts.get() && System.currentTimeMillis() < deadline) {
                Thread.sleep(100L)
            }

            if (validHts.get()) {
                state = state.copy(htsConnected = true, status = "ConnectNext HTS active — application data received")
                publish()
                recordChannel("CN DIRECT DATA CHANNEL CONFIRMED ch$serverChannel")
                return true
            }

            recordChannel("CN DIRECT SILENT ch$serverChannel — no valid HTS frame within 15s")
            false
        } catch (e: Exception) {
            if (!intentionalDisconnect) recordChannel("CN DIRECT ch$serverChannel FAILED: ${e.message ?: e.javaClass.simpleName}")
            false
        } finally {
            if (!intentionalDisconnect && !incomingSppAccepted.get()) {
                try { socket.close() } catch (_: Exception) {}
                if (classicSocket === socket) classicSocket = null
                if (htsSocket === socket) htsSocket = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestSdpAndWait(device: BluetoothDevice): List<UUID> {
        // Android's bonded-device UUID cache is useful for diagnostics but does not
        // reliably expose ConnectNext's proprietary SppLink UUID. Force a live SDP
        // request and merge the result with the known UUID recovered from the vehicle.
        val cached = try { device.uuids?.map { it.uuid }.orEmpty() } catch (_: SecurityException) { emptyList() }
        if (cached.isNotEmpty()) {
            state = state.copy(
                classicSdpStatus = "Cached SDP UUIDs: ${cached.size} (refreshing live…)",
                classicServiceUuids = cached.map { it.toString() }.distinct()
            )
            publish()
        }
        val latch = CountDownLatch(1)
        sdpLatch = latch
        sdpAddress = device.address
        state = state.copy(
            classicSdpStatus = "Requesting SDP service UUIDs…",
            classicServiceUuids = cached.map { it.toString() }.distinct()
        )
        publish()
        val requested = try { device.fetchUuidsWithSdp() } catch (_: Exception) { false }
        if (!requested) {
            sdpLatch = null
            sdpAddress = null
            val result = (cached + knownConnectNextUuids).distinct()
            state = state.copy(
                classicSdpStatus = "SDP request could not be started — using cached/known UUIDs (${result.size})",
                classicServiceUuids = result.map { it.toString() }
            )
            publish()
            return result
        }
        latch.await(6, TimeUnit.SECONDS)
        val live = try { device.uuids?.map { it.uuid }.orEmpty().distinct() } catch (_: SecurityException) { emptyList() }
        val result = (live + cached + knownConnectNextUuids).distinct()
        state = state.copy(
            classicSdpStatus = if (result.isEmpty()) "SDP finished — no service UUIDs reported" else "SDP finished — ${result.size} service UUID(s) (live=${live.size})",
            classicServiceUuids = result.map { it.toString() }
        )
        publish()
        sdpLatch = null
        sdpAddress = null
        return result
    }

    private fun writeHts(bytes: ByteArray, label: String): Boolean {
        synchronized(htsTxLock) {
            try {
                val socket = htsSocket ?: return false
                if (!socket.isConnected || intentionalDisconnect) return false
                socket.outputStream.write(bytes)
                socket.outputStream.flush()
                emitTx(bytes.toHex(), label)
                return true
            } catch (e: Exception) {
                recordChannel("HTS WRITE FAILED: ${e.message ?: e.javaClass.simpleName}")
                return false
            }
        }
    }

    private fun nextHtsTxSequence(): Int = synchronized(htsTxLock) { htsNextTxSequence++ }

    /** ConnectNext application payload: LE16 byte length including NUL, UTF-8 text, NUL. */
    private fun observedAppBody(text: String): ByteArray {
        val payload = text.toByteArray(Charsets.UTF_8) + byteArrayOf(0)
        val out = java.io.ByteArrayOutputStream(payload.size + 2)
        out.write(payload.size and 0xFF)
        out.write((payload.size ushr 8) and 0xFF)
        out.write(payload)
        return out.toByteArray()
    }

    private fun readHtsLoop(socket: android.bluetooth.BluetoothSocket, device: BluetoothDevice) {
        val input = try { socket.inputStream } catch (e: Exception) {
            setReadError("HTS input stream unavailable: ${e.message ?: e.javaClass.simpleName}")
            return
        }
        val buffer = ByteArray(4096)
        try {
            while (socket.isConnected && !intentionalDisconnect) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                emitRx(buffer.copyOf(count).toHex())
                htsReassembler.append(buffer.copyOf(count)) { mirrored ->
                    handleMirroredHtsFrame(mirrored, device)
                }
            }
        } catch (e: Exception) {
            if (!intentionalDisconnect) setReadError("HTS RFCOMM read failed: ${e.message ?: e.javaClass.simpleName}")
        } finally {
            if (!intentionalDisconnect) {
                if (htsSocket === socket) htsSocket = null
                if (classicSocket === socket) classicSocket = null
                incomingSppAccepted.set(false)
                connectNextMirrorSession.reset()
                val tripKey = (state.selectedDevice?.address ?: selectedVehicleKey())?.uppercase(Locale.US)?.takeIf { it.isNotBlank() }
                if (tripKey != null) vehicleSnapshotDb.finalizeActiveTrip(tripKey)
                resetEvidenceSession()
                liveSession.set(LiveSessionState())
                tripDetectionBaseline = null
                lastTripProgressPersistAt = 0L
                autoConnectInProgress = false
                connectNextAttemptActive.set(false)
                state = state.copy(htsConnected = false, status = "HTS connection closed", cnProtocolStage = "DISCONNECTED", cnAuthenticated = false)
                publish()
                recordChannel("HTS DIRECT CLOSED — authoritative socket released")
                scheduleAutoConnect(1500L)
            }
        }
    }

    // Evidence-based ConnectNext 6.40 session state. This replaces the old speculative
    // cn_handshake/auth JSON path for the live RFCOMM session. The supplied HCI capture shows
    // the HU first sends HTS_VERSION/NETWORK information, then the phone sends NETWORK_ID and
    // Sl_Supported_Apps, after which the HU advertises services and the phone opens the services.
    private val evidenceServicesSeen = linkedSetOf<String>()
    private val evidenceServicesRequested = linkedSetOf<String>()
    private val evidenceServiceQueue = ArrayDeque<String>()
    private var evidenceServiceAwaiting: String? = null
    private val evidenceServiceAttempts = mutableMapOf<String, Int>()
    private val evidenceServiceFailed = linkedSetOf<String>()
    private val evidenceServiceConnectionIds = mutableMapOf<String, Int>()
    private var evidenceVersionSeen = false
    private var evidenceNetworkSent = false
    private var evidenceAppsSent = false
    private var observedNetworkIdentityJson: String? = null
    private var observedVehicleUid: String? = null
    private var evidenceSupportedAppsAckSeen = false
    private var evidenceDeviceDetailsSeen = false
    private var evidenceDeviceDetailsLengthRequested = false
    private var evidenceDeviceDetailsLengthResponded = false
    private var evidenceSessionStarted = false
    private var evidenceServiceDiscoveryWindowScheduled = false
    private var evidenceServiceDiscoveryWindowElapsed = false

    // Official 6.40 service-request order observed on the wire. Do not reorder based
    // on the order in which Srvc_Available notifications happen to arrive.
    private val observedVehicleServices = listOf(
        "W3WService", "VehicleInfoService", "DriveNextService", "DoodleService", "RCAService"
    )

    /**
     * The HU's PEER_DEVICE_ID is the phone's OWN_DEVICE_ID for this live session.
     * Peer 19 captures proved that hard-coding client ID 20 in SupportedApps and
     * service requests makes the advertised identity disagree with NETWORK_ID.
     */
    private fun currentHtsClientId(): Int =
        observedPeerDeviceId?.takeIf { it in 1..255 } ?: ConnectNextHtsWire.CLIENT_ID

    // The HTS control wire type is selected by the current HU peer/link ID.
    // This is separate from currentHtsClientId(), which remains unchanged.
    private fun currentHtsControlType(): Int = when (observedPeerDeviceId) {
        19 -> 0x13
        20 -> 0x14
        else -> ConnectNextHtsWire.CONTROL_TEXT
    }

    private fun isHtsControlType(type: Int): Boolean = type == 0x13 || type == 0x14

    private fun sendObservedNetworkIdentity() {
        if (evidenceNetworkSent || htsSocket?.isConnected != true) return
        val networkId = observedNetworkIdentityJson
        if (networkId.isNullOrBlank()) {
            recordChannel("EVIDENCE NETWORK_ID TX SKIPPED — current HU NETWORK_ID was not decoded")
            return
        }

        // Official ConnectNext 6.40 phone-originated NETWORK_ID frame:
        // [LE16 innerLength][01 00][3-byte BE JSON length][JSON].
        // Mirror only the current HU NETWORK_ID string; do not replay prior vehicle data.
        val huOwnDeviceId = observedHuOwnDeviceId
        val huPeerDeviceId = observedPeerDeviceId
        if (huOwnDeviceId == null || huPeerDeviceId == null) {
            recordChannel("EVIDENCE NETWORK_ID TX SKIPPED — current HU OWN/PEER device IDs were not decoded")
            return
        }

        // ConnectNext 6.40 mirrors the CURRENT HU link identity:
        // HU OWN=x, PEER=y  -> phone OWN=y, PEER=x.
        // Do not hardcode phone OWN_DEVICE_ID=20 for NETWORK_ID negotiation.
        val response = JSONObject().apply {
            put("NETWORK_ID", networkId)
            put("OWN_DEVICE_ID", huPeerDeviceId)
            put("PEER_DEVICE_ID", huOwnDeviceId)
            put("HTS_VERSION", "2.0")
        }
        val payload = response.toString().toByteArray(Charsets.UTF_8)
        val body = ConnectNextHtsWire.networkIdentityBody(payload)
        val seq = nextHtsTxSequence()
        val sent = writeHts(ConnectNextHtsWire.encodeData(seq, ConnectNextHtsWire.NETWORK_ID, body, 0x3E), "HTS-NETWORK-ID-TX")
        if (sent) {
            evidenceNetworkSent = true
            recordChannel("EVIDENCE TX NETWORK_ID seq=$seq payloadBytes=${payload.size} huOwn=$huOwnDeviceId huPeer=$huPeerDeviceId phoneOwn=$huPeerDeviceId phonePeer=$huOwnDeviceId mirrored-current-hu-identity")
            // IMPORTANT: do not proactively send any text length-control here. The
            // official 6.40 session first receives HU -> phone length-control 0x24
            // for Sl_Inf_Dev_Details, and only then does the phone announce 0x4C for
            // Sl_Supported_Apps. Keeping this receive-driven prevents the negotiation
            // from stalling after the NETWORK_ID ACK.
            evidenceDeviceDetailsLengthRequested = true
        }
    }

    private fun isObservedLengthControl(frame: MirroredHtsFrame): Int? {
        // The vehicle sends the same exact binary length-control body as type 0x13
        // in Peer 19 sessions and type 0x14 in Peer 20 sessions. Parse the payload
        // signature first; treating 0x13 as an ordinary unknown frame stalls Peer 19.
        if (frame.messageType != 0x13 && frame.messageType != ConnectNextHtsWire.CONTROL_TEXT) return null
        val b = frame.body
        if (b.size != 6) return null
        if ((b[0].toInt() and 0xFF) != 0x04) return null
        if (b[1].toInt() != 0 || b[2].toInt() != 0 || b[3].toInt() != 0 || b[4].toInt() != 0) return null
        return b[5].toInt() and 0xFF
    }

    private fun respondToObservedLengthControl(expectedLength: Int) {
        if (!evidenceDeviceDetailsLengthRequested || evidenceDeviceDetailsLengthResponded) return
        if (expectedLength != 0x24) {
            recordChannel("EVIDENCE RX unexpected HU text length-control=$expectedLength; waiting for 0x24")
            return
        }
        val supportedAppsText = "Sl_Supported_Apps,USB,${currentHtsClientId()},com.mmi.navimaps,NaviMaps,Mirroring_mode,0,1,0,1,2"
        val advertisedLen = supportedAppsText.toByteArray(Charsets.UTF_8).size + 1
        val lenSeq = nextHtsTxSequence()
        val announced = writeHts(
            ConnectNextHtsWire.encodeData(
                lenSeq,
                currentHtsControlType(),
                ConnectNextHtsWire.textLengthControl(advertisedLen),
                0x3E
            ),
            "HTS-SUPPORTED-APPS-LENGTH-TX"
        )
        if (announced) {
            evidenceDeviceDetailsLengthResponded = true
            recordChannel("EVIDENCE TX SupportedApps lengthControl seq=$lenSeq payloadLen=$advertisedLen in-response-to-HU-length-0x24")
        } else {
            recordChannel("EVIDENCE TX SupportedApps lengthControl FAILED seq=$lenSeq — will await another HU length-control")
        }
    }

    private fun sendObservedSupportedApps() {
        if (evidenceAppsSent || htsSocket?.isConnected != true || !evidenceDeviceDetailsSeen) return

        // Official session: after Sl_Inf_Dev_Details arrives, the phone sends
        // SupportedApps as the next 0x14 text packet. Its length was advertised
        // earlier by the phone-side length-control frame.
        val text = "Sl_Supported_Apps,USB,${currentHtsClientId()},com.mmi.navimaps,NaviMaps,Mirroring_mode,0,1,0,1,2"
        val textSeq = nextHtsTxSequence()
        val sent = writeHts(
            ConnectNextHtsWire.encodeData(textSeq, currentHtsControlType(), ConnectNextHtsWire.textBody(text), 0x3E),
            "HTS-SUPPORTED-APPS-TX"
        )
        if (sent) {
            evidenceAppsSent = true
            recordChannel("EVIDENCE TX Sl_Supported_Apps textSeq=$textSeq clientId=${currentHtsClientId()}")
        } else {
            recordChannel("EVIDENCE TX Sl_Supported_Apps FAILED textSeq=$textSeq")
        }
    }

    private fun processNextEvidenceService() {
        if (evidenceServiceAwaiting != null || htsSocket?.isConnected != true) return

        // Mirror the official ConnectNext service-client lifecycle: the service
        // layer does not start remote-client connections until the SupportedApps
        // exchange has been acknowledged by the HU. Service availability is then
        // consumed as a per-service signal, with one connection context at a time.
        // This gate is intentionally above transport/HTS and does not change any
        // already-proven wire framing.
        if (!evidenceSupportedAppsAckSeen) {
            recordChannel("EVIDENCE SERVICE CONNECT WAITING — Sl_Supported_App_Ack not received")
            return
        }

        // The official 6.40 capture observes the complete service set before the
        // first remote-client request. Keep the existing short discovery window so
        // we do not race the HU's asynchronous Srvc_Available notifications.
        val allKnownServicesSeen = observedVehicleServices.all { it in evidenceServicesSeen }
        if (!allKnownServicesSeen && !evidenceServiceDiscoveryWindowElapsed) {
            if (!evidenceServiceDiscoveryWindowScheduled) {
                evidenceServiceDiscoveryWindowScheduled = true
                mainHandler.postDelayed({
                    evidenceServiceDiscoveryWindowElapsed = true
                    evidenceServiceDiscoveryWindowScheduled = false
                    processNextEvidenceService()
                }, 1000L)
            }
            return
        }
        val next = observedVehicleServices.firstOrNull { service ->
            service in evidenceServicesSeen && service !in evidenceServicesRequested && service !in evidenceServiceFailed
        } ?: return
        evidenceServiceQueue.remove(next)
        requestObservedService(next)
    }

    private fun requestObservedService(service: String) {
        if (service !in observedVehicleServices || service in evidenceServicesRequested || service in evidenceServiceFailed || evidenceServiceAwaiting != null) return
        if (htsSocket?.isConnected != true) return

        evidenceServiceAwaiting = service
        val text = "Req_Remote_Client_Con,$service,${currentHtsClientId()}"
        val textPayloadLength = text.toByteArray(Charsets.UTF_8).size + 1

        // Official 6.40: every phone-originated request text is immediately
        // preceded by a phone-originated length-control frame. Both consume
        // consecutive phone-side DATA sequence numbers.
        val controlSeq = nextHtsTxSequence()
        val announced = writeHts(
            ConnectNextHtsWire.encodeData(
                controlSeq,
                currentHtsControlType(),
                ConnectNextHtsWire.textLengthControl(textPayloadLength),
                0x3E
            ),
            "HTS-SERVICE-CONNECT-LENGTH-TX"
        )
        recordChannel("EVIDENCE TX service lengthControl service=$service seq=$controlSeq payloadLen=$textPayloadLength sent=$announced")
        if (!announced) {
            evidenceServiceAwaiting = null
            evidenceServiceQueue.addFirst(service)
            recordChannel("EVIDENCE TX SERVICE LENGTH FAILED service=$service seq=$controlSeq — queued for retry")
            return
        }

        val seq = nextHtsTxSequence()
        val sent = writeHts(
            ConnectNextHtsWire.encodeData(seq, currentHtsControlType(), ConnectNextHtsWire.textBody(text), 0x22),
            "HTS-SERVICE-CONNECT-TX"
        )
        if (!sent) {
            evidenceServiceAwaiting = null
            evidenceServiceQueue.addFirst(service)
            recordChannel("EVIDENCE TX SERVICE FAILED service=$service seq=$seq — queued for retry")
            return
        }

        state = state.copy(
            cnLastCommand = text,
            cnServices = (state.cnServices + service).distinct().takeLast(30)
        )
        publish()
        val attempt = (evidenceServiceAttempts[service] ?: 0) + 1
        evidenceServiceAttempts[service] = attempt
        recordChannel("EVIDENCE TX $text textSeq=$seq awaiting response attempt=$attempt")
        mainHandler.postDelayed({
            if (intentionalDisconnect || htsSocket?.isConnected != true) return@postDelayed
            if (evidenceServiceAwaiting != service || service in evidenceServicesRequested) return@postDelayed
            evidenceServiceAwaiting = null
            if (attempt < 3) {
                recordChannel("EVIDENCE SERVICE TIMEOUT $service attempt=$attempt — retrying")
                evidenceServiceQueue.addFirst(service)
            } else {
                evidenceServiceFailed += service
                evidenceServiceQueue.removeAll { it == service }
                recordChannel("EVIDENCE SERVICE FAILED $service after 3 attempts — marked failed; advancing")
            }
            processNextEvidenceService()
        }, 4000L)
    }

    private fun markConnectNextProgress() {
        connectNextLastProgressAt = System.currentTimeMillis()
    }

    private fun connectNextRecoveryThresholdMs(): Long {
        return when {
            evidenceServicesRequested.contains("VehicleInfoService") ||
                evidenceServiceConnectionIds.containsKey("VehicleInfoService") -> 120_000L
            evidenceSupportedAppsAckSeen || evidenceAppsSent || evidenceDeviceDetailsSeen -> 90_000L
            evidenceNetworkSent || evidenceVersionSeen -> 90_000L
            else -> 90_000L
        }
    }

    @SuppressLint("MissingPermission")
    private fun recoverStaleConnectNextSession(now: Long, reason: String) {
        if (intentionalDisconnect || !incomingSppAccepted.compareAndSet(true, false)) return
        val stalledFor = if (connectNextLastProgressAt > 0L) now - connectNextLastProgressAt else 0L
        recordChannel("CONNECTNEXT SESSION RECOVERY reason=$reason stalledFor=${stalledFor}ms peer=${observedPeerDeviceId ?: -1} stage=${state.cnProtocolStage}")
        connectNextWatchdogHandler.removeCallbacks(connectNextWatchdog)
        val socket = htsSocket
        htsSocket = null
        classicSocket = null
        try { socket?.close() } catch (_: Exception) {}
        connectNextMirrorSession.reset()
        resetEvidenceSession()
        htsReassembler.reset()
        x451LiveDecoder.reset()
        liveSession.set(LiveSessionState())
        connectNextAttemptActive.set(false)
        autoConnectInProgress = false
        state = state.copy(
            htsConnected = false,
            liveVisValidated = false,
            status = "RECONNECTING — ConnectNext session stalled",
            readError = "ConnectNext application session did not progress; re-arming SppLink",
            cnProtocolStage = "RECONNECTING",
            cnAuthenticated = false
        )
        publish()
        recordChannel("CONNECTNEXT STALE SESSION CLOSED — scheduling fresh SppLink lifecycle")
        scheduleAutoConnect(1_000L)
    }

    private fun resetEvidenceSession() {
        evidenceServicesSeen.clear()
        evidenceServicesRequested.clear()
        evidenceServiceQueue.clear()
        evidenceServiceAwaiting = null
        evidenceServiceAttempts.clear()
        evidenceServiceFailed.clear()
        evidenceServiceConnectionIds.clear()
        evidenceVersionSeen = false
        evidenceNetworkSent = false
        evidenceAppsSent = false
        evidenceSupportedAppsAckSeen = false
        evidenceDeviceDetailsSeen = false
        evidenceDeviceDetailsLengthRequested = false
        evidenceDeviceDetailsLengthResponded = false
        evidenceSessionStarted = false
        evidenceServiceDiscoveryWindowScheduled = false
        evidenceServiceDiscoveryWindowElapsed = false
        observedNetworkIdentityJson = null
        observedVehicleUid = null
        observedHuOwnDeviceId = null
        observedPeerDeviceId = null
        connectNextNegotiationStartedAt = 0L
        connectNextSessionAcceptedAt = 0L
        htsAckActions.clear()
    }

    private fun startNextGattOperation(g: BluetoothGatt) {
        if (gattOperationRunning) return
        val op = if (gattOperations.isEmpty()) null else gattOperations.removeFirst()
        if (op == null) {
            state = state.copy(status = "BLE services ready — listening for notifications")
            publish()
            return
        }
        gattOperationRunning = true
        try {
            op.invoke()
        } catch (e: SecurityException) {
            gattOperationRunning = false
            setReadError("Bluetooth permission was revoked")
            startNextGattOperation(g)
        } catch (e: Exception) {
            gattOperationRunning = false
            setReadError("GATT operation failed: ${e.message ?: e.javaClass.simpleName}")
            startNextGattOperation(g)
        }
    }

    private fun properties(p: Int): String {
        val list = mutableListOf<String>()
        if (p and BluetoothGattCharacteristic.PROPERTY_READ != 0) list += "READ"
        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) list += "WRITE"
        if (p and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) list += "WRITE_NR"
        if (p and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) list += "NOTIFY"
        if (p and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) list += "INDICATE"
        return list.joinToString(" • ").ifEmpty { "NONE" }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        intentionalDisconnect = true
        connectNextWatchdogHandler.removeCallbacks(connectNextWatchdog)
        connectNextLastProgressAt = 0L
        connectNextNegotiationStartedAt = 0L
        connectNextSessionAcceptedAt = 0L
        connectNextAttemptActive.set(false)
        autoConnectGeneration++
        autoConnectHandler.removeCallbacksAndMessages(null)
        val selectedAddress = state.selectedDevice?.address
        stopBleScan()
        stopClassicDiscovery()
        try { gatt?.disconnect() } catch (_: Exception) { }
        try { gatt?.close() } catch (_: Exception) { }
        gatt = null
        try { classicSocket?.close() } catch (_: Exception) { }
        closeAllSppServers()
        try { htsSocket?.close() } catch (_: Exception) { }
        classicSocket = null
        htsSocket = null
        requestedHtsServices.clear()
        resetEvidenceSession()
        synchronized(htsTxLock) {
            htsSequence = 0
            htsNextTxSequence = 0
        }
        htsReassembler.reset()
        x451LiveDecoder.reset()
        incomingSppAccepted.set(false)
        if (selectedAddress != null) {
            connectedAddresses -= selectedAddress
            updateConnected(selectedAddress, false)
        }
        if (state.selectedDevice != null || state.status != "Idle") {
            state = state.copy(selectedDevice = null, status = "Disconnected", characteristics = emptyList(), gattDescriptors = emptyList(), rxPackets = 0, rxBytes = 0, lastRxAt = "Never", txPackets = 0, txBytes = 0, lastTxAt = "Never", readError = "None", lastRxTransport = "Never", connectedAt = "Never", channelAttempts = emptyList(), diagnosticEvents = emptyList(), htsConnected = false, htsVersion = "Unknown", htsFrames = 0, htsJsonFrames = 0, htsBinaryFrames = 0, htsLastSequence = -1, htsLastMessageType = "None", htsServices = emptyList(), htsVehicleBinaryFrames = 0, htsMessageTypes = emptyList(), htsLastPayloadPreview = "None", cnProtocolStage = "DISCONNECTED", cnAuthenticated = false, cnHandshakeSeen = false, cnHeartbeatCount = 0, cnDiscoverySeen = false, cnSubscribedSignals = emptyList(), cnServices = emptyList(), cnLastCommand = "None", cnLastResponse = "None", cnAuthDetail = "Not started", liveVisValidated = false)
            publish()
        }
    }

    /**
     * Analyze a Samsung/Android Bluetooth HCI snoop capture exported as either
     * a raw btsnoop_hci.log file or a bug-report ZIP containing that log.
     * This is intentionally offline/passive: it never opens a Bluetooth socket
     * and never transmits anything to the vehicle.
     */
    private fun java.io.InputStream.readLimitedBytes(maxBytes: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(32 * 1024)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            require(total <= maxBytes) { "HCI capture is larger than ${maxBytes / (1024 * 1024)} MB" }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    fun analyzeHciCapture(uri: Uri) {
        Thread {
            try {
                val files = mutableListOf<Pair<String, ByteArray>>()
                context.contentResolver.openInputStream(uri).use { raw ->
                    requireNotNull(raw) { "Unable to open selected capture" }
                    val buffered = BufferedInputStream(raw)
                    buffered.mark(8)
                    val magic = ByteArray(8)
                    val got = buffered.read(magic)
                    buffered.reset()
                    if (got == 8 && magic.contentEquals(byteArrayOf(0x62,0x74,0x73,0x6e,0x6f,0x6f,0x70,0x00))) {
                        files += (uri.lastPathSegment ?: "btsnoop_hci.log") to buffered.readLimitedBytes(MAX_HCI_CAPTURE_BYTES)
                    } else {
                        ZipInputStream(buffered).use { zip ->
                            while (true) {
                                val entry = zip.nextEntry ?: break
                                if (!entry.isDirectory && entry.name.substringAfterLast('/').lowercase(Locale.US).let { it == "btsnoop_hci.log" || it == "btsnoop_hci.log.last" }) {
                                    files += entry.name to zip.readLimitedBytes(MAX_HCI_CAPTURE_BYTES)
                                }
                            }
                        }
                    }
                }
                require(files.isNotEmpty()) { "No btsnoop_hci.log or btsnoop_hci.log.last found" }
                val result = analyzeBtsnoopFiles(files)
                state = state.copy(
                    hciCaptureName = files.joinToString(", ") { it.first.substringAfterLast('/') },
                    hciRecords = result.records,
                    hciAclPackets = result.aclPackets,
                    hciConnectNextPackets = result.connectNextPackets,
                    hciVendorFrames = result.vendorFrames,
                    hciDlcis = result.dlcis,
                    hciServices = result.services,
                    hciLastPayloadPreview = result.lastPreview,
                    hciOdo = result.odo,
                    hciFuelPercent = result.fuelPercent,
                    hciDteKm = result.dteKm,
                    hciVehicleTempC = result.vehicleTempC,
                    hciFanSpeed = result.fanSpeed,
                    hciAcOn = result.acOn,
                    hciAutoMode = result.autoMode,
                    hciEcoMode = result.ecoMode,
                    hciAirFlowPattern = result.airFlowPattern,
                    hciRecirculation = result.recirculation,
                    hciMaxDefrost = result.maxDefrost,
                    hciRearDefog = result.rearDefog,
                    hciHighFanSpeed = result.highFanSpeed,
                    hciLowFanSpeed = result.lowFanSpeed,
                    hciHighTempC = result.highTempC,
                    hciLowTempC = result.lowTempC,
                    hciAnalysisStatus = result.status,
                    status = result.status
                )
                result.evidence.forEach { protocolLogger.log("HCI $it") }
                protocolLogger.log("HCI DECODED ODO=${result.odo?.toString() ?: "unavailable"} km FUEL=${result.fuelPercent?.toString() ?: "unavailable"}% DTE=${result.dteKm?.toString() ?: "unavailable"} km")
                publish()
            } catch (e: Exception) {
                state = state.copy(hciAnalysisStatus = "HCI import failed: ${e.message ?: e.javaClass.simpleName}", status = "HCI import failed")
                publish()
            }
        }.start()
    }

    private data class HciAnalysisResult(
        val records: Long, val aclPackets: Long, val connectNextPackets: Long,
        val vendorFrames: Long, val dlcis: List<String>, val services: List<String>,
        val lastPreview: String, val odo: Int?, val fuelPercent: Int?, val dteKm: Int?,
        val vehicleTempC: Int?, val fanSpeed: Int?, val acOn: Boolean?, val autoMode: Boolean?,
        val ecoMode: Boolean?, val airFlowPattern: Int?, val recirculation: Boolean?,
        val maxDefrost: Boolean?, val rearDefog: Boolean?, val highFanSpeed: Int?, val lowFanSpeed: Int?,
        val highTempC: Int?, val lowTempC: Int?, val status: String, val evidence: List<String>
    )

    private fun analyzeBtsnoopFiles(files: List<Pair<String, ByteArray>>): HciAnalysisResult {
        var records = 0L
        var aclPackets = 0L
        var vendorFrames = 0L
        val dlcis = linkedSetOf<String>()
        val services = linkedSetOf<String>()
        val evidence = ArrayDeque<String>()
        var lastPreview = "None"

        data class L2State(
            val expected: Int,
            val cid: Int,
            val body: ByteArrayOutputStream,
            val recordIndex: Long
        )
        val l2States = mutableMapOf<String, L2State>()
        val aclCountsByHandle = mutableMapOf<Int, Long>()
        val candidateHandles = linkedSetOf<Int>()
        val htsStreams = mutableMapOf<String, ByteArrayOutputStream>()
        // Verified against ConnectNext decoded vehicle data in the supplied captures:
        // ODO is the u32 LE value following the 0x5E2718 marker; the instrument-cluster
        // block begins with u32 121,1,0,2 and fields 3/4 are fuel % and DTE km.
        var decodedOdo: Int? = null
        var decodedFuel: Int? = null
        var decodedDte: Int? = null
        var decodedVehicleTemp: Int? = null
        var decodedFanSpeed: Int? = null
        var decodedAcOn: Boolean? = null
        var decodedAutoMode: Boolean? = null
        var decodedEcoMode: Boolean? = null
        var decodedAirFlow: Int? = null
        var decodedRecirc: Boolean? = null
        var decodedMaxDefrost: Boolean? = null
        var decodedRearDefog: Boolean? = null
        var decodedHighFan: Int? = null
        var decodedLowFan: Int? = null
        var decodedHighTemp: Int? = null
        var decodedLowTemp: Int? = null

        fun addEvidence(value: String) {
            evidence.addLast(value)
            while (evidence.size > 200) evidence.removeFirst()
        }

        fun u32be(a: ByteArray, o: Int): Long =
            ((a[o].toLong() and 255L) shl 24) or
                ((a[o + 1].toLong() and 255L) shl 16) or
                ((a[o + 2].toLong() and 255L) shl 8) or
                (a[o + 3].toLong() and 255L)

        fun u16le(a: ByteArray, o: Int): Int =
            (a[o].toInt() and 255) or ((a[o + 1].toInt() and 255) shl 8)

        fun sanitizePreview(text: String): String {
            val jsonStart = text.indexOf('{')
            val printable = if (jsonStart >= 0) text.substring(jsonStart) else text
            return printable.take(220).replace(
                Regex("(?i)(VIN|vehicle.?identifier)\\s*[=:]\\s*\\\"[^\\\"]+"),
                "VIN/vehicleIdentifier=REDACTED"
            )
        }

        fun u32le(a: ByteArray, o: Int): Int =
            (a[o].toInt() and 255) or
                ((a[o + 1].toInt() and 255) shl 8) or
                ((a[o + 2].toInt() and 255) shl 16) or
                ((a[o + 3].toInt() and 255) shl 24)

        fun ByteArray.containsSequence(pattern: ByteArray): Boolean {
            if (pattern.isEmpty() || size < pattern.size) return false
            outer@ for (i in 0..size - pattern.size) {
                for (j in pattern.indices) if (this[i + j] != pattern[j]) continue@outer
                return true
            }
            return false
        }

        fun ByteArray.indexOfSequence(pattern: ByteArray, start: Int = 0): Int {
            if (pattern.isEmpty()) return start.coerceIn(0, size)
            if (start < 0 || start >= size || pattern.size > size) return -1
            var i = start
            val last = size - pattern.size
            while (i <= last) {
                var j = 0
                while (j < pattern.size && this[i + j] == pattern[j]) j++
                if (j == pattern.size) return i
                i++
            }
            return -1
        }


        fun decodeVerifiedVehicleSignals(body: ByteArray) {
            // Defensive bounds checks are intentional: HTS bodies can end in the
            // middle of a nested frame. A truncated body must never abort the
            // entire HCI import.
            try {
                val odoMarker = byteArrayOf(0x5E, 0x27, 0x18)
                var o = 0
                while (o + odoMarker.size <= body.size) {
                    val at = body.indexOfSequence(odoMarker, o)
                    if (at < 0) break
                    // 5E 27 18 00 00 00 00 00 [ODO u32 LE]
                    if (at >= 0 && at <= body.size - 12 &&
                        body[at + 3] == 0.toByte() && body[at + 4] == 0.toByte() &&
                        body[at + 5] == 0.toByte() && body[at + 6] == 0.toByte() &&
                        body[at + 7] == 0.toByte()) {
                        val value = u32le(body, at + 8)
                        if (value in 0..1_000_000) decodedOdo = value
                    }
                    o = at + odoMarker.size
                }

                // Instrument Cluster 4 verified record: 121,1,0,2, Fuel, DTE, ...
                val sigPrefix = byteArrayOf(0x79,0x00,0x00,0x00, 0x01,0x00,0x00,0x00,
                    0x00,0x00,0x00,0x00, 0x02,0x00,0x00,0x00)
                o = 0
                while (o + sigPrefix.size <= body.size) {
                    val at = body.indexOfSequence(sigPrefix, o)
                    if (at < 0) break
                    if (at <= body.size - (sigPrefix.size + 24)) {
                        val fuel = u32le(body, at + 16)
                        val dte = u32le(body, at + 20)
                        if (fuel in 0..100 && dte in 0..2000) {
                            decodedFuel = fuel
                            decodedDte = dte
                        }
                    }
                    o = at + sigPrefix.size
                }
            } catch (_: IndexOutOfBoundsException) {
                // Ignore only this malformed/truncated body; continue parsing the
                // remaining HCI records.
            }
        }

        fun consumeHtsBuffer(key: String, depth: Int): Pair<Int, String> {
            val buffer = htsStreams.getOrPut("$depth:$key") { ByteArrayOutputStream() }
            var count = 0
            var preview = "None"
            while (true) {
                val bytes = buffer.toByteArray()
                var start = -1
                var i = 0
                while (i + 1 < bytes.size) {
                    if ((bytes[i].toInt() and 255) == 0x7E && (bytes[i + 1].toInt() and 255) == 0x55) {
                        start = i
                        break
                    }
                    i++
                }
                if (start < 0) {
                    if (bytes.lastOrNull()?.toInt()?.and(255) == 0x7E) {
                        buffer.reset()
                        buffer.write(byteArrayOf(0x7E))
                    } else {
                        buffer.reset()
                    }
                    break
                }
                if (start > 0) {
                    val keep = bytes.copyOfRange(start, bytes.size)
                    buffer.reset()
                    buffer.write(keep)
                    continue
                }
                if (bytes.size < 8) break

                val declared = u16le(bytes, 6)
                if (declared < 3 || declared > 65535) {
                    buffer.reset()
                    buffer.write(bytes.copyOfRange(1, bytes.size))
                    continue
                }
                val total = 8 + declared
                if (bytes.size < total) break

                val frame = bytes.copyOfRange(0, total)
                val msgType = frame[8].toInt() and 255
                val dataLen = u16le(frame, 9)
                if (dataLen < 0 || declared != 3 + dataLen || 11 + dataLen > frame.size) {
                    buffer.reset()
                    buffer.write(bytes.copyOfRange(1, bytes.size))
                    continue
                }

                val sequence = u32le(frame, 2)
                val body = frame.copyOfRange(11, 11 + dataLen)
                decodeVerifiedVehicleSignals(body)

                // Evidence-backed HVAC JSON fields observed in the supplied ConnectNext
                // btsnoop capture. JSON is parsed wherever it occurs in the HTS body;
                // this is deliberately read-only and never fabricates missing values.
                val bodyText = body.toString(Charsets.UTF_8)
                Regex("\\{[^{}]{0,2048}\\}").findAll(bodyText).forEach { m ->
                    try {
                        val o = JSONObject(m.value)
                        if (o.has("VehicleTemp")) decodedVehicleTemp = o.optInt("VehicleTemp").takeIf { it in 0..60 }
                        if (o.has("FanSpeed")) decodedFanSpeed = o.optInt("FanSpeed").takeIf { it in 0..20 }
                        if (o.has("AirFlowDistrubPattern")) decodedAirFlow = o.optInt("AirFlowDistrubPattern")
                        if (o.has("AirReCirculation")) decodedRecirc = o.optInt("AirReCirculation") != 0
                        if (o.has("ACOn")) decodedAcOn = o.optInt("ACOn") != 0
                        if (o.has("AutoModeSet")) decodedAutoMode = o.optInt("AutoModeSet") != 0
                        if (o.has("EcoModeSet")) decodedEcoMode = o.optInt("EcoModeSet") != 0
                        if (o.has("MaxDefrostSet")) decodedMaxDefrost = o.optInt("MaxDefrostSet") != 0
                        if (o.has("RearDefogSet")) decodedRearDefog = o.optInt("RearDefogSet") != 0
                        if (o.has("HighFanSpeedVal")) decodedHighFan = o.optInt("HighFanSpeedVal").takeIf { it in 0..20 }
                        if (o.has("LowFanSpeedVal")) decodedLowFan = o.optInt("LowFanSpeedVal").takeIf { it in 0..20 }
                        if (o.has("HighTempVal")) decodedHighTemp = o.optInt("HighTempVal").takeIf { it in 0..60 }
                        if (o.has("LowTempVal")) decodedLowTemp = o.optInt("LowTempVal").takeIf { it in 0..60 }
                    } catch (_: Exception) { }
                }
                val text = body.toString(Charsets.UTF_8)
                val jsonStart = text.indexOf('{')
                val hasJson = jsonStart >= 0
                val safe = when {
                    hasJson -> sanitizePreview(text)
                    msgType == 0x1B -> "HTS vehicle/service binary • ${body.size} bytes"
                    text.any { it.isLetterOrDigit() } -> text.filter { it == '\n' || it == '\r' || it == '\t' || it in ' '..'~' }.take(220)
                    else -> "HTS 0x%02X • %d bytes".format(msgType, body.size)
                }

                vendorFrames++
                count++
                preview = safe
                val serviceRegex = Regex("(?:Srvc_Available|Req_Remote_Client_Con|Resp_Remote_Client_Con),([A-Za-z0-9_]+)")
                serviceRegex.findAll(text).forEach { services += it.groupValues[1] }
                if (hasJson || text.contains("Srvc_Available")) {
                    addEvidence("HTS JSON/TEXT depth=$depth seq=$sequence ${sanitizePreview(text)}")
                } else if (msgType == 0x1B) {
                    addEvidence("HTS VEHICLE/SERVICE BINARY depth=$depth seq=$sequence bytes=${body.size}")
                }

                // ConnectNext commonly wraps additional HTS 0x55 frames inside
                // the body of 0x1B/0x1E messages. Feed those bytes into a separate
                // stream so climate/service JSON is decoded instead of discarded.
                if (depth < 2 && body.containsSequence(byteArrayOf(0x7E, 0x55))) {
                    val nestedKey = key
                    htsStreams.getOrPut("${depth + 1}:$nestedKey") { ByteArrayOutputStream() }.write(body)
                    val nestedResult = consumeHtsBuffer(nestedKey, depth + 1)
                    if (nestedResult.second != "None") preview = nestedResult.second
                }

                val remain = bytes.copyOfRange(total, bytes.size)
                buffer.reset()
                buffer.write(remain)
            }
            return count to preview
        }

        fun processL2cap(handle: Int, direction: Int, body: ByteArray, cid: Int, recordIndex: Long) {
            var offset = 0
            while (offset + 4 <= body.size) {
                val address = body[offset].toInt() and 255
                val control = body[offset + 1].toInt() and 255
                val dlci = address ushr 2
                if (control != 0xFF && control != 0xEF) break

                val firstLength = body[offset + 2].toInt() and 255
                val rfLen: Int
                val headerSize: Int
                if ((firstLength and 1) != 0) {
                    rfLen = firstLength ushr 1
                    headerSize = 4
                } else {
                    if (offset + 5 > body.size) break
                    val secondLength = body[offset + 3].toInt() and 255
                    rfLen = (firstLength ushr 1) or ((secondLength ushr 1) shl 7)
                    headerSize = 5
                }
                val end = offset + headerSize + rfLen
                if (rfLen <= 0 || end > body.size) break

                dlcis += "DLCI $dlci (RFCOMM address 0x%02X, L2CAP CID 0x%04X)".format(address, cid)
                candidateHandles += handle
                val payload = body.copyOfRange(offset + headerSize, end)
                val result = run {
                    val key = "$handle:$direction:dlci=$dlci"
                    val stream = htsStreams.getOrPut("0:$key") { ByteArrayOutputStream() }
                    stream.write(payload)
                    consumeHtsBuffer(key, 0)
                }
                if (result.second != "None") lastPreview = result.second
                offset = end
                // Some L2CAP records contain zero padding after the RFCOMM frame.
                if (offset < body.size && body[offset] == 0.toByte()) break
            }
            addEvidence("HTS RFCOMM DLCI(s) handle=$handle direction=$direction cid=0x%04X record=$recordIndex".format(cid))
        }

        fun processAcl(packet: ByteArray, recordIndex: Long, direction: Int) {
            if (packet.size < 5 || (packet[0].toInt() and 255) != 2) return
            val handleFlags = u16le(packet, 1)
            val handle = handleFlags and 0x0FFF
            val pb = (handleFlags ushr 12) and 0x3
            val dataLength = u16le(packet, 3)
            if (dataLength <= 0 || packet.size < 5 + dataLength) return
            val acl = packet.copyOfRange(5, 5 + dataLength)
            aclPackets++
            aclCountsByHandle[handle] = (aclCountsByHandle[handle] ?: 0L) + 1L
            val key = "$handle:$direction"

            if (pb == 2 || pb == 3) {
                if (acl.size < 4) return
                val l2Length = u16le(acl, 0)
                val cid = u16le(acl, 2)
                if (l2Length <= 0 || l2Length > 65535) {
                    l2States.remove(key)
                    return
                }
                val state = L2State(l2Length, cid, ByteArrayOutputStream(), recordIndex)
                state.body.write(acl, 4, acl.size - 4)
                l2States[key] = state
                if (state.body.size() >= l2Length) {
                    l2States.remove(key)
                    processL2cap(handle, direction, state.body.toByteArray().copyOf(l2Length), cid, recordIndex)
                }
            } else if (pb == 1) {
                val state = l2States[key] ?: return
                state.body.write(acl)
                if (state.body.size() >= state.expected) {
                    l2States.remove(key)
                    processL2cap(handle, direction, state.body.toByteArray().copyOf(state.expected), state.cid, state.recordIndex)
                }
            }
        }

        for ((name, bytes) in files) {
            require(bytes.size >= 16 && bytes.copyOfRange(0, 8).contentEquals(
                byteArrayOf(0x62, 0x74, 0x73, 0x6E, 0x6F, 0x6F, 0x70, 0x00)
            )) { "Invalid btsnoop file: $name" }
            l2States.clear()
            htsStreams.clear()
            var pos = 16
            var fileRecords = 0L
            while (pos + 24 <= bytes.size) {
                val included = u32be(bytes, pos + 4).toInt()
                val original = u32be(bytes, pos).toInt()
                if (included < 0 || included > 1024 * 1024 || pos + 24 + included > bytes.size) break
                val packet = bytes.copyOfRange(pos + 24, pos + 24 + included)
                val direction = if (((u32be(bytes, pos + 8)) and 1L) != 0L) 1 else 0
                fileRecords++
                records++
                try {
                    processAcl(packet, fileRecords, direction)
                } catch (_: IndexOutOfBoundsException) {
                    addEvidence("skipped malformed ACL record=$fileRecords")
                }
                pos += 24 + included
                if (original < included) break
            }
            addEvidence("parsed $name records=$fileRecords")
        }

        var connectNextPackets = 0L
        candidateHandles.forEach { connectNextPackets += aclCountsByHandle[it] ?: 0L }
        val status = when {
            vendorFrames > 0 -> "HCI analysis found ConnectNext HTS traffic across detected RFCOMM DLCIs (dynamic L2CAP CID)"
            candidateHandles.isNotEmpty() -> "HCI analysis found RFCOMM traffic, but no complete HTS frame was decoded"
            else -> "HCI analysis found no ConnectNext HTS RFCOMM traffic"
        }
        return HciAnalysisResult(
            records,
            aclPackets,
            connectNextPackets,
            vendorFrames,
            dlcis.toList(),
            services.toList(),
            lastPreview,
            decodedOdo,
            decodedFuel,
            decodedDte,
            decodedVehicleTemp,
            decodedFanSpeed,
            decodedAcOn,
            decodedAutoMode,
            decodedEcoMode,
            decodedAirFlow,
            decodedRecirc,
            decodedMaxDefrost,
            decodedRearDefog,
            decodedHighFan,
            decodedLowFan,
            decodedHighTemp,
            decodedLowTemp,
            status,
            evidence.toList()
        )
    }

    fun currentLogFileName(): String = protocolLogger.currentFileName() ?: "A new log will be created when capture starts"
    fun currentLogUri(): Uri? = protocolLogger.currentUri()
    fun logDirectoryPath(): String = protocolLogger.directoryDescription()
    fun closeLog() = protocolLogger.close()

    /** Detaches only the UI observer; the long-lived vehicle manager stays active. */
    fun clearStateListener() { stateListener = null }

    fun close() {
        disconnect()
        try { if (receiverRegistered) context.unregisterReceiver(bluetoothStateReceiver) } catch (_: Exception) { }
        receiverRegistered = false
        stateListener = null
        packetListener = null
    }

    fun clearDevices() {
        discovered.clear()
        state = state.copy(devices = emptyList(), selectedDevice = null, status = "Device list cleared")
        publish()
    }

    /**
     * Coalesces high-frequency Bluetooth updates so a busy HU cannot force Compose
     * to recompose once per packet. Counters/state remain updated on the producer
     * thread; only publication to the UI is rate limited.
     */
    private fun publishHighRate() {
        // Coalesce all high-frequency Bluetooth updates into one UI snapshot every
        // 100 ms. Packet capture must never scale Compose work with packet volume.
        if (!highRatePublishScheduled.compareAndSet(false, true)) return
        mainHandler.postDelayed({
            try { publish() } finally { highRatePublishScheduled.set(false) }
        }, 100L)
    }

    private fun publish() {
        val live = liveSession.get()
        // Overlay the atomic session state onto the diagnostic snapshot. Other
        // background callbacks may have copied an older `state`; they must never
        // be able to erase a real validated LIVE transition before it reaches UI.
        val snapshot = state.copy(
            devices = discovered.values.toList().takeLast(100),
            htsConnected = live.htsConnected,
            liveOdo = live.liveOdo,
            liveFuelPercent = live.liveFuelPercent,
            liveRpm = live.liveRpm,
            liveDteKm = live.liveDteKm,
            liveTripA = live.liveTripA,
            liveTripB = live.liveTripB,
            liveAvgFuelEcoTripA = live.liveAvgFuelEcoTripA,
            liveAvgFuelEcoTripB = live.liveAvgFuelEcoTripB,
            liveVisValidated = live.liveVisValidated,
            liveDataAt = live.liveDataAt,
            liveDataAtMs = live.liveDataAtMs
        )
        mainExecutor.execute { stateListener?.invoke(snapshot) }
    }

    private fun ByteArray.indexOfSequenceForLive(pattern: ByteArray, start: Int = 0): Int {
        if (pattern.isEmpty()) return start.coerceIn(0, size)
        var i = start.coerceAtLeast(0)
        while (i + pattern.size <= size) {
            var j = 0
            while (j < pattern.size && this[i + j] == pattern[j]) j++
            if (j == pattern.size) return i
            i++
        }
        return -1
    }

    private fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

    private fun handleMirroredHtsFrame(frame: MirroredHtsFrame, device: BluetoothDevice) {
        connectNextMirrorSession.onFrame(frame)
        if (frame.directionType == ConnectNextHtsWire.ACK) {
            val action = htsAckActions.remove(frame.sequence)
            if (action != null) action.invoke() else recordChannel("HTS ACK RX seq=${frame.sequence}")
            return
        }
        if (!frame.checksumValid) {
            recordChannel("HTS FRAME REJECTED checksum-invalid seq=${frame.sequence} type=0x%02X".format(frame.messageType ?: -1))
            return
        }

        // ACK the HU first, exactly as the captured session does, and only then advance
        // application-state or issue the next client message. Unknown ACK families remain passive.
        writeHts(ConnectNextHtsWire.encodeAck(frame.sequence), "HTS-ACK-TX")

        val type = frame.messageType ?: -1
        val typeLabel = "0x%02X".format(type)
        // A validated HTS frame is authoritative evidence that the accepted
        // RFCOMM session is alive. Keep this outside the mutable diagnostic state
        // so concurrent RX/ACL callbacks cannot overwrite the connection phase.
        liveSession.updateAndGet { it.copy(htsConnected = true) }
        // The HU assigns a NEW connection id to VehicleInfoService in every session
        // (2026-09-21 logs: 27, 37, 47, 56, 66, 76) and it is the message type of the
        // vehicle data frames. It was captured in evidenceServiceConnectionIds but
        // never used, while 0x25/0x2F were hard-coded. Use the negotiated id; the
        // legacy constants are only a fallback before the id is known.
        val vehicleInfoConnId = evidenceServiceConnectionIds["VehicleInfoService"]
            ?.let { it and 0xFF }   // the wire message type is one byte
            ?.takeIf { it != 0 && !isHtsControlType(it) }
        val isX451Frame = if (vehicleInfoConnId != null) {
            type == vehicleInfoConnId
        } else {
            type == ConnectNextHtsWire.VEHICLE_PACKET_X451 ||
                type == ConnectNextHtsWire.VEHICLE_PACKET_X451_LIVE
        }
        val kind = when {
            isX451Frame -> ConnectNextMessageKind.VEHICLE_BINARY
            isHtsControlType(type) -> ConnectNextMessageKind.CONTROL_TEXT
            else -> ConnectNextMessageClassifier.classify(frame)
        }
        state = state.copy(
            htsConnected = true,
            htsFrames = state.htsFrames + 1,
            htsJsonFrames = state.htsJsonFrames + if (kind == ConnectNextMessageKind.CONTROL_TEXT) 1 else 0,
            htsBinaryFrames = state.htsBinaryFrames + if (kind == ConnectNextMessageKind.VEHICLE_BINARY) 1 else 0,
            htsVehicleBinaryFrames = state.htsVehicleBinaryFrames + if (kind == ConnectNextMessageKind.VEHICLE_BINARY) 1 else 0,
            htsLastSequence = frame.sequence.toLong(),
            htsLastMessageType = typeLabel,
            status = "ConnectNext HTS active — frame received"
        )
        publishHighRate()

        if (kind == ConnectNextMessageKind.VEHICLE_BINARY) {
            recordChannel("HTS VEHICLE SERVICE RAW seq=${frame.sequence} type=${typeLabel} bytes=${frame.body.size} checksum=VALID")
            recordChannel("HTS VEHICLE SERVICE HEX ${frame.body.toHex()}")
            // X451 0x25 frames can arrive before the asynchronous NETWORK_ID/UID
            // exchange completes. The 2026-09-19 capture proves this ordering: the
            // first valid 0x25 frame arrives at 14:22:49, while uid=544216271207 is
            // not observed until 14:26. Do not drop an otherwise valid X451 frame
            // merely because the UID is not known yet. If a UID is already known,
            // still enforce the evidence-backed vehicle match.
            val x451UidAllowed = observedVehicleUid == null ||
                observedVehicleUid == VehiclePacketParser_X451.EXACT_UID_VARIANT

            if (isX451Frame && x451UidAllowed) {
                val wrapperSummary = X451VehicleWrapper.summary(frame.body)
                recordChannel(
                    "EVIDENCE X451 SELECTED uid=${observedVehicleUid ?: "PENDING"} " +
                        "htsType=$typeLabel $wrapperSummary"
                )

                // Read-only live VIS extraction. The X451 decoder is fed only
                // the X451-selected 0x25 VehicleInfo frames. Legacy 0x1B
                // packets are retained as raw evidence and cannot overwrite
                // the X451 live snapshot.
                x451LiveDecoder.consume(frame.body)?.let { v ->
                    recordChannel(
                        "X451 VIS VALUES source=${v.source} validated=${v.validated} " +
                            "ODO=${v.odo ?: "-"} FUEL=${v.fuelPercent ?: "-"} " +
                            "DTE=${v.dteKm ?: "-"} RPM=${v.rpm ?: "-"} TRIPA=${v.tripA ?: "-"} TRIPB=${v.tripB ?: "-"} " +
                            "AVG_A=${v.avgFuelEcoTripA ?: "-"} AVG_B=${v.avgFuelEcoTripB ?: "-"}"
                    )
                    if (v.validated) {
                        val liveNow = System.currentTimeMillis()
                        val liveAt = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(liveNow))
                        liveSession.updateAndGet { current ->
                            current.copy(
                                htsConnected = true,
                                liveOdo = v.odo ?: current.liveOdo,
                                liveRpm = v.rpm ?: current.liveRpm,
                                liveFuelPercent = v.fuelPercent ?: current.liveFuelPercent,
                                liveDteKm = v.dteKm ?: current.liveDteKm,
                                liveTripA = v.tripA ?: current.liveTripA,
                                liveTripB = v.tripB ?: current.liveTripB,
                                liveAvgFuelEcoTripA = v.avgFuelEcoTripA ?: current.liveAvgFuelEcoTripA,
                                liveAvgFuelEcoTripB = v.avgFuelEcoTripB ?: current.liveAvgFuelEcoTripB,
                                liveVisValidated = true,
                                liveDataAt = liveAt,
                                liveDataAtMs = liveNow
                            )
                        }
                        val tripKey = (state.selectedDevice?.address ?: selectedVehicleKey())?.uppercase(Locale.US)?.takeIf { it.isNotBlank() }
                        val previousTripSample = tripDetectionBaseline
                        state = state.copy(
                            liveOdo = v.odo ?: state.liveOdo,
                            liveRpm = v.rpm ?: state.liveRpm,
                            liveFuelPercent = v.fuelPercent ?: state.liveFuelPercent,
                            liveDteKm = v.dteKm ?: state.liveDteKm,
                            liveTripA = v.tripA ?: state.liveTripA,
                            liveTripB = v.tripB ?: state.liveTripB,
                            liveAvgFuelEcoTripA = v.avgFuelEcoTripA ?: state.liveAvgFuelEcoTripA,
                            liveAvgFuelEcoTripB = v.avgFuelEcoTripB ?: state.liveAvgFuelEcoTripB,
                            liveVisValidated = true,
                            liveDataAt = liveAt,
                            liveDataAtMs = liveNow
                        )
                        connectNextWatchdogHandler.removeCallbacks(connectNextWatchdog)
                        markConnectNextProgress()
                        persistValidatedVisSnapshot(v)
                        if (tripKey != null) {
                            // ODO is the authoritative physical-movement signal whenever it is
                            // available. Trip A/B can advance slightly while stationary, which
                            // must not keep an active trip alive past the 5-minute idle threshold.
                            // Fall back to Trip A/B only when ODO is unavailable in either sample.
                            val moved = if (previousTripSample != null) {
                                // ODO is emitted only by the 1-second/234-byte snapshot, while
                                // Trip A/B are emitted by the much faster 614-byte stream. The
                                // decoder carries the last ODO forward between snapshot frames;
                                // treating that carried value as fresh suppresses movement until
                                // the integer ODO advances by a whole kilometre. Only a fresh ODO
                                // increase or a validated fractional Trip A/B increase is evidence
                                // of movement. Trip A/B are distance counters (0.1 km resolution),
                                // so this also preserves genuinely short drives.
                                val odoMoved = v.odoFresh && v.odo != null && previousTripSample.odo != null &&
                                    v.odo > previousTripSample.odo
                                val tripAMoved = v.tripCountersFresh && previousTripSample.tripA != null && v.tripA != null &&
                                    v.tripA - previousTripSample.tripA >= 0.05f
                                val tripBMoved = v.tripCountersFresh && previousTripSample.tripB != null && v.tripB != null &&
                                    v.tripB - previousTripSample.tripB >= 0.05f
                                odoMoved || tripAMoved || tripBMoved
                            } else {
                                false
                            }
                            val tripNow = System.currentTimeMillis()
                            // A movement sample after 5+ minutes of inactivity belongs to a
                            // NEW trip. startDriveIfNeeded() atomically finalizes the stale
                            // active row before creating the new active row. For stationary
                            // samples, the independent Application watchdog remains responsible
                            // for finalization even after Bluetooth disconnects.
                            val finalizedForInactivity = if (!moved) {
                                vehicleSnapshotDb.maybeFinalizeInactiveTrip(tripKey, tripNow, v.odo, v.tripA, v.tripB)
                            } else {
                                false
                            }
                            if (moved) {
                                vehicleSnapshotDb.startDriveIfNeeded(
                                    tripKey,
                                    tripNow,
                                    previousTripSample?.odo ?: v.odo,
                                    previousTripSample?.tripA ?: v.tripA,
                                    previousTripSample?.tripB ?: v.tripB,
                                    previousTripSample?.fuelPercent ?: v.fuelPercent
                                )
                            }
                            if (finalizedForInactivity) {
                                vehicleSnapshotDb.upsertDailyTelemetrySample(
                                    VehicleSnapshotDb.TelemetrySample(
                                        timestamp = tripNow, vehicleKey = tripKey, odo = v.odo,
                                        fuelPercent = v.fuelPercent, dteKm = v.dteKm, tripA = v.tripA,
                                        tripB = v.tripB, avgA = v.avgFuelEcoTripA, avgB = v.avgFuelEcoTripB
                                    )
                                )
                            }
                            // Update active-trip duration/last values even between distance-counter
                            // changes, but throttle stationary progress writes to one per second.
                            // A confirmed movement is always persisted immediately.
                            if (!finalizedForInactivity && (moved || tripNow - lastTripProgressPersistAt >= 1_000L)) {
                                vehicleSnapshotDb.recordDriveSample(
                                    tripKey, tripNow, v.odo, v.tripA, v.tripB,
                                    v.avgFuelEcoTripA ?: v.avgFuelEcoTripB, v.fuelPercent,
                                    movementConfirmed = moved
                                )
                                lastTripProgressPersistAt = tripNow
                            }
                        }
                        tripDetectionBaseline = v
                        publishHighRate()
                    }
                }
            } else if (isX451Frame) {
                recordChannel(
                    "EVIDENCE X451 FRAME type=$typeLabel uid=${observedVehicleUid ?: "UNKNOWN"} " +
                        "parser-selection=REJECTED_UID"
                )
            }
            return processObservedHtsControl(frame)
        }
        if (kind == ConnectNextMessageKind.NETWORK_ID) {
            val networkJson = decodeObservedNetworkJson(frame.body)
            if (networkJson != null) {
                observedNetworkIdentityJson = networkJson.optString("NETWORK_ID", null)
                observedVehicleUid = extractVehicleUid(observedNetworkIdentityJson)
                observedHuOwnDeviceId = networkJson.optInt("OWN_DEVICE_ID", -1).takeIf { it >= 0 }
                observedPeerDeviceId = networkJson.optInt("PEER_DEVICE_ID", -1).takeIf { it >= 0 }
                recordChannel(
                    "HTS NETWORK_ID decoded own=${networkJson.optInt("OWN_DEVICE_ID", -1)} " +
                        "peer=${networkJson.optInt("PEER_DEVICE_ID", -1)} " +
                        "hts=${networkJson.optString("HTS_VERSION", "")}" +
                        " uid=${observedVehicleUid ?: "UNKNOWN"}"
                )
                if (connectNextNegotiationStartedAt == 0L) {
                    connectNextNegotiationStartedAt = System.currentTimeMillis()
                }
                processObservedNetworkIdentity(networkJson)
                markConnectNextProgress()
            } else {
                recordChannel("HTS NETWORK_ID decode failed — preserving raw frame")
            }
            return
        }
        if (kind == ConnectNextMessageKind.CONTROL_TEXT) {
            recordChannel("HTS CONTROL ${decodeObservedText(frame.body).take(320)}")
        }
        processObservedHtsControl(frame)
    }

    private fun decodeObservedNetworkJson(body: ByteArray): JSONObject? {
        if (body.size < 7) return null
        val declaredInner = (body[0].toInt() and 0xFF) or ((body[1].toInt() and 0xFF) shl 8)
        val marker = body[2].toInt() and 0xFF
        val jsonLength: Int
        val jsonStart: Int
        when {
            marker == 0x04 -> {
                // HU-originated: [LE16 innerLength][04][LE32 JSON length][JSON]
                jsonLength = le32Observed(body, 3)
                jsonStart = 7
            }
            body.size >= 7 && marker == 0x01 && (body[3].toInt() and 0xFF) == 0x00 -> {
                // Phone-originated: [LE16 innerLength][01 00][3-byte BE JSON length][JSON]
                jsonLength = ((body[4].toInt() and 0xFF) shl 16) or
                    ((body[5].toInt() and 0xFF) shl 8) or
                    (body[6].toInt() and 0xFF)
                jsonStart = 7
            }
            else -> return null
        }
        if (jsonLength < 2 || jsonStart + jsonLength > body.size) return null
        val expectedInner = if (marker == 0x04) 5 + jsonLength else 5 + jsonLength
        if (declaredInner != expectedInner) return null
        return try {
            JSONObject(body.copyOfRange(jsonStart, jsonStart + jsonLength).toString(Charsets.UTF_8))
        } catch (_: Exception) { null }
    }

    private fun le32Observed(body: ByteArray, offset: Int): Int =
        (body[offset].toInt() and 0xFF) or
            ((body[offset + 1].toInt() and 0xFF) shl 8) or
            ((body[offset + 2].toInt() and 0xFF) shl 16) or
            ((body[offset + 3].toInt() and 0xFF) shl 24)

    private fun decodeObservedText(body: ByteArray): String {
        if (body.isEmpty()) return ""
        if (body.size >= 2) {
            val declared = (body[0].toInt() and 0xFF) or ((body[1].toInt() and 0xFF) shl 8)
            if (declared <= body.size - 2) {
                val payload = body.copyOfRange(2, 2 + declared)
                return payload.toString(Charsets.UTF_8).trimEnd('\u0000')
            }
        }
        return ""
    }

    private fun extractVehicleUid(networkId: String?): String? {
    if (networkId.isNullOrBlank()) return null

    val plain = Regex("\"UID_NUMBER\"\\s*:\\s*\"([^\"]+)\"")
        .find(networkId)?.groupValues?.getOrNull(1)

    if (!plain.isNullOrBlank()) return plain

    return Regex("UID_NUMBER\\s*[:=]\\s*([0-9A-Za-z]+)")
        .find(networkId)?.groupValues?.getOrNull(1)
    }

    private fun processObservedNetworkIdentity(networkJson: JSONObject) {
        val htsVersion = networkJson.optString("HTS_VERSION", "")
        if (htsVersion.isBlank()) return
        evidenceVersionSeen = true
        if (!evidenceSessionStarted) {
            evidenceSessionStarted = true
            state = state.copy(
                cnProtocolStage = "NEGOTIATING",
                cnHandshakeSeen = true,
                cnAuthDetail = "Observed ConnectNext HTS 2.0 session"
            )
            publish()
        }
        sendObservedNetworkIdentity()
        recordChannel("EVIDENCE HTS_VERSION observed — mirrored current HU NETWORK_ID response prepared")
    }

    private fun processObservedHtsControl(frame: MirroredHtsFrame) {
        // Binary length-control MUST be recognized before attempting UTF-8 text
        // decoding. This is the proven fix that unlocked the application-level
        // Sl_Supported_App_Ack exchange.
        val announcedLength = isObservedLengthControl(frame)
        if (announcedLength != null) {
            recordChannel("EVIDENCE RX HU lengthControl payloadLen=$announcedLength seq=${frame.sequence}")
            respondToObservedLengthControl(announcedLength)
            markConnectNextProgress()
            return
        }

        val text = decodeObservedText(frame.body)
        if (text.isBlank()) return
        if (text.contains("Sl_Inf_Dev_Details", true)) {
            evidenceDeviceDetailsSeen = true
            markConnectNextProgress()
            sendObservedSupportedApps()
        }
        if (text.contains("Sl_Supported_App_Ack", true)) {
            evidenceSupportedAppsAckSeen = true
            markConnectNextProgress()
            recordChannel("EVIDENCE RX Sl_Supported_App_Ack — HU accepted SupportedApps advertisement; service clients unlocked")
            processNextEvidenceService()
        }
        Regex("Srvc_Available,([A-Za-z0-9_]+),").findAll(text).forEach { m ->
            val service=m.groupValues[1]
            if (service in observedVehicleServices) {
                evidenceServicesSeen += service
                if (service !in evidenceServicesRequested && !evidenceServiceQueue.contains(service)) evidenceServiceQueue.addLast(service)
            }
        }
        Regex("Resp_Remote_Client_Con,([A-Za-z0-9_]+),Srvc_Conn_Success,(-?\\d+)").find(text)?.let { m ->
            val service=m.groupValues[1]
            val connId=m.groupValues[2]
            evidenceServicesSeen += service
            evidenceServicesRequested += service
            evidenceServiceFailed.remove(service)
            evidenceServiceConnectionIds[service] = connId.toIntOrNull() ?: -1
            markConnectNextProgress()
            evidenceServiceAwaiting=null
            state=state.copy(
                cnDiscoverySeen=true,
                cnProtocolStage=if(service=="VehicleInfoService") "ACTIVE" else state.cnProtocolStage,
                cnServices=(state.cnServices+service).distinct().takeLast(30),
                cnLastResponse="Resp_Remote_Client_Con,$service,Srvc_Conn_Success,$connId"
            )
            if (service == "VehicleInfoService") {
                recordChannel("EVIDENCE VEHICLE INFO CHANNEL CONNECTED id=$connId — raw 0x1B packets retained; no unproven field decoding")
            }
            publish()
            recordChannel("EVIDENCE SERVICE CONNECTED service=$service serviceConnectionId=$connId clientId=${currentHtsClientId()}")
        }
        processNextEvidenceService()
    }


}
