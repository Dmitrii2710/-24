package com.system.monitor;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;

public class InfoFragment extends Fragment {
    private String[][] data;
    private String title;

    public InfoFragment(String title, String[][] data) {
        this.title = title;
        this.data = data;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_info, container, false);
        LinearLayout layout = root.findViewById(R.id.infoContainer);

        // Section card
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        card.setLayoutParams(cardParams);

        for (int i = 0; i < data.length; i++) {
            View row = inflater.inflate(R.layout.row_item, card, false);
            ((TextView) row.findViewById(R.id.labelView)).setText(data[i][0]);
            ((TextView) row.findViewById(R.id.valueView)).setText(data[i][1]);

            // Alternating row background
            if (i % 2 == 0) {
                row.setBackgroundColor(Color.parseColor("#0AFFFFFF"));
            }

            // Divider (not after last item)
            card.addView(row);

            if (i < data.length - 1) {
                View divider = new View(getContext());
                LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                dp.setMarginStart(16);
                dp.setMarginEnd(16);
                divider.setLayoutParams(dp);
                divider.setBackgroundColor(Color.parseColor("#1AFFFFFF"));
                card.addView(divider);
            }
        }

        layout.addView(card);
        return root;
    }
}