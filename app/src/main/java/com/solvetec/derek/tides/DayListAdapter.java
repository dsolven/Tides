package com.solvetec.derek.tides;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.solvetec.derek.tides.data.TidesContract;
import com.solvetec.derek.tides.utils.DateUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by dsolven on 10/21/2017.
 */

public class DayListAdapter extends RecyclerView.Adapter<DayListAdapter.DayViewHolder> {

    private static final String TAG = DayListAdapter.class.getCanonicalName();
    private ListItemClickListener mOnClickListener;
    private int mNumberOfItems;
    private List<HiloDay> mHiloDays;
    private Context mContext;

    /**
     * The interface that receives onClick messages.
     */
    public interface ListItemClickListener {
        void onListItemClick(int clickedItemIndex, Long clickedItemDate);
    }

    /**
     * Constructor for DayListAdapter that accepts number of items to display and the specification
     * for the ListItemClickListener.
     *
     * @param numberOfItems Number of items to display in list
     * @param listener Listener for list item clicks
     */
    public DayListAdapter(int numberOfItems, ListItemClickListener listener, List<HiloDay> hiloDays, Context context) {
        mNumberOfItems = numberOfItems;
        mOnClickListener = listener;
        mHiloDays = hiloDays;
        mContext = context;
    }

    /**
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param parent The ViewGroup that these ViewHolders are contained within.
     * @param viewType  If your RecyclerView has more than one type of item (which ours doesn't) you
     *                  can use this viewType integer to provide a different layout. See
     *                  {@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)}
     *                  for more details.
     * @return A new DayViewHolder that holds the View for each list item
     */
    @Override
    public DayViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.day_list_item, parent, false);
        DayViewHolder viewHolder = new DayViewHolder(view);

        return viewHolder;
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the correct
     * indices in the list for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(DayListAdapter.DayViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: holder = " + holder.toString() + ", position = " + position);
        holder.bind(position); // TODO: 10/21/2017 This should eventually populate the viewholder with the database data.
    }

    @Override
    public int getItemCount() {
        return mNumberOfItems;
    }


    /**
     * Cache of children views for a list item.
     */
    class DayViewHolder extends RecyclerView.ViewHolder
            implements OnClickListener{

        TextView listItemDateTextView;
        TextView listItemSummaryTextView;

        /**
         *
         * @param itemView
         */
        public DayViewHolder(View itemView) {
            super(itemView);

            listItemDateTextView = (TextView) itemView.findViewById(R.id.tv_item_date);
            listItemSummaryTextView = (TextView) itemView.findViewById(R.id.tv_item_tide_summary);
            itemView.setOnClickListener(this);
        }

        // TODO: 10/21/2017 Update the documentation for the "bind" method. It's from the GreenAdapter example right now.
        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to display the appropriate text within a list item.
         * @param listIndex Position of the item in the list
         */
        void bind(int listIndex) {
            // TODO: 10/21/2017 This should eventually be populated by the database data.
            if(listIndex >= mHiloDays.size()) {
                return;
            }
            HiloDay hiloDay = mHiloDays.get(listIndex);

//            listItemDateTextView.setText(DateUtils.getDateString(hiloDay.timestamp, mContext.getString(R.string.format_date_weekday_date_and_time)));

            // Parse data into single line
            String summary = "";
            for(Double val : hiloDay.data) {
                summary += val.toString() + " ";
            }
            listItemSummaryTextView.setText(summary);
        }

        /**
         * Called whenever a user clicks on an item in the list.
         * @param v The View that was clicked.
         */
        @Override
        public void onClick(View v) {
            int clickedPosition = getAdapterPosition();
            HiloDay hiloDay = mHiloDays.get(clickedPosition);
            mOnClickListener.onListItemClick(clickedPosition, hiloDay.timestamp);
        }
    }
}
