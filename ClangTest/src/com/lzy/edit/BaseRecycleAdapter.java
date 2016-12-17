package com.lzy.edit;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.List;
import android.text.SpannableString;

public abstract class BaseRecycleAdapter<T> extends RecyclerView.Adapter<BaseRecycleAdapter.ViewHolder> {

	protected Context mContext;
	protected int mLayoutId;
	protected List<T> mDataList;
	protected OnItemClickListener onItemClickListener;


	public BaseRecycleAdapter(Context context, List<T> dataList, int layoutId) {
		mContext = context;
		mDataList = dataList;
		mLayoutId = layoutId;
	}

	public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
		this.onItemClickListener = onItemClickListener;
	}

	public OnItemClickListener getOnItemClickListener() {
		return onItemClickListener;
	}

	

	@Override
	public BaseRecycleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
		// TODO: Implement this method
		ViewHolder holder = ViewHolder.getHolder(mContext, parent, mLayoutId);
		return holder;
	}

	@Override
	public void onBindViewHolder(final BaseRecycleAdapter.ViewHolder holder, int position) {
		// TODO: Implement this method
		convertView(holder, mDataList.get(position), position);

		// handle itemView's click and long click event 
		if (onItemClickListener != null) {
			final int pos = holder.getLayoutPosition();
			holder.itemView.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View v) {
						// TODO: Implement this method
						onItemClickListener.onItemClick(holder, v, pos);
					}
				});
		}

		if (onItemClickListener != null) {
			final int pos = holder.getLayoutPosition();
			holder.itemView.setOnLongClickListener(new OnLongClickListener(){

					@Override
					public boolean onLongClick(View v) {
						// TODO: Implement this method
						onItemClickListener.OnItemLongClick(holder, v, pos);
						return true;
					}
				});
		}
	}

	@Override
	public int getItemCount() {
		// TODO: Implement this method
		return mDataList.size();
	}


	public abstract void convertView(ViewHolder holder, T data, int position);

	public static class ViewHolder extends RecyclerView.ViewHolder {

		private SparseArray<View> mSparseArray;
		private View mConvertView;
		private Context mContext;

		public ViewHolder(Context context, ViewGroup parent, View itemView) {
			super(itemView);
			mContext = context;
			mConvertView = itemView;
			mSparseArray = new SparseArray<View>();
		}

		// Get view holder
		public static ViewHolder getHolder(Context context, ViewGroup parent, int layoutId) {
			View itemView = LayoutInflater.from(context).inflate(layoutId, parent, false);
			ViewHolder holder = new ViewHolder(context, parent, itemView);
			return holder;
		}

		// Get view
		public <T extends View> T getView(int viewId) {
			View view = mSparseArray.get(viewId);
			if (view == null) {
				view = mConvertView.findViewById(viewId);
				mSparseArray.put(viewId, view);
			}
			return (T)view;
		}


		// Set text for TextView
		public void setText(int viewId, String text) {
			View view = getView(viewId);
			if (view instanceof TextView) {
				((TextView)view).setText(text);
			}
		}
		
		
		
		// Set text for TextView
		public void setText(int viewId, String text,int color) {
			View view = getView(viewId);
			if (view instanceof TextView) {
				((TextView)view).setTextColor(color);
				((TextView)view).setText(text);
			}
		}
	}

	public interface OnItemClickListener {
		void onItemClick(ViewHolder holder, View v, int position);
		void OnItemLongClick(ViewHolder holder, View v, int position);
	}
}
