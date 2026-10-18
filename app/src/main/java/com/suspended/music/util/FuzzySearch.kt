package com.suspended.music.util

import com.suspended.music.data.model.Song

/**
 * True fuzzy matching on top of the prefix-only FTS index.
 *
 * The FTS4 prefix wildcard (`beat*`) only surfaces results that start with the
 * typed text, so a typo like "bettle" for "Beatles" never matches. This util
 * performs a tokenized, edit-distance-based pass over the library so typos and
 * small misspellings still find the right tracks. It is meant to *augment* FTS
 * results, not replace them, and to rank suggestions by closeness.
 */
object FuzzySearch {

    private const val MAX_LIBRARY_SIZE = 60_000

    /**
     * Returns the most relevant [limit] songs for [query], ranked by a closeness
     * score. Empty query yields an empty list. Runs on the caller's thread.
     */
    fun search(query: String, songs: List<Song>, limit: Int = 25): List<Song> =
        searchScored(query, songs, limit).map { it.first }

    /**
     * Same as [search] but returns each match with its closeness score, so callers
     * can merge fuzzy hits with other result sources by rank instead of appending.
     */
    fun searchScored(query: String, songs: List<Song>, limit: Int = 25): List<Pair<Song, Float>> {
        val trimmed = query.trim()
        if (trimmed.isBlank() || songs.isEmpty()) return emptyList()
        if (songs.size > MAX_LIBRARY_SIZE) return emptyList()

        val queryTokens = normalize(trimmed).split(' ').filter { it.isNotEmpty() }
        if (queryTokens.isEmpty()) return emptyList()

        return songs.asSequence()
            .mapNotNull { song ->
                val score = scoreSong(queryTokens, song)
                if (score > 0f) song to score else null
            }
            .sortedByDescending { it.second }
            .take(limit)
            .toList()
    }

    private fun scoreSong(queryTokens: List<String>, song: Song): Float {
        val title = normalize(song.title)
        val artist = normalize(song.artist)
        val album = normalize(song.album)
        val composer = normalize(song.composer)

        var total = 0f
        var matched = 0
        for (token in queryTokens) {
            val best = maxOf(
                bestTokenScore(token, title),
                bestTokenScore(token, artist),
                bestTokenScore(token, album),
                bestTokenScore(token, composer)
            )
            if (best <= 0f) return 0f // every query token must find something
            total += best
            matched++
        }

        if (matched == 0) return 0f

        // Boost when the whole (normalized) query appears contiguously in the
        // title — the most likely intent. Cheap and very effective at ranking.
        val fullQuery = queryTokens.joinToString(" ")
        var boost = 0f
        if (title.contains(fullQuery)) boost += 0.6f
        else if (artist.contains(fullQuery) || album.contains(fullQuery)) boost += 0.3f

        return (total / matched) + boost
    }

    /**
     * Best fuzzy score for [token] against the token list of [candidateText].
     * Exact/prefix/substring matches score highest; edit-distance matches are
     * accepted only when the query token is reasonably long (short tokens are
     * too easy to over-match on distance alone).
     */
    private fun bestTokenScore(token: String, candidateText: String): Float {
        if (candidateText.isEmpty()) return 0f
        if (candidateText.contains(token)) {
            return if (candidateText.startsWith(token)) 1.0f else 0.85f
        }

        val candidateTokens = candidateText.split(' ')
        var best = 0f
        for (candidate in candidateTokens) {
            if (candidate.length < 2 || token.length < 2) continue
            val score = similarity(token, candidate)
            if (score > best) best = score
        }

        // Require a genuine similarity floor and that the token is long enough
        // for distance to be meaningful (avoids "ab" -> "cd" false positives).
        return if (best >= 0.66f && token.length >= 3) best else 0f
    }

    /** 1 - normalized Levenshtein distance, in [0f, 1f]. 1 = identical. */
    internal fun similarity(a: String, b: String): Float {
        if (a == b) return 1f
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1f
        return 1f - (levenshtein(a, b).toFloat() / maxLen.toFloat())
    }

    internal fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,      // insertion
                    prev[j] + 1,          // deletion
                    prev[j - 1] + cost    // substitution
                )
            }
            // Roll the current row into `prev` for the next iteration.
            curr.copyInto(prev)
        }
        return prev[b.length]
    }

    /**
     * Lowercase + strip punctuation so "St." and "st" and "Liam's" compare cleanly.
     * Separators are replaced with spaces (not deleted) so "AC/DC" stays two
     * tokens, and `\p{L}`/`\p{N}` keep non-Latin scripts searchable.
     */
    private fun normalize(text: String): String =
        text.lowercase().replace(NON_ALPHANUMERIC, " ")

    private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}\\s]")
}
