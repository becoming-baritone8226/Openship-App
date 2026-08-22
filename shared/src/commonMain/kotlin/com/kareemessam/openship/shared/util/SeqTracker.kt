package com.kareemessam.openship.shared.util

class SeqTracker {
    var lastSeq: Long = 0L
        private set

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

