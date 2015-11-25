package com.sam.blechat;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

public class ChatAdapter extends BaseAdapter {

    private final List<BLEMessage> chatMessages;
    private Activity context;

    public ChatAdapter(Activity context, List<BLEMessage> chatMessages) {
        this.context = context;
        this.chatMessages = chatMessages;
    }

    @Override
    public int getCount() {
        if (chatMessages != null) {
            return chatMessages.size();
        } else {
            return 0;
        }
    }

    @Override
    public BLEMessage getItem(int position) {
        if (chatMessages != null) {
            return chatMessages.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        BLEMessage chatMessage = getItem(position);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = vi.inflate(R.layout.list_item_chat_msg, null);
            holder = createViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        boolean myMsg = chatMessage.isSelf();
        //to simulate whether it me or other sender
        setAlignment(holder, myMsg);
        holder.txtMessage.setText(chatMessage.getText());
        String dateToShow = "BAD-DATE";
        String date = chatMessage.getDate();
        String[] splitDate = date.split("\\.", 2);
        if (splitDate.length > 1) {
            dateToShow = splitDate[0];
        }
        holder.txtDate.setText(dateToShow);
        holder.txtUserName.setText(chatMessage.getUser() + ":");
        BLEMessageState msgState = chatMessage.getState();
        if (msgState == BLEMessageState.UPLOADING) {
            holder.stateImage.setImageResource(R.drawable.msg_upload);
        } else if (msgState == BLEMessageState.TRANSMITTING) {
            holder.stateImage.setImageResource(R.drawable.msg_transmitting);
        } else if (msgState == BLEMessageState.SUCCESS) {
            holder.stateImage.setImageResource(R.drawable.msg_delivered);
        } else if (msgState == BLEMessageState.FAILURE) {
            holder.stateImage.setImageResource(R.drawable.msg_error);
        }

        return convertView;
    }

    public void add(BLEMessage message) {
        chatMessages.add(message);
    }

    public void add(List<BLEMessage> messages) {
        chatMessages.addAll(messages);
    }

    private void setAlignment(ViewHolder holder, boolean isMe) {
        if (!isMe) {
            holder.contentWithBG.setBackgroundResource(R.drawable.in_message_bg);

            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            holder.contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            holder.content.setLayoutParams(lp);
            layoutParams = (LinearLayout.LayoutParams) holder.txtMessage.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            holder.txtMessage.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.txtDate.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            holder.txtDate.setLayoutParams(layoutParams);
        } else {
            holder.contentWithBG.setBackgroundResource(R.drawable.out_message_bg);

            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            holder.contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            holder.content.setLayoutParams(lp);
            layoutParams = (LinearLayout.LayoutParams) holder.txtMessage.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            holder.txtMessage.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.txtDate.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            holder.txtDate.setLayoutParams(layoutParams);
        }
    }

    private ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.txtMessage = (TextView) v.findViewById(R.id.txtMessage);
        holder.content = (LinearLayout) v.findViewById(R.id.content);
        holder.contentWithBG = (LinearLayout) v.findViewById(R.id.contentWithBackground);
        holder.txtDate = (TextView) v.findViewById(R.id.txtInfo);
        holder.txtUserName = (TextView) v.findViewById(R.id.user_name);
        holder.stateImage = (ImageView) v.findViewById(R.id.image_msg_state);
        return holder;
    }

    private static class ViewHolder {
        public TextView txtMessage;
        public TextView txtDate;
        public TextView txtUserName;
        public LinearLayout content;
        public LinearLayout contentWithBG;
        public ImageView stateImage;
    }
}