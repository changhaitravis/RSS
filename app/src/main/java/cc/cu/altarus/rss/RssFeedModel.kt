package cc.cu.altarus.rss

/**
 * Data model representing an RSS feed item.
 */
data class RssFeedModel(
    var title: String? = null,
    var link: String? = null,
    var description: String? = null,
    var imgSrc: String? = null
)
