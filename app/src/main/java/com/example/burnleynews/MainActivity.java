package com.example.burnleynews;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.Xml;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DiffUtil;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

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

        swipeRefreshLayout.setOnRefreshListener(this::fetchNews);

        fetchNews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    private void fetchNews() {
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

            // Sort the newly fetched articles before calculating the difference
            allParsedArticles.sort((o1, o2) -> {
                if (o1.getPubDate() == null || o2.getPubDate() == null) return 0;
                return o2.getPubDate().compareTo(o1.getPubDate());
            });

            runOnUiThread(() -> {
                // Use DiffUtil to calculate the changes and update the adapter efficiently
                final NewsDiffCallback diffCallback = new NewsDiffCallback(this.articleList, allParsedArticles);
                final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

                this.articleList.clear();
                this.articleList.addAll(allParsedArticles);
                diffResult.dispatchUpdatesTo(adapter);

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
                            String decodedTitle = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();
                            String cleanedTitle = decodedTitle.replace("￼", "").trim();
                            currentArticle.setTitle(cleanedTitle);
                        } else if (tagName.equalsIgnoreCase("link")) {
                            currentArticle.setLink(text);
                        } else if (tagName.equalsIgnoreCase("description")) {
                            String decodedDescription = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString();
                            String cleanedDescription = decodedDescription
                                    .replace("[&#8230;]", "...")
                                    .replace("￼", "")
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

    // Inner class to handle the DiffUtil logic
    private static class NewsDiffCallback extends DiffUtil.Callback {
        private final List<NewsArticle> oldList;
        private final List<NewsArticle> newList;

        NewsDiffCallback(List<NewsArticle> oldList, List<NewsArticle> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            // Use the article link as a unique identifier
            return Objects.equals(oldList.get(oldItemPosition).getLink(), newList.get(newItemPosition).getLink());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            // The equals() method in NewsArticle will check if all fields are the same
            return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
        }
    }
}
