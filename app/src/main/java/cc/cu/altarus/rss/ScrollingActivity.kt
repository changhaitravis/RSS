package cc.cu.altarus.rss

import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.util.Xml
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import org.apache.commons.lang3.StringEscapeUtils
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.io.StringReader
import java.net.URL
import java.util.ArrayList

class ScrollingActivity : AppCompatActivity() {

    private lateinit var scrollingRecycler: RecyclerView
    private lateinit var editFeedUrl: EditText
    private lateinit var refreshFAB: FloatingActionButton
    private var addFAB: FloatingActionButton? = null
    private lateinit var swipeLayout: SwipeRefreshLayout

    private var feedModelList: List<RssFeedModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        editFeedUrl = findViewById(R.id.rssFeedUrl) as EditText
        swipeLayout = findViewById(R.id.swipeRefreshLayout) as SwipeRefreshLayout
        scrollingRecycler = findViewById(R.id.recyclerView) as RecyclerView
        scrollingRecycler.layoutManager = LinearLayoutManager(this)

        refreshFAB = findViewById(R.id.refresh) as FloatingActionButton
        addFAB = findViewById(R.id.add) as? FloatingActionButton

        editFeedUrl.setOnKeyListener { _, keyCode, event ->
            if (event != null && event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                FetchFeedTask().execute()
                true
            } else {
                false
            }
        }

        editFeedUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                FetchFeedTask().execute()
                true
            } else {
                false
            }
        }

        refreshFAB.setOnClickListener {
            FetchFeedTask().execute()
        }

        swipeLayout.setOnRefreshListener {
            FetchFeedTask().execute()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_settings) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private inner class FetchFeedTask : AsyncTask<Void, Void, Boolean>() {
        private var urlAddr: String = ""

        override fun onPreExecute() {
            swipeLayout.isRefreshing = true
            urlAddr = editFeedUrl.text.toString().trim()
        }

        override fun doInBackground(vararg params: Void?): Boolean {
            if (TextUtils.isEmpty(urlAddr)) {
                return false
            }
            return try {
                if (!urlAddr.startsWith("http://") && !urlAddr.startsWith("https://")) {
                    urlAddr = "http://$urlAddr"
                }
                val url = URL(urlAddr)
                val inputStream = url.openConnection().getInputStream()
                feedModelList = parseFeed(inputStream)
                true
            } catch (e: IOException) {
                Log.e(TAG, "IO error fetching feed", e)
                false
            } catch (e: XmlPullParserException) {
                Log.e(TAG, "XML parse error fetching feed", e)
                false
            }
        }

        override fun onPostExecute(success: Boolean) {
            swipeLayout.isRefreshing = false
            if (success) {
                scrollingRecycler.adapter = RssFeedListAdapter(feedModelList)
            } else {
                Toast.makeText(
                    this@ScrollingActivity,
                    "Enter a valid Rss feed url",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun parseFeed(inputStream: InputStream): List<RssFeedModel> {
            val items = ArrayList<RssFeedModel>()
            try {
                val xmlPullParser = Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                    setInput(inputStream, null)
                }
                val imgSrcXmlPullParser = Xml.newPullParser().apply {
                    setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                }

                while (xmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                    if (xmlPullParser.eventType != XmlPullParser.START_TAG) {
                        continue
                    }
                    val name = xmlPullParser.name
                    if (name == "item" || name == "entry") {
                        items.add(readItem(xmlPullParser, imgSrcXmlPullParser))
                    }
                }
                return items
            } finally {
                inputStream.close()
            }
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun readItem(
            parser: XmlPullParser,
            imgSrcXmlPullParser: XmlPullParser
        ): RssFeedModel {
            var title: String? = null
            var description: String? = null
            var link: String? = null
            var imgSrc: String? = null

            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }
                when (parser.name) {
                    "title" -> title = readTitle(parser)
                    "description" -> description = readDescription(parser)
                    "link" -> link = readLink(parser)
                    else -> skip(parser)
                }
            }

            if (!description.isNullOrEmpty()) {
                try {
                    val unescaped = StringEscapeUtils.unescapeHtml4(description)
                    imgSrcXmlPullParser.setInput(StringReader(unescaped))
                    while (imgSrcXmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
                        if (imgSrcXmlPullParser.eventType != XmlPullParser.START_TAG) {
                            continue
                        }
                        if (imgSrcXmlPullParser.name == "img") {
                            imgSrc = imgSrcXmlPullParser.getAttributeValue(null, "src")
                            if (!imgSrc.isNullOrEmpty()) {
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing imgSrc from description", e)
                }
            }

            return RssFeedModel(title, link, description, imgSrc)
        }

        @Throws(IOException::class, XmlPullParserException::class)
        private fun readTitle(parser: XmlPullParser): String {
            parser.require(XmlPullParser.START_TAG, null, "title")
            val title = readText(parser)
            parser.require(XmlPullParser.END_TAG, null, "title")
            return title
        }

        @Throws(IOException::class, XmlPullParserException::class)
        private fun readLink(parser: XmlPullParser): String {
            parser.require(XmlPullParser.START_TAG, null, "link")
            val href = parser.getAttributeValue(null, "href")
            val link = if (!href.isNullOrEmpty()) {
                while (parser.next() != XmlPullParser.END_TAG) {
                    // skip children if any
                }
                href
            } else {
                readText(parser)
            }
            parser.require(XmlPullParser.END_TAG, null, "link")
            return link
        }

        @Throws(IOException::class, XmlPullParserException::class)
        private fun readDescription(parser: XmlPullParser): String {
            parser.require(XmlPullParser.START_TAG, null, "description")
            val description = readText(parser)
            parser.require(XmlPullParser.END_TAG, null, "description")
            return description
        }

        @Throws(IOException::class, XmlPullParserException::class)
        private fun readText(parser: XmlPullParser): String {
            var result = ""
            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.text ?: ""
                parser.nextTag()
            }
            return result
        }

        @Throws(XmlPullParserException::class, IOException::class)
        private fun skip(parser: XmlPullParser) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                throw IllegalStateException("Expected START_TAG but was ${parser.eventType}")
            }
            var depth = 1
            while (depth != 0) {
                when (parser.next()) {
                    XmlPullParser.END_TAG -> depth--
                    XmlPullParser.START_TAG -> depth++
                }
            }
        }
    }

    companion object {
        private const val TAG = "ScrollingActivity"
    }
}
