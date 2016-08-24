package uk.ac.cam.cl.lm649.bonjourtesting.menu.phonebook;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

import uk.ac.cam.cl.lm649.bonjourtesting.R;
import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.phonenumbers.Contact;
import uk.ac.cam.cl.lm649.bonjourtesting.database.tables.phonenumbers.DbTablePhoneNumbers;

public class CustomAdapter extends ArrayAdapter<Contact> {

    private final Context context;
    private final List<Contact> objects;
    private final LayoutInflater inflater;

    public CustomAdapter(Context context, @NonNull List<Contact> objects) {
        super(context, -1, objects);
        this.context = context;
        this.objects = objects;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView;
        if (convertView == null) {
            rowView = inflater.inflate(R.layout.row_in_list_with_toggle, parent, false);
        } else {
            rowView = convertView;
        }

        final Contact contactEntry = objects.get(position);

        TextView textViewMain = (TextView) rowView.findViewById(R.id.textViewMain);
        textViewMain.setText(contactEntry.toString());

        SeekBar seekBar = (SeekBar) rowView.findViewById(R.id.seekBar);
        seekBar.setMax(Contact.GossipingStatus.values().length - 1);
        seekBar.setProgress(contactEntry.getGossipingStatus().getValue());

        final TextView textViewSeekBarStatus = (TextView) rowView.findViewById(R.id.textViewSeekbarState);
        textViewSeekBarStatus.setText(contactEntry.getGossipingStatus().getText());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    contactEntry.setGossipingStatus(Contact.GossipingStatus.fromIntVal(seekBar.getProgress()));
                    textViewSeekBarStatus.setText(contactEntry.getGossipingStatus().getText());
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                DbTablePhoneNumbers.smartUpdateEntry(contactEntry);
            }
        });

        return rowView;
    }

}
