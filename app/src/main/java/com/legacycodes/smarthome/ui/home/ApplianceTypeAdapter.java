package com.legacycodes.smarthome.ui.addedit;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.legacycodes.smarthome.R;
import com.legacycodes.smarthome.domain.model.ApplianceType;

/**
 * Grid adapter for the appliance type picker on the Add/Edit screen.
 *
 * Unlike the ApplianceAdapter, this doesn't use ListAdapter — the item
 * set is fixed (the 10 ApplianceType enum values), so a simple
 * RecyclerView.Adapter with a selected-index field is enough.
 *
 * Selection model:
 *   - Exactly one type can be selected at a time
 *   - Tapping the selected tile does nothing (no deselect — you must pick one)
 *   - Selected tile: rose icon, rose text, rose-tinted border
 *   - Unselected tile: gray icon, dark text, subtle outline
 */
public class ApplianceTypeAdapter extends RecyclerView.Adapter<ApplianceTypeAdapter.VH> {

    public interface OnTypeSelected {
        void onSelected(ApplianceType type);
    }

    private final OnTypeSelected listener;
    private ApplianceType selected;
    private int selectedPos = -1;

    public ApplianceTypeAdapter(OnTypeSelected listener) {
        this.listener = listener;
    }

    public ApplianceType getSelected() {
        return selected;
    }

    public void setSelected(ApplianceType type) {
        this.selected = type;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_type_option, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ApplianceType type = ApplianceType.values()[position];
        holder.bind(type, type == selected, listener);
    }

    @Override
    public int getItemCount() {
        return ApplianceType.values().length;
    }

    static class VH extends RecyclerView.ViewHolder {
        final View container;
        final ImageView icon;
        final TextView label;

        VH(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.typeContainer);
            icon      = itemView.findViewById(R.id.ivTypeIcon);
            label     = itemView.findViewById(R.id.tvTypeLabel);
        }

        void bind(ApplianceType type, boolean isSelected, OnTypeSelected listener) {
            label.setText(type.getDisplayName());
            icon.setImageResource(iconFor(type));

            int context = R.color.rose_primary;
            int rose = ContextCompat.getColor(itemView.getContext(), R.color.rose_primary);
            int textPrimary = ContextCompat.getColor(itemView.getContext(), R.color.text_primary);
            int textSecondary = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);
            int roseContainer = ContextCompat.getColor(itemView.getContext(), R.color.rose_container);
            int outline = ContextCompat.getColor(itemView.getContext(), R.color.outline);

            if (isSelected) {
                icon.setImageTintList(ColorStateList.valueOf(rose));
                label.setTextColor(rose);
                applyContainerBackground(roseContainer, rose, 2);
            } else {
                icon.setImageTintList(ColorStateList.valueOf(textSecondary));
                label.setTextColor(textPrimary);
                applyContainerBackground(0, outline, 1);
            }

            container.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSelected(type);
                }
            });
        }

        private void applyContainerBackground(int fillColor, int strokeColor, int strokeDp) {
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(16));
            if (fillColor != 0) {
                bg.setColor(fillColor);
            }
            bg.setStroke(dpToPx(strokeDp), strokeColor);
            container.setBackground(bg);
        }

        private int dpToPx(float dp) {
            return Math.round(dp * itemView.getResources().getDisplayMetrics().density);
        }

        private static int iconFor(ApplianceType type) {
            switch (type) {
                case LIGHT:     return R.drawable.ic_lightbulb;
                case FAN:       return R.drawable.ic_fan;
                case AC:        return R.drawable.ic_ac;
                case TV:        return R.drawable.ic_tv;
                case SOCKET:    return R.drawable.ic_socket;
                case HEATER:    return R.drawable.ic_heater;
                case PUMP:      return R.drawable.ic_pump;
                case DOOR_LOCK: return R.drawable.ic_lock;
                case SPEAKER:   return R.drawable.ic_speaker;
                case OTHER:
                default:        return R.drawable.ic_other;
            }
        }
    }
}