package com.example.efleamarket.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter cho ô chọn có gõ tìm kiếm, KHÔNG phân biệt dấu tiếng Việt.
 * Ví dụ: gõ "ba dinh" hoặc "Ba Đình" đều tìm ra "Phường Ba Đình".
 */
public class AccentInsensitiveAdapter extends BaseAdapter implements Filterable {

    private final LayoutInflater inflater;
    private final int itemLayout;
    private final List<String> allItems;
    private final List<String> normalizedItems = new ArrayList<>();
    private List<String> shownItems;

    public AccentInsensitiveAdapter(Context context, int itemLayout, List<String> items) {
        this.inflater = LayoutInflater.from(context);
        this.itemLayout = itemLayout;
        this.allItems = new ArrayList<>(items);
        this.shownItems = this.allItems;
        for (String s : allItems) normalizedItems.add(normalize(s));
    }

    /** Bỏ dấu + chữ thường: "Phường Ba Đình" → "phuong ba dinh". */
    public static String normalize(String s) {
        if (s == null) return "";
        String lower = s.toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim();
    }

    @Override public int getCount() { return shownItems.size(); }
    @Override public String getItem(int position) { return shownItems.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv = (TextView) (convertView != null
                ? convertView
                : inflater.inflate(itemLayout, parent, false));
        tv.setText(getItem(position));
        return tv;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                String q = normalize(constraint == null ? "" : constraint.toString());
                List<String> matched;
                if (q.isEmpty()) {
                    matched = allItems;
                } else {
                    matched = new ArrayList<>();
                    for (int i = 0; i < allItems.size(); i++) {
                        if (normalizedItems.get(i).contains(q)) matched.add(allItems.get(i));
                    }
                }
                results.values = matched;
                results.count = matched.size();
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                shownItems = results.values != null ? (List<String>) results.values : allItems;
                notifyDataSetChanged();
            }
        };
    }
}
