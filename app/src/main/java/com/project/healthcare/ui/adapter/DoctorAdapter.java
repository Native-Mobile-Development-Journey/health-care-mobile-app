package com.project.healthcare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.data.models.Doctor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    public interface OnDoctorClickListener {
        void onDoctorClick(Doctor doctor);
    }

    private final List<Doctor> items = new ArrayList<>();
    private final OnDoctorClickListener listener;

    public DoctorAdapter(OnDoctorClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Doctor> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor_recommendation, parent, false);
        return new DoctorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        Doctor item = items.get(position);
        holder.name.setText(item.name);
        holder.specialty.setText(item.specialty);
        holder.rating.setText(item.rating > 0
                ? String.format(Locale.getDefault(), "%.1f ★", item.rating)
                : holder.root.getContext().getString(R.string.value_unavailable));

        holder.root.setOnClickListener(v -> listener.onDoctorClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {

        final View root;
        final TextView name;
        final TextView specialty;
        final TextView rating;

        DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.item_doctor_root);
            name = itemView.findViewById(R.id.text_doctor_name);
            specialty = itemView.findViewById(R.id.text_doctor_specialty);
            rating = itemView.findViewById(R.id.text_doctor_rating);
        }
    }
}
