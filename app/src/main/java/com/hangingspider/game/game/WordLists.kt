package com.hangingspider.game.game

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Sorted, uppercase word list with fast membership and prefix checks. */
class WordIndex(words: Collection<String>) {
    val words: List<String> = words.map { it.uppercase() }.distinct().sorted()
    private val set = HashSet(this.words)
    private val byLength: Map<Int, List<String>> = this.words.groupBy { it.length }

    fun isWord(word: String): Boolean = word in set

    fun hasPrefix(prefix: String): Boolean {
        var lo = 0
        var hi = words.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (words[mid] < prefix) lo = mid + 1 else hi = mid
        }
        return lo < words.size && words[lo].startsWith(prefix)
    }

    fun withPrefix(prefix: String): List<String> {
        var lo = 0
        var hi = words.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (words[mid] < prefix) lo = mid + 1 else hi = mid
        }
        val out = ArrayList<String>()
        while (lo < words.size && words[lo].startsWith(prefix)) out.add(words[lo++])
        return out
    }

    fun ofLength(length: Int): List<String> = byLength[length].orEmpty()
}

/**
 * SCOWL-derived word lists bundled in assets/words (see LICENSE.txt there).
 * [common] is everyday vocabulary used for answers and hidden targets;
 * [dictionary] is the larger list used to accept player guesses.
 */
object WordLists {
    lateinit var common: WordIndex
        private set
    lateinit var dictionary: WordIndex
        private set

    private val mutex = Mutex()

    val isLoaded: Boolean get() = ::dictionary.isInitialized

    suspend fun load(context: Context) = mutex.withLock {
        if (isLoaded) return@withLock
        withContext(Dispatchers.IO) {
            fun read(name: String) = context.assets.open("words/$name").bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.toList()
            }
            common = WordIndex(read("common.txt"))
            dictionary = WordIndex(read("dictionary.txt"))
        }
    }
}
