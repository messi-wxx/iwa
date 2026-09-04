package com.cq.iwa.readmeter

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build

class NfcHelper(private val activity: Activity) {

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    private val pendingIntent: PendingIntent = PendingIntent.getActivity(
        activity,
        0,
        Intent(activity, activity.javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_MUTABLE,
    )

    private val filters: Array<IntentFilter> = arrayOf(
        IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            runCatching { addDataType("*/*") }
        },
        IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
        IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
    )

    private val techLists: Array<Array<String>> = arrayOf(
        arrayOf(Ndef::class.java.name),
        arrayOf(NdefFormatable::class.java.name),
    )

    fun enable() {
        adapter?.enableForegroundDispatch(activity, pendingIntent, filters, techLists)
    }

    fun disable() {
        adapter?.disableForegroundDispatch(activity)
    }

    fun readMeterCode(intent: Intent?): String? {
        if (intent == null) return null
        val messages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        } ?: return null
        return messages.filterIsInstance<NdefMessage>().flatMap { it.records.toList() }
            .mapNotNull(::parseText)
            .firstOrNull { it.isNotBlank() }
    }

    private fun parseText(record: NdefRecord): String? {
        if (record.tnf != NdefRecord.TNF_WELL_KNOWN || !record.type.contentEquals(NdefRecord.RTD_TEXT)) {
            return null
        }
        return runCatching {
            val payload = record.payload
            val languageLength = payload[0].toInt() and 0x3F
            String(payload, languageLength + 1, payload.size - languageLength - 1, Charsets.UTF_8)
        }.getOrNull()
    }
}
