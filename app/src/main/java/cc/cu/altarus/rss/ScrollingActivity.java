package cc.cu.altarus.rss;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import 	android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;
import javax.xml.parsers.*;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ScrollingActivity extends AppCompatActivity {
    private static final String TAG = "ScrollingActivity";

    private RecyclerView scrollingRecycler;
    private EditText editFeedUrl;
    private FloatingActionButton refreshFAB;
    private FloatingActionButton addFAB;
    private SwipeRefreshLayout swipeLayout;

    private List<RssFeedModel> feedModelList;

    DocumentBuilderFactory dbf;
    DocumentBuilder docBuild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        editFeedUrl = (EditText) findViewById(R.id.rssFeedUrl);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        scrollingRecycler = (RecyclerView) findViewById(R.id.recyclerView);
        scrollingRecycler.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton refreshFAB = (FloatingActionButton) findViewById(R.id.refresh);
        FloatingActionButton addFAB = (FloatingActionButton) findViewById(R.id.add);

        editFeedUrl.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // If the event is a key-down event on the "enter" button
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    new FetchFeedTask().execute((Void) null);
                    return true;
                }
                return false;
            }
        });
        editFeedUrl.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEND) {
                    new FetchFeedTask().execute((Void) null);
                }
                return false;
            }
        });

        refreshFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new FetchFeedTask().execute((Void) null);
            }
        });

        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh() {
                new FetchFeedTask().execute((Void) null);
            }
        });

        //DocumentBuilder Config
        dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        try {
            docBuild = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class FetchFeedTask extends AsyncTask<Void, Void, Boolean> {
        private String urlAddr;

        @Override
        protected void onPreExecute() {
            swipeLayout.setRefreshing(true);
            urlAddr = editFeedUrl.getText().toString();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if(TextUtils.isEmpty(urlAddr)){
                return false;
            }
            try {
                if(!urlAddr.startsWith("http://") && !urlAddr.startsWith("https://"))
                    urlAddr = "http://" + urlAddr;
                URL url = new URL(urlAddr);
                InputStream inputStream = url.openConnection().getInputStream();
                feedModelList = parseFeed(inputStream);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error", e);
            } catch (XmlPullParserException e) {
                Log.e(TAG, "Error", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            swipeLayout.setRefreshing(false);

            if (success) {
                //mFeedTitleTextView.setText("Feed Title: " + mFeedTitle);
                //mFeedDescriptionTextView.setText("Feed Description: " + mFeedDescription);
                //mFeedLinkTextView.setText("Feed Link: " + mFeedLink);
                // Fill RecyclerView
                scrollingRecycler.setAdapter(new RssFeedListAdapter(feedModelList));
            } else {
                Toast.makeText(ScrollingActivity.this,
                        "Enter a valid Rss feed url",
                        Toast.LENGTH_LONG
                ).show();
            }
        }

        private List<RssFeedModel> parseFeed(InputStream inputStream) throws XmlPullParserException, IOException {
            String title = null;
            String link = null;
            String description = null;
            String imgSrc = null;

            boolean isItem = false;

            List<RssFeedModel> items = new ArrayList<>();

            try {
                XmlPullParser xmlPullParser = Xml.newPullParser();
                XmlPullParser imgSrcXmlPullParser = Xml.newPullParser();
                xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                imgSrcXmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                xmlPullParser.setInput(inputStream, null);

                xmlPullParser.require(XmlPullParser.START_TAG, null, "rss");

                while (xmlPullParser.next() != XmlPullParser.END_TAG) {
                    if (xmlPullParser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String name = xmlPullParser.getName();
                    // Starts by looking for the entry tag
                    if (name.equals("entry")) {
                        items.add(readItem(xmlPullParser, imgSrcXmlPullParser));
                    } else {
                        skip(xmlPullParser);
                    }
                }

                return items;
            } finally {
                inputStream.close();
            }
        }
    }

    private RssFeedModel readItem(XmlPullParser parser, XmlPullParser imgSrcXmlPullParser) throws XmlPullParserException, IOException{
        parser.require(XmlPullParser.START_TAG, null, "item");
        String title = null;
        String description = null;
        String link = null;
        String imgSrc = null;

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("title")) {
                title = readTitle(parser);
            } else if (name.equals("description")) {
                description = readDescription(parser);
            } else if (name.equals("link")) {
                link = readLink(parser);
            } else {
                skip(parser);
            }
        }

        imgSrcXmlPullParser.setInput(new StringReader(StringEscapeUtils.unescapeHtml4(result)));
        while (imgSrcXmlPullParser.next() != XmlPullParser.END_DOCUMENT) {
            if (imgSrcXmlPullParser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String subDescrName = imgSrcXmlPullParser.getName();
            // Starts by looking for the entry tag
            if (subDescrName.equals("img")) {
                imgSrc = imgSrcXmlPullParser.getAttributeValue(null, "src");
            }
        }
        return new RssFeedModel(title, link, description, imgSrc);

    }

    // Processes title tags in the feed.
    private String readTitle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "title");
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "title");
        return title;
    }

    // Processes link tags in the feed.
    private String readLink(XmlPullParser parser) throws IOException, XmlPullParserException {
        String link = "";
        parser.require(XmlPullParser.START_TAG, null, "link");
        String tag = parser.getName();
        String relType = parser.getAttributeValue(null, "rel");
        if (tag.equals("link")) {
            if (relType.equals("alternate")){
                link = parser.getAttributeValue(null, "href");
                parser.nextTag();
            }
        }
        parser.require(XmlPullParser.END_TAG, null, "link");
        return link;
    }

    // Processes description tags in the feed.
    private String readDescription(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "description");
        String description = readText(parser);
        parser.require(XmlPullParser.END_TAG, null, "description");
        return description;
    }

    // For the tags title and description, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}

