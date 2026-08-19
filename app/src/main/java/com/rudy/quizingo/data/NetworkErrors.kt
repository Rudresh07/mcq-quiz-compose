package com.rudy.quizingo.data

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Maps a network failure to copy a user can actually act on, instead of the raw
 *  exception message (e.g. "Unable to resolve host \"...\": No address associated
 *  with hostname") that [Throwable.message] would otherwise surface verbatim. */
fun Throwable.toUserMessage(): String = when (this) {
    is UnknownHostException -> "No internet connection. Check your network and try again."
    is SocketTimeoutException -> "The request timed out. Please try again."
    is HttpException -> "Something went wrong on our end. Please try again."
    is IOException -> "Couldn't connect. Check your internet connection and try again."
    else -> "Something went wrong. Please try again."
}
