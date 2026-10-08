/*
 * Copyright (c) 2024 Auxio Project
 * MetadataSanitizer.kt is part of Auxio.
 */

package org.oxycblt.musikr.tag.interpret

object MetadataSanitizer {

    @Volatile
    var customExclusions: List<String> = emptyList()

    @Volatile
    var customArtistMerges: Map<String, String> = emptyMap()

    fun updateSettings(exclusionsText: String?, artistMergesText: String?) {
        customExclusions = exclusionsText?.split(Regex("""[,\n\r]+"""))
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() } ?: emptyList()

        val merges = mutableMapOf<String, String>()
        artistMergesText?.lines()?.forEach { line ->
            val parts = line.split(Regex("""[=->:]+"""), limit = 2)
            if (parts.size == 2) {
                val from = parts[0].trim().lowercase()
                val to = parts[1].trim()
                if (from.isNotBlank() && to.isNotBlank()) {
                    merges[from] = to
                }
            }
        }
        customArtistMerges = merges
    }

    private data class ArtistRule(val regex: Regex, val canonical: String)

    private val MASTER_ARTIST_RULES = listOf(
        ArtistRule(Regex("""(?:a\s*\.?\s*r\s*\.?\s*rahman|allah\s*rakha\s*rahman|arrahman|rahman\s*ar|^rahman$|^arr$)""", RegexOption.IGNORE_CASE), "A. R. Rahman"),
        ArtistRule(Regex("""(?:anirudh|rockstar\s*anirudh|^ani$)""", RegexOption.IGNORE_CASE), "Anirudh Ravichander"),
        ArtistRule(Regex("""(?:yuvan\s*shankar\s*raja|yuvan\s*shankar|^yuvan$|^ysr$|^u1$)""", RegexOption.IGNORE_CASE), "Yuvan Shankar Raja"),
        ArtistRule(Regex("""(?:harris\s*jayaraj|harris\s*jeyaraj|^harris$|^hj$)""", RegexOption.IGNORE_CASE), "Harris Jayaraj"),
        ArtistRule(Regex("""(?:ilaiyaraaja|ilayaraja|ilayaraaja|isaignani|^raaja$|raaja\s*sir)""", RegexOption.IGNORE_CASE), "Ilaiyaraaja"),
        ArtistRule(Regex("""(?:sid\s*sriram|sidsriram|^sid$)""", RegexOption.IGNORE_CASE), "Sid Sriram"),
        ArtistRule(Regex("""(?:balasubrahmanyam|balasubramaniam|balasubramanyam|^spb$|^s\.?p\.?b\.?$)""", RegexOption.IGNORE_CASE), "S. P. Balasubrahmanyam"),
        ArtistRule(Regex("""(?:k\s*\.?\s*s\s*\.?\s*chith?ra|ks\s*chith?ra|^chithra$|^chitra$)""", RegexOption.IGNORE_CASE), "K. S. Chithra"),
        ArtistRule(Regex("""(?:s\s*\.?\s*janaki|^janaki$)""", RegexOption.IGNORE_CASE), "S. Janaki"),
        ArtistRule(Regex("""(?:g\s*\.?\s*v\s*\.?\s*prakash|gv\s*prakash|^gvp$)""", RegexOption.IGNORE_CASE), "G. V. Prakash Kumar"),
        ArtistRule(Regex("""(?:d\s*\.?\s*imman|^imman$)""", RegexOption.IGNORE_CASE), "D. Imman"),
        ArtistRule(Regex("""(?:santhosh\s*narayanan|santosh\s*narayanan|^sana$)""", RegexOption.IGNORE_CASE), "Santhosh Narayanan"),
        ArtistRule(Regex("""(?:hip\s*hop\s*tamizha|hiphop\s*tamizha|hiphop\s*thamizha|^adhi$)""", RegexOption.IGNORE_CASE), "Hiphop Tamizha"),
        ArtistRule(Regex("""(?:antho*ny\s*da+sa?n|antho*nyda+sa?n)""", RegexOption.IGNORE_CASE), "Anthony Daasan"),
        ArtistRule(Regex("""(?:shreya\s*ghoshal|shreya\s*ghosal|^shreya$)""", RegexOption.IGNORE_CASE), "Shreya Ghoshal"),
        ArtistRule(Regex("""(?:jonita\s*gandhi|^jonita$)""", RegexOption.IGNORE_CASE), "Jonita Gandhi"),
        ArtistRule(Regex("""(?:chinmayi)""", RegexOption.IGNORE_CASE), "Chinmayi Sripaada"),
        ArtistRule(Regex("""(?:pradeep\s*kumar|^pradeep$)""", RegexOption.IGNORE_CASE), "Pradeep Kumar"),
        ArtistRule(Regex("""(?:haricharan)""", RegexOption.IGNORE_CASE), "Haricharan"),
        ArtistRule(Regex("""(?:^karthik$)""", RegexOption.IGNORE_CASE), "Karthik"),
        ArtistRule(Regex("""(?:vijay\s*antony)""", RegexOption.IGNORE_CASE), "Vijay Antony"),
        ArtistRule(Regex("""(?:thenisai\s*thendral\s*deva|^deva$)""", RegexOption.IGNORE_CASE), "Deva"),
        ArtistRule(Regex("""(?:vidyasagar)""", RegexOption.IGNORE_CASE), "Vidyasagar"),
        ArtistRule(Regex("""(?:sean\s*roldan)""", RegexOption.IGNORE_CASE), "Sean Roldan"),
        ArtistRule(Regex("""(?:sam\s*c\.?\s*s\.?|^sam\s*cs$)""", RegexOption.IGNORE_CASE), "Sam C. S."),
        ArtistRule(Regex("""(?:thaman\s*s|s\s*\.?\s*thaman|^thaman$)""", RegexOption.IGNORE_CASE), "Thaman S"),
        ArtistRule(Regex("""(?:m\s*\.?\s*m\s*\.?\s*keeravani|keerava+ni|marakathamani)""", RegexOption.IGNORE_CASE), "M. M. Keeravani"),
        ArtistRule(Regex("""(?:arijit\s*singh|^arijit$)""", RegexOption.IGNORE_CASE), "Arijit Singh"),
        ArtistRule(Regex("""(?:atif\s*aslam)""", RegexOption.IGNORE_CASE), "Atif Aslam"),
        ArtistRule(Regex("""(?:yo\s*yo\s*honey\s*singh|honey\s*singh)""", RegexOption.IGNORE_CASE), "Yo Yo Honey Singh"),
        ArtistRule(Regex("""(?:thalapathy\s*vijay|^vijay$)""", RegexOption.IGNORE_CASE), "Vijay"),
        ArtistRule(Regex("""(?:kamal\s*ha+san|^kamal$)""", RegexOption.IGNORE_CASE), "Kamal Haasan"),
        ArtistRule(Regex("""(?:silambarasan|simbu|^str$)""", RegexOption.IGNORE_CASE), "Silambarasan TR"),
        ArtistRule(Regex("""(?:sivakarthikeyan|^sk$)""", RegexOption.IGNORE_CASE), "Sivakarthikeyan"),
        ArtistRule(Regex("""(?:k\s*\.?\s*j\s*\.?\s*yesudas|^yesudas$)""", RegexOption.IGNORE_CASE), "K. J. Yesudas"),
        ArtistRule(Regex("""(?:shankar\s*mahadevan)""", RegexOption.IGNORE_CASE), "Shankar Mahadevan"),
        ArtistRule(Regex("""(?:hariharan)""", RegexOption.IGNORE_CASE), "Hariharan"),
        ArtistRule(Regex("""(?:unni\s*menon)""", RegexOption.IGNORE_CASE), "Unni Menon"),
        ArtistRule(Regex("""(?:swarnalatha)""", RegexOption.IGNORE_CASE), "Swarnalatha"),
        ArtistRule(Regex("""(?:sujatha)""", RegexOption.IGNORE_CASE), "Sujatha"),
        ArtistRule(Regex("""(?:sadhana\s*sargam)""", RegexOption.IGNORE_CASE), "Sadhana Sargam"),
        ArtistRule(Regex("""(?:anuradha\s*sriram)""", RegexOption.IGNORE_CASE), "Anuradha Sriram"),
        ArtistRule(Regex("""(?:naresh\s*iyer)""", RegexOption.IGNORE_CASE), "Naresh Iyer"),
        ArtistRule(Regex("""(?:benny\s*dayal)""", RegexOption.IGNORE_CASE), "Benny Dayal"),
        ArtistRule(Regex("""(?:andrea\s*jeremiah|^andrea$)""", RegexOption.IGNORE_CASE), "Andrea Jeremiah"),
        ArtistRule(Regex("""(?:^dhee$)""", RegexOption.IGNORE_CASE), "Dhee"),
        ArtistRule(Regex("""(?:kapil\s*kapilan)""", RegexOption.IGNORE_CASE), "Kapil Kapilan"),
        ArtistRule(Regex("""(?:p\s*\.?\s*unnikrishnan|unnikrishnan|^unni\s*krishnan$)""", RegexOption.IGNORE_CASE), "P. Unnikrishnan"),
        ArtistRule(Regex("""(?:the\s*weeknd|^weeknd$)""", RegexOption.IGNORE_CASE), "The Weeknd"),
        ArtistRule(Regex("""(?:taylor\s*swift)""", RegexOption.IGNORE_CASE), "Taylor Swift"),
        ArtistRule(Regex("""(?:ed\s*sheeran)""", RegexOption.IGNORE_CASE), "Ed Sheeran"),
        ArtistRule(Regex("""(?:coldplay)""", RegexOption.IGNORE_CASE), "Coldplay")
    )

    private val WEBSITE_JUNK_REGEX = Regex(
        """[\(\[\{][^\)\]\}]*(?:masstamilan|starmusiq|isaimini|tamiltunes|52wap|mp3tamizha|senlyrics|sensongs|kuttyweb|pendujatt|pagalworld|naasongs|raaga|gaana|jiosaavn|wynk|spotify|apple\s*music)[^\)\]\}]*[\)\]\}]""",
        RegexOption.IGNORE_CASE
    )

    private val TRAILING_JUNK_REGEX = Regex(
        """(?:[\s\-_–—|:\(\[\{/•·]+)?\s*(?:-\s*)?(?:masstamilan|starmusiq|isaimini|tamiltunes|52wap|mp3tamizha|senlyrics|sensongs|kuttyweb|pendujatt|pagalworld|naasongs).*$""",
        RegexOption.IGNORE_CASE
    )

    private val DOMAIN_WORD_REGEX = Regex(
        """(?:masstamilan|starmusiq|isaimini|tamiltunes|52wap|mp3tamizha|senlyrics|sensongs|kuttyweb|pendujatt|pagalworld|naasongs)(\.\w+)?""",
        RegexOption.IGNORE_CASE
    )

    private val QUALITY_AND_VIDEO_TAGS_REGEX = Regex(
        """\s*[\(\[](?:128\s*kbps|192\s*kbps|256\s*kbps|320\s*kbps|flac|lossless|cdrip|webrip|vbr|cbr|Official|Music\s+Video|Video\s+Song|Audio|Lyric\s+Video|Lyrics|HD|4K|8K|Visualizer|Live|Remix|Full\s+Song|Promo|Teaser|Trailer|Lyrical|Official\s+Audio|Official\s+Video)[^\)\]]*[\)\]]""",
        RegexOption.IGNORE_CASE
    )

    private val TRACK_NUMBER_PREFIX_REGEX = Regex("""^\s*\d{1,3}\s*[\.\-_–—:]\s*""")
    private val SPAM_CHARS_REGEX = Regex("""[@#\$%\^\*~_=\<\>\{\}\[\]\\\|`"]""")
    private val BOUNDARY_PUNCT_REGEX = Regex("""^[\s\-_–—|:\(\[\{/•·.,!]+|[\s\-_–—|:\(\[\{/•·.,!]+$""")
    private val MULTI_SPACE_REGEX = Regex("""\s+""")
    private val ARTIST_ROLE_PREFIX_REGEX = Regex("""^(?:singers?|vocals?|music(?:\s+by)?|composed\s+by|sung\s+by|artist|starring)\s*[:\-–—]\s*""", RegexOption.IGNORE_CASE)
    private val INITIALS_FORMAT_REGEX = Regex("""([A-Za-z])\.""")

    private val MULTI_ARTIST_SPLIT_REGEX = Regex(
        """(?:,|\s+&\s+|\s+(?:feat\.?|ft\.?|featuring)\s+|\s*/\s*|;|\s+\+\s+|\s+with\s+|\s+and\s+|\s+[xX]\s+|\s+vs\.?\s+)""",
        RegexOption.IGNORE_CASE
    )

    fun cleanString(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = raw.trim()

        // Apply custom user-defined word exclusions
        for (ex in customExclusions) {
            if (ex.isNotBlank()) {
                s = s.replace(Regex(Regex.escape(ex), RegexOption.IGNORE_CASE), "")
            }
        }

        s = s.replace(WEBSITE_JUNK_REGEX, "")
        s = s.replace(TRAILING_JUNK_REGEX, "")
        s = s.replace(DOMAIN_WORD_REGEX, "")
        s = s.replace(QUALITY_AND_VIDEO_TAGS_REGEX, "")
        s = s.replace(SPAM_CHARS_REGEX, " ")
        s = s.replace(BOUNDARY_PUNCT_REGEX, "").trim()
        s = s.replace(MULTI_SPACE_REGEX, " ")
        return s
    }

    fun cleanTitle(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = cleanString(raw)
        s = s.replace(TRACK_NUMBER_PREFIX_REGEX, "")
        s = s.replace(BOUNDARY_PUNCT_REGEX, "").trim()
        return s.ifBlank { raw.trim() }
    }

    fun cleanAlbum(raw: String?): String {
        val s = cleanString(raw)
        return s.ifBlank { raw?.trim() ?: "Single" }
    }

    fun normalizeArtist(raw: String?): String {
        var clean = cleanString(raw)
        if (clean.isBlank()) return ""

        clean = clean.replace(ARTIST_ROLE_PREFIX_REGEX, "")
        clean = clean.replace(BOUNDARY_PUNCT_REGEX, "").trim()
        if (clean.isBlank()) return ""

        // Check user custom artist merges first
        val lower = clean.lowercase()
        customArtistMerges[lower]?.let { return it }

        for (rule in MASTER_ARTIST_RULES) {
            if (rule.regex.containsMatchIn(clean)) {
                return rule.canonical
            }
        }

        clean = clean.replace(INITIALS_FORMAT_REGEX, "$1. ").replace(MULTI_SPACE_REGEX, " ").trim()

        return clean.split(" ").joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    fun splitAndSanitizeArtists(rawList: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (item in rawList) {
            val clean = cleanString(item)
            if (clean.isBlank()) continue
            val tokens = clean.split(MULTI_ARTIST_SPLIT_REGEX)
            for (t in tokens) {
                val norm = normalizeArtist(t)
                if (norm.isNotBlank() && !norm.equals("Unknown Artist", ignoreCase = true) && !norm.equals("YouTube", ignoreCase = true)) {
                    if (!result.contains(norm)) {
                        result.add(norm)
                    }
                }
            }
        }
        return result
    }
}
