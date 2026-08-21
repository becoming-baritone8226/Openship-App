package com.kareemessam.openship.shared.util

class SeqTracker {
    private var lastSeq: Long = 0L

    fun update(seq: Long) {
        if (seq > lastSeq) {
            lastSeq = seq
        }
    }

    fun getResumeParam(): String = lastSeq.toString()

    fun reset() {
        lastSeq = 0L
    }
}
