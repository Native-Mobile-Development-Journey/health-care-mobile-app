package com.project.healthcare.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.project.healthcare.R;
import com.project.healthcare.data.models.Appointment;

import java.util.ArrayList;
import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    public interface OnAppointmentInteractionListener {
        void onAppointmentOpen(Appointment appointment);

        void onAppointmentAction(Appointment appointment);
    }

    private final List<Appointment> items = new ArrayList<>();
    private final OnAppointmentInteractionListener listener;

    public AppointmentAdapter(OnAppointmentInteractionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Appointment> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment item = items.get(position);

        holder.doctor.setText(item.doctorName);
        holder.specialty.setText(item.specialty);
        holder.hospital.setText(item.hospital);
        holder.time.setText(item.time);
        holder.date.setText(item.date);

        String normalizedStatus = item.normalizedStatus();
        holder.status.setText(normalizedStatus);
        if (Appointment.STATUS_CANCELED.equals(normalizedStatus)) {
            holder.status.setBackgroundResource(R.drawable.bg_status_cancel);
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.red_500));
            holder.actionButton.setText(R.string.action_new_appointment);
        } else if (Appointment.STATUS_COMPLETED.equals(normalizedStatus)) {
            holder.status.setBackgroundResource(R.drawable.bg_status_complete);
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.green_500));
            holder.actionButton.setText(R.string.action_new_appointment);
        } else {
            holder.status.setBackgroundResource(R.drawable.bg_status_upcoming_blue);
            holder.status.setTextColor(holder.itemView.getContext().getColor(R.color.blue_500));
            holder.actionButton.setText(R.string.action_reschedule);
        }

        holder.root.setOnClickListener(v -> listener.onAppointmentOpen(item));
        holder.actionButton.setOnClickListener(v -> listener.onAppointmentAction(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        final View root;
        final TextView doctor;
        final TextView specialty;
        final TextView hospital;
        final TextView status;
        final TextView time;
        final TextView date;
        final Button actionButton;

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.item_appointment_card);
            doctor = itemView.findViewById(R.id.text_appointment_doctor);
            specialty = itemView.findViewById(R.id.text_appointment_specialty);
            hospital = itemView.findViewById(R.id.text_appointment_hospital);
            status = itemView.findViewById(R.id.text_appointment_status);
            time = itemView.findViewById(R.id.text_appointment_time);
            date = itemView.findViewById(R.id.text_appointment_date);
            actionButton = itemView.findViewById(R.id.button_appointment_action);
        }
    }
}
