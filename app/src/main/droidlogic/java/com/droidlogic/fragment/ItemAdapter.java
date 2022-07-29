package com.droidlogic.fragment;

import java.util.LinkedList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.LinearLayout;

import com.droidlogic.dtvkit.inputsource.R;

public class ItemAdapter extends BaseAdapter {

    private LinkedList<ItemDetail> mData;
    private Context mContext;

    public ItemAdapter(LinkedList<ItemDetail> mData, Context mContext) {
        this.mData = mData;
        this.mContext = mContext;
    }

    public void reFill(LinkedList<ItemDetail> data) {
        mData.clear();
        mData.addAll(data);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            if (mData.get(position).isSatelliteList()) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_satellite,parent,false);
            } else {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.list_item_parameter,parent,false);
            }
            holder = new ViewHolder();
            holder.container = (LinearLayout) convertView.findViewById(R.id.textview_title);
            holder.select = (TextView) convertView.findViewById(R.id.item_select);
            holder.textview2 = (TextView) convertView.findViewById(R.id.textview_second);
            holder.leftArray = (View) convertView.findViewById(R.id.left_array_text);
            holder.textview3 = (TextView) convertView.findViewById(R.id.textview_third);
            holder.rightArray = (View) convertView.findViewById(R.id.right_array_text);
            convertView.setTag(holder);
        } else{
            holder = (ViewHolder) convertView.getTag();
        }

        int editStatus = mData.get(position).getEditStatus();
        boolean enable = mData.get(position).isEnable();
        if (editStatus == ItemDetail.NONE_EDIT) {
            holder.select.setVisibility(View.INVISIBLE);
            holder.leftArray.setVisibility(View.GONE);
            holder.rightArray.setVisibility(View.GONE);
        } else if (editStatus == ItemDetail.SWITCH_EDIT) {
            holder.select.setVisibility(View.INVISIBLE);
            //holder.leftArray.setVisibility(View.VISIBLE);
            //holder.rightArray.setVisibility(View.VISIBLE);
            //hide array when can't be edited
            if (enable) {
                holder.textview2.setEnabled(true);
                holder.leftArray.setEnabled(true);
                holder.rightArray.setEnabled(true);
                holder.leftArray.setVisibility(View.VISIBLE);
                holder.rightArray.setVisibility(View.VISIBLE);
            } else {
                holder.textview2.setEnabled(false);
                holder.leftArray.setEnabled(false);
                holder.rightArray.setEnabled(false);
                holder.leftArray.setVisibility(View.INVISIBLE);
                holder.rightArray.setVisibility(View.INVISIBLE);
            }
        } else {
            holder.select.setVisibility(View.VISIBLE);
            holder.leftArray.setVisibility(View.GONE);
            holder.rightArray.setVisibility(View.GONE);
            if (editStatus == ItemDetail.SELECT_EDIT) {
                holder.select.setBackgroundResource(R.drawable.item_choose);
            } else {
                holder.select.setVisibility(View.INVISIBLE);
            }
        }
        holder.textview2.setText(mData.get(position).getFirstText());
        if (mData.get(position).getSecondText() == null) {
            holder.textview3.setVisibility(View.GONE);
        } else {
            holder.textview3.setVisibility(View.VISIBLE);
            holder.textview3.setText(mData.get(position).getSecondText());
        }
        if (mData.get(position).isSatelliteList()) {
            holder.leftArray.setVisibility(View.GONE);
            holder.rightArray.setVisibility(View.GONE);
            holder.textview3.setVisibility(View.GONE);
        }
        return convertView;
    }

    public static class ItemDetail {
        private int mEditStatus = 0;
        private String mFirstText;
        private String mSecondText;
        private boolean mIsSatellite = true;
        private boolean mEnable = true;

        public static final int NONE_EDIT = 0;
        public static final int SELECT_EDIT = 1;
        public static final int NOT_SELECT_EDIT = 2;
        public static final int SWITCH_EDIT = 3;

        public ItemDetail() {
        }

        public ItemDetail(int editStatus, String first, String second, boolean isSatellite) {
            this.mEditStatus = editStatus;
            this.mFirstText = first;
            this.mSecondText = second;
            this.mIsSatellite = isSatellite;
        }

        public int getEditStatus() {
            return mEditStatus;
        }

        public String getFirstText() {
            return mFirstText;
        }

        public String getSecondText() {
            return mSecondText;
        }

        public boolean isEnable() {
            return mEnable;
        }

        public void setEditStatus(int editStatus) {
            this.mEditStatus = editStatus;
        }

        public void setFirstText(String first) {
            this.mFirstText = first;
        }

        public void setSecondText(String second) {
            this.mSecondText = second;
        }

        public void setEnable(boolean enable) {
            this.mEnable = enable;
        }

        public boolean isSatelliteList() {return this.mIsSatellite;}
    }

    public class ViewHolder{
        LinearLayout container;
        TextView select;
        TextView textview2;
        View leftArray;
        TextView textview3;
        View rightArray;
    }
}
