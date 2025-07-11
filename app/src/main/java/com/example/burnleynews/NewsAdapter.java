package com.example.burnleynews;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private final List<NewsArticle> articles;
    // By creating the date formatter once and reusing it, we avoid creating
    // a new object for every single list item, which is more efficient.
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public NewsAdapter(List<NewsArticle> articles) {
        this.articles = articles;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_news, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsArticle article = articles.get(position);
        holder.title.setText(article.getTitle());
        holder.description.setText(article.getDescription());
        holder.source.setText(article.getSource());

        // Format and display the publication date using our single formatter instance.
        if (article.getPubDate() != null) {
            holder.date.setText(DATE_FORMATTER.format(article.getPubDate()));
        } else {
            holder.date.setText(""); // Clear the text if no date is available
        }

        // Set a click listener on the entire item view
        holder.itemView.setOnClickListener(v -> {
            String url = article.getLink();
            // Check if the link is valid before trying to open it
            if (url != null && !url.isEmpty()) {
                Context context = v.getContext();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView description;
        final TextView source;
        final TextView date; // TextView for the date

        NewsViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.newsTitle);
            description = view.findViewById(R.id.newsDescription);
            source = view.findViewById(R.id.newsSource);
            date = view.findViewById(R.id.newsDate); // Initialize the date TextView
        }
    }
}
