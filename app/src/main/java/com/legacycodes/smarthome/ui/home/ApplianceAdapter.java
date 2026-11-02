package com.legacycodes.smarthome.ui.home;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
 * Phase 3.9 adds the "Living Light" pulse:
 *   - When a toggle flips on, the icon container scales 1.0 → 1.08 → 1.0
 *     with an overshoot interpolator, over 400ms. Feels like a small heartbeat.
 *   - The icon tint animates from text_secondary → rose_primary, so the color
 *     shifts smoothly instead of snapping.
 *   - The icon container background crossfades between the two oval drawables.
 *
 * The pulse only fires on real user taps (not on initial bind), which is why
 * we track the "previous state" per view holder.
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

        // Track previous state per view holder so we only pulse on real transitions,
        // not on initial bind or on notifyItemChanged without a state change.
        private int lastState = -1;

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
            int rosePrimary = ContextCompat.getColor(itemView.getContext(), R.color.rose_primary);
            int textSecondary = ContextCompat.getColor(itemView.getContext(), R.color.text_secondary);

            // Tint animation: if state actually changed, animate the color transition.
            // If this is initial bind (lastState == -1), snap to correct color instantly.
            if (lastState != -1 && lastState != item.state) {
                animateIconTint(on ? textSecondary : rosePrimary,
                        on ? rosePrimary : textSecondary);
                if (on) {
                    pulseIcon();
                }
            } else {
                ivIcon.setImageTintList(ColorStateList.valueOf(on ? rosePrimary : textSecondary));
            }

            iconContainer.setBackgroundResource(
                    on ? R.drawable.bg_icon_container_on : R.drawable.bg_icon_container_off);

            // Switch: clear listener first, set checked, reattach — avoids
            // programmatic setChecked firing onToggleClick
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

            lastState = item.state;
        }

        /** Living Light pulse: scale 1.0 → 1.08 → 1.0 with overshoot easing. */
        private void pulseIcon() {
            iconContainer.animate().cancel();
            iconContainer.setScaleX(1f);
            iconContainer.setScaleY(1f);
            iconContainer.animate()
                    .scaleX(1.08f)
                    .scaleY(1.08f)
                    .setDuration(160)
                    .setInterpolator(new OvershootInterpolator(2.5f))
                    .withEndAction(() -> iconContainer.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(240)
                            .setInterpolator(new OvershootInterpolator(1.2f))
                            .start())
                    .start();
        }

        /** Animate the ImageView tint between two colors over 300ms. */
        private void animateIconTint(int from, int to) {
            ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
            anim.setDuration(300);
            anim.addUpdateListener(a -> {
                int color = (int) a.getAnimatedValue();
                ivIcon.setImageTintList(ColorStateList.valueOf(color));
            });
            anim.start();
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