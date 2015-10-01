package moremote.moapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.HashMap;

import moremote.moapp.wrap.UserStatus;

/**
 * Created by lintzuhsiu on 14/11/5.
 */
public class FriendListAdapter extends BaseAdapter {

    private Context context;
    private HashMap<String, UserStatus> friends;
    private LayoutInflater inflater;

    public FriendListAdapter(Context context, HashMap<String, UserStatus> friends) {
        this.context = context;
        this.friends = friends;
        inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return friends.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
//        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

//        ViewHolder holder;
//        if(convertView == null){
//            convertView = inflater.inflate(R.layout.friend_list_item,null);
//            holder = new ViewHolder();
//            holder.friendNameTV = (TextView) convertView.findViewById(R.id.friend_name);
//            holder.statusTV = (TextView) convertView.findViewById(R.id.status);
//            holder.typeTV = (TextView) convertView.findViewById(R.id.type);
//            convertView.setTag(holder);
//        }else{
//            holder = (ViewHolder)convertView.getTag();
//        }
//
//        Iterator it = friends.entrySet().iterator();
//        for (int i = 0; i <= position && it.hasNext(); i++) {
//            Map.Entry<String, UserStatus> pairs = (Map.Entry) it.next();
//            if (i == position) {
//                UserStatus item = pairs.getValue();
//                holder.friendNameTV.setText(pairs.getKey());
//                holder.statusTV.setText(item.getStatus());
//                holder.typeTV.setText(item.getType());
//            }
//        }

        return convertView;
    }

}

class  ViewHolder{

    TextView friendNameTV;
    TextView statusTV;
    TextView typeTV;

}
