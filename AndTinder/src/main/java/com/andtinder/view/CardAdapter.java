package com.andtinder.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.andtinder.R;
import com.andtinder.model.CardModel;

import java.util.ArrayList;

public class CardAdapter extends BaseAdapter {
	private Context mContext;

	private final Object mLock = new Object();
	private ArrayList<CardModel> mDataList;

	public CardAdapter(Context context) {
		mContext = context;
		mDataList = new ArrayList<>();
	}

	public void add(CardModel item) {
		synchronized (mLock) {
			mDataList.add(item);
		}
		notifyDataSetChanged();
	}

	public void pop(CardModel item) {
		synchronized (mLock) {
			mDataList.remove(item);
		}
		notifyDataSetChanged();
	}


	@Override
	public Object getItem(int position) {
		return getCardModel(position);
	}

	public CardModel getCardModel(int position) {
		synchronized (mLock) {
			return mDataList.get(mDataList.size() - 1 - position);
		}
	}

	@Override
	public int getCount() {
		return mDataList.size();
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).hashCode();
	}


	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		CardModel model = mDataList.get(position);

		if(convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.std_card_inner, parent, false);
		}

		((ImageView) convertView.findViewById(R.id.image)).setImageDrawable(model.getCardImageDrawable());
		((TextView) convertView.findViewById(R.id.description)).setText(model.getDescription());

		return convertView;
	}


}
