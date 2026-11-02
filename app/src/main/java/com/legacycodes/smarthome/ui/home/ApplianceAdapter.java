package com.legacycodes.smarthome.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.legacycodes.smarthome.R;
import com.legacycodes.smarthome.data.local.entity.ApplianceEntity;
import com.legacycodes.smarthome.domain.model.ApplianceType;

/**
 * RecyclerView adapter for the appliance list.
 *
 * Uses ListAdapter + DiffUtil — this gives us per-item animations
 * for free (add/remove/update) with no manual notifyItemXxx calls.
 *
 * Diff policy: two appliances are "the same" if their ids match,
 * and "equal" if every visible field matches. When the state changes
 * (toggle), only that card re-renders, not the whole list.
 */
public class ApplianceAdapter extends ListAdapter<ApplianceEntity, ApplianceAdapter.VH> {

    public interface OnApplianceClickListener {
        void onCardClick(ApplianceEntity appliance);
        void onToggleClick(ApplianceEntity appliance, boolean newCheckedState);
    }

    private final OnApplianceClickListener listener;

    public ApplianceAdapter(OnApplianceClickListener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ApplianceEntity> DIFF =
            new DiffUtil.ItemCallback<ApplianceEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ApplianceEntity a,
                                               @NonNull ApplianceEntity b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ApplianceEntity a,
                                                  @NonNull ApplianceEntity b) {
                    return a.id == b.id
                            && a.deviceId == b.deviceId
                            && a.gpioPin == b.gpioPin
                            && a.state == b.state
                            && eq(a.name, b.name)
                            && eq(a.type, b.type)
                            && eq(a.room, b.room);
                }

                private boolean eq(String x, String y) {
                    return x == null ? y == null : x.equals(y);
                }
            };

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_appliance_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView cardRoot;
        final FrameLayout iconContainer;
        final ImageView ivIcon;
        final TextView tvName;
        final TextView tvSubtitle;
        final SwitchMaterial switchState;

        VH(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.cardRoot);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            switchState = itemView.findViewById(R.id.switchState);
        }

        void bind(ApplianceEntity item, OnApplianceClickListener listener) {
            tvName.setText(item.name != null ? item.name : "Unnamed");

            ApplianceType type = ApplianceType.fromName(item.type);
            tvSubtitle.setText(type.getDisplayName() + " · GPIO " + item.gpioPin);

            ivIcon.setImageResource(iconFor(type));

            boolean on = item.state == 1;
            ivIcon.setImageTintList(itemView.getContext().getColorStateList(
                    on ? R.color.rose_primary : R.color.text_secondary));
            iconContainer.setBackgroundResource(
                    on ? R.drawable.bg_icon_container_on : R.drawable.bg_icon_container_off);

            // Remove the listener first to avoid NPE / spurious toggles during rebind
            switchState.setOnCheckedChangeListener(null);
            switchState.setChecked(on);
            switchState.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onToggleClick(item, isChecked);
                }
            });

            cardRoot.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCardClick(item);
                }
            });
        }

        /** Map ApplianceType to the vector icon we shipped in 3.2. */
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