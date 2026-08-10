package com.satwant.focusguard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.ViewHolder> {

    private final List<AppInfo> apps;

    public AppListAdapter(List<AppInfo> apps) {
        this.apps = apps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = apps.get(position);
        holder.label.setText(app.label);
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(app.blocked);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> app.blocked = isChecked);
    }

    @Override
    public int getItemCount() {
        return apps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.appLabel);
            checkBox = itemView.findViewById(R.id.appCheckBox);
        }
    }
}
