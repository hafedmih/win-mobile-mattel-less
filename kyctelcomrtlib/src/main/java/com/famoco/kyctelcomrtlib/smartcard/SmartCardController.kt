package com.famoco.kyctelcomrtlib.smartcard

import android.content.Context
import android.nfc.tech.IsoDep
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.famoco.androidpcsclib.Card
import com.famoco.androidpcsclib.CardTerminal
import com.famoco.androidpcsclib.CommandAPDU
import com.famoco.androidpcsclib.PCSC
import com.famoco.androidpcsclib.PCSC.PCSCCallbackInterface
import com.famoco.kyctelcomrtlib.smartcard.APDUUtils.Companion.parseIdentityNewCard
import kotlinx.coroutines.*
import java.io.*
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

class SmartCardController(context: Context) :
    PCSCCallbackInterface {
    companion object {
        private val TAG = SmartCardController::class.simpleName
        private const val NO_READER_FOUND = "no reader found"
    }

    private val pcsc = PCSC(context, this)
    private var selectedTerminal: CardTerminal? = null
    private var card: Card? = null

    /*------------------ Coroutine Scopes ------------------*/
    private val commandCoroutineScope = CustomCoroutineScope()
    /*--------------------------------------------------*/

    /*------------------ Live Data to observe ------------------*/
    private val _readerStateLiveData = MutableLiveData<ReaderState>().apply { value = ReaderState.NOT_INITIALIZED }
    val readerStateLiveData: LiveData<ReaderState> = _readerStateLiveData

    private val _connectionError = MutableLiveData<String>().apply { value = "" }
    val connectionError: LiveData<String> = _connectionError

    private val _cardStateLiveData = MutableLiveData<CardState>().apply { value =
        CardState.NO_CARD_DETECTED
    }
    val cardStateLiveData: LiveData<CardState> = _cardStateLiveData

    private val _currentOperation = MutableLiveData<Operation>().apply { value = Operation.IDLE }
    val currentOperation: LiveData<Operation> = _currentOperation

    private val _atrLiveData = MutableLiveData<String>().apply { value = "" }
    val atrLiveData: LiveData<String> = _atrLiveData

    private val _apduLiveData = MutableLiveData<String>().apply { value = "" }
    val apduLiveData: LiveData<String> = _apduLiveData

    private val _apduErrorMessageLiveData = MutableLiveData<String>().apply { value = "" }
    val apduErrorMessageLiveData: LiveData<String> = _apduErrorMessageLiveData

    private val _rapduLiveData = MutableLiveData<String>().apply { value = "" }
    val rapduLiveData: LiveData<String> = _rapduLiveData

    private val _cardNumberLiveData = MutableLiveData<String>().apply { value = "" }
    val cardNumberLiveData: LiveData<String> = _cardNumberLiveData

    private val _identityLiveData = MutableLiveData<Identity?>().apply { value = null }
    val identityLiveData: LiveData<Identity?> = _identityLiveData

    private val _matchLiveData = MutableLiveData<Boolean?>().apply { value = null }
    val matchLiveData: LiveData<Boolean?> = _matchLiveData

    private val _attemptLeft = MutableLiveData<Int?>().apply { value = null }
    val attemptLeft: LiveData<Int?> = _attemptLeft

    /*------------------------------------------------------*/

    fun init() {
        Log.i(TAG, "init")
        if (readerStateLiveData.value == ReaderState.INITIALIZED) {
            Log.w(TAG, "reader already initialized")
            return
        }

        commandCoroutineScope.launch {
            Log.i(TAG, "initializing card reader")
            _readerStateLiveData.postValue(ReaderState.INITIALIZING)
            pcsc.enableCCIDLogs(true)
            if (pcsc.terminals.isEmpty()) {
                Log.i(TAG, "registering devices")
                pcsc.init()
                if (pcsc.terminals.isEmpty()) {
                    Log.i(TAG, "no reader found")
                    _connectionError.postValue(NO_READER_FOUND)
                    _readerStateLiveData.postValue(ReaderState.NOT_INITIALIZED)
                } else {
                    Log.i(TAG, "card reader initialized")
                    onTerminalListChange(pcsc.terminals)
                    onCardStatusChange(selectedTerminal, selectedTerminal?.isCardPresent)
                    _readerStateLiveData.postValue(ReaderState.INITIALIZED)
                }
            }
        }
    }

    fun unInit() {
        Log.i(TAG, "unInit")
        commandCoroutineScope.launch {
            try {
                pcsc.close()
            } catch (e: Exception) {
                // nothing to do
            }
            _readerStateLiveData.postValue(ReaderState.NOT_INITIALIZED)
            _cardStateLiveData.postValue(CardState.NO_CARD_DETECTED)
            _currentOperation.postValue(Operation.IDLE)
            _atrLiveData.postValue("")
            _apduLiveData.postValue("")
            _apduErrorMessageLiveData.postValue("")
            _rapduLiveData.postValue("")
            _attemptLeft.postValue(null)
            _cardNumberLiveData.postValue("")
            _identityLiveData.postValue(null)
            _matchLiveData.postValue(null)
        }
    }

    fun resetLiveData() {
        _currentOperation.postValue(Operation.IDLE)
        _atrLiveData.postValue("")
        _apduLiveData.postValue("")
        _rapduLiveData.postValue("")
        _apduErrorMessageLiveData.postValue("")
        _rapduLiveData.postValue("")
        _attemptLeft.postValue(null)
        _cardNumberLiveData.postValue("")
        _identityLiveData.postValue(null)
        _matchLiveData.postValue(null)
    }

    fun askCardNumber(isoDep: IsoDep) {
        Log.i(TAG, "ask card number")
        commandCoroutineScope.launch {
            _currentOperation.postValue(Operation.ASK_CARD_NUMBER)
            askCardNumberInternal(isoDep)
            _currentOperation.postValue(Operation.IDLE)
        }
    }

    fun askIdentity(isoDep: IsoDep) {
        Log.i(TAG, "ask identity")
        commandCoroutineScope.launch {
            _currentOperation.postValue(Operation.ASK_IDENTITY)
            askIdentityInternal(isoDep)
            _currentOperation.postValue(Operation.IDLE)
        }
    }

    fun askBiometry(isoDep: IsoDep,template: ByteArray, chosenFinger: FingerEnum) {
        Log.i(TAG, "ask biometry")
        commandCoroutineScope.launch {
            _currentOperation.postValue(Operation.ASK_MATCH_ON_CARD)
            askBiometryInternal(isoDep ,template, chosenFinger)
            _currentOperation.postValue(Operation.IDLE)
        }
    }

    fun askMockBiometry(chosenFinger: FingerEnum) {
        Log.i(TAG, "ask mock biometry")
        commandCoroutineScope.launch {
            //Mock 2000 ms with valid answer
            _currentOperation.postValue(Operation.ASK_MATCH_ON_CARD)
            Log.i(TAG, "askMockBiometry: matching with the ${chosenFinger.name} finger")
            delay(2000)
            _attemptLeft.postValue(15)
            _matchLiveData.postValue(true)
            _currentOperation.postValue(Operation.IDLE)
        }
    }

    private suspend fun askCardNumberInternal(isoDep: IsoDep) {
        var command =
            APDUUtils.formatSelectAidApdu(HexUtils.hexStringToByteArray(APDUUtils.AID_BIOMETRY))
        var res = sendApdu(isoDep,command)
        if (res.first != SWEnum.ISO7816_0x9000.hexValue) {
            Log.w(TAG, "card number: ask application id failed")
            _cardNumberLiveData.postValue("")
            return
        }

        command =
            APDUUtils.formatSelectDataSetApdu(HexUtils.hexStringToByteArray(APDUUtils.CARD_NUMBER_EF_FIX))
        res = sendApdu(isoDep,command)
        if (res.first != SWEnum.ISO7816_0x9000.hexValue) {
            Log.w(TAG, "card number: file selection failed")
            _cardNumberLiveData.postValue("")
            return
        }

        command = APDUUtils.formatReadBinaryFileApdu(0)
        res = sendApdu(isoDep,command)
        if (res.first != SWEnum.ISO7816_0x9000.hexValue) {
            Log.w(TAG, "card number: read file failed")
            _cardNumberLiveData.postValue("")
            return
        }
        val cardNumber = APDUUtils.parseCardNumber(res.second)
        Log.i(TAG, "card number: $cardNumber")
        _cardNumberLiveData.postValue(cardNumber)
    }

    private suspend fun askIdentityInternal(isoDep: IsoDep) {
        val cardNumber = cardNumberLiveData.value
        if (cardNumber == null) {
            askCardNumberInternal(isoDep)
        }

        val newCard = cardNumberLiveData.value?.let { value ->
            value.isNotEmpty()
                    && !value.first().isDigit()
                    && !value.startsWith("RC", ignoreCase = true)
        } ?: false

        var command = APDUUtils.formatSelectAidApdu(HexUtils.hexStringToByteArray(APDUUtils.AID_IDENTITE))
        var res = sendApdu(isoDep,command)
        if (res.first != SWEnum.ISO7816_0x9000.hexValue) {
            Log.w(TAG, "identity: ask application id failed")
            return
        }

        command =
            APDUUtils.formatSelectDataSetApdu(HexUtils.hexStringToByteArray(APDUUtils.IDENTITE_EF_FIX))
        res = sendApdu(isoDep,command)
        if (res.first != SWEnum.ISO7816_0x9000.hexValue) {
            Log.w(TAG, "identity: file selection failed")
            return
        }

        var data = ByteArray(0)
        var len = 0
        var end = false
        while (!end) {
            command = APDUUtils.formatReadBinaryFileApdu(len)
            res = sendApdu(isoDep,command, true)
            end = (res.first != SWEnum.ISO7816_0x9000.hexValue)
            data += res.second
            len += res.second.size
        }

        if (data.isEmpty()) {
            Log.w(TAG, "identity: read file failed")
            return
        }

//        val identity = if (newCard == true) APDUUtils.parseIdentityNewCard(data) else APDUUtils.parseIdentity(data)
        val (sw, rawIdentityData) = readIdentityFileInChunks(isoDep, 4636)
        val identity = parseIdentityNewCard(rawIdentityData);

        Log.i(TAG, "personalNumber: ${identity.personalNumber}")
        Log.i(TAG, "firstnameLoc: ${identity.firstnameLoc}")
        Log.i(TAG, "firstName: ${identity.firstName}")
        Log.i(TAG, "fatherFirstNameLoc: ${identity.fatherFirstNameLoc}")
        Log.i(TAG, "fatherFirstName: ${identity.fatherFirstName}")
        Log.i(TAG, "lastNameLoc: ${identity.lastNameLoc}")
        Log.i(TAG, "lastName: ${identity.lastName}")
        Log.i(TAG, "sexLoc: ${identity.sexLoc}")
        Log.i(TAG, "sex: ${identity.sex}")
        Log.i(TAG, "dateOfBirth: ${identity.dateOfBirth}")
        Log.i(TAG, "placeOfBirthLoc: ${identity.placeOfBirthLoc}")
        Log.i(TAG, "placeOfBirth: ${identity.placeOfBirth}")
        Log.i(TAG, "newCard: ${newCard}")

        _identityLiveData.postValue(identity)
    }

    private suspend fun askBiometryInternal(isoDep: IsoDep,template: ByteArray, chosenFinger: FingerEnum) {



        var command =
            APDUUtils.formatSelectAidApdu(HexUtils.hexStringToByteArray(APDUUtils.AID_BIOMETRY))
        var res = sendApdu(isoDep,command)
        if (res.first != SWEnum.ISO7816_0x9000.hexValue) {
            Log.w(TAG, "biometry: ask application id failed")
            return
        }

        command = APDUUtils.formatSelectDataSetApdu(HexUtils.hexStringToByteArray(APDUUtils.BIOMETRY_CARD_ID))
        res = sendApdu(isoDep,command)
        if (res.first != SWEnum.ISO7816_0x9000.hexValue) {
            Log.w(TAG, "biometry: file selection failed")
            return
        }

        command = APDUUtils.formatReadBinaryFileApdu(0)
        res = sendApdu(isoDep,command)
        if (res.first != SWEnum.ISO7816_0x9000.hexValue) {
            Log.w(TAG, "biometry: read file failed")
            return
        }

        command = when (chosenFinger) {
            FingerEnum.RIGHT -> {
                APDUUtils.formatMatchApdu(template, APDUUtils.BIOMETRY_FINGER_11.toByte())
            }
            FingerEnum.LEFT -> {
                APDUUtils.formatMatchApdu(template, APDUUtils.BIOMETRY_FINGER_12.toByte())
            }
        }

        // USE sendChainedApdu INSTEAD OF sendApdu
        // This ensures the large template is split correctly for NFC
        res = sendChainedApdu(isoDep, command)



        if (res.first != SWEnum.ISO7816_0x9000.hexValue) {
            setAttemptLeft(res.first)
            Log.i(TAG, "biometry: matching ${chosenFinger.name} finger failed")
            _matchLiveData.postValue(false)
            return
        }
        Log.i(TAG, "biometry: matching ${chosenFinger.name} finger succeed")
        _attemptLeft.postValue(15)
        _matchLiveData.postValue(true)
    }

//    private suspend fun sendApdu(command: ByteArray, ignoreError: Boolean = false): Pair<String, ByteArray> {
//        val commandString = HexUtils.byteArrayToHexString(command)
//        Log.i(TAG, "send apdu >= $commandString")
//        _apduLiveData.postValue(commandString)
//        val commandApdu = CommandAPDU(command)
//        val r = card?.basicChannel?.transmit(commandApdu)
//        val resIntSuccessCode = r?.sw
//        val resSuccessCode = if (resIntSuccessCode != null) HexUtils.integerToHexString(
//            resIntSuccessCode
//        ).uppercase()
//        else ""
//        if (resSuccessCode != SWEnum.ISO7816_0x9000.hexValue && !ignoreError) {
//            Log.i(TAG, "rapdu <= empty - $resSuccessCode")
//            _apduErrorMessageLiveData.postValue(getSWDescription(resSuccessCode))
//            _rapduLiveData.postValue("empty - $resSuccessCode")
//            return Pair(resSuccessCode, ByteArray(0))
//        }
//
//        val data = r?.data ?: ByteArray(0)
//        val dataString = HexUtils.byteArrayToHexString(data)
//        Log.i(TAG, "rapdu <= $dataString - $resSuccessCode")
//        _rapduLiveData.postValue("$dataString - $resSuccessCode")
//        return Pair(resSuccessCode, data)
//    }

    suspend fun readIdentityFileInChunks(isoDep: IsoDep, totalLength: Int): Pair<String, ByteArray> {
        val fullData = ByteArrayOutputStream()
        var offset = 0
        val chunkSize = 220

        while (offset < totalLength) {
            val lengthToRead = minOf(chunkSize, totalLength - offset)

            val p1 = (offset shr 8).toByte()
            val p2 = (offset and 0xFF).toByte()
            val le = lengthToRead.toByte()

            // READ BINARY command
            val cmd = byteArrayOf(0x00, 0xB0.toByte(), p1, p2, le)

            val (sw, data) = sendApdu(isoDep, cmd)

            if (sw == SWEnum.ISO7816_0x9000.hexValue || sw == "9000") {
                fullData.write(data)
                offset += data.size
            } else {
                return Pair(sw, ByteArray(0))
            }
        }

        return Pair("9000", fullData.toByteArray())
    }
    private suspend fun sendApdu(
        isoDep: IsoDep,
        command: ByteArray,
        ignoreError: Boolean = false
    ): Pair<String, ByteArray> { // Opened with curly brace
        // Explicit return here fixes the "Found: Unit" error
        return withContext(Dispatchers.IO) {

            var currentCommand = command
            var accumulatedData = ByteArray(0)

            try {
                while (true) {
                    val commandString = HexUtils.byteArrayToHexString(currentCommand)
                    Log.i(TAG, "send apdu (IsoDep) >= $commandString")
                    _apduLiveData.postValue(commandString)

                    val response: ByteArray = isoDep.transceive(currentCommand)

                    if (response.size < 2) {
                        val err = "NO_SW"
                        _apduErrorMessageLiveData.postValue("Invalid response")
                        return@withContext Pair(err, ByteArray(0))
                    }

                    val sw1 = response[response.size - 2]
                    val sw2 = response[response.size - 1]
                    val sw = String.format("%02X%02X", sw1, sw2)

                    val chunkData = response.copyOf(response.size - 2)
                    accumulatedData = accumulatedData + chunkData

                    // Handle "More Data" (61 XX)
                    if (sw1 == 0x61.toByte()) {
                        currentCommand = byteArrayOf(0x00, 0xC0.toByte(), 0x00, 0x00, sw2)
                        continue
                    }

                    // Handle "Wrong Length" (6C XX)
                    if (sw1 == 0x6C.toByte()) {
                        currentCommand = command.copyOf(command.size - 1) + sw2
                        continue
                    }

                    val totalHex = HexUtils.byteArrayToHexString(accumulatedData)
                    Log.i(TAG, "rapdu <= $totalHex - $sw")
                    _rapduLiveData.postValue("$totalHex - $sw")

                    if (sw != SWEnum.ISO7816_0x9000.hexValue && !ignoreError) {
                        _apduErrorMessageLiveData.postValue(getSWDescription(sw))
                        return@withContext Pair(sw, ByteArray(0))
                    }

                    return@withContext Pair(sw, accumulatedData)
                }
                // This line is unreachable because of the loop/returns, but satisfies the compiler if needed
                return@withContext Pair("ERROR", ByteArray(0))

            } catch (ex: Exception) {
                Log.e(TAG, "IsoDep transceive error", ex)
                _apduErrorMessageLiveData.postValue("IsoDep Error: ${ex.message}")
                return@withContext Pair("ERROR", ByteArray(0))
            }
        }
    }
    private fun readData(context: Context, fileName: String): ByteArray {
        context.assets.open(fileName)

        val inputStream = context.assets.open(fileName)
        val buffer = ByteArray(inputStream.available())
        val output = ByteArrayOutputStream()
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
        return output.toByteArray()
    }

    private fun setAttemptLeft(swString: String) {
        for (sw in SWEnum.values()) {
            if ((swString == sw.hexValue) && swString.startsWith("63C")){
                _attemptLeft.postValue(
                    HexUtils.hexStringToByteArray(
                        "0" + swString.last().toString()
                    )[0].toInt())
                break
            }
        }
    }

    private fun getSWDescription(resSuccessCode: String): String {
        for (sw in SWEnum.values()) {
            if (resSuccessCode == sw.hexValue) {
                return sw.description
            }
        }
        return "An Error occurred with the card ($resSuccessCode)"
    }

    class CustomCoroutineScope internal constructor() : CoroutineScope {
        private val dispatcher = Executors.newSingleThreadExecutor()
            .asCoroutineDispatcher()

        override val coroutineContext: CoroutineContext =
            dispatcher + Job() + CoroutineExceptionHandler { coroutineContext: CoroutineContext, throwable: Throwable ->
                GlobalScope.launch { println("Caught $throwable") }
            }
    }

    override fun onTerminalListChange(cardTerminals: MutableList<CardTerminal>?) {
        Log.i(TAG, "onTerminalListChange")
        if (cardTerminals.isNullOrEmpty()) {
            Log.i(TAG, "terminal list empty => reset")
            selectedTerminal = null
            card = null
            _connectionError.postValue("")
            _readerStateLiveData.postValue(ReaderState.NOT_INITIALIZED)
            _currentOperation.postValue(Operation.IDLE)
            _cardStateLiveData.postValue(CardState.NO_CARD_DETECTED)
            _atrLiveData.postValue("")
            _apduLiveData.postValue("")
            _rapduLiveData.postValue("")
            _apduErrorMessageLiveData.postValue("")
            _rapduLiveData.postValue("")
            _attemptLeft.postValue(null)
            _cardNumberLiveData.postValue("")
            _identityLiveData.postValue(null)
            _matchLiveData.postValue(null)
        } else {
            Log.i(TAG, "terminal list not empty => initialized")
            selectedTerminal = cardTerminals[0]
            _readerStateLiveData.postValue(ReaderState.INITIALIZED)
        }
    }

    override fun onCardStatusChange(cardTerminal: CardTerminal?, isCardPresent: Boolean?) {
        Log.i(TAG, "onCardStatusChange: ${cardTerminal?.name} - $isCardPresent")
        if (cardTerminal == null) {
            Log.w(TAG, "no terminal found")
            return
        }
        if (isCardPresent == true) {
            card = cardTerminal.connect("*")
            val atr = cardTerminal.atr
            Log.i(TAG, "receiving atr <= $atr")
            _atrLiveData.postValue(atr.toString())
            if (cardTerminal.cardValid && atr.bytes?.isNotEmpty() == true) {
                _cardStateLiveData.postValue(CardState.CARD_DETECTED)
            } else {
                _cardStateLiveData.postValue(CardState.INVALID_CARD)
            }
        }
        if (isCardPresent == false) {
            card = null
            _cardStateLiveData.postValue(CardState.NO_CARD_DETECTED)
            _apduErrorMessageLiveData.postValue("")
            _currentOperation.postValue(Operation.IDLE)
        }
    }
    /**
     * Splits a large APDU into smaller chunks (Command Chaining) for NFC (IsoDep).
     * This mimics what the PCSC/Terminal driver does automatically.
     */
    private suspend fun sendChainedApdu(isoDep: IsoDep, fullCommand: ByteArray): Pair<String, ByteArray> {
        // 1. If command is small enough for NFC, send it normally
        if (fullCommand.size <= 250) {
            return sendApdu(isoDep, fullCommand)
        }

        // 2. Parse the APDU Header (CLA INS P1 P2 Lc)
        // We assume standard Short APDU header structure (5 bytes) + Data
        val cla = fullCommand[0]
        val ins = fullCommand[1]
        val p1 = fullCommand[2]
        val p2 = fullCommand[3]

        // Extract the data payload (Skip the first 5 bytes: CLA INS P1 P2 Lc)
        // Note: This logic assumes the APDU generated by APDUUtils is formatted as [Header 5 bytes] + [Data]
        val data = fullCommand.copyOfRange(5, fullCommand.size)

        var offset = 0
        val chunkSize = 240 // Safe chunk size for NFC (max is usually 261)

        var lastResponse: Pair<String, ByteArray> = Pair("ERROR", ByteArray(0))

        while (offset < data.size) {
            val lengthToWrite = minOf(chunkSize, data.size - offset)
            val isLastChunk = (offset + lengthToWrite >= data.size)

            val chunkData = data.copyOfRange(offset, offset + lengthToWrite)

            // 3. Construct the Chained Header
            // If it's NOT the last chunk, set the 5th bit of CLA (0x10) to indicate "More Data Coming"
            val currentCla = if (isLastChunk) cla else (cla.toInt() or 0x10).toByte()
            val currentLc = lengthToWrite.toByte()

            // 4. Build the Packet: CLA INS P1 P2 Lc + DataChunk
            val chunkApdu = ByteArray(5 + lengthToWrite)
            chunkApdu[0] = currentCla
            chunkApdu[1] = ins
            chunkApdu[2] = p1
            chunkApdu[3] = p2
            chunkApdu[4] = currentLc
            System.arraycopy(chunkData, 0, chunkApdu, 5, lengthToWrite)

            // 5. Send this chunk
            lastResponse = sendApdu(isoDep, chunkApdu)

            // If any intermediate chunk fails, abort immediately
            if (lastResponse.first != SWEnum.ISO7816_0x9000.hexValue && lastResponse.first != "9000") {
                Log.e(TAG, "Chained APDU failed at offset $offset with SW: ${lastResponse.first}")
                return lastResponse
            }

            offset += lengthToWrite
        }

        // Return the result of the LAST chunk (which contains the actual Verify result)
        return lastResponse
    }
}