package lt.demo.stethoscope.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRecycleAdapter<VH extends RecyclerView.ViewHolder, ITEM>
        extends RecyclerView.Adapter<VH> {

    protected final List<ITEM> mList = new ArrayList<>();

    protected final Context mContext;
    protected final LayoutInflater inflater;
    private OnItemClickListener listener;

    public BaseRecycleAdapter(Context context) {
        mContext = context;
        inflater = LayoutInflater.from(mContext);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onItemClick(view, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public ITEM getItem(int position) {
        return mList.get(position);
    }

    public void setItems(List<ITEM> list) {
        mList.clear();
        mList.addAll(list);
        notifyDataSetChanged();
    }

    public void addItem(ITEM item) {
        mList.add(item);
        notifyDataSetChanged();
    }

    public void removeAll() {
        mList.clear();
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    protected String getString(@StringRes int resId) {
        return mContext.getString(resId);
    }

    public interface OnItemClickListener {
        void onItemClick(View itemVIew, int position);
    }
}
