package com.example.burnleynews;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.Xml;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Using a Map to associate URLs with friendly source names.
    private static final Map<String, String> RSS_FEEDS = new LinkedHashMap<>() {{
        put("https://www.lancashiretelegraph.co.uk/sport/football/burnley_fc/rss/", "Lancashire Telegraph");
        put("https://www.uptheclarets.com/feed", "Up The Clarets");
        put("https://www.burnleyexpress.net/sport/football/burnley-fc/rss", "Burnley Express");
        put("https://www.theguardian.com/football/burnley/rss", "The Guardian");
        put("https://www.sportsmole.co.uk/football/burnley/rss.xml", "Sports Mole");
        put("https://thefootballfaithful.com/tag/burnley/feed/", "The Football Faithful");
    }};

    private NewsAdapter adapter;
    private final List<NewsArticle> articleList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;
    // Create a single-thread executor to handle background tasks.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.newsRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new NewsAdapter(articleList);
        recyclerView.setAdapter(adapter);

        // Set the listener for the swipe-to-refresh action using a method reference.
        swipeRefreshLayout.setOnRefreshListener(this::fetchNews);

        // Fetch the news for the first time
        fetchNews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shut down the executor when the activity is destroyed to prevent leaks.
        executor.shutdown();
    }

    private void fetchNews() {
        // Show the refresh indicator
        swipeRefreshLayout.setRefreshing(true);
        executor.execute(() -> {
            List<NewsArticle> allParsedArticles = new ArrayList<>();

            for (Map.Entry<String, String> entry : RSS_FEEDS.entrySet()) {
                String url = entry.getKey();
                String sourceName = entry.getValue();
                try (InputStream stream = new URL(url).openStream()) {
                    allParsedArticles.addAll(parseRss(stream, sourceName));
                } catch (IOException | XmlPullParserException e) {
                    Log.e(TAG, "Error fetching or parsing RSS feed: " + url, e);
                }
            }

            runOnUiThread(() -> {
                articleList.clear();
                articleList.addAll(allParsedArticles);

                // Use List.sort (available from API 24)
                articleList.sort((o1, o2) -> {
                    if (o1.getPubDate() == null || o2.getPubDate() == null) return 0;
                    return o2.getPubDate().compareTo(o1.getPubDate());
                });

                // While notifyDataSetChanged works, for optimal performance in a production app,
                // consider using DiffUtil to calculate and dispatch more specific list updates.
                adapter.notifyDataSetChanged();
                // Hide the refresh indicator now that the data is loaded
                swipeRefreshLayout.setRefreshing(false);
            });
        });
    }

    private List<NewsArticle> parseRss(InputStream stream, String sourceName) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(stream, null);

        List<NewsArticle> articles = new ArrayList<>();
        NewsArticle currentArticle = null;
        String text = "";
        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tagName = parser.getName();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    if (tagName.equalsIgnoreCase("item")) {
                        currentArticle = new NewsArticle();
                        currentArticle.setSource(sourceName);
                    }
                    break;

                case XmlPullParser.TEXT:
                    text = parser.getText();
                    break;

                case XmlPullParser.END_TAG:
                    if (currentArticle != null) {
                        if (tagName.equalsIgnoreCase("title")) {
                            // Use the modern Html.fromHtml and clean the object replacement character.
                            String decodedTitle = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();
                            String cleanedTitle = decodedTitle.replace("￼", "").trim();
                            currentArticle.setTitle(cleanedTitle);
                        } else if (tagName.equalsIgnoreCase("link")) {
                            currentArticle.setLink(text);
                        } else if (tagName.equalsIgnoreCase("description")) {
                            // Use the modern Html.fromHtml and clean unwanted characters.
                            String decodedDescription = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();
                            String cleanedDescription = decodedDescription
                                    .replace("[&#8230;]", "...")
                                    .replace("￼", "") // Remove the object replacement character
                                    .trim();
                            currentArticle.setDescription(cleanedDescription);
                        } else if (tagName.equalsIgnoreCase("pubDate")) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
                                Date pubDate = sdf.parse(text);
                                currentArticle.setPubDate(pubDate);
                            } catch (ParseException e) {
                                Log.w(TAG, "Could not parse date for article: " + text);
                                currentArticle.setPubDate(null);
                            }
                        } else if (tagName.equalsIgnoreCase("item")) {
                            articles.add(currentArticle);
                        }
                    }
                    break;
            }
            eventType = parser.next();
        }
        return articles;
    }
}
